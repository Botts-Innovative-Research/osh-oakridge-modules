package com.botts.impl.sensor.rs350;

import com.botts.impl.sensor.rs350.messages.RS350Message;
import com.botts.impl.utils.n42.RadInstrumentDataType;
import org.sensorhub.impl.utils.rad.RADHelper;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;

public class MessageHandler {

    final LinkedList<String> messageQueue = new LinkedList<>();

    RADHelper radHelper = new RADHelper();

    private final InputStream msgIn;

    private final String messageDelimiter;

    public interface MessageListener {

        void onNewMessage(RS350Message message);
    }

    private final ArrayList<MessageListener> listeners = new ArrayList<>();

    private final AtomicBoolean isProcessing = new AtomicBoolean(true);

    private final Thread messageReader = new Thread(new Runnable() {
        @Override
        public void run() {

            boolean continueProcessing = true;

            try {

                ArrayList<Character> buffer = new ArrayList<>();

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

            String currentMessage = null;

            synchronized (messageQueue) {

                try {

                    while (messageQueue.isEmpty()) {

                        messageQueue.wait();

                    }

                    currentMessage = messageQueue.removeFirst();

                } catch (InterruptedException e) {

                    throw new RuntimeException(e);
                }
            }

            if (currentMessage != null && !currentMessage.isEmpty()) {



                try {
                    RadInstrumentDataType radInstrumentDataType = radHelper.getRadInstrumentData(currentMessage);
                    listeners.forEach(messageListener -> messageListener.onNewMessage(new RS350Message(radInstrumentDataType)));
                }
                catch (Exception e){
                    System.out.println("Current Message: ");
                    System.out.println(currentMessage);
                }
            }

            synchronized (isProcessing) {

                continueProcessing = isProcessing.get();
            }
        }
    });

    public MessageHandler(InputStream msgIn, String messageDelimiter) {
        this.msgIn = msgIn;
        this.messageDelimiter = messageDelimiter;

        this.messageReader.start();
        this.messageNotifier.start();
    }

    public void addMessageListener(MessageListener listener) {

        listeners.add(listener);
    }

    public void stopProcessing() {

        synchronized (isProcessing) {

            isProcessing.set(false);
        }
    }
}
