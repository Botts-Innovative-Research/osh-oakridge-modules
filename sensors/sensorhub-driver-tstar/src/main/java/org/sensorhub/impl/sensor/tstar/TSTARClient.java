//package org.sensorhub.impl.sensor.tstar;
//
//import java.net.URI;
//import java.net.http.HttpClient;
//import java.net.http.WebSocket;
//
//import java.util.concurrent.CountDownLatch;
//
//public class TSTARClient {
//    public WebSocket ws;
//    String authToken;
//    String campaignId;
//    String openWsMessage;
//
//    public TSTARClient(String authtoken, String campaignId) throws Exception {
//        this.authToken = authtoken;
//        this.campaignId = campaignId;
//        this.openWsMessage = "{\"authToken\": \"" + authToken + "\", \"campaignId\": \"" + campaignId +"\"}";
//    }
//
//    TSTARMessageHandler messageHandler;
//
////        String message = "{\"authToken\": \"" + authToken + "\", \"campaignId\": \"" + campaignId + "\"}";
//     public void start() throws InterruptedException {
//            CountDownLatch latch = new CountDownLatch(1);
//
//            ws = HttpClient
//                    .newHttpClient()
//                    .newWebSocketBuilder()
//                    .buildAsync(URI.create("ws://127.0.0.1:10024/monitor"), new WebSocketClient(messageHandler, latch))
//                    .join();
//
////            String message =
////                    "{\"authToken\": " +
////                            "\"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySWQiOjQsInJvbGUiOiJzZXJ2ZXJfYWRtaW4iLCJpYXQiOjE3MjMwMzc3NzQsImV4cCI6MTcyMzA4MDk3NH0.uHlCMD93oklkcQt6qozLdZvRjNUrjdmMVr7Q5Tw9Leo\",\"campaignId\": 3}";
//            ws.sendText(openWsMessage, true);
//            latch.await();
//        }
//
//    }
//    public class WebSocketClient implements WebSocket.Listener {
//        public final CountDownLatch latch;
//        TSTARMessageHandler messageHandler;
//        public WebSocketClient(TSTARMessageHandler messageHandler, CountDownLatch latch) {
//            this.messageHandler = messageHandler;
//            this.latch = latch;
//        }
////        public WebSocketClient(CountDownLatch latch) { this.latch = latch; }
//        @Override
//        public void onOpen(WebSocket webSocket) {
//            System.out.println("Websocket connected... ");
//            WebSocket.Listener.super.onOpen(webSocket);
//        }
//        @Override
//        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
//            System.out.println("Received: " + data);
//            latch.countDown();
//            return WebSocket.Listener.super.onText(webSocket, data, last);
//        }
//        @Override
//        public void onError(WebSocket webSocket, Throwable error) {
//            System.out.println("Websocket input has been closed... " + webSocket.toString());
//            WebSocket.Listener.super.onError(webSocket, error);
//        }
////        private BufferedReader getInput() {
////            return new BufferedReader(new InputStreamReader(System.in));
////        }
////        public interface MessageHandler {
////
////             void handleMessage(CharSequence message);
////        }
//
//    }
//}



