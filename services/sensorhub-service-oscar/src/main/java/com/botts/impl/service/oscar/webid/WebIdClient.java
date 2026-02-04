package com.botts.impl.service.oscar.webid;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

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

    public org.sensorhub.impl.utils.rad.model.WebIdAnalysis analyze(WebIdRequest webIdRequest) throws IOException, InterruptedException {
        if (webIdRequest == null) {
            throw new IllegalArgumentException("WebIdRequest cannot be null");
        }

        String boundary = UUID.randomUUID().toString();
        byte[] body;
        try {
            body = buildMultipartBody(webIdRequest, boundary);
        } finally {
            closeQuietly(webIdRequest.getForeground());
            closeQuietly(webIdRequest.getBackground());
        }

        StringBuilder uriBuilder = new StringBuilder(apiRoot + "analysis");
        List<String> queryParams = new ArrayList<>();

        if (webIdRequest.getDrf() != null && !webIdRequest.getDrf().isBlank()) {
            queryParams.add("drf=" + webIdRequest.getDrf().replace(" ", "%20"));
        }
        if (webIdRequest.synthesizeBackground()) {
            queryParams.add("synthesizeBackground=true");
        }
        if (!queryParams.isEmpty()) {
            uriBuilder.append("?").append(String.join("&", queryParams));
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uriBuilder.toString()))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new WebIdAnalysisException("Analysis request failed with HTTP status " + response.statusCode(),
                    response.statusCode(), response.body());
        }

        return parseAnalysisResponse(response.body());
    }

    private void closeQuietly(InputStream stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException ignored) {
            }
        }
    }

    private byte[] buildMultipartBody(WebIdRequest request, String boundary) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        if (request.getBackground() != null) {
            appendFilePart(baos, boundary, "foreground", "foreground.n42", request.getForeground());
            appendFilePart(baos, boundary, "background", "background.n42", request.getBackground());
        } else {
            appendFilePart(baos, boundary, "file", "spectrum.n42", request.getForeground());
        }

        baos.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return baos.toByteArray();
    }

    private void appendFilePart(ByteArrayOutputStream baos, String boundary, String fieldName, String fileName, InputStream inputStream) throws IOException {
        if (inputStream == null) {
            throw new IllegalArgumentException("InputStream for " + fieldName + " cannot be null");
        }
        String partHeader = "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"" + fieldName + "\"; filename=\"" + fileName + "\"\r\n" +
                "Content-Type: application/xml\r\n\r\n";
        baos.write(partHeader.getBytes(StandardCharsets.UTF_8));
        baos.write(inputStream.readAllBytes());
        baos.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    private org.sensorhub.impl.utils.rad.model.WebIdAnalysis parseAnalysisResponse(String responseBody) {
        JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();

        List<org.sensorhub.impl.utils.rad.model.IsotopeAnalysis> isotopes = new ArrayList<>();
        if (json.has("isotopes") && json.get("isotopes").isJsonArray()) {
            JsonArray isotopesArray = json.getAsJsonArray("isotopes");
            for (JsonElement element : isotopesArray) {
                JsonObject isotopeObj = element.getAsJsonObject();
                org.sensorhub.impl.utils.rad.model.IsotopeAnalysis isotope = new org.sensorhub.impl.utils.rad.model.IsotopeAnalysis.Builder()
                        .name(getStringOrNull(isotopeObj, "name"))
                        .type(getStringOrNull(isotopeObj, "type"))
                        .confidence(getFloatOrZero(isotopeObj, "confidence"))
                        .confidenceStr(getStringOrNull(isotopeObj, "confidenceStr"))
                        .countRate(getFloatOrZero(isotopeObj, "countRate"))
                        .build();
                isotopes.add(isotope);
            }
        }

        List<String> warnings = new ArrayList<>();
        if (json.has("analysisWarnings") && json.get("analysisWarnings").isJsonArray()) {
            JsonArray warningsArray = json.getAsJsonArray("analysisWarnings");
            for (JsonElement element : warningsArray) {
                warnings.add(element.getAsString());
            }
        }

        String errorMessage = getStringOrNull(json, "errorMessage");
        if (errorMessage == null) {
            errorMessage = getStringOrNull(json, "error");
        }

        return new org.sensorhub.impl.utils.rad.model.WebIdAnalysis.Builder()
                .isotopes(isotopes)
                .estimatedDose(getDoubleOrZero(json, "estimatedDose"))
                .chi2(getDoubleOrZero(json, "chi2"))
                .analysisError(getIntOrZero(json, "analysisError"))
                .errorMessage(errorMessage)
                .analysisWarnings(warnings)
                .drf(getStringOrNull(json, "drf"))
                .isotopeString(getStringOrNull(json, "isotopeString"))
                .build();
    }

    private String getStringOrNull(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : null;
    }

    private float getFloatOrZero(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsFloat() : 0f;
    }

    private double getDoubleOrZero(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsDouble() : 0d;
    }

    private int getIntOrZero(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsInt() : 0;
    }
}
