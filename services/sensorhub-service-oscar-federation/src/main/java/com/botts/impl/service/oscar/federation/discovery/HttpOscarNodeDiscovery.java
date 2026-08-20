package com.botts.impl.service.oscar.federation.discovery;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Set;
import com.botts.impl.service.oscar.OSCARSystem;
import com.botts.impl.service.oscar.federation.credentials.FederationCredentials;
import com.botts.impl.service.oscar.federation.model.NodeRegistrationRequest;
import com.botts.impl.service.oscar.federation.security.BasicAuth;
import com.botts.impl.service.oscar.federation.security.UpstreamEndpointPolicy;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class HttpOscarNodeDiscovery implements OscarNodeDiscovery
{
    private final HttpClient client;
    private final UpstreamEndpointPolicy endpointPolicy;
    private final Duration requestTimeout;

    public HttpOscarNodeDiscovery(HttpClient client, UpstreamEndpointPolicy endpointPolicy, Duration requestTimeout)
    {
        this.client = client;
        this.endpointPolicy = endpointPolicy;
        this.requestTimeout = requestTimeout;
    }

    @Override
    public DiscoveredOscarNode discover(NodeRegistrationRequest request)
    {
        URI base = endpointPolicy.validateAndNormalize(request.getUpstreamBaseUri(), request.isPrivateNetworkAllowed());
        URI systemsUri = base.resolve("/sensorhub/api/systems?validTime=latest&limit=10000");
        char[] password = request.copyPassword();
        try (FederationCredentials credentials = new FederationCredentials(request.getUsername(), password))
        {
            HttpRequest httpRequest = HttpRequest.newBuilder(systemsUri)
                .timeout(requestTimeout)
                .header("Accept", "application/json")
                .header("Authorization", BasicAuth.headerValue(credentials))
                .GET()
                .build();
            HttpResponse<String> response = client.send(httpRequest,
                HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8));
            if (response.statusCode() == 401 || response.statusCode() == 403)
                throw new IllegalArgumentException("Remote OSCAR credentials were rejected");
            if (response.statusCode() < 200 || response.statusCode() >= 300)
                throw new IllegalArgumentException("Remote systems API returned HTTP " + response.statusCode());

            Set<String> uids = new LinkedHashSet<>();
            collectOscarUids(JsonParser.parseString(response.body()), uids);
            if (uids.isEmpty())
                throw new IllegalArgumentException("Remote endpoint does not expose an OSCAR system");
            if (uids.size() > 1)
                throw new IllegalArgumentException("Remote endpoint exposes multiple OSCAR system identities");
            return new DiscoveredOscarNode(uids.iterator().next());
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Remote OSCAR discovery was interrupted", e);
        }
        catch (java.io.IOException e)
        {
            throw new IllegalArgumentException("Cannot connect to remote OSCAR node", e);
        }
        finally
        {
            java.util.Arrays.fill(password, '\0');
        }
    }

    @Override
    public void verify(String expectedUid, NodeRegistrationRequest request)
    {
        String actualUid = discover(request).getOscarSystemUid();
        if (!expectedUid.equals(actualUid))
            throw new IllegalArgumentException("Remote OSCAR identity does not match the registered UID");
    }

    private static void collectOscarUids(JsonElement element, Set<String> result)
    {
        if (element == null || element.isJsonNull())
            return;
        if (element.isJsonArray())
        {
            element.getAsJsonArray().forEach(child -> collectOscarUids(child, result));
            return;
        }
        if (!element.isJsonObject())
            return;

        JsonObject object = element.getAsJsonObject();
        JsonElement uid = object.get("uid");
        if (uid != null && uid.isJsonPrimitive())
        {
            String value = uid.getAsString();
            if (value.startsWith(OSCARSystem.UID))
                result.add(value);
        }
        object.entrySet().forEach(entry -> collectOscarUids(entry.getValue(), result));
    }
}
