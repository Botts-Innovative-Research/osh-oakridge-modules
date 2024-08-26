package org.sensorhub.impl.sensor.tstar;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class TSTARDriver extends AbstractSensorModule<TSTARConfig> {

    static final Logger log = LoggerFactory.getLogger(TSTARDriver.class);
    static final String SENSOR_UID_PREFIX = "urn:osh:sensor:tstar:";

    //Connections
    public String loginURL;
    public String apiURL;
    public String authToken;
    public String campaignId;
    public HttpClient httpClient;
    TSTARMessageHandler messageHandler;


    //Outputs
    TSTARAuditLogOutput auditLogOutput;
    TSTARCampaignOutput campaignOutput;
    TSTAREventOutput eventOutput;
    TSTARMessageLogOutput messageLogOutput;
    TSTARPositionOutput positionOutput;
    TSTARUnitOutput unitOutput;
    TSTARUnitLogOutput unitLogOutput;


    public TSTARDriver() {
        httpClient = HttpClient.newHttpClient();
    }


    public String getAuthToken(String username, String password) throws URISyntaxException, IOException,
            InterruptedException {

        String body = "{\"email\": \"" + username + "\", \"password\": \"" + password + "\"}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(loginURL))
                .headers("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = HttpClient.newBuilder()
                .build()
                .send(request, HttpResponse.BodyHandlers.ofString());

        String responseBody = response.body();

        JsonElement jElement = new Gson().fromJson(responseBody, JsonElement.class);
        JsonObject payload = jElement.getAsJsonObject();
        payload = payload.getAsJsonObject("payload");
        config.authToken = payload.get("token").getAsString();
        authToken = config.authToken;
        return authToken;
    }
    public String getCampaigns() throws URISyntaxException,
            IOException,
            InterruptedException {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(apiURL + "/campaigns"))
                .headers("Content-Type", "application/json", "Authorization", "Bearer " + authToken)
                .GET()
                .build();

        HttpResponse<String> response = HttpClient.newBuilder()
                .build()
                .send(request, HttpResponse.BodyHandlers.ofString());

        String responseBody = response.body();
        System.out.println(responseBody);

        JsonElement jElement = new Gson().fromJson(responseBody, JsonElement.class);
        JsonArray payload = jElement.getAsJsonObject().getAsJsonArray("payload");
        config.campaignId = payload.get(0).getAsJsonObject().getAsJsonPrimitive("id").getAsString();

        campaignId = config.campaignId;
        return campaignId;
    }

    public void setConfiguration(TSTARConfig config) {
        super.setConfiguration(config);

        // compute full host URL
        loginURL = "http://" + config.http.remoteHost + ":" + config.http.remotePort + "/api/login";
        apiURL = "http://" + config.http.remoteHost + ":" + config.http.remotePort + "/api";
    }
    @Override
    public void doInit() {

        // generate IDs
        generateUniqueID("urn:osh:sensor:tstar:", config.serialNumber);
        generateXmlID("TSTAR_", config.serialNumber);

        auditLogOutput = new TSTARAuditLogOutput(this);
        addOutput(auditLogOutput, false);
        auditLogOutput.init();

        campaignOutput = new TSTARCampaignOutput(this);
        addOutput(campaignOutput, false);
        campaignOutput.init();

        eventOutput = new TSTAREventOutput(this);
        addOutput(eventOutput, false);
        eventOutput.init();

        messageLogOutput = new TSTARMessageLogOutput(this);
        addOutput(messageLogOutput, false);
        messageLogOutput.init();

        positionOutput = new TSTARPositionOutput(this);
        addOutput(positionOutput, false);
        positionOutput.init();

        unitOutput = new TSTARUnitOutput(this);
        addOutput(unitOutput, false);
        unitOutput.init();

        unitLogOutput = new TSTARUnitLogOutput(this);
        addOutput(unitLogOutput, false);
        unitLogOutput.init();


        try {
            getAuthToken(config.username, config.password);
            getCampaigns();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    @Override
    protected void updateSensorDescription()
    {
        synchronized (sensorDescLock)
        {
            super.updateSensorDescription();
            sensorDescription.setDescription("TSTAR data");
        }
    }
    @Override
    public void doStart() {

        logger.info("Starting Messenger");
        messageHandler = new TSTARMessageHandler(authToken, campaignId, auditLogOutput, campaignOutput, eventOutput,
                messageLogOutput, positionOutput, unitLogOutput, unitOutput);
        logger.info("Messenger Started");

        try{
            messageHandler.connectWS("ws://" + config.http.remoteHost+ ":" + config.http.remotePort + "/monitor");
            logger.info("connected to WS");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
    @Override
    public void doStop() {}

    @Override
    public boolean isConnected() {return false;}

}
