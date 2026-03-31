package com.botts.impl.service.oscar.webid;

import com.botts.api.service.bucket.IBucketStore;
import com.botts.impl.service.bucket.handler.DefaultObjectHandler;
import com.botts.impl.service.bucket.util.InvalidRequestException;
import com.botts.impl.service.bucket.util.MultipartRequestParser;
import com.botts.impl.service.bucket.util.RequestContext;
import com.botts.impl.service.oscar.cambio.CambioConverter;
import com.botts.impl.system.lane.LaneSystem;
import com.google.gson.JsonArray;
import net.opengis.swe.v20.DataBlock;
import org.sensorhub.api.common.BigId;
import org.sensorhub.api.common.IdEncoder;
import org.sensorhub.api.ISensorHub;
import org.sensorhub.api.data.ObsData;
import org.sensorhub.api.datastore.DataStoreException;
import org.sensorhub.api.datastore.obs.DataStreamFilter;
import org.sensorhub.api.utils.OshAsserts;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.impl.system.SystemDatabaseTransactionHandler;
import org.sensorhub.impl.utils.rad.model.Occupancy;
import org.sensorhub.impl.utils.rad.model.WebIdAnalysis;
import org.sensorhub.impl.utils.rad.output.N42Output;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;

public class WebIdResourceHandler extends DefaultObjectHandler {

    private static final Logger logger = LoggerFactory.getLogger(WebIdResourceHandler.class);
    private static final Set<String> WEB_ID_FILE_EXTENSIONS = new CambioConverter().getSupportedInputFormats();

    private static final String RADDATA_PREFIX = "RADDATA://";

    private final Pattern pattern;
    private final ISensorHub hub;
    private final WebIdClient webIdClient;
    private final WebIdAnalyzer webIdAnalyzer;

    public WebIdResourceHandler(IBucketStore bucketStore, ISensorHub hub, WebIdClient webIdClient) {
        super(bucketStore);
        this.hub = hub;
        this.webIdClient = webIdClient;
        webIdAnalyzer = new WebIdAnalyzer(bucketStore, webIdClient, hub);

        String joinedExtensions = String.join("|", WEB_ID_FILE_EXTENSIONS);
        String regex = new StringBuilder(".*\\.(")
                .append(joinedExtensions)
                .append(")")
                .toString();
        pattern = Pattern.compile(regex);
    }

    @Override
    public void doPut(RequestContext ctx) throws IOException, SecurityException {
        var bucketName = ctx.getBucketName();
        var objectKey = ctx.getObjectKey();
        var sec = ctx.getSecurityHandler();
        sec.checkPermission(sec.getBucketPermission(bucketName).put);

        // Extract query parameters
        var webIdContext = new WebIdRequestContext(ctx);

        if (!webIdContext.webIdEnabled)
            return;

        // PUT should only update one resource, so prioritize POST for FG + BG
        if (webIdContext.foregroundData != null && webIdContext.hasForegroundFileName()) {
            // Update the object in the bucket
            try {
                bucketStore.putObject(bucketName, objectKey, new ByteArrayInputStream(webIdContext.foregroundData), ctx.getHeaders());
            } catch (DataStoreException e) {
                throw new IOException(IBucketStore.FAILED_PUT_OBJECT + bucketName, e);
            }
        }

        ctx.getResponse().setStatus(HttpServletResponse.SC_OK);

        webIdAnalyzer.processWebIdRequest(webIdContext);
    }

    @Override
    public void doPost(RequestContext ctx) throws IOException, SecurityException {
        var bucketName = ctx.getBucketName();
        var sec = ctx.getSecurityHandler();
        sec.checkPermission(sec.getBucketPermission(bucketName).create);

        // Extract query parameters
        var webIdContext = new WebIdRequestContext(ctx);

        if (!webIdContext.webIdEnabled)
            return;

        List<String> resourceURIs = new ArrayList<>();

        // Save background data in bucket
        if (webIdContext.backgroundData != null
                // ensure we have a filename for put requests
                && webIdContext.backgroundFileName != null
                && !webIdContext.backgroundFileName.isBlank()) {
            try {
                bucketStore.putObject(bucketName, webIdContext.backgroundFileName,
                        new ByteArrayInputStream(webIdContext.backgroundData), ctx.getHeaders());
                // add to generated resource URI list
                resourceURIs.add(bucketStore.getRelativeResourceURI(bucketName, webIdContext.backgroundFileName));
            } catch (DataStoreException e) {
                logger.error("Failed to put background data from {} in bucket store", webIdContext.backgroundFileName, e);
            }
        }

        // For POST, we can have foreground data with no filename (i.e. QR code)
        if (webIdContext.foregroundData != null) {
            try {
                // put if foreground + background
                if (webIdContext.hasForegroundFileName()) {
                    bucketStore.putObject(bucketName, webIdContext.foregroundFileName,
                            new ByteArrayInputStream(webIdContext.foregroundData), ctx.getHeaders());
                    // add to generated resource URI list
                    resourceURIs.add(bucketStore.getRelativeResourceURI(bucketName, webIdContext.foregroundFileName));
                } else {
                    // create object with random UUID if just foreground data
                    String newObjectKey = bucketStore.createObject(bucketName,
                            new ByteArrayInputStream(webIdContext.foregroundData), ctx.getHeaders());
                    if (newObjectKey == null || newObjectKey.isBlank())
                        throw new IOException(IBucketStore.FAILED_CREATE_OBJECT + bucketName);

                    // set location header for single resource
                    ctx.getResponse().setHeader("Location", bucketStore.getRelativeResourceURI(bucketName, newObjectKey));
                }
                ctx.getResponse().setStatus(HttpServletResponse.SC_CREATED);
            } catch (Exception e) {
                throw new IOException("Failed to put foreground data in bucket store", e);
            }
        }

        if (!resourceURIs.isEmpty()) {
            JsonArray jsonArray = new JsonArray();
            for (var resourceURI : resourceURIs)
                jsonArray.add(resourceURI);
            ctx.getResponse().getWriter().write(jsonArray.toString());
        }

        webIdAnalyzer.processWebIdRequest(webIdContext);
    }

    private void processCambioConversion(WebIdRequestContext webIdContext) {
        // TODO make sure nothing special required for cambio conversion
    }



    private WebIdRequest createWebIdRequest(WebIdRequestContext webIdContext) {
        WebIdRequest.Builder requestBuilder = new WebIdRequest.Builder()
                .foreground(new ByteArrayInputStream(webIdContext.foregroundData));

        // include background but if no background data then we need to synthesize it
        if (webIdContext.backgroundData != null)
            requestBuilder.background(new ByteArrayInputStream(webIdContext.backgroundData));

        // client chooses whether to synthesize background data
        requestBuilder.synthesizeBackground(webIdContext.synthesizeBackground);

        if (webIdContext.drf != null && !webIdContext.drf.isBlank())
            requestBuilder.drf(webIdContext.drf);

        return requestBuilder.build();
    }

    /**
     * Checks if the data is QR code data in RADDATA:// format.
     */
    private boolean isRadDataQrCode(byte[] data) {
        if (data == null || data.length < RADDATA_PREFIX.length()) {
            return false;
        }
        String prefix = new String(data, 0, RADDATA_PREFIX.length(), StandardCharsets.UTF_8);
        return RADDATA_PREFIX.equals(prefix);
    }

    /**
     * Returns true if this handler should handle the request based on the webIdEnabled query parameter.
     * This allows QR code data to be POSTed directly to the bucket root without specifying a filename.
     */
    @Override
    public boolean canHandleRequest(HttpServletRequest request) {
        String webIdEnabledParam = request.getParameter("webIdEnabled");
        return Boolean.parseBoolean(webIdEnabledParam);
    }

    @Override
    public String getObjectPattern() {
        return this.pattern.pattern();
    }
}
