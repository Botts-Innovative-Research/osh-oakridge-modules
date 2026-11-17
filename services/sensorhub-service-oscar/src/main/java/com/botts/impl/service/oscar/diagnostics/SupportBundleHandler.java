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

package com.botts.impl.service.oscar.diagnostics;

import com.botts.api.service.bucket.IObjectHandler;
import com.botts.impl.service.bucket.util.RequestContext;
import com.botts.impl.service.bucket.util.ServiceErrors;
import com.botts.impl.service.oscar.Constants;
import com.botts.impl.service.oscar.OSCARServiceConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.sensorhub.api.ISensorHub;
import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.ModuleEvent.ModuleState;
import org.sensorhub.api.system.ISystemGroupDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.StringWriter;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Authenticated, read-only {@link IObjectHandler} serving
 * {@code GET /sensorhub/buckets/reports/support-bundle}.
 *
 * <p>Generates a redacted, size-bounded diagnostic {@code .zip} bundle containing
 * module registry states, JVM diagnostics, filesystem headroom, sanitized SensorHub
 * configuration, and rolling application log tails without exposing credentials,
 * secret files, or private keys.</p>
 */
public final class SupportBundleHandler implements IObjectHandler {
    public static final String BUCKET_NAME = Constants.REPORT_BUCKET;
    public static final String OBJECT_KEY = "support-bundle";

    public static final int DEFAULT_TAIL_LINES = 2_500;
    public static final int MIN_TAIL_LINES = 100;
    public static final int MAX_TAIL_LINES = 10_000;
    public static final int MAX_ZIP_BYTES = 15 * 1024 * 1024;

    static final int MAX_PRIMARY_LOG_READ_BYTES = 8 * 1024 * 1024;
    static final int MAX_MODULE_LOG_FILES = 10;
    static final int MAX_MODULE_LOG_TAIL_LINES = 200;
    static final int MAX_MODULE_LOG_READ_BYTES = 256 * 1024;
    static final int MAX_CONFIG_FILE_BYTES = 4 * 1024 * 1024;
    static final int MAX_HIGHLIGHT_LINES = 25;
    static final int MAX_STACK_FRAMES = 30;

    private static final Pattern OBJECT_PATTERN = Pattern.compile("support-bundle(?:/.*)?");
    private static final Pattern SAFE_NAME_CHARS = Pattern.compile("[^A-Za-z0-9._-]+");
    private static final DateTimeFormatter FILE_TS_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'", Locale.ROOT).withZone(ZoneOffset.UTC);
    private static final Gson PRETTY_GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create();

    public record DiagnosticPaths(
            Path stateDir,
            Path logDir,
            Path configFile,
            Path archiveDir,
            Path readinessMarker
    ) {
        public static DiagnosticPaths defaults() {
            Path stateDir = Path.of(System.getProperty("oscar.state.dir", ".")).toAbsolutePath().normalize();
            String configuredLogDir = System.getProperty("OSCAR_LOG_DIR");
            Path logDir = (configuredLogDir != null && !configuredLogDir.isBlank())
                    ? Path.of(configuredLogDir).toAbsolutePath().normalize()
                    : stateDir.resolve("logs").normalize();
            Path configFile = stateDir.resolve("config.json").normalize();
            Path archiveDir = Path.of("/var/lib/oscar/archive");
            Path readinessMarker = Path.of("/tmp/oscar-ready");
            return new DiagnosticPaths(stateDir, logDir, configFile, archiveDir, readinessMarker);
        }
    }

    private final ISensorHub hub;
    private final OSCARServiceConfig oscarConfig;
    private final DiagnosticPaths paths;
    private final Clock clock;
    private final Logger logger;

    public SupportBundleHandler(ISensorHub hub,
                                OSCARServiceConfig oscarConfig,
                                Logger logger) {
        this(hub, oscarConfig, DiagnosticPaths.defaults(), Clock.systemUTC(), logger);
    }

    public SupportBundleHandler(ISensorHub hub,
                                OSCARServiceConfig oscarConfig,
                                DiagnosticPaths paths,
                                Clock clock,
                                Logger logger) {
        this.hub = Objects.requireNonNull(hub, "hub");
        this.oscarConfig = oscarConfig;
        this.paths = Objects.requireNonNull(paths, "paths");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.logger = logger != null ? logger : LoggerFactory.getLogger(SupportBundleHandler.class);
    }

    @Override
    public boolean canHandleRequest(HttpServletRequest request) {
        if (request == null || request.getPathInfo() == null)
            return false;
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
        if (perms == null)
            throw ServiceErrors.forbidden(ctx.getBucketName());
        sec.checkPermission(perms.get);

        requireTargetResource(ctx);
        int tailLines = parseTailLines(ctx.getRequest().getParameter("tailLines"));

        Instant now = clock.instant();
        byte[] zipBytes = buildBundleZip(now, tailLines);
        String filename = "oscar-backend-support-" + FILE_TS_FORMAT.format(now) + ".zip";

        HttpServletResponse resp = ctx.getResponse();
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("application/zip");
        resp.setContentLength(zipBytes.length);
        resp.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        resp.setHeader("Cache-Control", "no-store");
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("Cross-Origin-Resource-Policy", "same-origin");
        resp.getOutputStream().write(zipBytes);
        resp.getOutputStream().flush();
    }

    @Override
    public void doPost(RequestContext ctx) throws IOException {
        throw ServiceErrors.unsupportedOperation("The support-bundle diagnostic endpoint is read-only");
    }

    @Override
    public void doPut(RequestContext ctx) throws IOException {
        throw ServiceErrors.unsupportedOperation("The support-bundle diagnostic endpoint is read-only");
    }

    @Override
    public void doDelete(RequestContext ctx) throws IOException {
        throw ServiceErrors.unsupportedOperation("The support-bundle diagnostic endpoint is read-only");
    }

    private static void requireTargetResource(RequestContext ctx)
            throws com.botts.impl.service.bucket.util.InvalidRequestException {
        if (!BUCKET_NAME.equals(ctx.getBucketName()))
            throw ServiceErrors.notFound(ctx.getObjectKey());
        String key = ctx.getObjectKey() == null ? "" : ctx.getObjectKey().replace('\\', '/');
        if (!OBJECT_KEY.equals(key))
            throw ServiceErrors.notFound(key);
    }

    static int parseTailLines(String raw)
            throws com.botts.impl.service.bucket.util.InvalidRequestException {
        if (raw == null || raw.isBlank())
            return DEFAULT_TAIL_LINES;
        try {
            int parsed = Integer.parseInt(raw.trim());
            if (parsed < 1)
                throw ServiceErrors.badRequest("tailLines must be a positive integer");
            return Math.max(MIN_TAIL_LINES, Math.min(MAX_TAIL_LINES, parsed));
        } catch (NumberFormatException e) {
            throw ServiceErrors.badRequest("tailLines must be a valid integer");
        }
    }

    byte[] buildBundleZip(Instant generatedAt, int tailLines) throws IOException {
        SupportBundleRedactor.RedactionStats redactionStats = new SupportBundleRedactor.RedactionStats();
        Map<String, byte[]> entries = new LinkedHashMap<>();

        // 1. Module registry snapshot (top-level + nested lane/system members)
        ModuleDiagnosticsResult moduleResult = collectModuleDiagnostics(redactionStats);
        entries.put("backend/modules.json", jsonBytes(moduleResult.modulesPayload));

        // 2. JVM runtime, memory, GC, and deadlock/blocked thread diagnostics
        Map<String, Object> jvmDiagnostics = collectJvmDiagnostics(redactionStats);
        entries.put("backend/jvm-diagnostics.json", jsonBytes(jvmDiagnostics));

        // 3. Storage & readiness summary
        Map<String, Object> storageSummary = collectStorageDiagnostics();
        entries.put("backend/storage-summary.json", jsonBytes(storageSummary));

        // 4. Sanitized SensorHub config.json (never reads secret files or keystores)
        String runtimeOscarVersion = resolveDefaultOscarVersion();
        String configOriginVersion = null;
        JsonElement redactedConfig = collectRedactedConfig(redactionStats);
        if (redactedConfig != null) {
            String detectedDeployment = extractDeploymentVersion(redactedConfig);
            if (detectedDeployment != null && !detectedDeployment.isBlank()) {
                configOriginVersion = detectedDeployment;
            }
            entries.put("backend/config.redacted.json", jsonBytes(redactedConfig));
        }
        String oscarVersion = (!"unknown".equals(runtimeOscarVersion) && !"local".equals(runtimeOscarVersion))
                ? runtimeOscarVersion
                : (configOriginVersion != null ? configOriginVersion : runtimeOscarVersion);

        // 5. Bounded rolling log tail + recent per-module logs
        LogCaptureResult logCapture = collectLogs(tailLines, redactionStats);
        entries.put("backend/oscar.log", logCapture.primaryLogText.getBytes(StandardCharsets.UTF_8));
        for (Map.Entry<String, String> modLog : logCapture.moduleLogs.entrySet()) {
            entries.put("backend/module-logs/" + modLog.getKey(),
                    modLog.getValue().getBytes(StandardCharsets.UTF_8));
        }

        // 6. Redaction report & top-level summary.json (written first in ZIP)
        Map<String, Object> redactionReport = buildRedactionReport(generatedAt, redactionStats);
        Map<String, Object> summary = buildSummary(
                generatedAt,
                oscarVersion,
                configOriginVersion,
                tailLines,
                moduleResult,
                jvmDiagnostics,
                storageSummary,
                logCapture,
                redactionStats);

        ByteArrayOutputStream rawBuffer = new ByteArrayOutputStream(128 * 1024);
        long epochMillis = generatedAt.toEpochMilli();
        try (ZipOutputStream zos = new ZipOutputStream(rawBuffer, StandardCharsets.UTF_8)) {
            zos.setLevel(Deflater.DEFAULT_COMPRESSION);
            writeZipEntry(zos, "summary.json", jsonBytes(summary), epochMillis);
            writeZipEntry(zos, "redaction-report.json", jsonBytes(redactionReport), epochMillis);
            for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                if (rawBuffer.size() + entry.getValue().length / 2 > MAX_ZIP_BYTES - 65_536) {
                    logger.warn("Support bundle reached size cap; omitting remaining entry {}", entry.getKey());
                    break;
                }
                writeZipEntry(zos, entry.getKey(), entry.getValue(), epochMillis);
            }
        }
        byte[] zipBytes = rawBuffer.toByteArray();
        if (zipBytes.length > MAX_ZIP_BYTES) {
            throw ServiceErrors.internalError("Support bundle exceeded maximum archive size bound");
        }
        return zipBytes;
    }

    private static void writeZipEntry(ZipOutputStream zos,
                                      String name,
                                      byte[] content,
                                      long epochMillis) throws IOException {
        ZipEntry entry = new ZipEntry(name);
        entry.setTime(epochMillis);
        zos.putNextEntry(entry);
        zos.write(content);
        zos.closeEntry();
    }

    private static byte[] jsonBytes(Object obj) {
        return (PRETTY_GSON.toJson(obj) + "\n").getBytes(StandardCharsets.UTF_8);
    }

    private record ModuleDiagnosticsResult(
            Map<String, Object> modulesPayload,
            Map<String, Object> summaryCounts,
            List<Map<String, Object>> faultedModules
    ) {}

    private ModuleDiagnosticsResult collectModuleDiagnostics(SupportBundleRedactor.RedactionStats stats) {
        List<Map<String, Object>> moduleEntries = new ArrayList<>();
        List<Map<String, Object>> faultedModules = new ArrayList<>();
        Map<String, Integer> stateCounts = new LinkedHashMap<>();

        Collection<IModule<?>> loaded = hub.getModuleRegistry() != null
                ? hub.getModuleRegistry().getLoadedModules()
                : List.of();
        List<IModule<?>> sortedRoots = new ArrayList<>(loaded);
        sortedRoots.sort(Comparator.comparing(m -> safeString(m.getLocalID()), String.CASE_INSENSITIVE_ORDER));

        Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        for (IModule<?> module : sortedRoots) {
            appendModuleRecursive(module, null, visited, moduleEntries, faultedModules, stateCounts, stats);
        }

        Map<String, Object> summaryCounts = new LinkedHashMap<>();
        summaryCounts.put("total", moduleEntries.size());
        summaryCounts.put("byState", stateCounts);
        summaryCounts.put("faultedCount", faultedModules.size());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("schemaVersion", 1);
        payload.put("counts", summaryCounts);
        payload.put("modules", moduleEntries);
        return new ModuleDiagnosticsResult(payload, summaryCounts, faultedModules);
    }

    private void appendModuleRecursive(IModule<?> module,
                                       String parentId,
                                       Set<Object> visited,
                                       List<Map<String, Object>> out,
                                       List<Map<String, Object>> faulted,
                                       Map<String, Integer> stateCounts,
                                       SupportBundleRedactor.RedactionStats stats) {
        if (module == null || !visited.add(module))
            return;

        String id = safeString(module.getLocalID());
        String name = safeString(module.getName());
        String moduleClass = module.getClass().getName();
        boolean autoStart = module.getConfiguration() != null && module.getConfiguration().autoStart;
        ModuleState state = module.getCurrentState();
        String stateLabel = state != null ? state.name() : "UNKNOWN";
        stateCounts.merge(stateLabel, 1, Integer::sum);

        String statusMessage = SupportBundleRedactor.redactText(safeString(module.getStatusMessage()), stats);
        Throwable currentError = module.getCurrentError();
        String errorMessage = null;
        String errorStack = null;
        if (currentError != null) {
            errorMessage = SupportBundleRedactor.redactText(
                    currentError.getClass().getName() + ": " + safeString(currentError.getMessage()),
                    stats);
            errorStack = SupportBundleRedactor.redactText(boundedStackTrace(currentError), stats);
        }

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", id);
        item.put("name", name);
        item.put("moduleClass", moduleClass);
        if (parentId != null)
            item.put("parentId", parentId);
        item.put("autoStart", autoStart);
        item.put("state", stateLabel);
        if (!statusMessage.isBlank())
            item.put("statusMessage", statusMessage);
        if (errorMessage != null)
            item.put("error", errorMessage);
        if (errorStack != null)
            item.put("errorStack", errorStack);
        out.add(item);

        boolean unexpectedAutoStartStop = autoStart && parentId == null && state != ModuleState.STARTED;
        if (currentError != null || unexpectedAutoStartStop) {
            Map<String, Object> fault = new LinkedHashMap<>();
            fault.put("id", id);
            fault.put("name", name);
            fault.put("moduleClass", moduleClass);
            if (parentId != null)
                fault.put("parentId", parentId);
            fault.put("autoStart", autoStart);
            fault.put("state", stateLabel);
            if (errorMessage != null)
                fault.put("error", errorMessage);
            else if (!statusMessage.isBlank())
                fault.put("statusMessage", statusMessage);
            faulted.add(fault);
        }

        if (module instanceof ISystemGroupDriver<?> group && group.getMembers() != null) {
            List<IModule<?>> children = new ArrayList<>();
            for (Object member : group.getMembers().values()) {
                if (member instanceof IModule<?> childModule)
                    children.add(childModule);
            }
            children.sort(Comparator.comparing(m -> safeString(m.getLocalID()), String.CASE_INSENSITIVE_ORDER));
            for (IModule<?> child : children) {
                appendModuleRecursive(child, id, visited, out, faulted, stateCounts, stats);
            }
        }
    }

    private Map<String, Object> collectJvmDiagnostics(SupportBundleRedactor.RedactionStats stats) {
        Map<String, Object> jvm = new LinkedHashMap<>();
        RuntimeMXBean runtimeMx = ManagementFactory.getRuntimeMXBean();
        MemoryMXBean memoryMx = ManagementFactory.getMemoryMXBean();
        ThreadMXBean threadMx = ManagementFactory.getThreadMXBean();

        Map<String, Object> runtimeInfo = new LinkedHashMap<>();
        runtimeInfo.put("javaVersion", System.getProperty("java.version", ""));
        runtimeInfo.put("javaVendor", System.getProperty("java.vendor", ""));
        runtimeInfo.put("vmName", runtimeMx.getVmName());
        runtimeInfo.put("vmVersion", runtimeMx.getVmVersion());
        runtimeInfo.put("pid", ProcessHandle.current().pid());
        runtimeInfo.put("startTime", Instant.ofEpochMilli(runtimeMx.getStartTime()).toString());
        runtimeInfo.put("uptimeMs", runtimeMx.getUptime());
        runtimeInfo.put("availableProcessors", Runtime.getRuntime().availableProcessors());
        List<String> redactedArgs = new ArrayList<>();
        for (String arg : runtimeMx.getInputArguments()) {
            redactedArgs.add(SupportBundleRedactor.redactText(arg, stats));
        }
        runtimeInfo.put("jvmArguments", redactedArgs);
        jvm.put("runtime", runtimeInfo);

        Map<String, Object> osInfo = new LinkedHashMap<>();
        osInfo.put("name", System.getProperty("os.name", ""));
        osInfo.put("arch", System.getProperty("os.arch", ""));
        osInfo.put("version", System.getProperty("os.version", ""));
        jvm.put("os", osInfo);

        Map<String, Object> memoryInfo = new LinkedHashMap<>();
        memoryInfo.put("heap", memoryUsageMap(memoryMx.getHeapMemoryUsage()));
        memoryInfo.put("nonHeap", memoryUsageMap(memoryMx.getNonHeapMemoryUsage()));
        List<Map<String, Object>> pools = new ArrayList<>();
        for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("name", pool.getName());
            p.put("type", pool.getType().name());
            p.put("usage", memoryUsageMap(pool.getUsage()));
            pools.add(p);
        }
        memoryInfo.put("pools", pools);
        jvm.put("memory", memoryInfo);

        List<Map<String, Object>> gcList = new ArrayList<>();
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            Map<String, Object> g = new LinkedHashMap<>();
            g.put("name", gc.getName());
            g.put("collectionCount", gc.getCollectionCount());
            g.put("collectionTimeMs", gc.getCollectionTime());
            gcList.add(g);
        }
        jvm.put("garbageCollectors", gcList);

        Map<String, Object> threads = new LinkedHashMap<>();
        threads.put("threadCount", threadMx.getThreadCount());
        threads.put("daemonThreadCount", threadMx.getDaemonThreadCount());
        threads.put("peakThreadCount", threadMx.getPeakThreadCount());

        Map<Thread.State, Integer> byState = new EnumMap<>(Thread.State.class);
        for (Thread.State s : Thread.State.values())
            byState.put(s, 0);

        long[] deadlockedIds = threadMx.findDeadlockedThreads();
        threads.put("deadlockedThreadCount", deadlockedIds != null ? deadlockedIds.length : 0);

        List<Map<String, Object>> contendedThreads = new ArrayList<>();
        ThreadInfo[] infos = threadMx.dumpAllThreads(false, false);
        for (ThreadInfo info : infos) {
            if (info == null)
                continue;
            byState.merge(info.getThreadState(), 1, Integer::sum);
            boolean isDeadlocked = containsId(deadlockedIds, info.getThreadId());
            String tName = info.getThreadName();
            boolean isActiveLifecycleWorker = tName != null
                    && tName.startsWith("ModuleRegistry-")
                    && info.getStackTrace() != null
                    && info.getStackTrace().length > 0
                    && !info.getStackTrace()[0].getClassName().startsWith("jdk.internal.misc.Unsafe");
            if (isDeadlocked || info.getThreadState() == Thread.State.BLOCKED || isActiveLifecycleWorker) {
                if (contendedThreads.size() < 20) {
                    Map<String, Object> t = new LinkedHashMap<>();
                    t.put("threadId", info.getThreadId());
                    t.put("threadName", info.getThreadName());
                    t.put("state", info.getThreadState().name());
                    t.put("deadlocked", isDeadlocked);
                    if (info.getLockName() != null)
                        t.put("lockName", info.getLockName());
                    if (info.getLockOwnerName() != null)
                        t.put("lockOwnerName", info.getLockOwnerName());
                    List<String> frames = new ArrayList<>();
                    StackTraceElement[] stack = info.getStackTrace();
                    for (int i = 0; i < Math.min(stack.length, MAX_STACK_FRAMES); i++) {
                        frames.add(stack[i].toString());
                    }
                    t.put("stack", frames);
                    contendedThreads.add(t);
                }
            }
        }
        threads.put("byState", byState);
        threads.put("blockedOrDeadlockedThreads", contendedThreads);
        jvm.put("threads", threads);
        return jvm;
    }

    private static boolean containsId(long[] ids, long target) {
        if (ids == null)
            return false;
        for (long id : ids) {
            if (id == target)
                return true;
        }
        return false;
    }

    private static Map<String, Object> memoryUsageMap(MemoryUsage usage) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (usage == null)
            return map;
        map.put("initBytes", usage.getInit());
        map.put("usedBytes", usage.getUsed());
        map.put("committedBytes", usage.getCommitted());
        map.put("maxBytes", usage.getMax());
        return map;
    }

    private Map<String, Object> collectStorageDiagnostics() {
        Map<String, Object> storage = new LinkedHashMap<>();
        storage.put("stateDir", describeFilesystemPath(paths.stateDir()));
        storage.put("filesDir", describeFilesystemPath(paths.stateDir().resolve("files")));
        storage.put("logDir", describeFilesystemPath(paths.logDir()));
        storage.put("archiveDir", describeFilesystemPath(paths.archiveDir()));

        Map<String, Object> readiness = new LinkedHashMap<>();
        Path marker = paths.readinessMarker();
        boolean readyExists = marker != null && Files.isRegularFile(marker);
        readiness.put("markerPath", marker != null ? marker.toString() : "");
        readiness.put("ready", readyExists);
        if (readyExists) {
            try {
                readiness.put("lastModified", Files.getLastModifiedTime(marker).toInstant().toString());
            } catch (IOException ignored) {
                // Best-effort diagnostic metadata.
            }
        }
        storage.put("applicationReadiness", readiness);
        return storage;
    }

    private static Map<String, Object> describeFilesystemPath(Path path) {
        Map<String, Object> info = new LinkedHashMap<>();
        if (path == null) {
            info.put("exists", false);
            return info;
        }
        info.put("path", path.toString());
        boolean exists = Files.exists(path);
        info.put("exists", exists);
        if (!exists)
            return info;
        try {
            FileStore store = Files.getFileStore(path);
            long total = store.getTotalSpace();
            long usable = store.getUsableSpace();
            info.put("totalBytes", total);
            info.put("usableBytes", usable);
            if (total > 0L) {
                double usedPct = ((double) (total - usable) / (double) total) * 100.0;
                info.put("usedPercent", Math.round(usedPct * 100.0) / 100.0);
            }
        } catch (IOException e) {
            info.put("error", e.getMessage());
        }
        return info;
    }

    private JsonElement collectRedactedConfig(SupportBundleRedactor.RedactionStats stats) {
        Path configPath = paths.configFile();
        if (configPath == null || !Files.isRegularFile(configPath, LinkOption.NOFOLLOW_LINKS))
            return null;
        try {
            long size = Files.size(configPath);
            if (size <= 0L || size > MAX_CONFIG_FILE_BYTES) {
                JsonObject placeholder = new JsonObject();
                placeholder.addProperty("skippedReason",
                        "Configuration file size (" + size + " bytes) is outside diagnostic bounds");
                return placeholder;
            }
            String rawJson = Files.readString(configPath, StandardCharsets.UTF_8);
            JsonElement parsed = JsonParser.parseString(rawJson);
            return SupportBundleRedactor.redactJson(parsed, stats);
        } catch (Exception e) {
            JsonObject fallback = new JsonObject();
            fallback.addProperty("parseError",
                    SupportBundleRedactor.redactText(safeString(e.getMessage()), stats));
            return fallback;
        }
    }

    private record LogCaptureResult(
            String primaryLogText,
            int totalTailLinesCaptured,
            int errorLineCount,
            int warnLineCount,
            List<String> recentHighlights,
            Map<String, String> moduleLogs
    ) {}

    private LogCaptureResult collectLogs(int tailLines, SupportBundleRedactor.RedactionStats stats) {
        Path primaryLog = paths.logDir() != null ? paths.logDir().resolve("oscar.log") : null;
        List<String> primaryLines = List.of();
        if (primaryLog != null && Files.isRegularFile(primaryLog, LinkOption.NOFOLLOW_LINKS)) {
            try {
                primaryLines = readLastLinesUtf8(primaryLog, tailLines, MAX_PRIMARY_LOG_READ_BYTES);
            } catch (IOException e) {
                primaryLines = List.of("[support-bundle] Could not read oscar.log: " + safeString(e.getMessage()));
            }
        }

        StringBuilder primaryBuilder = new StringBuilder(Math.max(256, primaryLines.size() * 96));
        int errorCount = 0;
        int warnCount = 0;
        Deque<String> highlights = new ArrayDeque<>(MAX_HIGHLIGHT_LINES);

        if (primaryLines.isEmpty()) {
            primaryBuilder.append("# No entries found at ")
                    .append(primaryLog != null ? primaryLog : "logs/oscar.log")
                    .append('\n');
        } else {
            for (String rawLine : primaryLines) {
                String sanitized = SupportBundleRedactor.redactText(rawLine, stats);
                primaryBuilder.append(sanitized).append('\n');
                if (isErrorLine(sanitized)) {
                    errorCount++;
                    pushHighlight(highlights, sanitized);
                } else if (isWarnLine(sanitized)) {
                    warnCount++;
                    pushHighlight(highlights, sanitized);
                }
            }
        }

        Map<String, String> moduleLogs = collectRecentModuleLogs(stats, highlights);
        return new LogCaptureResult(
                primaryBuilder.toString(),
                primaryLines.size(),
                errorCount,
                warnCount,
                new ArrayList<>(highlights),
                moduleLogs);
    }

    private Map<String, String> collectRecentModuleLogs(SupportBundleRedactor.RedactionStats stats,
                                                        Deque<String> highlights) {
        Map<String, String> result = new LinkedHashMap<>();
        if (paths.stateDir() == null)
            return result;
        Path moduleDataDir = paths.stateDir().resolve(".moduledata");
        if (!Files.isDirectory(moduleDataDir, LinkOption.NOFOLLOW_LINKS))
            return result;

        record CandidateLog(Path path, String safeName, long lastModifiedMs) {}
        List<CandidateLog> candidates = new ArrayList<>();
        try (Stream<Path> children = Files.list(moduleDataDir)) {
            children.forEach(child -> {
                if (!Files.isDirectory(child, LinkOption.NOFOLLOW_LINKS))
                    return;
                Path logTxt = child.resolve("log.txt");
                if (!Files.isRegularFile(logTxt, LinkOption.NOFOLLOW_LINKS))
                    return;
                try {
                    long mtime = Files.getLastModifiedTime(logTxt, LinkOption.NOFOLLOW_LINKS).toMillis();
                    String dirName = child.getFileName().toString();
                    String cleanName = SAFE_NAME_CHARS.matcher(dirName).replaceAll("_");
                    if (cleanName.isBlank() || ".".equals(cleanName))
                        cleanName = "root";
                    candidates.add(new CandidateLog(logTxt, cleanName + ".log", mtime));
                } catch (IOException ignored) {
                    // Ignore unreadable module log candidate.
                }
            });
        } catch (IOException ignored) {
            return result;
        }

        candidates.sort(Comparator.comparingLong(CandidateLog::lastModifiedMs).reversed());
        int count = Math.min(candidates.size(), MAX_MODULE_LOG_FILES);
        for (int i = 0; i < count; i++) {
            CandidateLog candidate = candidates.get(i);
            try {
                List<String> lines = readLastLinesUtf8(
                        candidate.path(), MAX_MODULE_LOG_TAIL_LINES, MAX_MODULE_LOG_READ_BYTES);
                if (lines.isEmpty())
                    continue;
                StringBuilder sb = new StringBuilder(lines.size() * 80);
                for (String rawLine : lines) {
                    String sanitized = SupportBundleRedactor.redactText(rawLine, stats);
                    sb.append(sanitized).append('\n');
                    if (isErrorLine(sanitized)) {
                        pushHighlight(highlights, "[" + candidate.safeName() + "] " + sanitized);
                    }
                }
                result.put(candidate.safeName(), sb.toString());
            } catch (IOException ignored) {
                // Best-effort module log collection.
            }
        }
        return result;
    }

    private static void pushHighlight(Deque<String> highlights, String line) {
        if (line == null || line.isBlank())
            return;
        String trimmed = line.length() > 400 ? line.substring(0, 400) + "..." : line;
        if (highlights.size() >= MAX_HIGHLIGHT_LINES)
            highlights.removeFirst();
        highlights.addLast(trimmed);
    }

    private static boolean isErrorLine(String line) {
        return line.contains(" ERROR ") || line.startsWith("ERROR") || line.contains("Exception:");
    }

    private static boolean isWarnLine(String line) {
        return line.contains(" WARN ") || line.startsWith("WARN");
    }

    static List<String> readLastLinesUtf8(Path file, int maxLines, int maxBytes) throws IOException {
        if (maxLines <= 0 || maxBytes <= 0 || !Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS))
            return List.of();
        byte[] tailBytes;
        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "r")) {
            long length = raf.length();
            if (length <= 0L)
                return List.of();
            int toRead = (int) Math.min(length, (long) maxBytes);
            long startOffset = length - toRead;
            raf.seek(startOffset);
            tailBytes = new byte[toRead];
            raf.readFully(tailBytes);
            String chunk = new String(tailBytes, StandardCharsets.UTF_8);
            String[] rawLines = chunk.split("\\r?\\n", -1);
            int firstIdx = startOffset > 0L && rawLines.length > 1 ? 1 : 0;
            int lastExclusive = rawLines.length;
            if (lastExclusive > firstIdx && rawLines[lastExclusive - 1].isEmpty())
                lastExclusive--;
            int available = Math.max(0, lastExclusive - firstIdx);
            int takeFrom = Math.max(firstIdx, lastExclusive - maxLines);
            List<String> lines = new ArrayList<>(Math.min(available, maxLines));
            for (int i = takeFrom; i < lastExclusive; i++) {
                lines.add(rawLines[i]);
            }
            return lines;
        }
    }

    private static Map<String, Object> buildRedactionReport(Instant generatedAt,
                                                            SupportBundleRedactor.RedactionStats stats) {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("schemaVersion", 1);
        report.put("generatedAt", generatedAt.toString());
        report.put("policy", "automatic-edge-redaction");
        report.put("neverCollected", List.of(
                "/run/secrets/*",
                "secrets/*",
                "tls/*.key",
                ".keystore/*",
                "hivemq-data/*",
                ".moduledata/oscar-identity.json",
                "remote-access/*"
        ));
        Map<String, Object> counts = new LinkedHashMap<>();
        counts.put("jsonFieldsRedacted", stats.jsonFieldsRedacted());
        counts.put("uriCredentialsRedacted", stats.uriCredentialsRedacted());
        counts.put("authHeadersRedacted", stats.authHeadersRedacted());
        counts.put("cookiesRedacted", stats.cookiesRedacted());
        counts.put("querySecretsRedacted", stats.querySecretsRedacted());
        counts.put("keyValueSecretsRedacted", stats.keyValueSecretsRedacted());
        counts.put("pemBlocksRedacted", stats.pemBlocksRedacted());
        counts.put("emailsMasked", stats.emailsMasked());
        counts.put("totalRedactions", stats.totalRedactions());
        report.put("counts", counts);
        return report;
    }

    private Map<String, Object> buildSummary(Instant generatedAt,
                                             String oscarVersion,
                                             String configOriginVersion,
                                             int tailLinesRequested,
                                             ModuleDiagnosticsResult moduleResult,
                                             Map<String, Object> jvmDiagnostics,
                                             Map<String, Object> storageSummary,
                                             LogCaptureResult logCapture,
                                             SupportBundleRedactor.RedactionStats redactionStats) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("schemaVersion", 1);
        summary.put("bundleType", "oscar-support-bundle");
        summary.put("bundleSource", "backend");
        summary.put("generatedAt", generatedAt.toString());
        summary.put("oscarVersion", oscarVersion);
        if (configOriginVersion != null && !configOriginVersion.equals(oscarVersion))
            summary.put("configOriginVersion", configOriginVersion);
        summary.put("nodeId", oscarConfig != null && oscarConfig.nodeId != null
                ? oscarConfig.nodeId : "default");

        summary.put("modules", moduleResult.summaryCounts);
        summary.put("faultedModules", moduleResult.faultedModules);
        summary.put("storage", storageSummary);

        Map<String, Object> jvmQuick = new LinkedHashMap<>();
        jvmQuick.put("runtime", jvmDiagnostics.get("runtime"));
        jvmQuick.put("os", jvmDiagnostics.get("os"));
        Map<?, ?> mem = (Map<?, ?>) jvmDiagnostics.get("memory");
        if (mem != null)
            jvmQuick.put("heap", mem.get("heap"));
        Map<?, ?> threads = (Map<?, ?>) jvmDiagnostics.get("threads");
        if (threads != null) {
            jvmQuick.put("threadCount", threads.get("threadCount"));
            jvmQuick.put("deadlockedThreadCount", threads.get("deadlockedThreadCount"));
        }
        summary.put("jvm", jvmQuick);

        Map<String, Object> logsSummary = new LinkedHashMap<>();
        logsSummary.put("tailLinesRequested", tailLinesRequested);
        logsSummary.put("primaryLinesCaptured", logCapture.totalTailLinesCaptured);
        logsSummary.put("moduleLogFilesCaptured", logCapture.moduleLogs.size());
        logsSummary.put("errorLineCount", logCapture.errorLineCount);
        logsSummary.put("warnLineCount", logCapture.warnLineCount);
        logsSummary.put("recentHighlights", logCapture.recentHighlights);
        summary.put("logs", logsSummary);

        Map<String, Object> redactionQuick = new LinkedHashMap<>();
        redactionQuick.put("applied", true);
        redactionQuick.put("totalRedactions", redactionStats.totalRedactions());
        summary.put("redaction", redactionQuick);
        return summary;
    }

    private static String resolveDefaultOscarVersion() {
        String propVersion = System.getProperty("oscar.version");
        if (propVersion != null && !propVersion.isBlank())
            return propVersion.trim();
        String envVersion = System.getenv("OSCAR_VERSION");
        if (envVersion != null && !envVersion.isBlank())
            return envVersion.trim();
        return "unknown";
    }

    private static String extractDeploymentVersion(JsonElement configRoot) {
        if (configRoot == null || !configRoot.isJsonArray())
            return null;
        for (JsonElement elem : configRoot.getAsJsonArray()) {
            if (!elem.isJsonObject())
                continue;
            JsonObject obj = elem.getAsJsonObject();
            JsonElement deploymentName = obj.get("deploymentName");
            if (deploymentName != null && deploymentName.isJsonPrimitive()) {
                String text = deploymentName.getAsString().trim();
                if (text.startsWith("OSCAR "))
                    return text.substring("OSCAR ".length()).trim();
                if (!text.isEmpty())
                    return text;
            }
        }
        return null;
    }

    private static String boundedStackTrace(Throwable error) {
        StringWriter sw = new StringWriter(512);
        try (PrintWriter pw = new PrintWriter(sw)) {
            error.printStackTrace(pw);
        }
        String[] lines = sw.toString().split("\\r?\\n");
        if (lines.length <= MAX_STACK_FRAMES)
            return sw.toString().trim();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < MAX_STACK_FRAMES; i++) {
            sb.append(lines[i]).append('\n');
        }
        sb.append("\t... (").append(lines.length - MAX_STACK_FRAMES).append(" more lines truncated)");
        return sb.toString();
    }

    private static String safeString(String value) {
        return value != null ? value : "";
    }
}
