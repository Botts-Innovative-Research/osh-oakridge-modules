package org.sensorhub.impl.sensor.tstar;


import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class TSTARWebSocketClient implements WebSocketListener {

        static final Logger log = LoggerFactory.getLogger(TSTARWebSocketClient.class);
        public TSTARWebSocketClient(TSTARMessageHandler messageHandler) {

            this.messageHandler = messageHandler;
        }
        Session session = null;
        TSTARMessageHandler messageHandler;


    public void sendMessage(String message) {
        try {
            this.session.getRemote().sendString((message));
        } catch (IOException ex) {
//            Logger.getLogger(BasicClient.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    @Override
    public void onWebSocketConnect(Session session) {
        log.info("Websocket Connected: " + session.toString());
        this.session = session;
        messageHandler.sendMsg(messageHandler.openConnection());
        log.info("Authorization Token & Campaign ID sent");
    }

    @Override
    public void onWebSocketText(String message) {
        log.info("Message Received:");
        log.info(message);
        try {
            messageHandler.handleMsg(message);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onWebSocketBinary(byte[] bytes, int i, int i1) {}
    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        log.info("Websocket Closed: " + reason);
    }
    @Override
    public void onWebSocketError(Throwable cause) {
        log.error("Websocket Error: " + cause.toString());
    }

}
