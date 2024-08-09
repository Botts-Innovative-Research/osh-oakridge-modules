package org.sensorhub.impl.sensor.tstar;

import org.sensorhub.impl.sensor.tstar.TSTARMessageHandler;

import java.net.http.WebSocket;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;

//package org.sensorhub.impl.sensor.tstar;
//
//
//import jakarta.websocket.*;
//import org.eclipse.jetty.websocket.api.Session;
//import org.eclipse.jetty.websocket.api.WebSocketListener;
//
//import java.io.IOException;
//import java.net.URI;
//
//public class WebSocketClient implements WebSocketListener {
//
//
//    public WebSocketClient(TSTARMessageHandler messageHandler) {
//        this.messageHander = messageHandler;
//    }
//
//    Session session = null;
//    TSTARMessageHandler messageHandler;
//            try {
//        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
//        container.connectToServer(this, endpointURI);
//        sendMessage("{\"authToken\": \"" + authToken + "\",\"campaignId\": \"" + campaignId + "\"}");
//    } catch (Exception e) {
//        throw new RuntimeException(e);
//    }
//}
//
//@OnOpen
//public void onOpen(Session session){
//    this.session = session;
//    try {
//        session.getBasicRemote().sendText("Opening connection");
//    } catch (IOException ex){
//        System.out.println(ex);
//    }
//}
//
//public void addMessageHandler(MessageHandler msgHandler) {
//    this.handler = msgHandler;
//}
//
//@OnMessage
//public void processMessage(String message) {
//    System.out.println("Received message in client: " + message);
//}
//
//public void sendMessage(String message) {
//    try {
//        this.session.getBasicRemote().sendText(message);
//    } catch (IOException ex) {
////                Logger.getLogger(BasicClient.class.getName()).log(Level.SEVERE, null, ex);
//    }
//}
//
//
//public static interface MessageHandler {
//
//    public void handleMessage(String message);
//}
//}
//
public class WebSocketClient implements WebSocket.Listener {
        public final CountDownLatch latch;
        TSTARMessageHandler messageHandler;
        public WebSocketClient(TSTARMessageHandler messageHandler, CountDownLatch latch) {
            this.messageHandler = messageHandler;
            this.latch = latch;
        }
//        public WebSocketClient(CountDownLatch latch) { this.latch = latch; }
        @Override
        public void onOpen(WebSocket webSocket) {
            System.out.println("Websocket connected... ");
            WebSocket.Listener.super.onOpen(webSocket);
        }
        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            System.out.println("Received: " + data);
//            messageHandler.handleMsg(data);
            latch.countDown();
            return WebSocket.Listener.super.onText(webSocket, data, last);
        }
        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            System.out.println("Websocket input has been closed... " + webSocket.toString());
            WebSocket.Listener.super.onError(webSocket, error);
        }

    }
