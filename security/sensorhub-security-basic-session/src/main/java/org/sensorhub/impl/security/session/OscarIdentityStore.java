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

package org.sensorhub.impl.security.session;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import org.sensorhub.api.ISensorHub;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.IModule;
import org.sensorhub.api.security.ISecurityManager;
import org.sensorhub.api.security.IUserInfo;
import org.sensorhub.api.security.IUserRegistry;
import org.sensorhub.impl.module.ModuleRegistry;
import org.sensorhub.impl.security.BasicSecurityRealm;
import org.sensorhub.impl.security.BasicSecurityRealmConfig;
import org.sensorhub.impl.security.BasicSecurityRealmConfig.RoleConfig;
import org.sensorhub.impl.security.BasicSecurityRealmConfig.UserConfig;
import org.slf4j.Logger;

/**
 * Persistent, atomic identity and session store for OSCAR (`oscar-identity.json`)
 * synchronized with SensorHub's {@link BasicSecurityRealm}.
 *
 * <p>Stores SHA-256 session digests (surviving browser restarts, host reboots, and
 * {@code oscar upgrade}), per-user passkey credentials, Operational View lane scopes,
 * forced password-change flags, login rate-limiting state, and scoped federation
 * service tokens.</p>
 */
public final class OscarIdentityStore {
    public static final String ROLE_ADMIN = "admin";
    public static final String ROLE_OPERATOR = "operator";
    public static final String ROLE_VIEWER = "viewer";
    public static final String FED_SERVICE_USER_PREFIX = "svc-fed-";
    public static final String FED_TOKEN_PREFIX = "oscar_fed_";
    public static final String INVITE_TOKEN_PREFIX = "oscar_inv_";
    public static final int DEFAULT_INVITE_EXPIRY_HOURS = 168; // 7 days
    public static final int MAX_BULK_ENROLL_USERS = 500;

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9._@-]{2,64}$");
    private static final Pattern VIEW_KEY_PATTERN = Pattern.compile("^[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?$");
    private static final long CHALLENGE_TTL_MILLIS = 5 * 60_000L;
    private static final int MAX_PENDING_CHALLENGES = 512;
    private static final long SESSION_ACCESS_FLUSH_INTERVAL_MILLIS = 5 * 60_000L;
    private static final long RATE_LIMIT_WINDOW_MILLIS = 5 * 60_000L;
    private static final long RATE_LIMIT_COOLDOWN_MILLIS = 60_000L;
    private static final int RATE_LIMIT_MAX_FAILURES = 10;

    private static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create();

    public static final class PasskeyRecord {
        public String id;
        public String name;
        public String publicKeyCose;
        public int algorithm;
        public String aaguid;
        public List<String> transports = new ArrayList<>();
        public long signCount;
        public boolean backupEligible;
        public boolean backupState;
        public long createdAt;
        public long lastUsedAt;
    }

    public static final class UserProfileRecord {
        public String userId;
        public String displayName;
        public boolean enabled = true;
        public boolean mustChangePassword;
        public String passkeyUserId;
        public List<String> allowedViews = new ArrayList<>();
        public String authSource = "local";
        public long createdAt;
        public long lastLoginAt;
        public List<PasskeyRecord> passkeys = new ArrayList<>();
    }

    public static final class PersistedSession {
        public String tokenHash;
        public String username;
        public long createdAt;
        public volatile long lastAccessAt;
        public boolean keepSignedIn;

        public PersistedSession() {
        }

        public PersistedSession(String tokenHash, String username, long now, boolean keepSignedIn) {
            this.tokenHash = tokenHash;
            this.username = username;
            this.createdAt = now;
            this.lastAccessAt = now;
            this.keepSignedIn = keepSignedIn;
        }

        public boolean isExpired(long now, long idleTimeoutMillis, long absoluteTimeoutMillis) {
            if (idleTimeoutMillis > 0 && now - lastAccessAt >= idleTimeoutMillis)
                return true;
            if (absoluteTimeoutMillis > 0 && now - createdAt >= absoluteTimeoutMillis)
                return true;
            return false;
        }
    }

    public static final class FederationTokenRecord {
        public String id;
        public String serviceUserId;
        public String label;
        public String role = ROLE_VIEWER;
        public String tokenSha256;
        public String tokenPrefix;
        public boolean enabled = true;
        public long createdAt;
        public String createdBy;
        public long expiresAt;
        public volatile long lastUsedAt;
    }

    public static final class EnrollmentInviteRecord {
        public String id;
        public String username;
        public String tokenSha256;
        public String tokenPrefix;
        public long createdAt;
        public long expiresAt;
        public String createdBy;
    }

    private static final class PersistedRoot {
        int schemaVersion = 1;
        Map<String, UserProfileRecord> users = new LinkedHashMap<>();
        Map<String, PersistedSession> sessions = new LinkedHashMap<>();
        Map<String, FederationTokenRecord> federationTokens = new LinkedHashMap<>();
        Map<String, EnrollmentInviteRecord> enrollmentInvites = new LinkedHashMap<>();
    }

    public record PendingChallenge(
            String challenge,
            String purpose,
            String username,
            String rpId,
            String redirectTarget,
            boolean keepSignedIn,
            String oidcCodeVerifier,
            String oidcNonce,
            long expiresAt
    ) {}

    private static final class RateBucket {
        int failures;
        long windowStartedAt;
        long blockedUntil;
    }

    public record CreatedFederationToken(FederationTokenRecord metadata, String rawToken) {}

    public record CreatedOrResetUserResult(UserProfileRecord profile, String role, String generatedPassword) {}

    public record IssuedEnrollmentInvite(EnrollmentInviteRecord metadata, String rawToken) {}

    public record BulkUserSpec(String username, String displayName, String role, List<String> allowedViews) {}

    public record BulkEnrolledUser(
            UserProfileRecord profile,
            String role,
            boolean createdNew,
            String temporaryPassword,
            String inviteToken,
            long inviteExpiresAt
    ) {}

    public record BulkRowError(int row, String username, String error) {}

    public record BulkEnrollmentOutcome(
            int createdCount,
            int reissuedCount,
            int skippedCount,
            List<BulkEnrolledUser> enrolled,
            List<BulkRowError> errors
    ) {}

    private final ISensorHub hub;
    private final ISecurityManager securityManager;
    private final Path storeFile;
    private final boolean persistSessions;
    private final Logger log;

    private final Map<String, UserProfileRecord> profiles = new ConcurrentHashMap<>();
    private final Map<String, PersistedSession> sessionsByHash = new ConcurrentHashMap<>();
    private final Map<String, FederationTokenRecord> federationTokensByHash = new ConcurrentHashMap<>();
    private final Map<String, FederationTokenRecord> federationTokensById = new ConcurrentHashMap<>();
    private final Map<String, EnrollmentInviteRecord> enrollmentInvitesByHash = new ConcurrentHashMap<>();
    private final Map<String, PendingChallenge> challenges = new ConcurrentHashMap<>();
    private final Map<String, RateBucket> rateBuckets = new ConcurrentHashMap<>();
    private volatile long lastSessionFlushAt;

    public OscarIdentityStore(ISensorHub hub,
                              ISecurityManager securityManager,
                              Path storeFile,
                              boolean persistSessions,
                              Logger log) {
        this.hub = hub;
        this.securityManager = securityManager;
        this.storeFile = storeFile;
        this.persistSessions = persistSessions;
        this.log = log;
        initialize();
    }

    private synchronized void initialize() {
        ensureDefaultRolesInRealm();
        loadFromDisk();
        reconcileWithSecurityRealm();
    }

    private BasicSecurityRealm findBasicRealm() {
        if (hub == null || hub.getModuleRegistry() == null)
            return null;
        for (IModule<?> mod : hub.getModuleRegistry().getLoadedModules()) {
            if (mod instanceof BasicSecurityRealm realm)
                return realm;
        }
        return null;
    }

    private synchronized void ensureDefaultRolesInRealm() {
        BasicSecurityRealm realm = findBasicRealm();
        if (realm == null || realm.getConfiguration() == null)
            return;

        BasicSecurityRealmConfig cfg = realm.getConfiguration();
        boolean changed = false;

        if (findRole(cfg, ROLE_ADMIN) == null) {
            RoleConfig admin = new RoleConfig();
            admin.roleID = ROLE_ADMIN;
            admin.name = "Administrator";
            admin.description = "Full access to OSCAR Viewer, Users & Access, and SensorHub Admin";
            admin.allow.add("*");
            cfg.roles.add(admin);
            changed = true;
        }

        if (findRole(cfg, ROLE_OPERATOR) == null) {
            RoleConfig op = new RoleConfig();
            op.roleID = ROLE_OPERATOR;
            op.name = "Operator";
            op.description = "Monitor lanes, acknowledge alarms, submit adjudications, and generate reports";
            op.allow.add("*");
            op.deny.add("webadmin[*]");
            op.deny.add("csapi[*]/delete");
            op.deny.add("bucketservice[*]/delete/sitemap");
            op.deny.add("bucketservice[*]/put/spreadsheets");
            op.deny.add("bucketservice[*]/create/spreadsheets");
            op.deny.add("bucketservice[*]/delete/spreadsheets");
            cfg.roles.add(op);
            changed = true;
        }

        if (findRole(cfg, ROLE_VIEWER) == null) {
            RoleConfig viewer = new RoleConfig();
            viewer.roleID = ROLE_VIEWER;
            viewer.name = "Viewer (Read-Only)";
            viewer.description = "Read-only access to live lanes, events, video/waveforms, and existing reports";
            viewer.allow.add("fileserver[*]");
            viewer.allow.add("csapi[*]/get");
            viewer.allow.add("csapi[*]/list");
            viewer.allow.add("csapi[*]/stream");
            viewer.allow.add("bucketservice[*]/get");
            viewer.allow.add("bucketservice[*]/list");
            viewer.deny.add("webadmin[*]");
            viewer.deny.add("csapi[*]/create");
            viewer.deny.add("csapi[*]/update");
            viewer.deny.add("csapi[*]/delete");
            viewer.deny.add("bucketservice[*]/put");
            viewer.deny.add("bucketservice[*]/create");
            viewer.deny.add("bucketservice[*]/delete");
            cfg.roles.add(viewer);
            changed = true;
        }

        if (changed) {
            try {
                boolean wasStarted = realm.isStarted();
                if (wasStarted)
                    realm.stop();
                realm.init();
                if (wasStarted)
                    realm.start();
                saveHubConfigurationQuietly();
            } catch (SensorHubException e) {
                log.warn("Could not refresh BasicSecurityRealm after seeding default OSCAR roles", e);
            }
        }
    }

    private static RoleConfig findRole(BasicSecurityRealmConfig cfg, String roleId) {
        if (cfg.roles == null)
            return null;
        for (RoleConfig r : cfg.roles) {
            if (roleId.equals(r.roleID))
                return r;
        }
        return null;
    }

    private static UserConfig findRealmUser(BasicSecurityRealmConfig cfg, String userId) {
        if (cfg.users == null)
            return null;
        for (UserConfig u : cfg.users) {
            if (userId.equals(u.userID))
                return u;
        }
        return null;
    }

    private void loadFromDisk() {
        if (storeFile == null || !Files.isRegularFile(storeFile))
            return;
        try {
            String raw = Files.readString(storeFile, StandardCharsets.UTF_8);
            PersistedRoot root = GSON.fromJson(raw, PersistedRoot.class);
            if (root == null)
                return;
            if (root.users != null) {
                for (Map.Entry<String, UserProfileRecord> e : root.users.entrySet()) {
                    UserProfileRecord rec = e.getValue();
                    if (rec == null)
                        continue;
                    if (rec.userId == null)
                        rec.userId = e.getKey();
                    if (rec.passkeyUserId == null || !PasswordHasher.isValidUnpaddedBase64Url(rec.passkeyUserId))
                        rec.passkeyUserId = PasswordHasher.randomBase64Url(32);
                    if (rec.allowedViews == null)
                        rec.allowedViews = new ArrayList<>();
                    if (rec.passkeys == null)
                        rec.passkeys = new ArrayList<>();
                    profiles.put(rec.userId, rec);
                }
            }
            if (persistSessions && root.sessions != null) {
                sessionsByHash.putAll(root.sessions);
            }
            if (root.federationTokens != null) {
                for (FederationTokenRecord tok : root.federationTokens.values()) {
                    if (tok != null && tok.id != null && tok.tokenSha256 != null) {
                        federationTokensById.put(tok.id, tok);
                        federationTokensByHash.put(tok.tokenSha256, tok);
                    }
                }
            }
            if (root.enrollmentInvites != null) {
                long now = System.currentTimeMillis();
                for (EnrollmentInviteRecord inv : root.enrollmentInvites.values()) {
                    if (inv != null && inv.tokenSha256 != null && inv.username != null && inv.expiresAt > now) {
                        enrollmentInvitesByHash.put(inv.tokenSha256, inv);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to read OSCAR identity store from {}", storeFile, e);
        }
    }

    private synchronized void reconcileWithSecurityRealm() {
        IUserRegistry registry = safeUserRegistry();
        if (registry == null)
            return;
        long now = System.currentTimeMillis();
        boolean dirty = false;
        for (IUserInfo user : registry.values()) {
            if (user == null || user.getId() == null || user.getId().startsWith(FED_SERVICE_USER_PREFIX))
                continue;
            UserProfileRecord profile = profiles.get(user.getId());
            if (profile == null) {
                profile = new UserProfileRecord();
                profile.userId = user.getId();
                profile.displayName = (user.getName() != null && !user.getName().isBlank())
                        ? user.getName()
                        : user.getId();
                profile.enabled = true;
                profile.mustChangePassword = false;
                profile.passkeyUserId = PasswordHasher.randomBase64Url(32);
                profile.createdAt = now;
                profiles.put(profile.userId, profile);
                dirty = true;
            }
        }
        if (dirty)
            saveToDisk();
    }

    private IUserRegistry safeUserRegistry() {
        try {
            return securityManager != null ? securityManager.getUserRegistry() : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    public synchronized void saveToDisk() {
        if (storeFile == null)
            return;
        try {
            Path parent = storeFile.getParent();
            if (parent != null)
                Files.createDirectories(parent);

            PersistedRoot root = new PersistedRoot();
            List<String> sortedUsers = new ArrayList<>(profiles.keySet());
            sortedUsers.sort(String.CASE_INSENSITIVE_ORDER);
            for (String u : sortedUsers) {
                root.users.put(u, profiles.get(u));
            }
            if (persistSessions) {
                root.sessions.putAll(sessionsByHash);
            }
            for (FederationTokenRecord tok : federationTokensById.values()) {
                root.federationTokens.put(tok.id, tok);
            }
            long saveNow = System.currentTimeMillis();
            for (EnrollmentInviteRecord inv : enrollmentInvitesByHash.values()) {
                if (inv != null && inv.id != null && inv.expiresAt > saveNow) {
                    root.enrollmentInvites.put(inv.id, inv);
                }
            }

            Path tmp = storeFile.resolveSibling(storeFile.getFileName().toString() + ".tmp");
            Files.writeString(tmp, GSON.toJson(root) + "\n", StandardCharsets.UTF_8);
            try {
                Files.setPosixFilePermissions(tmp, EnumSet.of(
                        PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
            } catch (UnsupportedOperationException ignored) {
                // Non-POSIX filesystem in Windows local unit tests.
            }
            try {
                Files.move(tmp, storeFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, storeFile, StandardCopyOption.REPLACE_EXISTING);
            }
            lastSessionFlushAt = System.currentTimeMillis();
        } catch (IOException e) {
            log.error("Failed to persist OSCAR identity store to {}", storeFile, e);
        }
    }

    private void saveHubConfigurationQuietly() {
        if (hub == null || hub.getModuleRegistry() == null)
            return;
        try {
            hub.getModuleRegistry().saveModulesConfiguration();
        } catch (Exception e) {
            log.warn("Could not save SensorHub configuration after user update", e);
        }
    }

    // -------------------------------------------------------------------------
    // Rate Limiting
    // -------------------------------------------------------------------------

    public boolean isRateLimited(String remoteAddr, String username) {
        long now = System.currentTimeMillis();
        return isBucketBlocked(rateBuckets.get("ip:" + safeKey(remoteAddr)), now)
                || (username != null && isBucketBlocked(rateBuckets.get("user:" + safeKey(username.toLowerCase(Locale.ROOT))), now));
    }

    private static boolean isBucketBlocked(RateBucket b, long now) {
        if (b == null)
            return false;
        synchronized (b) {
            return b.blockedUntil > now;
        }
    }

    public void recordAuthFailure(String remoteAddr, String username) {
        long now = System.currentTimeMillis();
        incrementBucket("ip:" + safeKey(remoteAddr), now);
        if (username != null && !username.isBlank()) {
            incrementBucket("user:" + safeKey(username.toLowerCase(Locale.ROOT)), now);
        }
        if (rateBuckets.size() > 2048) {
            rateBuckets.entrySet().removeIf(e -> {
                synchronized (e.getValue()) {
                    return now - e.getValue().windowStartedAt > RATE_LIMIT_WINDOW_MILLIS
                            && e.getValue().blockedUntil <= now;
                }
            });
        }
    }

    public void recordAuthSuccess(String remoteAddr, String username) {
        if (username != null) {
            rateBuckets.remove("user:" + safeKey(username.toLowerCase(Locale.ROOT)));
            UserProfileRecord profile = ensureProfile(username);
            profile.lastLoginAt = System.currentTimeMillis();
            saveToDisk();
        }
    }

    private void incrementBucket(String key, long now) {
        RateBucket b = rateBuckets.computeIfAbsent(key, k -> new RateBucket());
        synchronized (b) {
            if (now - b.windowStartedAt > RATE_LIMIT_WINDOW_MILLIS) {
                b.failures = 0;
                b.windowStartedAt = now;
            }
            b.failures++;
            if (b.failures >= RATE_LIMIT_MAX_FAILURES) {
                b.blockedUntil = now + RATE_LIMIT_COOLDOWN_MILLIS;
            }
        }
    }

    private static String safeKey(String raw) {
        return raw == null ? "unknown" : raw.trim();
    }

    // -------------------------------------------------------------------------
    // Sessions (SHA-256 hashed on disk)
    // -------------------------------------------------------------------------

    public String createSession(String previousRawToken, String username, boolean keepSignedIn) {
        if (previousRawToken != null && !previousRawToken.isEmpty()) {
            sessionsByHash.remove(PasswordHasher.sha256Hex(previousRawToken));
        }
        String rawToken = PasswordHasher.randomBase64Url(32);
        String tokenHash = PasswordHasher.sha256Hex(rawToken);
        long now = System.currentTimeMillis();
        sessionsByHash.put(tokenHash, new PersistedSession(tokenHash, username, now, keepSignedIn));
        if (persistSessions)
            saveToDisk();
        return rawToken;
    }

    public PersistedSession validateAndTouchSession(String rawToken,
                                                    long idleTimeoutMillis,
                                                    long absoluteTimeoutMillis) {
        if (rawToken == null || rawToken.isEmpty() || rawToken.length() > 256)
            return null;
        String tokenHash = PasswordHasher.sha256Hex(rawToken);
        PersistedSession session = sessionsByHash.get(tokenHash);
        if (session == null)
            return null;

        long now = System.currentTimeMillis();
        if (session.isExpired(now, idleTimeoutMillis, absoluteTimeoutMillis)
                || !isUserActive(session.username)) {
            sessionsByHash.remove(tokenHash);
            if (persistSessions)
                saveToDisk();
            return null;
        }

        session.lastAccessAt = now;
        if (persistSessions && now - lastSessionFlushAt >= SESSION_ACCESS_FLUSH_INTERVAL_MILLIS) {
            saveToDisk();
        }
        return session;
    }

    public void removeSession(String rawToken) {
        if (rawToken == null || rawToken.isEmpty())
            return;
        if (sessionsByHash.remove(PasswordHasher.sha256Hex(rawToken)) != null && persistSessions) {
            saveToDisk();
        }
    }

    public int revokeSessionsForUser(String username, String exceptRawToken) {
        String keepHash = (exceptRawToken != null && !exceptRawToken.isEmpty())
                ? PasswordHasher.sha256Hex(exceptRawToken)
                : null;
        int[] removed = {0};
        sessionsByHash.entrySet().removeIf(e -> {
            if (username.equals(e.getValue().username) && !e.getKey().equals(keepHash)) {
                removed[0]++;
                return true;
            }
            return false;
        });
        if (removed[0] > 0 && persistSessions)
            saveToDisk();
        return removed[0];
    }

    public int countActiveSessionsForUser(String username, long idleTimeoutMillis, long absoluteTimeoutMillis) {
        long now = System.currentTimeMillis();
        int count = 0;
        for (PersistedSession s : sessionsByHash.values()) {
            if (username.equals(s.username) && !s.isExpired(now, idleTimeoutMillis, absoluteTimeoutMillis))
                count++;
        }
        return count;
    }

    public void pruneExpiredSessions(long idleTimeoutMillis, long absoluteTimeoutMillis) {
        long now = System.currentTimeMillis();
        boolean removed = sessionsByHash.entrySet().removeIf(entry ->
                entry.getValue().isExpired(now, idleTimeoutMillis, absoluteTimeoutMillis)
                        || !isUserActive(entry.getValue().username));
        boolean removedInvites = enrollmentInvitesByHash.entrySet().removeIf(entry ->
                entry.getValue().expiresAt <= now || !isUserActive(entry.getValue().username));
        if ((removed || removedInvites) && persistSessions)
            saveToDisk();
    }

    public int revokeEnrollmentInvitesForUser(String username) {
        if (username == null || username.isEmpty())
            return 0;
        int[] removed = {0};
        enrollmentInvitesByHash.entrySet().removeIf(entry -> {
            if (username.equals(entry.getValue().username)) {
                removed[0]++;
                return true;
            }
            return false;
        });
        return removed[0];
    }

    public void closeInMemory() {
        challenges.clear();
        if (!persistSessions)
            sessionsByHash.clear();
    }

    // -------------------------------------------------------------------------
    // User Profiles, Roles & Passwords
    // -------------------------------------------------------------------------

    public boolean isUserActive(String username) {
        if (username == null || username.isEmpty())
            return false;
        if (securityManager.getUserInfo(username) == null)
            return false;
        UserProfileRecord profile = profiles.get(username);
        return profile == null || profile.enabled;
    }

    public synchronized UserProfileRecord ensureProfile(String username) {
        UserProfileRecord existing = profiles.get(username);
        if (existing != null) {
            if (existing.passkeyUserId == null || !PasswordHasher.isValidUnpaddedBase64Url(existing.passkeyUserId)) {
                existing.passkeyUserId = PasswordHasher.randomBase64Url(32);
                saveToDisk();
            }
            return existing;
        }
        IUserInfo info = securityManager != null ? securityManager.getUserInfo(username) : null;
        UserProfileRecord created = new UserProfileRecord();
        created.userId = username;
        created.displayName = (info != null && info.getName() != null && !info.getName().isBlank())
                ? info.getName()
                : username;
        created.enabled = true;
        created.mustChangePassword = false;
        created.passkeyUserId = PasswordHasher.randomBase64Url(32);
        created.createdAt = System.currentTimeMillis();
        profiles.put(username, created);
        saveToDisk();
        return created;
    }

    public UserProfileRecord getProfile(String username) {
        return username == null ? null : profiles.get(username);
    }

    public Set<String> getUserRoles(String username) {
        IUserInfo info = securityManager != null ? securityManager.getUserInfo(username) : null;
        if (info == null || info.getRoles() == null)
            return Collections.emptySet();
        return new LinkedHashSet<>(info.getRoles());
    }

    public String getPrimaryRole(String username) {
        Set<String> roles = getUserRoles(username);
        if (roles.contains(ROLE_ADMIN))
            return ROLE_ADMIN;
        if (roles.contains(ROLE_OPERATOR))
            return ROLE_OPERATOR;
        if (roles.contains(ROLE_VIEWER))
            return ROLE_VIEWER;
        return roles.isEmpty() ? ROLE_VIEWER : roles.iterator().next();
    }

    public boolean isAdmin(String username) {
        return getUserRoles(username).contains(ROLE_ADMIN);
    }

    public boolean canAdjudicate(String username) {
        Set<String> roles = getUserRoles(username);
        return roles.contains(ROLE_ADMIN) || roles.contains(ROLE_OPERATOR);
    }

    public static void validateNewPassword(String password) {
        if (password == null || password.length() < 12)
            throw new IllegalArgumentException("Password must be at least 12 characters long");
        if (password.length() > 512)
            throw new IllegalArgumentException("Password must not exceed 512 characters");
    }

    public static List<String> sanitizeAllowedViews(Collection<String> rawViews) {
        if (rawViews == null || rawViews.isEmpty())
            return new ArrayList<>();
        Set<String> unique = new LinkedHashSet<>();
        for (String v : rawViews) {
            if (v == null)
                continue;
            String normalized = v.trim().toLowerCase(Locale.ROOT);
            if (normalized.isEmpty())
                continue;
            if (!VIEW_KEY_PATTERN.matcher(normalized).matches())
                throw new IllegalArgumentException("Invalid Operational View key: " + v);
            unique.add(normalized);
        }
        return new ArrayList<>(unique);
    }

    public static String normalizeRoleFlexible(String role) {
        if (role == null || role.trim().isEmpty())
            return ROLE_OPERATOR;
        String cleaned = role.trim().toLowerCase(Locale.ROOT);
        switch (cleaned) {
            case ROLE_ADMIN:
            case "administrator":
            case "supervisor":
                return ROLE_ADMIN;
            case ROLE_OPERATOR:
            case "op":
            case "adjudicator":
            case "inspector":
                return ROLE_OPERATOR;
            case ROLE_VIEWER:
            case "readonly":
            case "read-only":
            case "auditor":
            case "guest":
                return ROLE_VIEWER;
            default:
                throw new IllegalArgumentException("Role must be admin, operator, or viewer (got '" + role + "')");
        }
    }

    private static String normalizeRole(String role) {
        if (role == null)
            throw new IllegalArgumentException("Role is required");
        return normalizeRoleFlexible(role);
    }

    public synchronized CreatedOrResetUserResult createUser(String username,
                                                            String displayName,
                                                            String role,
                                                            Collection<String> allowedViews,
                                                            String explicitPassword,
                                                            boolean mustChangePassword) {
        if (username == null || !USERNAME_PATTERN.matcher(username.trim()).matches())
            throw new IllegalArgumentException("Username must be 2–64 characters using letters, numbers, '.', '_', '@', or '-'");
        String cleanUser = username.trim();
        if (cleanUser.toLowerCase(Locale.ROOT).startsWith(FED_SERVICE_USER_PREFIX))
            throw new IllegalArgumentException("Prefix 'svc-fed-' is reserved for federation service tokens");

        if (securityManager.getUserInfo(cleanUser) != null)
            throw new IllegalArgumentException("User already exists: " + cleanUser);

        String normalizedRole = normalizeRole(role);
        List<String> sanitizedViews = sanitizeAllowedViews(allowedViews);
        String generatedPassword = null;
        String rawPassword;
        if (explicitPassword == null || explicitPassword.isBlank()) {
            generatedPassword = PasswordHasher.generateTemporaryPassword();
            rawPassword = generatedPassword;
            mustChangePassword = true;
        } else {
            validateNewPassword(explicitPassword);
            rawPassword = explicitPassword;
        }

        String hashed = PasswordHasher.hashPassword(rawPassword);
        String cleanDisplay = (displayName == null || displayName.isBlank()) ? cleanUser : displayName.trim();
        if (cleanDisplay.length() > 128)
            cleanDisplay = cleanDisplay.substring(0, 128);

        upsertRealmUser(cleanUser, cleanDisplay, hashed, normalizedRole);

        UserProfileRecord profile = new UserProfileRecord();
        profile.userId = cleanUser;
        profile.displayName = cleanDisplay;
        profile.enabled = true;
        profile.mustChangePassword = mustChangePassword;
        profile.passkeyUserId = PasswordHasher.randomBase64Url(32);
        profile.allowedViews = sanitizedViews;
        profile.authSource = "local";
        profile.createdAt = System.currentTimeMillis();
        profiles.put(cleanUser, profile);
        saveToDisk();
        saveHubConfigurationQuietly();
        return new CreatedOrResetUserResult(profile, normalizedRole, generatedPassword);
    }

    public synchronized UserProfileRecord provisionOrSyncExternalUser(String username,
                                                                      String displayName,
                                                                      String role,
                                                                      String authSource) {
        String normalizedRole = normalizeRole(role);
        IUserInfo existing = securityManager.getUserInfo(username);
        String cleanDisplay = (displayName == null || displayName.isBlank()) ? username : displayName.trim();
        if (existing == null) {
            String unguessable = PasswordHasher.hashPassword(PasswordHasher.randomBase64Url(32));
            upsertRealmUser(username, cleanDisplay, unguessable, normalizedRole);
        } else {
            UserProfileRecord currentProfile = profiles.get(username);
            if (currentProfile != null && "oidc".equals(currentProfile.authSource)) {
                upsertRealmUser(username, cleanDisplay, existing.getPassword(), normalizedRole);
            }
        }
        UserProfileRecord profile = ensureProfile(username);
        if (displayName != null && !displayName.isBlank())
            profile.displayName = cleanDisplay;
        if (existing == null)
            profile.authSource = authSource;
        saveToDisk();
        saveHubConfigurationQuietly();
        return profile;
    }

    public synchronized UserProfileRecord updateUser(String actorUsername,
                                                     String targetUsername,
                                                     String displayName,
                                                     String role,
                                                     Boolean enabled,
                                                     Collection<String> allowedViews) {
        IUserInfo existing = securityManager.getUserInfo(targetUsername);
        if (existing == null || targetUsername.startsWith(FED_SERVICE_USER_PREFIX))
            throw new IllegalArgumentException("User not found: " + targetUsername);

        UserProfileRecord profile = ensureProfile(targetUsername);
        String newRole = role != null ? normalizeRole(role) : getPrimaryRole(targetUsername);
        boolean newEnabled = enabled != null ? enabled : profile.enabled;

        if (!newEnabled && targetUsername.equals(actorUsername))
            throw new IllegalArgumentException("You cannot disable your own active administrator account");

        boolean wasActiveAdmin = profile.enabled && isAdmin(targetUsername);
        boolean willBeActiveAdmin = newEnabled && ROLE_ADMIN.equals(newRole);
        if (wasActiveAdmin && !willBeActiveAdmin && countActiveAdmins() <= 1)
            throw new IllegalArgumentException("Cannot remove or disable the last active Administrator account");

        String cleanDisplay = (displayName != null && !displayName.isBlank())
                ? displayName.trim()
                : profile.displayName;
        if (cleanDisplay.length() > 128)
            cleanDisplay = cleanDisplay.substring(0, 128);

        upsertRealmUser(targetUsername, cleanDisplay, existing.getPassword(), newRole);
        profile.displayName = cleanDisplay;
        profile.enabled = newEnabled;
        if (allowedViews != null) {
            profile.allowedViews = ROLE_ADMIN.equals(newRole)
                    ? new ArrayList<>()
                    : sanitizeAllowedViews(allowedViews);
        }
        if (!newEnabled) {
            revokeEnrollmentInvitesForUser(targetUsername);
            revokeSessionsForUser(targetUsername, null);
        }
        saveToDisk();
        saveHubConfigurationQuietly();
        return profile;
    }

    public synchronized CreatedOrResetUserResult adminResetPassword(String targetUsername,
                                                                    String explicitPassword,
                                                                    boolean mustChangePassword) {
        IUserInfo existing = securityManager.getUserInfo(targetUsername);
        if (existing == null || targetUsername.startsWith(FED_SERVICE_USER_PREFIX))
            throw new IllegalArgumentException("User not found: " + targetUsername);

        String generatedPassword = null;
        String rawPassword;
        if (explicitPassword == null || explicitPassword.isBlank()) {
            generatedPassword = PasswordHasher.generateTemporaryPassword();
            rawPassword = generatedPassword;
            mustChangePassword = true;
        } else {
            validateNewPassword(explicitPassword);
            rawPassword = explicitPassword;
        }

        String hashed = PasswordHasher.hashPassword(rawPassword);
        String currentRole = getPrimaryRole(targetUsername);
        UserProfileRecord profile = ensureProfile(targetUsername);
        upsertRealmUser(targetUsername, profile.displayName, hashed, currentRole);
        profile.mustChangePassword = mustChangePassword;
        revokeSessionsForUser(targetUsername, null);
        saveToDisk();
        saveHubConfigurationQuietly();
        return new CreatedOrResetUserResult(profile, currentRole, generatedPassword);
    }

    public synchronized void changeOwnPassword(String username,
                                               String currentPassword,
                                               String newPassword,
                                               String keepSessionRawToken) {
        IUserInfo existing = securityManager.getUserInfo(username);
        if (existing == null)
            throw new IllegalArgumentException("User not found");
        if (!PasswordHasher.verifyPassword(existing.getPassword(), currentPassword))
            throw new SecurityException("Current password is incorrect");
        validateNewPassword(newPassword);

        String hashed = PasswordHasher.hashPassword(newPassword);
        UserProfileRecord profile = ensureProfile(username);
        upsertRealmUser(username, profile.displayName, hashed, getPrimaryRole(username));
        profile.mustChangePassword = false;
        revokeEnrollmentInvitesForUser(username);
        revokeSessionsForUser(username, keepSessionRawToken);
        saveToDisk();
        saveHubConfigurationQuietly();
    }

    public synchronized void completeForcedPasswordChange(String username,
                                                          String newPassword,
                                                          String keepSessionRawToken) {
        IUserInfo existing = securityManager.getUserInfo(username);
        if (existing == null)
            throw new IllegalArgumentException("User not found");
        validateNewPassword(newPassword);

        String hashed = PasswordHasher.hashPassword(newPassword);
        UserProfileRecord profile = ensureProfile(username);
        upsertRealmUser(username, profile.displayName, hashed, getPrimaryRole(username));
        profile.mustChangePassword = false;
        revokeEnrollmentInvitesForUser(username);
        revokeSessionsForUser(username, keepSessionRawToken);
        saveToDisk();
        saveHubConfigurationQuietly();
    }

    public synchronized void deleteUser(String actorUsername, String targetUsername) {
        if (targetUsername == null || targetUsername.equals(actorUsername))
            throw new IllegalArgumentException("You cannot delete your own active account");
        IUserInfo existing = securityManager.getUserInfo(targetUsername);
        if (existing == null || targetUsername.startsWith(FED_SERVICE_USER_PREFIX))
            throw new IllegalArgumentException("User not found: " + targetUsername);

        UserProfileRecord profile = profiles.get(targetUsername);
        boolean wasActiveAdmin = (profile == null || profile.enabled) && isAdmin(targetUsername);
        if (wasActiveAdmin && countActiveAdmins() <= 1)
            throw new IllegalArgumentException("Cannot delete the last active Administrator account");

        removeRealmUser(targetUsername);
        profiles.remove(targetUsername);
        revokeEnrollmentInvitesForUser(targetUsername);
        revokeSessionsForUser(targetUsername, null);
        saveToDisk();
        saveHubConfigurationQuietly();
    }

    private int countActiveAdmins() {
        IUserRegistry registry = safeUserRegistry();
        if (registry == null)
            return 0;
        int count = 0;
        for (IUserInfo u : registry.values()) {
            if (u == null || u.getId() == null || u.getId().startsWith(FED_SERVICE_USER_PREFIX))
                continue;
            if (u.getRoles() != null && u.getRoles().contains(ROLE_ADMIN)) {
                UserProfileRecord p = profiles.get(u.getId());
                if (p == null || p.enabled)
                    count++;
            }
        }
        return count;
    }

    private void upsertRealmUser(String userId, String displayName, String encodedPassword, String role) {
        BasicSecurityRealm realm = findBasicRealm();
        ModuleRegistry moduleReg = hub != null ? hub.getModuleRegistry() : null;

        UserConfig userCfg = null;
        if (realm != null && realm.getConfiguration() != null) {
            userCfg = findRealmUser(realm.getConfiguration(), userId);
            if (userCfg == null) {
                userCfg = new UserConfig();
                userCfg.userID = userId;
                realm.getConfiguration().users.add(userCfg);
            }
        } else {
            userCfg = new UserConfig();
            userCfg.userID = userId;
        }

        userCfg.name = displayName;
        userCfg.password = encodedPassword;
        userCfg.roles.clear();
        userCfg.roles.add(role);
        userCfg.allow.clear();
        userCfg.deny.clear();
        applyRoleFallbackPermissions(userCfg, role);
        if (moduleReg != null) {
            userCfg.refreshPermissionLists(moduleReg);
        }

        IUserRegistry registry = safeUserRegistry();
        if (registry != null) {
            registry.put(userId, userCfg);
        }
    }

    private static void applyRoleFallbackPermissions(UserConfig userCfg, String role) {
        if (ROLE_ADMIN.equals(role)) {
            userCfg.allow.add("*");
        } else if (ROLE_OPERATOR.equals(role)) {
            userCfg.allow.add("*");
            userCfg.deny.add("webadmin[*]");
            userCfg.deny.add("csapi[*]/delete");
            userCfg.deny.add("bucketservice[*]/delete/sitemap");
            userCfg.deny.add("bucketservice[*]/put/spreadsheets");
            userCfg.deny.add("bucketservice[*]/create/spreadsheets");
            userCfg.deny.add("bucketservice[*]/delete/spreadsheets");
        } else {
            userCfg.allow.add("fileserver[*]");
            userCfg.allow.add("csapi[*]/get");
            userCfg.allow.add("csapi[*]/list");
            userCfg.allow.add("csapi[*]/stream");
            userCfg.allow.add("bucketservice[*]/get");
            userCfg.allow.add("bucketservice[*]/list");
            userCfg.deny.add("webadmin[*]");
            userCfg.deny.add("csapi[*]/create");
            userCfg.deny.add("csapi[*]/update");
            userCfg.deny.add("csapi[*]/delete");
            userCfg.deny.add("bucketservice[*]/put");
            userCfg.deny.add("bucketservice[*]/create");
            userCfg.deny.add("bucketservice[*]/delete");
        }
    }

    private void removeRealmUser(String userId) {
        BasicSecurityRealm realm = findBasicRealm();
        if (realm != null && realm.getConfiguration() != null && realm.getConfiguration().users != null) {
            realm.getConfiguration().users.removeIf(u -> userId.equals(u.userID));
        }
        IUserRegistry registry = safeUserRegistry();
        if (registry != null) {
            registry.remove(userId);
        }
    }

    public List<UserProfileRecord> listHumanUsers() {
        reconcileWithSecurityRealm();
        List<UserProfileRecord> out = new ArrayList<>();
        for (UserProfileRecord p : profiles.values()) {
            if (p != null && p.userId != null && !p.userId.startsWith(FED_SERVICE_USER_PREFIX)
                    && securityManager.getUserInfo(p.userId) != null) {
                out.add(p);
            }
        }
        out.sort(Comparator.comparing(p -> p.userId, String.CASE_INSENSITIVE_ORDER));
        return out;
    }

    // -------------------------------------------------------------------------
    // Passkeys (WebAuthn Credentials)
    // -------------------------------------------------------------------------

    public record PasskeyMatch(UserProfileRecord user, PasskeyRecord credential) {}

    public PasskeyMatch findPasskeyByCredentialId(String credentialId) {
        if (!PasswordHasher.isValidUnpaddedBase64Url(credentialId))
            return null;
        for (UserProfileRecord user : profiles.values()) {
            if (user == null || user.passkeys == null)
                continue;
            for (PasskeyRecord pk : user.passkeys) {
                if (credentialId.equals(pk.id))
                    return new PasskeyMatch(user, pk);
            }
        }
        return null;
    }

    public synchronized PasskeyRecord registerPasskey(String username,
                                                      WebAuthnCodec.VerifiedRegistration verified,
                                                      String customName,
                                                      List<String> transports) {
        if (findPasskeyByCredentialId(verified.credentialId) != null)
            throw new IllegalArgumentException("This passkey credential is already registered");

        UserProfileRecord user = ensureProfile(username);
        long now = System.currentTimeMillis();
        PasskeyRecord rec = new PasskeyRecord();
        rec.id = verified.credentialId;
        String cleanName = (customName != null && !customName.isBlank())
                ? customName.trim()
                : verified.authenticatorLabel;
        if (cleanName.length() > 64)
            cleanName = cleanName.substring(0, 64);
        rec.name = cleanName;
        rec.publicKeyCose = verified.publicKeyCoseB64Url;
        rec.algorithm = verified.coseAlgorithm;
        rec.aaguid = verified.aaguid;
        if (transports != null) {
            for (String t : transports) {
                if (t != null && t.matches("^[a-z-]{2,20}$"))
                    rec.transports.add(t);
            }
        }
        rec.signCount = verified.signCount;
        rec.backupEligible = verified.backupEligible;
        rec.backupState = verified.backupState;
        rec.createdAt = now;
        rec.lastUsedAt = now;

        user.passkeys.add(rec);
        saveToDisk();
        return rec;
    }

    public synchronized void recordPasskeyAssertion(String username,
                                                    String credentialId,
                                                    WebAuthnCodec.VerifiedAssertion assertion) {
        UserProfileRecord user = profiles.get(username);
        if (user == null || user.passkeys == null)
            return;
        long now = System.currentTimeMillis();
        user.lastLoginAt = now;
        for (PasskeyRecord pk : user.passkeys) {
            if (credentialId.equals(pk.id)) {
                pk.signCount = assertion.signCount;
                pk.backupState = assertion.backupState;
                pk.lastUsedAt = now;
                break;
            }
        }
        saveToDisk();
    }

    public synchronized PasskeyRecord renamePasskey(String username, String credentialId, String newName) {
        if (newName == null || newName.trim().isEmpty() || newName.trim().length() > 64)
            throw new IllegalArgumentException("Passkey name must be between 1 and 64 characters");
        UserProfileRecord user = ensureProfile(username);
        for (PasskeyRecord pk : user.passkeys) {
            if (credentialId.equals(pk.id)) {
                pk.name = newName.trim();
                saveToDisk();
                return pk;
            }
        }
        throw new IllegalArgumentException("Passkey not found");
    }

    public synchronized boolean deletePasskey(String username, String credentialId) {
        UserProfileRecord user = ensureProfile(username);
        boolean removed = user.passkeys.removeIf(pk -> credentialId.equals(pk.id));
        if (removed)
            saveToDisk();
        return removed;
    }

    // -------------------------------------------------------------------------
    // Ephemeral Challenges (WebAuthn & OIDC PKCE)
    // -------------------------------------------------------------------------

    public PendingChallenge createChallenge(String purpose,
                                            String username,
                                            String rpId,
                                            String redirectTarget,
                                            boolean keepSignedIn,
                                            String oidcCodeVerifier,
                                            String oidcNonce) {
        long now = System.currentTimeMillis();
        challenges.entrySet().removeIf(e -> e.getValue().expiresAt() <= now);
        if (challenges.size() >= MAX_PENDING_CHALLENGES) {
            challenges.keySet().stream().findFirst().ifPresent(challenges::remove);
        }
        String token = PasswordHasher.randomBase64Url(32);
        PendingChallenge created = new PendingChallenge(
                token,
                purpose,
                username,
                rpId,
                redirectTarget,
                keepSignedIn,
                oidcCodeVerifier,
                oidcNonce,
                now + CHALLENGE_TTL_MILLIS);
        challenges.put(token, created);
        return created;
    }

    public PendingChallenge consumeChallenge(String token, String expectedPurpose) {
        if (token == null || token.isEmpty())
            return null;
        PendingChallenge removed = challenges.remove(token);
        if (removed == null)
            return null;
        if (removed.expiresAt() <= System.currentTimeMillis())
            return null;
        if (expectedPurpose != null && !expectedPurpose.equals(removed.purpose()))
            return null;
        return removed;
    }

    // -------------------------------------------------------------------------
    // Federation Service Tokens (HTTP Bearer + MQTT Scoped Service User)
    // -------------------------------------------------------------------------

    public synchronized CreatedFederationToken createFederationToken(String label,
                                                                     String role,
                                                                     int expiresInDays,
                                                                     String createdBy) {
        if (label == null || label.trim().isEmpty() || label.trim().length() > 80)
            throw new IllegalArgumentException("Federation token label must be 1–80 characters");
        String normalizedRole = normalizeRole(role);
        if (ROLE_ADMIN.equals(normalizedRole))
            throw new IllegalArgumentException("Federation tokens must be scoped to viewer or operator, never admin");

        String shortId = PasswordHasher.randomHex(6);
        String secret = PasswordHasher.randomBase64Url(32);
        String rawToken = FED_TOKEN_PREFIX + shortId + "_" + secret;
        String serviceUserId = FED_SERVICE_USER_PREFIX + shortId;
        long now = System.currentTimeMillis();

        upsertRealmUser(serviceUserId, "Federation: " + label.trim(), PasswordHasher.hashPassword(rawToken), normalizedRole);

        FederationTokenRecord rec = new FederationTokenRecord();
        rec.id = shortId;
        rec.serviceUserId = serviceUserId;
        rec.label = label.trim();
        rec.role = normalizedRole;
        rec.tokenSha256 = PasswordHasher.sha256Hex(rawToken);
        rec.tokenPrefix = FED_TOKEN_PREFIX + shortId + "_...";
        rec.enabled = true;
        rec.createdAt = now;
        rec.createdBy = createdBy;
        rec.expiresAt = expiresInDays > 0 ? now + (expiresInDays * 86_400_000L) : 0L;

        federationTokensById.put(shortId, rec);
        federationTokensByHash.put(rec.tokenSha256, rec);
        saveToDisk();
        saveHubConfigurationQuietly();
        return new CreatedFederationToken(rec, rawToken);
    }

    public FederationTokenRecord authenticateFederationToken(String rawBearerToken) {
        if (rawBearerToken == null || !rawBearerToken.startsWith(FED_TOKEN_PREFIX) || rawBearerToken.length() > 256)
            return null;
        String hash = PasswordHasher.sha256Hex(rawBearerToken);
        FederationTokenRecord rec = federationTokensByHash.get(hash);
        if (rec == null || !rec.enabled)
            return null;
        long now = System.currentTimeMillis();
        if (rec.expiresAt > 0 && now >= rec.expiresAt)
            return null;
        if (securityManager.getUserInfo(rec.serviceUserId) == null)
            return null;
        rec.lastUsedAt = now;
        return rec;
    }

    public synchronized boolean revokeFederationToken(String id) {
        FederationTokenRecord removed = federationTokensById.remove(id);
        if (removed == null)
            return false;
        federationTokensByHash.remove(removed.tokenSha256);
        removeRealmUser(removed.serviceUserId);
        saveToDisk();
        saveHubConfigurationQuietly();
        return true;
    }

    public List<FederationTokenRecord> listFederationTokens() {
        List<FederationTokenRecord> list = new ArrayList<>(federationTokensById.values());
        list.sort(Comparator.comparingLong((FederationTokenRecord r) -> r.createdAt).reversed());
        return list;
    }

    // -------------------------------------------------------------------------
    // One-Time User Enrollment Invites & Bulk Roster Provisioning
    // -------------------------------------------------------------------------

    private IssuedEnrollmentInvite mintEnrollmentInviteUnsaved(String username,
                                                               int expiresInHours,
                                                               String createdBy) {
        int clampedHours = expiresInHours <= 0
                ? DEFAULT_INVITE_EXPIRY_HOURS
                : Math.min(expiresInHours, 24 * 30);
        revokeEnrollmentInvitesForUser(username);

        String shortId = PasswordHasher.randomHex(6);
        String secret = PasswordHasher.randomBase64Url(32);
        String rawToken = INVITE_TOKEN_PREFIX + shortId + "_" + secret;
        long now = System.currentTimeMillis();

        EnrollmentInviteRecord rec = new EnrollmentInviteRecord();
        rec.id = shortId;
        rec.username = username;
        rec.tokenSha256 = PasswordHasher.sha256Hex(rawToken);
        rec.tokenPrefix = INVITE_TOKEN_PREFIX + shortId + "_...";
        rec.createdAt = now;
        rec.expiresAt = now + (clampedHours * 3_600_000L);
        rec.createdBy = createdBy;

        enrollmentInvitesByHash.put(rec.tokenSha256, rec);
        return new IssuedEnrollmentInvite(rec, rawToken);
    }

    public synchronized IssuedEnrollmentInvite issueEnrollmentInvite(String username,
                                                                     int expiresInHours,
                                                                     String createdBy) {
        if (!isUserActive(username) || username.startsWith(FED_SERVICE_USER_PREFIX))
            throw new IllegalArgumentException("Active user not found: " + username);
        IssuedEnrollmentInvite issued = mintEnrollmentInviteUnsaved(username, expiresInHours, createdBy);
        saveToDisk();
        return issued;
    }

    public EnrollmentInviteRecord peekValidEnrollmentInvite(String rawToken) {
        if (rawToken == null || !rawToken.startsWith(INVITE_TOKEN_PREFIX) || rawToken.length() > 256)
            return null;
        String hash = PasswordHasher.sha256Hex(rawToken.trim());
        EnrollmentInviteRecord rec = enrollmentInvitesByHash.get(hash);
        if (rec == null)
            return null;
        long now = System.currentTimeMillis();
        if (rec.expiresAt <= now || !isUserActive(rec.username)) {
            return null;
        }
        return rec;
    }

    public synchronized UserProfileRecord consumeInviteWithPassword(String rawToken, String newPassword) {
        EnrollmentInviteRecord inv = peekValidEnrollmentInvite(rawToken);
        if (inv == null)
            throw new SecurityException("Enrollment invite is invalid, expired, or has already been used.");
        validateNewPassword(newPassword);

        String username = inv.username;
        IUserInfo existing = securityManager.getUserInfo(username);
        if (existing == null)
            throw new SecurityException("User account no longer exists.");

        String hashed = PasswordHasher.hashPassword(newPassword);
        UserProfileRecord profile = ensureProfile(username);
        upsertRealmUser(username, profile.displayName, hashed, getPrimaryRole(username));
        profile.mustChangePassword = false;
        profile.lastLoginAt = System.currentTimeMillis();
        revokeEnrollmentInvitesForUser(username);
        saveToDisk();
        saveHubConfigurationQuietly();
        return profile;
    }

    public synchronized UserProfileRecord consumeInviteWithPasskey(String rawToken,
                                                                   WebAuthnCodec.VerifiedRegistration verified,
                                                                   String customName,
                                                                   List<String> transports,
                                                                   String optionalNewPassword) {
        EnrollmentInviteRecord inv = peekValidEnrollmentInvite(rawToken);
        if (inv == null)
            throw new SecurityException("Enrollment invite is invalid, expired, or has already been used.");
        if (findPasskeyByCredentialId(verified.credentialId) != null)
            throw new IllegalArgumentException("This passkey credential is already registered");

        String username = inv.username;
        IUserInfo existing = securityManager.getUserInfo(username);
        if (existing == null)
            throw new SecurityException("User account no longer exists.");

        String passwordHashToSave;
        if (optionalNewPassword != null && !optionalNewPassword.isBlank()) {
            validateNewPassword(optionalNewPassword);
            passwordHashToSave = PasswordHasher.hashPassword(optionalNewPassword);
        } else {
            // Rotate away any one-time temporary password so an unused fallback code cannot be replayed later
            passwordHashToSave = PasswordHasher.hashPassword(PasswordHasher.randomBase64Url(32));
        }

        UserProfileRecord profile = ensureProfile(username);
        upsertRealmUser(username, profile.displayName, passwordHashToSave, getPrimaryRole(username));

        long now = System.currentTimeMillis();
        PasskeyRecord pk = new PasskeyRecord();
        pk.id = verified.credentialId;
        String cleanPkName = (customName != null && !customName.isBlank())
                ? customName.trim()
                : verified.authenticatorLabel;
        if (cleanPkName.length() > 64)
            cleanPkName = cleanPkName.substring(0, 64);
        pk.name = cleanPkName;
        pk.publicKeyCose = verified.publicKeyCoseB64Url;
        pk.algorithm = verified.coseAlgorithm;
        pk.aaguid = verified.aaguid;
        if (transports != null) {
            for (String t : transports) {
                if (t != null && t.matches("^[a-z-]{2,20}$"))
                    pk.transports.add(t);
            }
        }
        pk.signCount = verified.signCount;
        pk.backupEligible = verified.backupEligible;
        pk.backupState = verified.backupState;
        pk.createdAt = now;
        pk.lastUsedAt = now;

        profile.passkeys.add(pk);
        profile.mustChangePassword = false;
        profile.lastLoginAt = now;
        revokeEnrollmentInvitesForUser(username);
        saveToDisk();
        saveHubConfigurationQuietly();
        return profile;
    }

    public static String humanizeUsername(String username) {
        if (username == null || username.isBlank())
            return "";
        String base = username.trim();
        int at = base.indexOf('@');
        if (at > 0)
            base = base.substring(0, at);
        String[] parts = base.split("[._-]+");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty())
                continue;
            if (sb.length() > 0)
                sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1)
                sb.append(part.substring(1));
        }
        return sb.length() > 0 ? sb.toString() : username.trim();
    }

    public synchronized BulkEnrollmentOutcome bulkEnrollUsers(List<BulkUserSpec> specs,
                                                              boolean reissueExisting,
                                                              int inviteExpiresInHours,
                                                              String actorUsername) {
        if (specs == null || specs.isEmpty())
            throw new IllegalArgumentException("At least one user row is required");
        if (specs.size() > MAX_BULK_ENROLL_USERS)
            throw new IllegalArgumentException("Bulk enrollment is limited to " + MAX_BULK_ENROLL_USERS + " users per batch");

        int createdCount = 0;
        int reissuedCount = 0;
        int skippedCount = 0;
        List<BulkEnrolledUser> enrolled = new ArrayList<>();
        List<BulkRowError> errors = new ArrayList<>();
        Set<String> seenInBatch = new LinkedHashSet<>();
        boolean mutated = false;

        int rowNum = 0;
        for (BulkUserSpec spec : specs) {
            rowNum++;
            String rawUser = spec != null && spec.username() != null ? spec.username().trim() : "";
            if (rawUser.isEmpty()) {
                errors.add(new BulkRowError(rowNum, "", "Missing username"));
                continue;
            }
            String lowerKey = rawUser.toLowerCase(Locale.ROOT);
            if (!seenInBatch.add(lowerKey)) {
                errors.add(new BulkRowError(rowNum, rawUser, "Duplicate username in uploaded list"));
                continue;
            }
            if (!USERNAME_PATTERN.matcher(rawUser).matches()) {
                errors.add(new BulkRowError(rowNum, rawUser,
                        "Username must be 2–64 characters using letters, numbers, '.', '_', '@', or '-'"));
                continue;
            }
            if (lowerKey.startsWith(FED_SERVICE_USER_PREFIX)) {
                errors.add(new BulkRowError(rowNum, rawUser, "Prefix 'svc-fed-' is reserved"));
                continue;
            }

            final String normalizedRole;
            final List<String> sanitizedViews;
            try {
                normalizedRole = normalizeRoleFlexible(spec.role());
                sanitizedViews = ROLE_ADMIN.equals(normalizedRole)
                        ? new ArrayList<>()
                        : sanitizeAllowedViews(spec.allowedViews());
            } catch (IllegalArgumentException ex) {
                errors.add(new BulkRowError(rowNum, rawUser, ex.getMessage()));
                continue;
            }

            String cleanDisplay = (spec.displayName() == null || spec.displayName().isBlank())
                    ? humanizeUsername(rawUser)
                    : spec.displayName().trim();
            if (cleanDisplay.length() > 128)
                cleanDisplay = cleanDisplay.substring(0, 128);

            IUserInfo existing = securityManager.getUserInfo(rawUser);
            if (existing != null) {
                if (!reissueExisting) {
                    skippedCount++;
                    continue;
                }
                if (rawUser.equals(actorUsername)) {
                    skippedCount++;
                    continue;
                }
                UserProfileRecord existingProfile = ensureProfile(rawUser);
                boolean wasActiveAdmin = existingProfile.enabled && isAdmin(rawUser);
                if (wasActiveAdmin && !ROLE_ADMIN.equals(normalizedRole) && countActiveAdmins() <= 1) {
                    errors.add(new BulkRowError(rowNum, rawUser, "Cannot demote the last active Administrator"));
                    continue;
                }

                String tempPw = PasswordHasher.generateTemporaryPassword();
                upsertRealmUser(rawUser, cleanDisplay, PasswordHasher.hashPassword(tempPw), normalizedRole);
                existingProfile.displayName = cleanDisplay;
                existingProfile.enabled = true;
                existingProfile.mustChangePassword = true;
                existingProfile.allowedViews = sanitizedViews;
                revokeSessionsForUser(rawUser, null);
                IssuedEnrollmentInvite invite = mintEnrollmentInviteUnsaved(rawUser, inviteExpiresInHours, actorUsername);
                enrolled.add(new BulkEnrolledUser(
                        existingProfile,
                        normalizedRole,
                        false,
                        tempPw,
                        invite.rawToken(),
                        invite.metadata().expiresAt));
                reissuedCount++;
                mutated = true;
                continue;
            }

            String tempPw = PasswordHasher.generateTemporaryPassword();
            upsertRealmUser(rawUser, cleanDisplay, PasswordHasher.hashPassword(tempPw), normalizedRole);

            UserProfileRecord createdProfile = new UserProfileRecord();
            createdProfile.userId = rawUser;
            createdProfile.displayName = cleanDisplay;
            createdProfile.enabled = true;
            createdProfile.mustChangePassword = true;
            createdProfile.passkeyUserId = PasswordHasher.randomBase64Url(32);
            createdProfile.allowedViews = sanitizedViews;
            createdProfile.authSource = "local";
            createdProfile.createdAt = System.currentTimeMillis();
            profiles.put(rawUser, createdProfile);

            IssuedEnrollmentInvite invite = mintEnrollmentInviteUnsaved(rawUser, inviteExpiresInHours, actorUsername);
            enrolled.add(new BulkEnrolledUser(
                    createdProfile,
                    normalizedRole,
                    true,
                    tempPw,
                    invite.rawToken(),
                    invite.metadata().expiresAt));
            createdCount++;
            mutated = true;
        }

        if (mutated) {
            saveToDisk();
            saveHubConfigurationQuietly();
        }
        return new BulkEnrollmentOutcome(createdCount, reissuedCount, skippedCount, enrolled, errors);
    }
}
