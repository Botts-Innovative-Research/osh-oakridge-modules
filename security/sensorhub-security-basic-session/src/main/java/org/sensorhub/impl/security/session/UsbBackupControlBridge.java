package org.sensorhub.impl.security.session;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Schema-validated file-queue bridge between authenticated OSCAR Administrator
 * web sessions ({@code /session/admin/usb-backup/*}) and the host-side USB
 * backup watcher ({@code oscar-usb-backup.sh} / {@code oscar-usb-backup.ps1})
 * without exposing {@code docker.sock} or elevated Linux capabilities inside
 * the unprivileged {@code oscar} container.
 */
final class UsbBackupControlBridge {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final Set<String> ALLOWED_ACTIONS = Set.of(
            "prepare-drive", "set-mode", "sync", "eject", "attach", "verify", "refresh-status");
    private static final Set<String> ALLOWED_MODES = Set.of("continual", "occasional", "disabled");
    private static final Set<String> ALLOWED_FILESYSTEMS = Set.of("ntfs", "ext4");
    private static final Pattern LINUX_BLOCK_DEV = Pattern.compile("^/dev/(?:sd[a-z][0-9]*|nvme[0-9]+n[0-9]+(?:p[0-9]+)?)$");
    private static final Pattern WINDOWS_DRIVE = Pattern.compile("^[A-Za-z]:?$");
    private static final Pattern UUID_TOKEN = Pattern.compile("^[A-Za-z0-9-]{4,64}$");

    private final Path controlDir;
    private final Logger log;

    UsbBackupControlBridge(Path identityStoreFile, Logger log) {
        if (identityStoreFile != null && identityStoreFile.getParent() != null) {
            this.controlDir = identityStoreFile.getParent().resolve("usb-backup-control");
        } else {
            this.controlDir = Paths.get("/var/lib/oscar/.moduledata/usb-backup-control");
        }
        this.log = log;
    }

    Path getControlDir() {
        return controlDir;
    }

    JsonObject readStatus() {
        Path statusFile = controlDir.resolve("status.json");
        if (Files.isRegularFile(statusFile)) {
            try {
                String raw = Files.readString(statusFile, StandardCharsets.UTF_8);
                JsonElement parsed = JsonParser.parseString(raw);
                if (parsed != null && parsed.isJsonObject()) {
                    JsonObject obj = parsed.getAsJsonObject();
                    if (!obj.has("agentActive")) {
                        obj.addProperty("agentActive", true);
                    }
                    return obj;
                }
            } catch (Exception e) {
                if (log != null) {
                    log.warn("Failed reading USB backup status.json: {}", e.getMessage());
                }
            }
        }
        JsonObject fallback = new JsonObject();
        fallback.addProperty("agentActive", false);
        fallback.addProperty("configured", false);
        fallback.addProperty("mode", "unconfigured");
        fallback.addProperty("intervalMinutes", 15);
        fallback.addProperty("driveState", "UNCONFIGURED");
        fallback.addProperty("backupKeyPresent", false);
        fallback.add("candidateDrives", new com.google.gson.JsonArray());
        return fallback;
    }

    synchronized JsonObject submitAction(JsonObject body, String adminUsername) throws IOException {
        if (body == null) {
            throw new IllegalArgumentException("Request body is required.");
        }
        String action = optString(body, "action").toLowerCase(Locale.ROOT);
        if (!ALLOWED_ACTIONS.contains(action)) {
            throw new IllegalArgumentException("Unsupported USB backup action: " + action);
        }

        JsonObject currentStatus = readStatus();
        if (currentStatus.has("activeJob") && currentStatus.get("activeJob").isJsonObject()) {
            JsonObject activeJob = currentStatus.getAsJsonObject("activeJob");
            String state = optString(activeJob, "state");
            if ("RUNNING".equalsIgnoreCase(state) && !"refresh-status".equals(action)) {
                throw new IllegalArgumentException(
                        "Another USB backup operation (" + optString(activeJob, "action") + ") is currently running.");
            }
        }

        Map<String, Object> req = new LinkedHashMap<>();
        String requestId = UUID.randomUUID().toString();
        req.put("requestId", requestId);
        req.put("requestedAt", Instant.now().toString());
        req.put("requestedBy", adminUsername == null ? "admin" : adminUsername);
        req.put("action", action);

        switch (action) {
            case "prepare-drive" -> {
                String device = optString(body, "device");
                String adoptUuid = optString(body, "adoptUuid");
                boolean noFormat = optBoolean(body, "noFormat", false);
                String confirm = optString(body, "confirm");
                String fs = optString(body, "filesystem").isEmpty()
                        ? "ntfs"
                        : optString(body, "filesystem").toLowerCase(Locale.ROOT);
                String mode = optString(body, "mode").isEmpty()
                        ? "continual"
                        : optString(body, "mode").toLowerCase(Locale.ROOT);
                int interval = optInt(body, "intervalMinutes", 15);

                if (device.isEmpty() && adoptUuid.isEmpty()) {
                    throw new IllegalArgumentException("Select a candidate external USB drive to initialize.");
                }
                if (!device.isEmpty()
                        && !LINUX_BLOCK_DEV.matcher(device).matches()
                        && !WINDOWS_DRIVE.matcher(device).matches()) {
                    throw new IllegalArgumentException("Invalid USB device identifier: " + device);
                }
                if (!adoptUuid.isEmpty() && !UUID_TOKEN.matcher(adoptUuid).matches()) {
                    throw new IllegalArgumentException("Invalid filesystem UUID.");
                }
                if (!noFormat && adoptUuid.isEmpty() && !"FORMAT".equals(confirm)) {
                    throw new IllegalArgumentException("Type FORMAT to confirm erasing and formatting the selected USB drive.");
                }
                if (!ALLOWED_FILESYSTEMS.contains(fs)) {
                    throw new IllegalArgumentException("Filesystem must be ntfs or ext4.");
                }
                if (!"continual".equals(mode) && !"occasional".equals(mode)) {
                    throw new IllegalArgumentException("Initial mode must be continual or occasional.");
                }
                if (interval < 5 || interval > 1440) {
                    throw new IllegalArgumentException("Interval minutes must be between 5 and 1440.");
                }
                req.put("device", device);
                req.put("adoptUuid", adoptUuid);
                req.put("noFormat", noFormat);
                req.put("confirm", confirm);
                req.put("filesystem", fs);
                req.put("mode", mode);
                req.put("intervalMinutes", interval);
            }
            case "set-mode" -> {
                String mode = optString(body, "mode").toLowerCase(Locale.ROOT);
                int interval = optInt(body, "intervalMinutes", 15);
                if (!ALLOWED_MODES.contains(mode)) {
                    throw new IllegalArgumentException("Mode must be continual, occasional, or disabled.");
                }
                if (interval < 5 || interval > 1440) {
                    throw new IllegalArgumentException("Interval minutes must be between 5 and 1440.");
                }
                req.put("mode", mode);
                req.put("intervalMinutes", interval);
            }
            case "sync" -> req.put("quiesce", optBoolean(body, "quiesce", false));
            case "eject" -> req.put("noSync", optBoolean(body, "noSync", false));
            case "verify" -> req.put("deep", optBoolean(body, "deep", false));
            case "attach", "refresh-status" -> {
                // No extra parameters needed
            }
        }

        writeRequestAtomically(req);

        // Wait briefly so fast actions (set-mode, refresh-status, or job transition to RUNNING)
        // immediately return fresh status to the browser UI.
        long waitMillis = ("set-mode".equals(action) || "refresh-status".equals(action)) ? 2500L : 900L;
        long deadline = System.currentTimeMillis() + waitMillis;
        while (System.currentTimeMillis() < deadline) {
            JsonObject latest = readStatus();
            if (latest.has("activeJob") && latest.get("activeJob").isJsonObject()) {
                JsonObject job = latest.getAsJsonObject("activeJob");
                if (requestId.equals(optString(job, "requestId"))) {
                    String st = optString(job, "state");
                    if (!"RUNNING".equalsIgnoreCase(st) || waitMillis <= 900L) {
                        return latest;
                    }
                }
            }
            try {
                Thread.sleep(120L);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        JsonObject pending = readStatus();
        if (!pending.has("activeJob")
                || !pending.get("activeJob").isJsonObject()
                || !requestId.equals(optString(pending.getAsJsonObject("activeJob"), "requestId"))) {
            JsonObject job = new JsonObject();
            job.addProperty("requestId", requestId);
            job.addProperty("action", action);
            job.addProperty("state", "QUEUED");
            job.addProperty("startedAt", Instant.now().toString());
            job.addProperty("message", "Queued for host USB backup agent...");
            pending.add("activeJob", job);
        }
        return pending;
    }

    synchronized String exportBackupKey(String adminUsername) throws IOException {
        String requestId = UUID.randomUUID().toString();
        Path escrowFile = controlDir.resolve("escrow-" + requestId + ".key");
        Files.deleteIfExists(escrowFile);

        Map<String, Object> req = new LinkedHashMap<>();
        req.put("requestId", requestId);
        req.put("requestedAt", Instant.now().toString());
        req.put("requestedBy", adminUsername == null ? "admin" : adminUsername);
        req.put("action", "export-backup-key");
        writeRequestAtomically(req);

        long deadline = System.currentTimeMillis() + 8_000L;
        try {
            while (System.currentTimeMillis() < deadline) {
                if (Files.isRegularFile(escrowFile)) {
                    String keyMaterial = Files.readString(escrowFile, StandardCharsets.UTF_8);
                    Files.deleteIfExists(escrowFile);
                    if (keyMaterial != null && !keyMaterial.isBlank()) {
                        return keyMaterial;
                    }
                }
                try {
                    Thread.sleep(100L);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        } finally {
            try {
                Files.deleteIfExists(escrowFile);
            } catch (IOException ignored) {
            }
        }
        throw new IllegalStateException(
                "Host USB backup agent did not supply backup-key.txt within 8 seconds. Confirm the host agent is active.");
    }

    private void writeRequestAtomically(Map<String, Object> req) throws IOException {
        Files.createDirectories(controlDir);
        Path tmp = controlDir.resolve(".request." + UUID.randomUUID() + ".tmp");
        Path dest = controlDir.resolve("request.json");
        Files.writeString(tmp, GSON.toJson(req) + "\n", StandardCharsets.UTF_8);
        Files.move(tmp, dest, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    private static String optString(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull() || !obj.get(key).isJsonPrimitive()) {
            return "";
        }
        return obj.get(key).getAsString().trim();
    }

    private static boolean optBoolean(JsonObject obj, String key, boolean fallback) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull() || !obj.get(key).isJsonPrimitive()) {
            return fallback;
        }
        return obj.get(key).getAsBoolean();
    }

    private static int optInt(JsonObject obj, String key, int fallback) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull() || !obj.get(key).isJsonPrimitive()) {
            return fallback;
        }
        return obj.get(key).getAsInt();
    }
}
