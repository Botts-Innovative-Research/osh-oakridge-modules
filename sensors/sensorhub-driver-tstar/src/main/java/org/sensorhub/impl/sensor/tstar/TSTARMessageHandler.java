package org.sensorhub.impl.sensor.tstar;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.sensorhub.impl.sensor.tstar.responses.Campaign;
import org.sensorhub.impl.sensor.tstar.responses.Event;
import org.sensorhub.impl.sensor.tstar.responses.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;

public class TSTARMessageHandler {
    TSTARCampaignOutput campaignOutput;
    TSTAREventOutput eventOutput;
    TSTARPositionOutput positionOutput;
    String openMessage;
    static final Logger log = LoggerFactory.getLogger(TSTARMessageHandler.class);


    public TSTARMessageHandler(String authToken, String campaignId, TSTARCampaignOutput campaignOutput,
                               TSTAREventOutput eventOutput, TSTARPositionOutput positionOutput) {
        this.campaignOutput = campaignOutput;
        this.eventOutput = eventOutput;
        this.positionOutput = positionOutput;
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

    public void handleMsg(String msg) throws IOException {
        System.out.println("Received message in client: " + msg);

            JsonElement jElement = new Gson().fromJson(msg, JsonElement.class);
            JsonPrimitive changeType = jElement.getAsJsonObject().getAsJsonPrimitive("changeType");
            JsonObject data = jElement.getAsJsonObject().getAsJsonObject("data").getAsJsonObject();

            String dataStr = data.toString();
            String type = changeType.getAsString();

            switch (type) {
                case "UNIT": {
//                    Unit unit = mapper.readValue(msg, Unit.class);
                    break;
                }
                case "CAMPAIGN": {
                    Campaign campaign = new Gson().fromJson(dataStr, Campaign.class);
                    campaignOutput.parse(campaign);
                    break;
                }
                case "EVENT" : {
                    Event event = new Gson().fromJson(dataStr, Event.class);
                    eventOutput.parse(event);
                    break;

                }
                case "UNIT_LOG" : {
                    break;
                }
                case "POSITION_LOG": {
                    Position position = new Gson().fromJson(dataStr, Position.class);
                    positionOutput.parse(position);
                    break;
                }
                case "MESSAGE_LOG": {
                    break;

                }
                case "AUDIT_LOG": {
                    break;

                }
            }
        }
    }

