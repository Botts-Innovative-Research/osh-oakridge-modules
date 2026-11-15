/*******************************************************************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 The Initial Developer is Botts Innovative Research Inc. Portions created by the Initial
 Developer are Copyright (C) 2025 the Initial Developer. All Rights Reserved.

 ******************************************************************************/

package com.botts.impl.service.oscar.reports;

import com.botts.api.service.bucket.IBucketService;
import com.botts.impl.service.oscar.OSCARServiceModule;
import com.botts.impl.service.oscar.OSCARSystem;
import com.botts.impl.service.oscar.reports.helpers.EventReportType;
import com.botts.impl.service.oscar.reports.helpers.ReportCmdType;
import com.botts.impl.service.oscar.reports.helpers.ReportFormat;
import com.botts.impl.service.oscar.reports.types.AdjudicationDetailsReport;
import com.botts.impl.service.oscar.reports.types.AdjudicationReport;
import com.botts.impl.service.oscar.reports.types.EventReport;
import com.botts.impl.service.oscar.reports.types.LaneReport;
import com.botts.impl.service.oscar.reports.types.RDSReport;
import com.botts.impl.service.oscar.reports.types.Report;
import com.botts.impl.service.oscar.reports.types.VehicleIncidentReport;
import com.botts.impl.service.oscar.vehicle.VehicleIncidentAggregator;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.command.CommandResult;
import org.sensorhub.api.command.CommandStatus;
import org.sensorhub.api.command.ICommandData;
import org.sensorhub.api.command.ICommandResult;
import org.sensorhub.api.command.ICommandStatus;
import org.sensorhub.api.command.IStreamingControlInterfaceWithResult;
import org.sensorhub.api.datastore.DataStoreException;
import org.sensorhub.impl.command.AbstractControlInterface;
import org.vast.swe.SWEHelper;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HexFormat;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.botts.impl.service.oscar.Constants.REPORT_BUCKET;

public class RequestReportControl extends AbstractControlInterface<OSCARSystem> implements IStreamingControlInterfaceWithResult {

    public static final String NAME = "requestReport";
    public static final String LABEL = "Request Report";
    public static final String DESCRIPTION = "Control to request operations, performance, and maintenance reports";

    public static final int RETRY_COUNT = 3;
    private static final int REPORT_WORKER_THREADS = 2;

    DataRecord commandStructure;
    DataComponent resultStructure;
    SWEHelper fac;

    OSCARServiceModule module;
    IBucketService bucketService;

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC);

    private final ExecutorService reportExecutor;
    private final ConcurrentHashMap<String, Object> fileWriteLocks = new ConcurrentHashMap<>();

    public RequestReportControl(OSCARSystem parent, OSCARServiceModule module) {
        super(NAME, parent);

        fac = new SWEHelper();
        this.module = module;
        this.bucketService = module.getBucketService();

        AtomicInteger workerSeq = new AtomicInteger(1);
        this.reportExecutor = Executors.newFixedThreadPool(REPORT_WORKER_THREADS, runnable -> {
            Thread thread = new Thread(runnable, "OSCAR-report-worker-" + workerSeq.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        });

        commandStructure = fac.createRecord()
                .name(NAME)
                .label(LABEL)
                .description(DESCRIPTION)
                .addField("reportType", fac.createCategory()
                        .label("Report Type")
                        .definition(SWEHelper.getPropertyUri("ReportType"))
                        .description("Type of report to request")
                        .addAllowedValues(ReportCmdType.class))
                .addField("startDateTime", fac.createTime()
                        .definition(SWEHelper.getPropertyUri("StartDateTime"))
                        .withIso8601Format()
                        .description("Start datetime (ISO 8601)"))
                .addField("endDateTime", fac.createTime()
                        .definition(SWEHelper.getPropertyUri("EndDateTime"))
                        .withIso8601Format()
                        .description("End datetime (ISO 8601)"))
                .addField("laneUID", fac.createText()
                        .label("Lane Unique Identifier")
                        .optional(true)
                        .definition(SWEHelper.getPropertyUri("LaneUID"))
                        .description("Identifier of the lane(s) to request"))
                .addField("eventType", fac.createCategory()
                        .label("Event Type")
                        .optional(true)
                        .definition(SWEHelper.getPropertyUri("EventType"))
                        .description("Identifier of the event requested")
                        .addAllowedValues(EventReportType.class))
                .build();

        resultStructure = fac.createRecord().name("result")
                .addField("reportPath", fac.createText())
                .build();

        try {
            if (!bucketService.getBucketStore().bucketExists(REPORT_BUCKET))
                bucketService.getBucketStore().createBucket(REPORT_BUCKET);
        } catch (DataStoreException e) {
            module.getLogger().error("Bucket already exists.", e);
        }
    }

    @Override
    public DataComponent getCommandDescription() {
        return commandStructure;
    }

    @Override
    public CompletableFuture<ICommandStatus> submitCommand(ICommandData command) {
        try {
            return CompletableFuture.supplyAsync(() -> executeReportCommand(command), reportExecutor);
        } catch (RejectedExecutionException rejected) {
            return CompletableFuture.completedFuture(
                    CommandStatus.failed(command.getID(), "Report worker pool is shutting down"));
        }
    }

    private ICommandStatus executeReportCommand(ICommandData command) {
        DataBlock paramData = command.getParams();

        ReportCmdType requestedType = ReportCmdType.valueOf(paramData.getStringValue(0));
        ReportCmdType type = requestedType.baseType();
        Instant start = paramData.getTimeStamp(1);
        Instant end = paramData.getTimeStamp(2);
        String laneUIDs = paramData.getStringValue(3);
        EventReportType eventType = parseEventType(paramData.getStringValue(4));
        ReportFormat format = requestedType.defaultFormat().effectiveFor(type);

        String resourceURI = null;
        String filePath = null;
        IReportHandler reportHandler = null;
        try {
            filePath = buildPath(type, start, end, laneUIDs, eventType, format);
            reportHandler = getReportHandler(type, start, end, laneUIDs, eventType, format);
        } catch (DataStoreException | RuntimeException e) {
            module.getLogger().error("Failed to build report {}", type, e);
        }

        for (int retryAttempt = 0; retryAttempt < RETRY_COUNT; retryAttempt++) {
            if (Thread.currentThread().isInterrupted()) {
                break;
            }
            try {
                resourceURI = handleReportGeneration(reportHandler, filePath);

                if (resourceURI != null) {
                    module.getLogger().info("Successfully generated report {}", resourceURI);
                    break;
                }
            } catch (DataStoreException | IOException e) {
                module.getLogger().error("Failed to build report {} (attempt {})", type, retryAttempt + 1, e);
            }
        }

        DataBlock resultData = resultStructure.createDataBlock();
        resultData.setStringValue(resourceURI);
        ICommandResult result = CommandResult.withData(resultData);

        return new CommandStatus.Builder()
                .withCommand(command.getID())
                .withStatusCode(resourceURI == null ? ICommandStatus.CommandStatusCode.FAILED : ICommandStatus.CommandStatusCode.ACCEPTED)
                .withResult(result)
                .build();
    }

    private static EventReportType parseEventType(String raw) {
        if (raw == null || raw.isBlank()) {
            return EventReportType.NONE;
        }
        try {
            return EventReportType.valueOf(raw.trim());
        } catch (IllegalArgumentException e) {
            return EventReportType.NONE;
        }
    }

    @Override
    public DataComponent getResultDescription() {
        return resultStructure;
    }

    public void requestStop() {
        reportExecutor.shutdownNow();
    }

    public void awaitStop(long timeout, TimeUnit unit) {
        try {
            reportExecutor.awaitTermination(Math.max(1L, timeout), unit);
        } catch (InterruptedException e) {
            reportExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private IReportHandler getReportHandler(
            ReportCmdType type,
            Instant start,
            Instant end,
            String laneUIDs,
            EventReportType eventType,
            ReportFormat format
    ) throws DataStoreException {
        if (type == ReportCmdType.LANE) {
            return out -> new LaneReport(out, start, end, laneUIDs, format, module);
        } else if (type == ReportCmdType.EVENT) {
            return out -> new EventReport(out, start, end, eventType, laneUIDs, format, module);
        } else if (type == ReportCmdType.ADJUDICATION) {
            return out -> new AdjudicationReport(out, start, end, laneUIDs, format, module);
        } else if (type == ReportCmdType.ADJUDICATION_DETAILS) {
            return out -> new AdjudicationDetailsReport(out, start, end, laneUIDs, format, module);
        } else if (type == ReportCmdType.VEHICLE_INCIDENT) {
            return out -> new VehicleIncidentReport(out, start, end, laneUIDs, format, module);
        } else if (type == ReportCmdType.RDS_SITE) {
            return out -> new RDSReport(out, start, end, laneUIDs, format, module);
        }
        return null;
    }

    private String handleReportGeneration(IReportHandler reportHandler, String filePath) throws DataStoreException, IOException {
        if (reportHandler == null || filePath == null || filePath.isBlank()) {
            return null;
        }
        Object fileLock = fileWriteLocks.computeIfAbsent(filePath, k -> new Object());
        synchronized (fileLock) {
            try (OutputStream out = bucketService.getBucketStore().putObject(REPORT_BUCKET, filePath, Collections.emptyMap())) {
                if (out == null) {
                    return null;
                }
                Report report = reportHandler.createReport(out);
                report.generate();
            }
        }
        return bucketService.getBucketStore().getRelativeResourceURI(REPORT_BUCKET, filePath);
    }

    String buildPath(ReportCmdType type, Instant start, Instant end, String laneUIDs, EventReportType eventType) {
        return buildPath(type, start, end, laneUIDs, eventType, ReportFormat.PDF);
    }

    String buildPath(
            ReportCmdType type,
            Instant start,
            Instant end,
            String laneUIDs,
            EventReportType eventType,
            ReportFormat format
    ) {
        ReportCmdType normalizedType = type.baseType();
        String startTime = DateTimeFormatter.ISO_INSTANT.format(start).replace(":", "-");
        String endTime = DateTimeFormatter.ISO_INSTANT.format(end).replace(":", "-");
        String scope = normalizedType == ReportCmdType.VEHICLE_INCIDENT
                ? buildVehicleIncidentScopeToken(laneUIDs)
                : buildScopeToken(laneUIDs);
        ReportFormat effectiveFormat = (format != null ? format : type.defaultFormat()).effectiveFor(normalizedType);

        String typeToken = (normalizedType == ReportCmdType.EVENT && eventType != null && eventType != EventReportType.NONE)
                ? normalizedType.name().toLowerCase(Locale.ROOT) + "-" + eventType.name().toLowerCase(Locale.ROOT)
                : normalizedType.name().toLowerCase(Locale.ROOT);

        return String.format(
                Locale.ROOT,
                "%s_%s_%s_%s_%s%s",
                module.getOSCARSystem().getNodeId(),
                typeToken,
                scope,
                startTime,
                endTime,
                effectiveFormat.extension()
        ).toLowerCase(Locale.ROOT);
    }

    private String buildVehicleIncidentScopeToken(String rawSpec) {
        VehicleIncidentAggregator.VehicleIncidentSpec parsed = VehicleIncidentAggregator.parseRequestSpec(rawSpec);
        String normVeh = parsed.normalizedVehicleId();
        String vehToken = normVeh.isEmpty()
                ? "all"
                : normVeh.substring(0, Math.min(16, normVeh.length())).toLowerCase(Locale.ROOT);
        String canonicalInput = rawSpec == null ? "" : rawSpec.trim();
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonicalInput.getBytes(StandardCharsets.UTF_8));
            return "veh-" + vehToken + "-" + HexFormat.of().formatHex(digest, 0, 6);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private String buildScopeToken(String laneUIDs) {
        if (laneUIDs == null || laneUIDs.isBlank() || laneUIDs.equals("NONE"))
            return "all";

        String canonicalScope = Arrays.stream(laneUIDs.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .sorted()
                .collect(Collectors.joining(","));
        if (canonicalScope.isEmpty())
            return "all";
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonicalScope.getBytes(StandardCharsets.UTF_8));
            return "scope-" + HexFormat.of().formatHex(digest, 0, 8);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
