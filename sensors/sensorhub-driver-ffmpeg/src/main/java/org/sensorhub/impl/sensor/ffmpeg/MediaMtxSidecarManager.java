package org.sensorhub.impl.sensor.ffmpeg;

import org.sensorhub.api.common.SensorHubException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Transparent loopback supervisor for an embedded {@code mediamtx} RTSP proxy process.
 *
 * <p>All camera configurations in OSCAR ({@code config.json}, SensorHub Admin UI, and CSV
 * imports/exports) remain real upstream camera URLs. When an {@code rtsp://} or {@code rtsps://}
 * camera stream is opened and the {@code mediamtx} binary is available, this manager:</p>
 * <ul>
 *   <li>Starts a single {@code mediamtx} process bound strictly to {@code 127.0.0.1} with
 *       per-boot 256-bit random credentials for both its Control API and RTSP reader port.</li>
 *   <li>Normalizes upstream RTSP URLs (collapsing accidental leading {@code //} stream paths that
 *       corrupt LIVE555 {@code CSeq} state on cameras such as Reolink).</li>
 *   <li>Deduplicates identical upstream streams across sensors on the same node using lease-based
 *       reference counting so physical cameras never receive duplicate sockets from one server.</li>
 *   <li>Immediately removes unused paths via the Control API so {@code mediamtx} sends a clean
 *       RTSP {@code TEARDOWN} when a camera is stopped or reconfigured.</li>
 *   <li>Verifies upstream {@code ready=true} state before handing the local loopback URL to FFmpeg
 *       so offline or unauthorized cameras truthfully report {@code Offline}.</li>
 *   <li>Automatically respawns {@code mediamtx} and restores all active leased paths if the child
 *       process ever exits unexpectedly.</li>
 * </ul>
 */
public final class MediaMtxSidecarManager implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(MediaMtxSidecarManager.class);

    public static final String PROP_ENABLED = "oscar.mediamtx.enabled";
    public static final String ENV_ENABLED = "OSCAR_MEDIAMTX_ENABLED";
    public static final String PROP_BIN = "oscar.mediamtx.bin";
    public static final String ENV_BIN = "OSCAR_MEDIAMTX_BIN";
    public static final String PROP_RTSP_PORT = "oscar.mediamtx.rtspPort";
    public static final String ENV_RTSP_PORT = "OSCAR_MEDIAMTX_RTSP_PORT";
    public static final String PROP_API_PORT = "oscar.mediamtx.apiPort";
    public static final String ENV_API_PORT = "OSCAR_MEDIAMTX_API_PORT";

    private static final String DEFAULT_BINARY_PATH = "/usr/local/bin/mediamtx";
    private static final int PREFERRED_RTSP_PORT = 18554;
    private static final int PREFERRED_API_PORT = 19997;
    private static final String LOOPBACK_HOST = "127.0.0.1";
    private static final String API_USER = "oscar-api";
    private static final String RTSP_USER = "oscar-rtsp";
    private static final long STARTUP_TIMEOUT_MS = 5_000L;
    private static final long MIN_READY_WAIT_MS = 1_000L;
    private static final long MAX_READY_WAIT_MS = 15_000L;
    private static final long READY_POLL_INTERVAL_MS = 100L;
    private static final long WATCHDOG_INTERVAL_SECONDS = 5L;

    private static final Pattern CREDENTIAL_REDACTION_PATTERN =
            Pattern.compile("(?i)(rtsps?://)([^/@\\s]+)@");
    private static final Pattern READY_TRUE_PATTERN =
            Pattern.compile("\"ready\"\\s*:\\s*true\\b");

    private static final MediaMtxSidecarManager INSTANCE = new MediaMtxSidecarManager(null, 0, 0, true);

    public static MediaMtxSidecarManager getInstance() {
        return INSTANCE;
    }

    public record NormalizedRtspTarget(String sourceUrl, String canonicalKey, String host, int port) {}

    private static final class PathEntry {
        final String pathName;
        final String canonicalKey;
        String sourceUrl;
        final Set<String> holders = new HashSet<>();
        long registeredGeneration = -1L;

        PathEntry(String pathName, String canonicalKey, String sourceUrl) {
            this.pathName = pathName;
            this.canonicalKey = canonicalKey;
            this.sourceUrl = sourceUrl;
        }
    }

    private record LeaseRegistration(String pathName, String sourceUrl, int rtspPort, String rtspSecret) {
        String localRtspUrl() {
            return "rtsp://" + RTSP_USER + ":" + rtspSecret + "@" + LOOPBACK_HOST + ":" + rtspPort + "/" + pathName;
        }
    }

    private final Object lock = new Object();
    private final Path configuredBinaryOverride;
    private final int preferredRtspPortOverride;
    private final int preferredApiPortOverride;
    private final HttpClient httpClient;
    private final SecureRandom secureRandom = new SecureRandom();
    private final AtomicBoolean bypassLogged = new AtomicBoolean(false);

    private final Map<String, PathEntry> pathsByKey = new HashMap<>();
    private final Map<String, String> leaseToKey = new HashMap<>();

    private Process process;
    private long processGeneration = 0L;
    private int activeRtspPort = -1;
    private int activeApiPort = -1;
    private String apiSecret;
    private String rtspSecret;
    private String basicAuthHeader;
    private Path runtimeDir;
    private Path configFile;
    private ScheduledExecutorService watchdogExecutor;
    private ScheduledFuture<?> watchdogFuture;
    private boolean closed = false;

    MediaMtxSidecarManager(Path binaryOverride, int rtspPortOverride, int apiPortOverride, boolean registerShutdownHook) {
        this.configuredBinaryOverride = binaryOverride;
        this.preferredRtspPortOverride = rtspPortOverride;
        this.preferredApiPortOverride = apiPortOverride;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
        if (registerShutdownHook) {
            Runtime.getRuntime().addShutdownHook(new Thread(this::close, "oscar-mediamtx-shutdown"));
        }
    }

    /**
     * Acquires or refreshes a reference-counted local MediaMTX stream lease for an RTSP URL.
     * Non-RTSP strings, loopback self-references, or environments without {@code mediamtx}
     * transparently return the normalized source string.
     */
    public String acquireStream(String leaseId, String rawConnectionString, long connectTimeoutMs)
            throws SensorHubException {
        Objects.requireNonNull(leaseId, "leaseId");
        if (rawConnectionString == null || rawConnectionString.isBlank()) {
            return rawConnectionString;
        }

        Optional<NormalizedRtspTarget> parsed = parseAndNormalizeRtspUrl(rawConnectionString);
        if (parsed.isEmpty()) {
            return rawConnectionString;
        }

        NormalizedRtspTarget target = parsed.get();
        if (!isEnabled()) {
            return target.sourceUrl();
        }

        Optional<Path> binary = resolveBinaryPath();
        if (binary.isEmpty()) {
            if (bypassLogged.compareAndSet(false, true)) {
                logger.info("MediaMTX binary not present; connecting directly to RTSP sources");
            }
            return target.sourceUrl();
        }

        LeaseRegistration registration;
        synchronized (lock) {
            if (closed) {
                throw new SensorHubException("MediaMTX sidecar manager is shut down");
            }
            if (isSelfLoopbackTargetLocked(target)) {
                return target.sourceUrl();
            }

            ensureProcessRunningLocked(binary.get());
            releaseLeaseIfDifferentKeyLocked(leaseId, target.canonicalKey());

            PathEntry entry = pathsByKey.computeIfAbsent(target.canonicalKey(), key ->
                    new PathEntry("cam_" + sha256Hex(key).substring(0, 24), key, target.sourceUrl()));
            entry.sourceUrl = target.sourceUrl();
            entry.holders.add(leaseId);
            leaseToKey.put(leaseId, target.canonicalKey());

            if (entry.registeredGeneration != processGeneration) {
                pushPathConfigLocked(entry);
                entry.registeredGeneration = processGeneration;
            }
            ensureWatchdogLocked(binary.get());
            registration = new LeaseRegistration(entry.pathName, entry.sourceUrl, activeRtspPort, rtspSecret);
        }

        long readyTimeoutMs = clampReadyTimeout(connectTimeoutMs);
        if (!waitForPathReady(binary.get(), registration.pathName(), readyTimeoutMs)) {
            throw new SensorHubException("RTSP camera source is unavailable or not ready: "
                    + redactCredentials(target.sourceUrl()));
        }

        synchronized (lock) {
            // In case MediaMTX healed onto new credentials/ports during readiness polling,
            // return the current active loopback endpoint for this still-leased path.
            String currentKey = leaseToKey.get(leaseId);
            PathEntry currentEntry = currentKey != null ? pathsByKey.get(currentKey) : null;
            if (currentEntry == null) {
                throw new SensorHubException("RTSP stream lease was released while opening "
                        + redactCredentials(target.sourceUrl()));
            }
            return "rtsp://" + RTSP_USER + ":" + rtspSecret
                    + "@" + LOOPBACK_HOST + ":" + activeRtspPort + "/" + currentEntry.pathName;
        }
    }

    /**
     * Releases any stream lease held by {@code leaseId}. When the last holder releases a path,
     * the path is immediately deleted from {@code mediamtx} so an RTSP {@code TEARDOWN} is sent
     * to the upstream camera.
     */
    public void releaseStream(String leaseId) {
        if (leaseId == null) {
            return;
        }
        synchronized (lock) {
            String key = leaseToKey.remove(leaseId);
            if (key == null) {
                return;
            }
            removeHolderFromKeyLocked(leaseId, key);
        }
    }

    /**
     * Recycles an unshared MediaMTX upstream path when {@code MpegTsProcessor.openStream()}
     * detects incomplete video codec parameters (for example, an IP camera whose RTSP session
     * continues dripping audio/RTCP packets while stalling its H.264 video track).
     */
    public void recycleStreamIfUnshared(String leaseId) {
        if (leaseId == null) {
            return;
        }
        synchronized (lock) {
            String key = leaseToKey.get(leaseId);
            if (key == null) {
                return;
            }
            PathEntry entry = pathsByKey.get(key);
            if (entry == null || entry.holders.size() > 1) {
                return;
            }
            deletePathConfigBestEffortLocked(entry.pathName);
            try {
                pushPathConfigLocked(entry);
                entry.registeredGeneration = processGeneration;
            } catch (SensorHubException e) {
                entry.registeredGeneration = -1L;
                logger.debug("Deferred MediaMTX path re-registration for {}: {}", entry.pathName, e.getMessage());
            }
        }
    }

    int getActivePathCount() {
        synchronized (lock) {
            return pathsByKey.size();
        }
    }

    int getActiveHolderCountForUrl(String rawUrl) {
        Optional<NormalizedRtspTarget> target = parseAndNormalizeRtspUrl(rawUrl);
        if (target.isEmpty()) {
            return 0;
        }
        synchronized (lock) {
            PathEntry entry = pathsByKey.get(target.get().canonicalKey());
            return entry == null ? 0 : entry.holders.size();
        }
    }

    boolean isProcessAlive() {
        synchronized (lock) {
            return process != null && process.isAlive();
        }
    }

    int getActiveRtspPort() {
        synchronized (lock) {
            return activeRtspPort;
        }
    }

    int getActiveApiPort() {
        synchronized (lock) {
            return activeApiPort;
        }
    }

    public static String redactCredentials(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return CREDENTIAL_REDACTION_PATTERN.matcher(text).replaceAll("$1***@");
    }

    /**
     * Normalizes an {@code rtsp://} or {@code rtsps://} URL, collapsing accidental multiple
     * leading slashes in the stream path (e.g. {@code //h264Preview_01_sub -> /h264Preview_01_sub})
     * and canonicalizing the deduplication key.
     */
    public static Optional<NormalizedRtspTarget> parseAndNormalizeRtspUrl(String rawUrl) {
        if (rawUrl == null) {
            return Optional.empty();
        }
        String trimmed = rawUrl.trim();
        String lower = trimmed.toLowerCase(Locale.ROOT);
        final String scheme;
        final int defaultPort;
        if (lower.startsWith("rtsp://")) {
            scheme = "rtsp";
            defaultPort = 554;
        } else if (lower.startsWith("rtsps://")) {
            scheme = "rtsps";
            defaultPort = 322;
        } else {
            return Optional.empty();
        }

        String remainder = trimmed.substring(scheme.length() + 3);
        int authorityEnd = findAuthorityEnd(remainder);
        String authority = authorityEnd >= 0 ? remainder.substring(0, authorityEnd) : remainder;
        String pathAndQuery = authorityEnd >= 0 ? remainder.substring(authorityEnd) : "/";
        if (authority.isEmpty()) {
            return Optional.empty();
        }

        String userInfo = null;
        String hostPort = authority;
        int lastAt = authority.lastIndexOf('@');
        if (lastAt >= 0) {
            userInfo = authority.substring(0, lastAt);
            hostPort = authority.substring(lastAt + 1);
            if (userInfo.indexOf('@') >= 0) {
                int colon = userInfo.indexOf(':');
                if (colon >= 0) {
                    String user = userInfo.substring(0, colon).replace("@", "%40");
                    String pass = userInfo.substring(colon + 1).replace("@", "%40");
                    userInfo = user + ":" + pass;
                } else {
                    userInfo = userInfo.replace("@", "%40");
                }
            }
        }
        if (hostPort.isEmpty()) {
            return Optional.empty();
        }

        HostPort parsedHostPort = parseHostPort(hostPort, defaultPort);
        if (parsedHostPort == null) {
            return Optional.empty();
        }

        String pathPart;
        String queryPart;
        int queryIdx = pathAndQuery.indexOf('?');
        if (queryIdx >= 0) {
            pathPart = pathAndQuery.substring(0, queryIdx);
            queryPart = pathAndQuery.substring(queryIdx);
        } else {
            pathPart = pathAndQuery;
            queryPart = "";
        }

        String normalizedPath = collapseLeadingSlashes(pathPart);
        String rebuiltAuthority = (userInfo != null && !userInfo.isEmpty() ? userInfo + "@" : "") + hostPort;
        String normalizedSourceUrl = scheme + "://" + rebuiltAuthority + normalizedPath + queryPart;

        String canonicalAuthority = (userInfo != null && !userInfo.isEmpty() ? userInfo + "@" : "")
                + parsedHostPort.canonicalHost() + ":" + parsedHostPort.port();
        String canonicalKey = scheme + "://" + canonicalAuthority + normalizedPath + queryPart;

        return Optional.of(new NormalizedRtspTarget(
                normalizedSourceUrl,
                canonicalKey,
                parsedHostPort.rawHost(),
                parsedHostPort.port()));
    }

    private record HostPort(String rawHost, String canonicalHost, int port) {}

    private static HostPort parseHostPort(String hostPort, int defaultPort) {
        String host;
        int port = defaultPort;
        if (hostPort.startsWith("[")) {
            int closeBracket = hostPort.indexOf(']');
            if (closeBracket <= 1) {
                return null;
            }
            host = hostPort.substring(0, closeBracket + 1);
            if (closeBracket + 1 < hostPort.length()) {
                if (hostPort.charAt(closeBracket + 1) != ':') {
                    return null;
                }
                String portStr = hostPort.substring(closeBracket + 2);
                if (!portStr.isEmpty()) {
                    port = parsePort(portStr);
                    if (port <= 0) {
                        return null;
                    }
                }
            }
        } else {
            int colon = hostPort.lastIndexOf(':');
            if (colon > 0) {
                host = hostPort.substring(0, colon);
                String portStr = hostPort.substring(colon + 1);
                if (!portStr.isEmpty()) {
                    port = parsePort(portStr);
                    if (port <= 0) {
                        return null;
                    }
                }
            } else if (colon == 0) {
                return null;
            } else {
                host = hostPort;
            }
        }
        if (host.isEmpty()) {
            return null;
        }
        return new HostPort(host, host.toLowerCase(Locale.ROOT), port);
    }

    private static int parsePort(String portStr) {
        try {
            int p = Integer.parseInt(portStr);
            return (p >= 1 && p <= 65535) ? p : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static int findAuthorityEnd(String remainder) {
        int slash = remainder.indexOf('/');
        int query = remainder.indexOf('?');
        int hash = remainder.indexOf('#');
        int end = -1;
        for (int candidate : new int[]{slash, query, hash}) {
            if (candidate >= 0 && (end < 0 || candidate < end)) {
                end = candidate;
            }
        }
        return end;
    }

    static String collapseLeadingSlashes(String path) {
        if (path == null || path.isEmpty()) {
            return "/";
        }
        int index = 0;
        while (index < path.length() && path.charAt(index) == '/') {
            index++;
        }
        return "/" + path.substring(index);
    }

    private boolean isSelfLoopbackTargetLocked(NormalizedRtspTarget target) {
        if (activeRtspPort <= 0 || target.port() != activeRtspPort) {
            return false;
        }
        String h = target.host().toLowerCase(Locale.ROOT);
        return "127.0.0.1".equals(h) || "localhost".equals(h) || "[::1]".equals(h);
    }

    private boolean isEnabled() {
        String prop = System.getProperty(PROP_ENABLED);
        if (prop != null && !prop.isBlank()) {
            return Boolean.parseBoolean(prop.trim());
        }
        String env = System.getenv(ENV_ENABLED);
        if (env != null && !env.isBlank()) {
            return Boolean.parseBoolean(env.trim());
        }
        return true;
    }

    private Optional<Path> resolveBinaryPath() {
        if (configuredBinaryOverride != null) {
            return isUsableExecutable(configuredBinaryOverride)
                    ? Optional.of(configuredBinaryOverride) : Optional.empty();
        }
        String prop = System.getProperty(PROP_BIN);
        if (prop != null && !prop.isBlank()) {
            Path p = Paths.get(prop.trim());
            return isUsableExecutable(p) ? Optional.of(p) : Optional.empty();
        }
        String env = System.getenv(ENV_BIN);
        if (env != null && !env.isBlank()) {
            Path p = Paths.get(env.trim());
            return isUsableExecutable(p) ? Optional.of(p) : Optional.empty();
        }
        Path defaultPath = Paths.get(DEFAULT_BINARY_PATH);
        return isUsableExecutable(defaultPath) ? Optional.of(defaultPath) : Optional.empty();
    }

    private static boolean isUsableExecutable(Path path) {
        return path != null && Files.isRegularFile(path) && Files.isExecutable(path);
    }

    private void releaseLeaseIfDifferentKeyLocked(String leaseId, String newKey) {
        String existingKey = leaseToKey.get(leaseId);
        if (existingKey != null && !existingKey.equals(newKey)) {
            leaseToKey.remove(leaseId);
            removeHolderFromKeyLocked(leaseId, existingKey);
        }
    }

    private void removeHolderFromKeyLocked(String leaseId, String key) {
        PathEntry entry = pathsByKey.get(key);
        if (entry == null) {
            return;
        }
        entry.holders.remove(leaseId);
        if (entry.holders.isEmpty()) {
            pathsByKey.remove(key);
            deletePathConfigBestEffortLocked(entry.pathName);
        }
    }

    private void ensureProcessRunningLocked(Path binaryPath) throws SensorHubException {
        if (process != null && process.isAlive() && isApiResponsiveLocked()) {
            return;
        }
        stopProcessOnlyLocked();
        startProcessLocked(binaryPath);
    }

    private void startProcessLocked(Path binaryPath) throws SensorHubException {
        try {
            int rtspPort = allocateLoopbackPort(resolvePreferredPort(
                    preferredRtspPortOverride, PROP_RTSP_PORT, ENV_RTSP_PORT, PREFERRED_RTSP_PORT, activeRtspPort));
            int apiPort = allocateLoopbackPort(resolvePreferredPort(
                    preferredApiPortOverride, PROP_API_PORT, ENV_API_PORT, PREFERRED_API_PORT, activeApiPort));
            while (apiPort == rtspPort) {
                apiPort = allocateLoopbackPort(0);
            }

            String newApiSecret = randomHex(32);
            String newRtspSecret = randomHex(32);

            if (runtimeDir == null || !Files.isDirectory(runtimeDir)) {
                Path baseTmp = Paths.get(System.getProperty(
                        "oscar.data.tmpdir", System.getProperty("java.io.tmpdir", "/tmp")));
                runtimeDir = createPrivateDirectory(baseTmp, "oscar-mediamtx-");
            }
            configFile = runtimeDir.resolve("mediamtx.yml");
            writePrivateConfigFile(configFile, renderConfigYaml(rtspPort, apiPort, newApiSecret, newRtspSecret));

            ProcessBuilder pb = new ProcessBuilder(
                    binaryPath.toAbsolutePath().toString(),
                    configFile.toAbsolutePath().toString());
            pb.redirectErrorStream(true);
            Process started = pb.start();

            this.process = started;
            this.activeRtspPort = rtspPort;
            this.activeApiPort = apiPort;
            this.apiSecret = newApiSecret;
            this.rtspSecret = newRtspSecret;
            this.basicAuthHeader = "Basic " + Base64.getEncoder().encodeToString(
                    (API_USER + ":" + newApiSecret).getBytes(StandardCharsets.UTF_8));
            this.processGeneration++;

            startLogConsumer(started);

            long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(STARTUP_TIMEOUT_MS);
            boolean ready = false;
            while (System.nanoTime() < deadline) {
                if (!started.isAlive()) {
                    throw new SensorHubException("MediaMTX sidecar exited prematurely with code " + started.exitValue());
                }
                if (isApiResponsiveLocked()) {
                    ready = true;
                    break;
                }
                Thread.sleep(50L);
            }
            if (!ready) {
                stopProcessOnlyLocked();
                throw new SensorHubException("Timed out waiting for MediaMTX loopback Control API on 127.0.0.1:" + apiPort);
            }

            logger.info("MediaMTX loopback RTSP proxy active on 127.0.0.1:{} (API 127.0.0.1:{})", rtspPort, apiPort);

            for (PathEntry entry : pathsByKey.values()) {
                pushPathConfigLocked(entry);
                entry.registeredGeneration = processGeneration;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            stopProcessOnlyLocked();
            throw new SensorHubException("Interrupted while starting MediaMTX sidecar", e);
        } catch (IOException e) {
            stopProcessOnlyLocked();
            throw new SensorHubException("Unable to start MediaMTX sidecar", e);
        }
    }

    private void ensureWatchdogLocked(Path binaryPath) {
        if (watchdogExecutor == null || watchdogExecutor.isShutdown()) {
            watchdogExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread t = new Thread(runnable, "oscar-mediamtx-watchdog");
                t.setDaemon(true);
                return t;
            });
        }
        if (watchdogFuture == null || watchdogFuture.isDone()) {
            watchdogFuture = watchdogExecutor.scheduleWithFixedDelay(
                    () -> runWatchdogCheck(binaryPath),
                    WATCHDOG_INTERVAL_SECONDS,
                    WATCHDOG_INTERVAL_SECONDS,
                    TimeUnit.SECONDS);
        }
    }

    private void runWatchdogCheck(Path binaryPath) {
        synchronized (lock) {
            if (closed || pathsByKey.isEmpty()) {
                return;
            }
            if (process != null && process.isAlive()) {
                return;
            }
            logger.warn("MediaMTX sidecar process exited unexpectedly; restarting and restoring {} active camera path(s)",
                    pathsByKey.size());
            try {
                ensureProcessRunningLocked(binaryPath);
            } catch (Exception e) {
                logger.error("Failed to restart MediaMTX sidecar during watchdog check: {}", e.getMessage());
            }
        }
    }

    private boolean waitForPathReady(Path binaryPath, String pathName, long timeoutMs) {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs);
        while (System.nanoTime() < deadline) {
            if (Thread.currentThread().isInterrupted()) {
                return false;
            }
            int apiPortSnapshot;
            String authSnapshot;
            synchronized (lock) {
                if (closed) {
                    return false;
                }
                if (process == null || !process.isAlive()) {
                    try {
                        ensureProcessRunningLocked(binaryPath);
                    } catch (SensorHubException e) {
                        logger.warn("MediaMTX recovery attempt failed while waiting for {}: {}", pathName, e.getMessage());
                    }
                }
                apiPortSnapshot = activeApiPort;
                authSnapshot = basicAuthHeader;
            }

            if (apiPortSnapshot > 0 && authSnapshot != null && queryPathReady(apiPortSnapshot, authSnapshot, pathName)) {
                return true;
            }

            try {
                Thread.sleep(READY_POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    private boolean queryPathReady(int apiPort, String authHeader, String pathName) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://" + LOOPBACK_HOST + ":" + apiPort + "/v3/paths/get/" + pathName))
                    .header("Authorization", authHeader)
                    .timeout(Duration.ofSeconds(2))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200 && isPathReadyJson(response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    static boolean isPathReadyJson(String json) {
        return json != null && READY_TRUE_PATTERN.matcher(json).find();
    }

    private boolean isApiResponsiveLocked() {
        if (activeApiPort <= 0 || basicAuthHeader == null) {
            return false;
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://" + LOOPBACK_HOST + ":" + activeApiPort + "/v3/paths/list"))
                    .header("Authorization", basicAuthHeader)
                    .timeout(Duration.ofMillis(1_000))
                    .GET()
                    .build();
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            return response.statusCode() == 200;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private void pushPathConfigLocked(PathEntry entry) throws SensorHubException {
        String payload = "{\"source\":\"" + escapeJson(entry.sourceUrl)
                + "\",\"rtspTransport\":\"tcp\",\"sourceOnDemand\":false}";
        try {
            HttpRequest addRequest = HttpRequest.newBuilder()
                    .uri(URI.create("http://" + LOOPBACK_HOST + ":" + activeApiPort
                            + "/v3/config/paths/add/" + entry.pathName))
                    .header("Authorization", basicAuthHeader)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(3))
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(addRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return;
            }
            if (response.statusCode() == 400) {
                HttpRequest patchRequest = HttpRequest.newBuilder()
                        .uri(URI.create("http://" + LOOPBACK_HOST + ":" + activeApiPort
                                + "/v3/config/paths/patch/" + entry.pathName))
                        .header("Authorization", basicAuthHeader)
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(3))
                        .method("PATCH", HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                        .build();
                HttpResponse<String> patchResponse = httpClient.send(patchRequest, HttpResponse.BodyHandlers.ofString());
                if (patchResponse.statusCode() == 200) {
                    return;
                }
                throw new SensorHubException("MediaMTX rejected path update for " + entry.pathName
                        + " (HTTP " + patchResponse.statusCode() + ")");
            }
            throw new SensorHubException("MediaMTX rejected path registration for " + entry.pathName
                    + " (HTTP " + response.statusCode() + ")");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SensorHubException("Interrupted registering MediaMTX path " + entry.pathName, e);
        } catch (IOException e) {
            throw new SensorHubException("Unable to register MediaMTX path " + entry.pathName, e);
        }
    }

    private void deletePathConfigBestEffortLocked(String pathName) {
        if (process == null || !process.isAlive() || activeApiPort <= 0 || basicAuthHeader == null) {
            return;
        }
        try {
            HttpRequest deleteRequest = HttpRequest.newBuilder()
                    .uri(URI.create("http://" + LOOPBACK_HOST + ":" + activeApiPort
                            + "/v3/config/paths/delete/" + pathName))
                    .header("Authorization", basicAuthHeader)
                    .timeout(Duration.ofSeconds(2))
                    .DELETE()
                    .build();
            httpClient.send(deleteRequest, HttpResponse.BodyHandlers.discarding());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.debug("Ignoring MediaMTX path cleanup error for {}: {}", pathName, e.getMessage());
        }
    }

    private static void startLogConsumer(Process started) {
        Thread logThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(started.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String safeLine = redactCredentials(line);
                    if (safeLine.contains(" ERR ") || safeLine.contains(" WAR ")) {
                        logger.debug("[mediamtx] {}", safeLine);
                    }
                }
            } catch (IOException ignored) {
                // Stream closed on process exit.
            }
        }, "oscar-mediamtx-logs");
        logThread.setDaemon(true);
        logThread.start();
    }

    private static String renderConfigYaml(int rtspPort, int apiPort, String apiSecret, String rtspSecret) {
        return "logLevel: warn\n"
                + "logDestinations: [stdout]\n"
                + "readTimeout: 15s\n"
                + "writeTimeout: 15s\n"
                + "writeQueueSize: 4096\n"
                + "authMethod: internal\n"
                + "authInternalUsers:\n"
                + "- user: " + API_USER + "\n"
                + "  pass: " + apiSecret + "\n"
                + "  ips: ['127.0.0.1']\n"
                + "  permissions:\n"
                + "  - action: api\n"
                + "- user: " + RTSP_USER + "\n"
                + "  pass: " + rtspSecret + "\n"
                + "  ips: ['127.0.0.1']\n"
                + "  permissions:\n"
                + "  - action: read\n"
                + "    path:\n"
                + "api: yes\n"
                + "apiAddress: 127.0.0.1:" + apiPort + "\n"
                + "metrics: no\n"
                + "pprof: no\n"
                + "playback: no\n"
                + "rtsp: yes\n"
                + "rtspTransports: [tcp]\n"
                + "rtspEncryption: \"no\"\n"
                + "rtspAddress: 127.0.0.1:" + rtspPort + "\n"
                + "rtmp: no\n"
                + "hls: no\n"
                + "webrtc: no\n"
                + "srt: no\n"
                + "paths: {}\n";
    }

    private static Path createPrivateDirectory(Path parent, String prefix) throws IOException {
        Files.createDirectories(parent);
        try {
            Set<PosixFilePermission> perms = EnumSet.of(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE,
                    PosixFilePermission.OWNER_EXECUTE);
            return Files.createTempDirectory(parent, prefix, PosixFilePermissions.asFileAttribute(perms));
        } catch (UnsupportedOperationException e) {
            return Files.createTempDirectory(parent, prefix);
        }
    }

    private static void writePrivateConfigFile(Path file, String content) throws IOException {
        Files.writeString(file, content, StandardCharsets.UTF_8);
        try {
            Files.setPosixFilePermissions(file, EnumSet.of(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE));
        } catch (UnsupportedOperationException ignored) {
            // Non-POSIX filesystems rely on user profile directory ACLs.
        }
    }

    private static int resolvePreferredPort(int explicitOverride, String propName, String envName,
                                            int defaultPreferred, int previousPort) {
        if (explicitOverride > 0) {
            return explicitOverride;
        }
        String prop = System.getProperty(propName);
        if (prop != null && !prop.isBlank()) {
            int parsed = parsePort(prop.trim());
            if (parsed > 0) {
                return parsed;
            }
        }
        String env = System.getenv(envName);
        if (env != null && !env.isBlank()) {
            int parsed = parsePort(env.trim());
            if (parsed > 0) {
                return parsed;
            }
        }
        return previousPort > 0 ? previousPort : defaultPreferred;
    }

    private static int allocateLoopbackPort(int preferredPort) throws IOException {
        InetAddress loopback = InetAddress.getByName(LOOPBACK_HOST);
        if (preferredPort > 0) {
            try (ServerSocket socket = new ServerSocket(preferredPort, 50, loopback)) {
                socket.setReuseAddress(true);
                return socket.getLocalPort();
            } catch (IOException busy) {
                // Fall back to an OS-assigned ephemeral loopback port.
            }
        }
        try (ServerSocket socket = new ServerSocket(0, 50, loopback)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        }
    }

    private static long clampReadyTimeout(long connectTimeoutMs) {
        if (connectTimeoutMs <= 0) {
            return 4_500L;
        }
        return Math.max(MIN_READY_WAIT_MS, Math.min(MAX_READY_WAIT_MS, connectTimeoutMs));
    }

    private String randomHex(int numBytes) {
        byte[] bytes = new byte[numBytes];
        secureRandom.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private static String escapeJson(String raw) {
        StringBuilder sb = new StringBuilder(raw.length() + 8);
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '"' -> sb.append("\\\"");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    private void stopProcessOnlyLocked() {
        if (process != null) {
            try {
                process.destroy();
                if (!process.waitFor(3, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                    process.waitFor(2, TimeUnit.SECONDS);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                process.destroyForcibly();
            } finally {
                process = null;
            }
        }
        if (configFile != null) {
            try {
                Files.deleteIfExists(configFile);
            } catch (IOException ignored) {
            }
            configFile = null;
        }
    }

    @Override
    public void close() {
        ScheduledExecutorService executorToStop;
        synchronized (lock) {
            closed = true;
            pathsByKey.clear();
            leaseToKey.clear();
            if (watchdogFuture != null) {
                watchdogFuture.cancel(true);
                watchdogFuture = null;
            }
            executorToStop = watchdogExecutor;
            watchdogExecutor = null;
            stopProcessOnlyLocked();
            if (runtimeDir != null) {
                try {
                    Files.deleteIfExists(runtimeDir);
                } catch (IOException ignored) {
                }
                runtimeDir = null;
            }
        }
        if (executorToStop != null) {
            executorToStop.shutdownNow();
        }
    }
}
