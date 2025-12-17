package com.botts.impl.service.oscar.webid;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashSet;
import java.util.Set;

public class WebIdClient {

    private final HttpClient httpClient;
    private final String apiRoot;

    public WebIdClient(String apiRoot) {
        this.apiRoot = apiRoot.endsWith("/") ? apiRoot : apiRoot + "/";
        this.httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
    }

    public Set<String> getPossibleDRFs() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiRoot + "info"))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();

        if (!json.has("Options"))
            throw new IllegalStateException("Options not found");

        JsonArray options = json.getAsJsonArray("Options");

        JsonObject drfOption = options.get(0).getAsJsonObject();

        if (drfOption.has("name") && !drfOption.get("name").getAsString().equals("drf"))
            throw new IllegalStateException("DRF list not found");

        if (!drfOption.has("possibleValues"))
            throw new IllegalArgumentException("possibleValues in DRF list not found");

        JsonArray possibleValues = drfOption.getAsJsonArray("possibleValues");

        return new HashSet<>(possibleValues.asList().stream().map(JsonElement::getAsString).toList());
    }

    public void analyze(InputStream foreground, InputStream background) {

    }

    public void analyze(InputStream foregroundWithBackground) {

    }

    public void analyze() {

    }

}
