package org.sensorhub.impl.sensor.tstar;

import com.google.gson.*;
import net.opengis.gml.v32.AbstractFeature;
import org.sensorhub.api.common.SensorHubException;
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
import java.util.Map;
import java.util.Set;
import org.sensorhub.impl.sensor.tstar.responses.*;


public class TSTARDriver extends AbstractSensorModule<TSTARConfig> {

    static final Logger log = LoggerFactory.getLogger(TSTARDriver.class);
    static final String SENSOR_UID_PREFIX = "urn:osh:sensor:tstar:";

    //Connections
    public String loginURL;
    public String apiURL;
    public String authToken;
    public String campaignId;
    public String uri;
    public HttpClient httpClient;
    TSTARMessageHandler messageHandler;

    //FOIs
    Set<String> foiIDs;
    Map<String, AbstractFeature> vehicleFois;

    //Outputs
    TSTAREventOutput eventOutput;
    TSTARPositionOutput positionOutput;
    TSTARCampaignOutput campaignOutput;

    public TSTARDriver() {
        httpClient = HttpClient.newHttpClient();
    }

    public void startConnection() throws Exception {
    setConfiguration(config);
//        uri = String.valueOf(new URI("ws://127.0.0.1:10024/monitor"));
        TSTARClient client = new TSTARClient(authToken, campaignId);
        client.start();

        String message = "{\"authToken\": " + authToken + ", \"campaignId\": " + campaignId +"}";
    }

    public String getAuthToken() throws URISyntaxException, IOException, InterruptedException {

        String body = "{\"email\": \"admin@gearsornl.com\", \"password\": \"imAgearHEADnow\"}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(loginURL))
                .headers("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = HttpClient.newBuilder()
                .build()
                .send(request, HttpResponse.BodyHandlers.ofString());

        String responseBody = response.body();

//        Gson gson = new Gson();
//        Map jsonJavaRootObject = new Gson().fromJson(responseBody, Map.class);
//        Object payloadObj = jsonJavaRootObject.get("payload");

        JsonElement jElement = new Gson().fromJson(responseBody, JsonElement.class);
        JsonObject payload = jElement.getAsJsonObject();
        payload = payload.getAsJsonObject("payload");
        config.authToken = payload.get("token").getAsString();
        authToken = config.authToken;
        return authToken;
    }

    public void setConfiguration(TSTARConfig config) {
        super.setConfiguration(config);

        // compute full host URL
        loginURL = "http://" + config.http.remoteHost + ":" + config.http.remotePort + "/api/login";
        apiURL = "http://" + config.http.remoteHost + ":" + config.http.remotePort + "/api";

//       POST http://wsl.localhost:10024/api/login
//       body:  {"email": "admin@gearsornl.com", "password": "imAgearHEADnow"}
//       headers: { "Content-Type": "application/json" },\n
//        config.http.user = "admin@gearsornl.com";
//        config.http.password = "imAgearHEADnow";
    }
    @Override
    public void doInit() throws SensorHubException {

        // generate IDs
        generateUniqueID("urn:osh:sensor:tstar:", config.serialNumber);
        generateXmlID("TSTAR_", config.serialNumber);

        // create foi maps
//        this.foiIDs = new LinkedHashSet<String>();
//        this.vehicleFois = new LinkedHashMap<String, AbstractFeature>();

        eventOutput = new TSTAREventOutput(this);
        addOutput(eventOutput, false);
        eventOutput.init();

        positionOutput = new TSTARPositionOutput(this);
        addOutput(positionOutput, false);
        positionOutput.init();

        campaignOutput = new TSTARCampaignOutput(this);
        campaignOutput.init();
        addOutput(campaignOutput, false);

        try {
            getAuthToken();
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
    public String getCampaigns() throws URISyntaxException,
            IOException,
            InterruptedException {

        HttpRequest request = HttpRequest.newBuilder()
//              .uri(new URI(getApiURL()+GETRequest))
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
        config.campaignId = payload.get(2).getAsJsonObject().getAsJsonPrimitive("id").getAsString();

        campaignId = config.campaignId;
        return campaignId;
    }
    @Override
    public void doStart() throws SensorHubException {
//        try {
//            startConnection();
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        } catch (URISyntaxException e) {
//            throw new RuntimeException(e);
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//    }
        logger.info("Starting Messenger");
        messageHandler = new TSTARMessageHandler(authToken, campaignId, campaignOutput, eventOutput,
                positionOutput);
        logger.info("Messenger Started");
//            if (started)
//                return;
        try{
            messageHandler.connectWS("ws://127.0.0.1:10024/monitor");
            logger.info("connected to WS");
//            messageHandler.sendMsg(messageHandler.openMessage);
//            logger.info("remote join req sent");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

    }
    @Override
    public void doStop() throws SensorHubException {
    }

    public String getLoginURL() {return loginURL;}
    public String getApiURL() {return apiURL;}
    public HttpClient getHttpClient() {return httpClient;}
    public String fetchAuthToken() {return authToken;}
    public String getCampaignId() {return campaignId;}

    public static String toJson(Object object) {
        Gson gson = new  GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(object);
        return json;
    }
    public void printJson(String json) {
        JsonParser parser = new JsonParser();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        JsonElement el = parser.parse(json);
        json = gson.toJson(el); // done
        System.err.println(json);
    }

    @Override
    public boolean isConnected() {return false;}

//    void addFoi(double recordTime, String vehicleID)
//    {
//        if (!vehicleFois.containsKey(vehicleID))
//        {
//            String name = vehicleID;
//            String uid = SENSOR_UID_PREFIX + config.fleetID + ':' + vehicleID;
//            String description = "Vehicle " + vehicleID + " from " + config.agencyName;
//
//            SMLHelper smlFac = new SMLHelper();
//
//            // generate small SensorML for FOI (in this case the system is the FOI)
//            PhysicalSystem foi = smlFac.newPhysicalSystem();
//            foi.setId(vehicleID);
//            foi.setUniqueIdentifier(uid);
//            foi.setName(name);
//            foi.setDescription(description);
//            /*ContactList contacts = smlFac.newContactList();
//            CIResponsibleParty contact = smlFac.newResponsibleParty();
//            contact.setOrganisationName(config.agencyName);
//            contact.setRole(new CodeListValueImpl("operator"));
//            contacts.addContact(contact);
//            foi.addContacts(contacts);*/
//
//            // update maps
//            foiIDs.add(uid);
//            vehicleFois.put(vehicleID, foi);
//
//            // send event
//            long now = System.currentTimeMillis();
//            eventHandler.publishEvent(new FoiEvent(now, vehicleID, this, foi, recordTime));
//
//            log.debug("New vehicle added as FOI: {}", uid);
//        }
//    }
//
//    @Override
//    public Collection<String> getEntityIDs()
//    {
//        return Collections.unmodifiableSet(vehicleFois.keySet());
//    }
//
//
//    @Override
//    public AbstractFeature getCurrentFeatureOfInterest()
//    {
//        return null;
//    }
//
//
//    @Override
//    public AbstractProcess getCurrentDescription(String entityID)
//    {
//        return null;
//    }
//
//
//    @Override
//    public double getLastDescriptionUpdate(String entityID)
//    {
//        return 0;
//    }
//
//
//    @Override
//    public AbstractFeature getCurrentFeatureOfInterest(String entityID)
//    {
//        return vehicleFois.get(entityID);
//    }
//
//
//    @Override
//    public Collection<? extends AbstractFeature> getFeaturesOfInterest()
//    {
//        return Collections.unmodifiableCollection(vehicleFois.values());
//    }
//
//
//    @Override
//    public Collection<String> getFeaturesOfInterestIDs()
//    {
//        return Collections.unmodifiableSet(foiIDs);
//    }
//
//
//    @Override
//    public Collection<String> getEntitiesWithFoi(String foiID)
//    {
//        if (!foiIDs.contains(foiID))
//            return Collections.EMPTY_SET;
//
//        String entityID = foiID.substring(foiID.lastIndexOf(':')+1);
//        return Arrays.asList(entityID);
//    }

}
