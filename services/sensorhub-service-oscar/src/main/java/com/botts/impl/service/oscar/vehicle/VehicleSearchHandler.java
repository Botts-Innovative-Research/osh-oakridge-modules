/*******************************************************************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 The Initial Developer is Botts Innovative Research Inc. Portions created by the Initial
 Developer are Copyright (C) 2026 the Initial Developer. All Rights Reserved.

 ******************************************************************************/

package com.botts.impl.service.oscar.vehicle;

import com.botts.api.service.bucket.IBucketStore;
import com.botts.api.service.bucket.IObjectHandler;
import com.botts.impl.service.bucket.util.InvalidRequestException;
import com.botts.impl.service.bucket.util.RequestContext;
import com.botts.impl.service.bucket.util.ServiceErrors;
import com.botts.impl.service.oscar.Constants;
import com.botts.impl.service.oscar.OSCARServiceModule;
import com.botts.impl.service.oscar.vehicle.VehicleIncidentAggregator.MatchMode;
import com.botts.impl.service.oscar.vehicle.VehicleIncidentAggregator.VehicleIncidentData;
import com.botts.impl.service.oscar.vehicle.VehicleIncidentAggregator.VehicleIncidentSpec;
import com.google.gson.stream.JsonWriter;
import org.sensorhub.api.datastore.DataStoreException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Authenticated {@link IObjectHandler} serving {@code GET /buckets/adjudication/vehicle-search}.
 *
 * <p>Executes a single-request node-wide vehicle-pass search across {@code CommandStatusStore}
 * adjudications and {@code vehicleOcr} datastreams (including archived {@code PARQUET_PRIMARY}
 * partitions), eliminating {@code O(lanes)} browser HTTP fan-out on 50-lane deployments and
 * federated multi-node views.</p>
 */
public final class VehicleSearchHandler implements IObjectHandler {

    public static final String BUCKET_NAME = Constants.ADJUDICATION_BUCKET;
    public static final String OBJECT_KEY = "vehicle-search";
    public static final int MAX_VEHICLE_QUERY_LENGTH = 128;

    private static final int MAX_CONCURRENT_SEARCHES = 8;
    private static final long PERMIT_TIMEOUT_SECONDS = 5L;
    private static final Pattern OBJECT_PATTERN = Pattern.compile("vehicle-search(?:/.*)?");

    private final OSCARServiceModule module;
    private final Semaphore searchPermits = new Semaphore(MAX_CONCURRENT_SEARCHES, true);

    public VehicleSearchHandler(IBucketStore bucketStore, OSCARServiceModule module) throws DataStoreException {
        Objects.requireNonNull(bucketStore, "bucketStore");
        this.module = Objects.requireNonNull(module, "module");
        if (!bucketStore.bucketExists(BUCKET_NAME)) {
            bucketStore.createBucket(BUCKET_NAME);
        }
    }

    @Override
    public boolean canHandleRequest(HttpServletRequest request) {
        if (request == null || request.getPathInfo() == null) {
            return false;
        }
        String pathInfo = request.getPathInfo().replace('\\', '/');
        String prefix = "/" + BUCKET_NAME + "/" + OBJECT_KEY;
        return prefix.equals(pathInfo) || pathInfo.startsWith(prefix + "/");
    }

    @Override
    public String getObjectPattern() {
        return OBJECT_PATTERN.pattern();
    }

    @Override
    public void doGet(RequestContext ctx) throws IOException, SecurityException {
        var sec = ctx.getSecurityHandler();
        var perms = sec.getBucketPermission(ctx.getBucketName());
        if (perms == null) {
            throw ServiceErrors.forbidden(ctx.getBucketName());
        }
        sec.checkPermission(perms.get);

        requireTargetResource(ctx);

        HttpServletRequest req = ctx.getRequest();
        String rawVehicleId = firstNonNull(req.getParameter("vehicleId"), req.getParameter("q"), "");
        if (rawVehicleId.length() > MAX_VEHICLE_QUERY_LENGTH) {
            throw ServiceErrors.badRequest("vehicleId exceeds maximum length of " + MAX_VEHICLE_QUERY_LENGTH + " characters");
        }
        String normVehicleId = VehicleIncidentAggregator.normalizeVehicleId(rawVehicleId);

        MatchMode defaultMode = normVehicleId.isEmpty() ? MatchMode.IS_NOT_EMPTY : MatchMode.CONTAINS;
        MatchMode matchMode = MatchMode.parse(req.getParameter("matchMode"), defaultMode);
        if ((matchMode == MatchMode.EQUALS || matchMode == MatchMode.CONTAINS || matchMode == MatchMode.STARTS_WITH)
                && normVehicleId.isEmpty()) {
            throw ServiceErrors.badRequest("vehicleId must contain at least one alphanumeric character for matchMode=" + matchMode.wireName());
        }

        Instant start = parseOptionalInstant(
                firstNonNull(req.getParameter("startTime"), req.getParameter("start"), null),
                "startTime",
                Instant.EPOCH
        );
        Instant end = parseOptionalInstant(
                firstNonNull(req.getParameter("endTime"), req.getParameter("end"), null),
                "endTime",
                Instant.now().plusSeconds(300L)
        );
        if (end.isBefore(start)) {
            throw ServiceErrors.badRequest("endTime must not precede startTime");
        }

        String laneUIDs = firstNonNull(req.getParameter("laneUID"), req.getParameter("laneUIDs"), "");
        boolean includeProfiles = Boolean.parseBoolean(req.getParameter("includeProfiles"));
        int limit = parseBoundedLimit(req.getParameter("limit"));

        boolean acquired;
        try {
            acquired = searchPermits.tryAcquire(PERMIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while waiting for vehicle search permit", e);
        }
        if (!acquired) {
            HttpServletResponse resp = ctx.getResponse();
            resp.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            resp.setContentType("application/json; charset=utf-8");
            byte[] busy = "{\"error\":\"Vehicle search concurrency limit reached; retry shortly\"}".getBytes(StandardCharsets.UTF_8);
            resp.setContentLength(busy.length);
            resp.getOutputStream().write(busy);
            resp.getOutputStream().flush();
            return;
        }

        try {
            VehicleIncidentSpec spec = new VehicleIncidentSpec(
                    rawVehicleId.trim(),
                    normVehicleId,
                    matchMode,
                    laneUIDs,
                    includeProfiles,
                    List.of()
            );
            VehicleIncidentData data = VehicleIncidentAggregator.queryVehicleIncident(module, spec, start, end, limit);

            ByteArrayOutputStream buffer = new ByteArrayOutputStream(2048);
            try (JsonWriter jw = new JsonWriter(new OutputStreamWriter(buffer, StandardCharsets.UTF_8))) {
                jw.setHtmlSafe(false);
                VehicleIncidentAggregator.writeJsonPayload(jw, data, false);
                jw.flush();
            }
            byte[] payload = buffer.toByteArray();

            HttpServletResponse resp = ctx.getResponse();
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
            resp.setContentType("application/json; charset=utf-8");
            resp.setHeader("Cache-Control", "private, no-store");
            resp.setHeader("X-Content-Type-Options", "nosniff");
            resp.setContentLength(payload.length);
            resp.getOutputStream().write(payload);
            resp.getOutputStream().flush();
        } finally {
            searchPermits.release();
        }
    }

    @Override
    public void doPost(RequestContext ctx) throws IOException {
        throw ServiceErrors.unsupportedOperation("Vehicle search endpoint is read-only (use GET)");
    }

    @Override
    public void doPut(RequestContext ctx) throws IOException {
        throw ServiceErrors.unsupportedOperation("Vehicle search endpoint is read-only (use GET)");
    }

    @Override
    public void doDelete(RequestContext ctx) throws IOException {
        throw ServiceErrors.unsupportedOperation("Vehicle search endpoint is read-only (use GET)");
    }

    private static void requireTargetResource(RequestContext ctx) throws InvalidRequestException {
        if (!BUCKET_NAME.equals(ctx.getBucketName())) {
            throw ServiceErrors.notFound(ctx.getObjectKey());
        }
        String key = ctx.getObjectKey() == null ? "" : ctx.getObjectKey().replace('\\', '/');
        if (!OBJECT_KEY.equals(key)) {
            throw ServiceErrors.notFound(key);
        }
    }

    private static Instant parseOptionalInstant(String raw, String paramName, Instant defaultValue) throws InvalidRequestException {
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        String trimmed = raw.trim();
        try {
            return Instant.parse(trimmed);
        } catch (DateTimeParseException ignored) {
        }
        try {
            long epochMillis = Long.parseLong(trimmed);
            return Instant.ofEpochMilli(epochMillis);
        } catch (NumberFormatException e) {
            throw ServiceErrors.badRequest("Invalid ISO-8601 timestamp or epoch millis for " + paramName + ": " + trimmed);
        }
    }

    private static int parseBoundedLimit(String rawLimit) throws InvalidRequestException {
        if (rawLimit == null || rawLimit.isBlank()) {
            return VehicleIncidentAggregator.DEFAULT_SEARCH_LIMIT;
        }
        try {
            int parsed = Integer.parseInt(rawLimit.trim());
            if (parsed < 1) {
                throw ServiceErrors.badRequest("limit must be >= 1");
            }
            return Math.min(parsed, VehicleIncidentAggregator.MAX_SEARCH_LIMIT);
        } catch (NumberFormatException e) {
            throw ServiceErrors.badRequest("Invalid integer limit: " + rawLimit);
        }
    }

    private static String firstNonNull(String a, String b, String fallback) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        if (b != null && !b.isBlank()) {
            return b;
        }
        return fallback;
    }
}
