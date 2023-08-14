package com.botts.impl.sensor.rs350;

import com.botts.impl.sensor.rs350.messages.RS350Message;
import com.botts.impl.utils.n42.RadInstrumentDataType;
import org.sensorhub.impl.utils.rad.RADHelper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
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

            int character;

            StringBuilder xmlDataBuffer = new StringBuilder();

            try {

                while (continueProcessing && ((character = msgIn.read()) != -1)) {

                    xmlDataBuffer.append((char) character);

                    String dataBufferString = xmlDataBuffer.toString();

                    if (dataBufferString.endsWith((messageDelimiter))) {

                        String[] n42Messages = dataBufferString.split(messageDelimiter);

                        for (String n42Message : n42Messages) {

                            n42Message = n42Message.replaceAll("\\<\\?xml(.+?)\\?\\>", "").trim();

                            synchronized (messageQueue) {

                                messageQueue.add(n42Message + messageDelimiter);

                                messageQueue.notifyAll();
                            }
                        }
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

                RadInstrumentDataType radInstrumentDataType = radHelper.getRadInstrumentData(currentMessage);

                listeners.forEach(messageListener -> messageListener.onNewMessage(new RS350Message(radInstrumentDataType)));
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
