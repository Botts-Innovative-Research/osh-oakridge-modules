package com.botts.impl.sensor.rs350;

import com.botts.impl.sensor.rs350.messages.RS350Message;
//import com.botts.impl.service.oscar.Constants;
import org.sensorhub.impl.utils.rad.webid.WebIdAnalyzer;
import org.sensorhub.impl.utils.rad.webid.WebIdRequestContext;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import com.botts.impl.utils.n42.RadInstrumentDataType;
import org.sensorhub.impl.utils.rad.RADHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;

public class MessageHandler {

    private static final Logger log = LoggerFactory.getLogger(MessageHandler.class);
    final LinkedList<String> messageQueue = new LinkedList<>();
    private final AbstractSensorModule<?> sensorModule;
    WebIdAnalyzer webIdAnalyzer;
    boolean hasAlarm = false;
    String laneUID;

    RADHelper radHelper = new RADHelper();

    private final InputStream msgIn;

    private final String messageDelimiter;

    private long timeSinceLastMessage;


    public interface StatusListener {
        void onNewMessage(RS350Message message);
    }

    public interface BackgroundListener {
        void onNewMessage(RS350Message message);
    }

    public interface ForegroundListener {
        void onNewMessage(RS350Message message);
    }

    public interface AlarmListener {
        void onNewMessage(RS350Message message);
    }

    private final ArrayList<StatusListener> statusListeners = new ArrayList<>();
    private final ArrayList<BackgroundListener> backgroundListeners = new ArrayList<>();
    private final ArrayList<ForegroundListener> foregroundListeners = new ArrayList<>();
    private final ArrayList<AlarmListener> alarmListeners = new ArrayList<>();
    //private final ArrayList<N42Output> n42Listeners = new ArrayList<>();

    private final AtomicBoolean isProcessing = new AtomicBoolean(true);

    private final Thread messageReader = new Thread(new Runnable() {
        @Override
        public void run() {

            boolean continueProcessing = true;

            try {

                ArrayList<Character> buffer = new ArrayList<>();
                timeSinceLastMessage = 0;

                while (continueProcessing) {

                    int character = msgIn.read();

                    // Detected STX
                    if (character == 0x02) {
                        character = msgIn.read();
                        // Detect ETX
                        while (character != 0x03 && character != -1) {
                            buffer.add((char)character);
                            character = msgIn.read();
                            if (character == -1){
                                System.out.println("did not read complete message");
                            }
                        }
                        StringBuilder sb = new StringBuilder(buffer.size());

                        for (char c : buffer) {

                            sb.append(c);
                        }

                        String n42Message = sb.toString().replaceAll("\\<\\?xml(.+?)\\?\\>", "").trim();

                        synchronized (messageQueue) {

                            messageQueue.add(n42Message);

                            messageQueue.notifyAll();
                        }
                        buffer.clear();
                    }

                    synchronized (isProcessing) {

                        continueProcessing = isProcessing.get();
                    }
                }
            } catch (IOException exception) {

            }
        }
    });

    private final Thread messageNotifier = new Thread(() -> {

        boolean continueProcessing = true;

        while (continueProcessing) {

            final String currentMessage;

            synchronized (messageQueue) {

                try {

                    while (messageQueue.isEmpty()) {

                        messageQueue.wait();

                    }

                    currentMessage = messageQueue.removeFirst();

                    //n42Listeners.forEach(listener -> listener.onNewMessage(currentMessage));

                } catch (InterruptedException e) {

                    throw new RuntimeException(e);
                }
            }

            if (currentMessage != null && !currentMessage.isEmpty()) {

                try {
                    RadInstrumentDataType radInstrumentDataType = radHelper.getRadInstrumentData(currentMessage);
                    RS350Message message = new RS350Message(radInstrumentDataType);

                    // Add webid handler call here

                    if (message.getRs350InstrumentCharacteristics() != null && message.getRs350Item() != null && message.getRs350LinEnergyCalibration() != null && message.getRs350CmpEnergyCalibration() != null) {
                        statusListeners.forEach(listener -> listener.onNewMessage(message));
                    }

                    if (message.getRs350BackgroundMeasurement() != null) {
                        backgroundListeners.forEach(listener -> listener.onNewMessage(message));
                    }

                    if (message.getRs350ForegroundMeasurement() != null) {
                        foregroundListeners.forEach(listener -> listener.onNewMessage(message));
                    }

                    if (message.getRs350RadAlarm() != null && message.getRs350DerivedData() != null) {
                        if (!hasAlarm) {
                            hasAlarm = true;
                            publishWebIdRequest(currentMessage);
                        }
                        alarmListeners.forEach(listener -> listener.onNewMessage(message));
                    } else {
                        hasAlarm = false;
                    }
                }
                catch (Exception e){
                    log.error("Error reading message: " + e, e);
                }
            }

            synchronized (isProcessing) {

                continueProcessing = isProcessing.get();
            }
        }
    });

    public MessageHandler(WebIdAnalyzer webIdAnalyzer, InputStream msgIn, String messageDelimiter, AbstractSensorModule<?> parentSensor) {
        this.sensorModule = parentSensor;
        this.webIdAnalyzer = webIdAnalyzer;
        this.msgIn = msgIn;
        this.messageDelimiter = messageDelimiter;
        laneUID = parentSensor.getParentSystemUID();

        this.messageReader.start();
        this.messageNotifier.start();
    }

    public void addStatusListener(StatusListener listener) {
        statusListeners.add(listener);
    }

    public void addBackgroundListener(BackgroundListener listener) {
        backgroundListeners.add(listener);
    }

    public void addForegroundListener(ForegroundListener listener) {
        foregroundListeners.add(listener);
    }

    public void addAlarmListener(AlarmListener listener) {
        alarmListeners.add(listener);
    }

    public void stopProcessing() {

        synchronized (isProcessing) {

            isProcessing.set(false);
        }
    }

    public long getTimeSinceLastMessage() {
        return timeSinceLastMessage;
    }

    private void publishWebIdRequest(String n42) {
        if (webIdAnalyzer == null) {
            return;
        }

        if (laneUID == null) {
            log.warn("No lane UID found for sensor module {}, using sensor UID instead", sensorModule.getName());
            laneUID = sensorModule.getUniqueIdentifier();
        }

        var requestBuilder = WebIdRequestContext.builder();
        String fileName = "rs350_" + laneUID + "_" + System.currentTimeMillis() + ".n42";

        requestBuilder.foregroundFile(fileName, n42.getBytes(StandardCharsets.UTF_8))
                .webIdEnabled(true)
                //.bucketName(Constants.REPORT_BUCKET) TODO Move constants to a module w/o dependencies?
                .bucketName("reports")
                .objectKey(fileName)
                .laneUid(laneUID)
                .synthesizeBackground(true)
                .occupancyObsId(null) // TODO
                .multipartRequest(false);

        this.webIdAnalyzer.processWebIdRequest(requestBuilder.build());
    }

}
