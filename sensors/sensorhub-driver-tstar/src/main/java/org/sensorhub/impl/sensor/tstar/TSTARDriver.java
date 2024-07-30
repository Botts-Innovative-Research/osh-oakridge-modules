package org.sensorhub.impl.sensor.tstar;

import com.google.gson.*;
import net.opengis.gml.v32.AbstractFeature;
import org.sensorhub.api.comm.ICommProvider;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.comm.RobustIPConnection;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.*;
import java.io.*;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;

import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;


public class TSTARDriver extends AbstractSensorModule<TSTARConfig> {

    static final Logger log = LoggerFactory.getLogger(TSTARDriver.class);

    static final String SENSOR_UID_PREFIX = "urn:osh:sensor:tstar:";

    //Connections
    public String loginURL;
    public String apiURL;
    public String authToken;
    ICommProvider<?> commProvider;
    RobustIPConnection connection;
    public HttpClient httpClient;


    //FOIs
    Set<String> foiIDs;
    Map<String, AbstractFeature> vehicleFois;


    //Outputs
    TSTAREventOutput eventOutput;
    TSTARPositionOutput positionOutput;
    TSTARCampaignOutput campaignOutput;

    public TSTARDriver() {
    }

    /* TASKS
    1. Assume that campaignIds or unitIds will be the way in which you will receive data
    Based on this how will the driver need to ping these data sources? GET requests will have to be
    sent every so often to pull data from the requested source. How will this data be parsed and
    what functions will be necessary in making these calls?

    Will the connection be via http?

    2. Explore FOI and how they are connected in the AVL driver - it will likely be a similar process
    for associating the data with a FOI in this driver
     */
    private Socket clientSocket;
    private Socket serverSocket;
    private PrintWriter out;
    private BufferedReader in;

    public void startConnection(String ip, int port) throws IOException {
        clientSocket = new Socket(ip, port);
        out = new PrintWriter(clientSocket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
    }

    public String getAuthToken() throws URISyntaxException, IOException, InterruptedException {

        String body = "{\"email\": \"admin@gearsornl.com\", \"password\": \"imAgearHEADnow\"}";

        httpClient = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(loginURL))
                    .headers("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body();

            Gson gson = new Gson();

//        Map jsonJavaRootObject = new Gson().fromJson(responseBody, Map.class);
//        Object payloadObj = jsonJavaRootObject.get("payload");

            JsonElement jElement = new Gson().fromJson(responseBody, JsonElement.class);
            JsonObject  payload = jElement.getAsJsonObject();
            payload = payload.getAsJsonObject("payload");
            authToken = payload.get("token").getAsString();
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
    public String getLoginURL() {return loginURL;}
    public String getApiURL() {return apiURL;}


    @Override
    public void init() throws SensorHubException {
        super.init();

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
        addOutput(campaignOutput, false);
        campaignOutput.init();

        try {
            setConfiguration(config);
            getAuthToken();
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

    public HttpClient getHttpClient() {
        return httpClient;
    }
    public void httpGetRequest(String GETRequest, String token, HttpClient client) throws URISyntaxException,
            IOException,
            InterruptedException {
        httpClient = client;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(getApiURL()+GETRequest))
                .headers("Content-Type", "application/json", "Authorization", "Bearer " + token)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        String responseBody = response.body();
        System.out.println(responseBody);
    }

    @Override
    public void start() throws SensorHubException {
        // init comm provider
    try {
        httpGetRequest("/campaigns", authToken, httpClient);
    } catch (URISyntaxException e) {
        throw new RuntimeException(e);
    } catch (IOException e) {
        throw new RuntimeException(e);
    } catch (InterruptedException e) {
        throw new RuntimeException(e);
    }

    serverSocket = new Socket(;
}

//        if (commProvider == null) {
//            try {
//                if (config.commSettings == null)
//                    throw new SensorHubException("No communication settings specified");
//
//                var moduleReg = getParentHub().getModuleRegistry();
//                commProvider = (ICommProvider<?>) moduleReg.loadSubModule(config.commSettings, true);
//                commProvider.start();
//            } catch (Exception e) {
//                commProvider = null;
//                throw new SensorException("Error while initializing communications ", e);
//            }
//        }
//        if (connection == null) {
//            try {
//                if (config.connection == null)
//                    throw new SensorHubException("No communication settings specified");
//
//                var moduleReg = getParentHub().getModuleRegistry();
//                connection = (connection) <?>) moduleReg.loadSubModule(config.com, true);
//                connection.start();
//            } catch (Exception e) {
//                connection = null;
//                throw new SensorException("Error while initializing communications ", e);
//            }
//        }
//        TSTAREventOutput.start(commProvider);
//    }

    @Override
    public void stop() throws SensorHubException {
        // stop comm provider
//        if (commProvider != null)
//        {
//            commProvider.stop();
//            commProvider = null;
//        }
    }

    @Override
    public boolean isConnected()
    {
        return connection.isConnected();
    }


//    @Override
//    public void cleanup() throws SensorHubException
//    {
//
//    }
//


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
