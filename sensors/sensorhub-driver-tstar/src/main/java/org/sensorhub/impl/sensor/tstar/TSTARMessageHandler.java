package org.sensorhub.impl.sensor.tstar;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import org.checkerframework.checker.units.qual.C;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.sensorhub.impl.sensor.tstar.responses.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class TSTARMessageHandler {
    TSTARAuditLogOutput auditLogOutput;
    TSTARCampaignOutput campaignOutput;
    TSTAREventOutput eventOutput;
    TSTARMessageLogOutput messageLogOutput;
    TSTARPositionOutput positionOutput;
    TSTARUnitLogOutput unitLogOutput;
    TSTARUnitOutput unitOutput;

    String openMessage;
    static final Logger log = LoggerFactory.getLogger(TSTARMessageHandler.class);


    public TSTARMessageHandler(String authToken, String campaignId,
                               TSTARAuditLogOutput auditLogOutput, TSTARCampaignOutput campaignOutput,
                               TSTAREventOutput eventOutput,
                               TSTARMessageLogOutput messageLogOutput, TSTARPositionOutput positionOutput,
                               TSTARUnitLogOutput unitLogOutput, TSTARUnitOutput unitOutput) {
        this.auditLogOutput = auditLogOutput;
        this.campaignOutput = campaignOutput;
        this.eventOutput = eventOutput;
        this.messageLogOutput = messageLogOutput;
        this.positionOutput = positionOutput;
        this.unitOutput = unitOutput;
        this.unitLogOutput = unitLogOutput;
        this.openMessage = "{\"authToken\": \"" + authToken + "\", \"campaignId\": \"" + campaignId + "\"}";
    }

    WebSocketClient client = new WebSocketClient();
    TSTARWebSocketClient socket = new TSTARWebSocketClient(this);

    public void connectWS(String wsURI) throws Exception {
        client.start();
        URI uri = URI.create(wsURI);
        client.connect(socket, uri);
    }

    public String openConnection() {
        return this.openMessage;
    }

    public void sendMsg(String msg) {
        log.info(msg);
        socket.sendMessage(msg);
    }
//    public static class CustomDeserializer implements JsonDeserializer<MessageLog.MsgLogMeta> {
//        @Override
//        public MessageLog.MsgLogMeta deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
//            List<String> keywords = null;
//            Gson gson = new Gson();
//            MessageLog.MsgLogMeta meta = gson.fromJson(json, MessageLog.MsgLogMeta.class);
//            JsonObject jsonObject = json.getAsJsonObject();
//
//            if (jsonObject.has("keywords")) {
//                JsonElement elem = jsonObject.get("keywords");
//                if (elem != null && !elem.isJsonNull()) {
//
//                    if (jsonObject.get("keywords").isJsonArray()) {
//                        keywords = gson.fromJson(jsonObject.get("keywords"), new TypeToken<List<String>>() {
//                        }.getType());
//                    } else {
//                        String keywordString = gson.fromJson(jsonObject.get("keywords"), String.class);
//                        keywords = new ArrayList<String>();
//                        list.addAll(Arrays.asList(keywordString.split(",")));
//                    }
//                }
//            }
//            meta.setKeywords(keywords);
//        }
//    }
//    public static class MessageLogDeserilizer implements JsonDeserializer {
//        @Override
//        public MessageLog deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
//
//            Gson gson = new Gson();
//            MessageLog messageLog = gson.fromJson(json, MessageLog.class);
//            JsonObject jsonObject = json.getAsJsonObject();
//            if (jsonObject.has("meta")) {
//                JsonElement elem = jsonObject.get("meta");
//                if (elem != null && !elem.isJsonNull()) {
//
//                    Gson gsonDeserializer = new GsonBuilder()
//                            .registerTypeAdapter(MessageLog.MsgLogMeta.class, new CustomDeserializer())
//                            .create();
//                    gsonDeserializer.fromJson(jsonObject.get("meta"), MessageLog.class);
//                }
//            }
//
//            return messageLog;
//        }
//    }
    public void handleMsg(String msg) throws IOException {
        System.out.println("Received message in client: " + msg);

            JsonElement jElement = new Gson().fromJson(msg, JsonElement.class);
            JsonElement content = jElement.getAsJsonObject().get("data");
            JsonPrimitive changeType = jElement.getAsJsonObject().getAsJsonPrimitive("changeType");


            JsonObject data = jElement.getAsJsonObject().getAsJsonObject("data").getAsJsonObject();
            String dataStr = data.toString();
            String type = changeType.getAsString();

            switch (type) {
                case "UNIT": {
                    Unit unit = new Gson().fromJson(dataStr, Unit.class);
                    unitOutput.parse(unit);
                    break;
                }
                case "CAMPAIGN": {
                    Campaign campaign = new Gson().fromJson(dataStr, Campaign.class);
                    campaignOutput.parse(campaign);
                    break;
                }
                case "EVENT": {
                    Event event = new Gson().fromJson(dataStr, Event.class);
                    eventOutput.parse(event);
                    break;
                }
                case "UNIT_LOG": {
                    UnitLog unitLog = new Gson().fromJson(dataStr, UnitLog.class);
                    unitLogOutput.parse(unitLog);
                    break;
                }
                case "POSITION_LOG": {
                    PositionLog position = new Gson().fromJson(dataStr, PositionLog.class);
                    positionOutput.parse(position);
                    break;
                }
                case "MESSAGE_LOG": {
//                    Gson gson = new Gson().newBuilder().registerTypeAdapter(MessageLog.MsgLogMeta.class,
//                            new TypeAdapter<MessageLog.class>() {
//                    })
                    MessageLog messageLog = new Gson().fromJson(dataStr, MessageLog.class);
                    messageLogOutput.parse(messageLog);
                    break;
                }
                case "AUDIT_LOG": {
                    AuditLog auditLog = new Gson().fromJson(dataStr, AuditLog.class);
                    auditLogOutput.parse(auditLog);
                    break;
                }
            }
        }
    }

