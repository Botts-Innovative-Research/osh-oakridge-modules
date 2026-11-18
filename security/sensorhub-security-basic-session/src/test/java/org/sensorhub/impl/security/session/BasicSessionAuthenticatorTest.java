package org.sensorhub.impl.security.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Authentication;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sensorhub.api.security.IPermissionPath;
import org.sensorhub.api.security.ISecurityManager;
import org.sensorhub.api.security.IUserInfo;
import org.sensorhub.api.security.IUserRegistry;
import org.sensorhub.impl.security.BasicSecurityRealmConfig.UserConfig;
import org.slf4j.LoggerFactory;


public class BasicSessionAuthenticatorTest
{
    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void rendersDefaultEnglishLoginWithAutofillPasskeysAndStrictCsp() throws Exception
    {
        BasicSessionAuthenticator auth = createAuthenticator(null, new InMemoryUserRegistry());
        ResponseCapture resp = new ResponseCapture();
        HttpServletRequest req = createRequest("GET", "/login", Map.of(), null, Map.of("Accept", "text/html"));

        Authentication result = auth.validateRequest(req, resp.proxy(), true);

        assertEquals(Authentication.SEND_CONTINUE, result);
        assertEquals(200, resp.status);
        assertEquals(
            "default-src 'none'; style-src 'unsafe-inline'; script-src 'self'; connect-src 'self'; img-src 'self' data:; frame-ancestors 'none'; base-uri 'none'; form-action 'self'",
            resp.headers.get("Content-Security-Policy"));

        String html = resp.bodyUtf8();
        assertTrue(html.contains("<html lang=\"en\">"));
        assertTrue(html.contains("<title>OSCAR sign in</title>"));
        assertTrue(html.contains("Sign in to continue.</p>"));
        assertTrue(html.contains("<label for=\"username\">Username</label>"));
        assertTrue(html.contains("id=\"username\""));
        assertTrue(html.contains("autocomplete=\"username webauthn\""));
        assertTrue(html.contains("spellcheck=\"false\""));
        assertTrue(html.contains("<label for=\"current-password\">Password</label>"));
        assertTrue(html.contains("id=\"current-password\""));
        assertTrue(html.contains("autocomplete=\"current-password\""));
        assertTrue(html.contains("id=\"toggle-password\""));
        assertTrue(html.contains("id=\"keep-signed-in\""));
        assertTrue(html.contains("id=\"passkey-signin-btn\""));
        assertTrue(html.contains("<button type=\"submit\">Sign in</button>"));
    }


    @Test
    public void explicitLangQuerySetsCookieAndRendersGreekLoginPreservingContinue() throws Exception
    {
        BasicSessionAuthenticator auth = createAuthenticator(null, new InMemoryUserRegistry());
        ResponseCapture resp = new ResponseCapture();
        HttpServletRequest req = createRequest(
            "GET",
            "/login",
            Map.of("lang", "el", "continue", "/sensorhub/admin"),
            null,
            Map.of("Accept", "text/html"));

        auth.validateRequest(req, resp.proxy(), true);

        assertEquals(200, resp.status);
        String setCookie = resp.setCookies.isEmpty() ? null : resp.setCookies.get(0);
        assertNotNull("Expected osh-language Set-Cookie header", setCookie);
        assertTrue(setCookie.startsWith("osh-language=el; Path=/;"));
        assertTrue(setCookie.contains("Secure"));

        String html = resp.bodyUtf8();
        assertTrue(html.contains("<html lang=\"el\">"));
        assertTrue(html.contains("<title>Σύνδεση OSCAR</title>"));
        assertTrue(html.contains("Όνομα χρήστη"));
        assertTrue(html.contains("Κωδικός πρόσβασης"));
        assertTrue(html.contains("<button type=\"submit\">Σύνδεση</button>"));
        assertTrue(html.contains("href=\"/login?lang=es&amp;continue=%2Fsensorhub%2Fadmin\""));
    }


    @Test
    public void resolvesCookieAndAcceptLanguageAndRendersLocalizedError() throws Exception
    {
        BasicSessionAuthenticator auth = createAuthenticator(null, new InMemoryUserRegistry());

        ResponseCapture esResp = new ResponseCapture();
        HttpServletRequest esReq = createRequest(
            "GET",
            "/login",
            Map.of(),
            new Cookie[] {new Cookie("osh-language", "es")},
            Map.of("Accept", "text/html", "Accept-Language", "en-US,en;q=0.9"));
        auth.validateRequest(esReq, esResp.proxy(), true);
        assertTrue(esResp.bodyUtf8().contains("<html lang=\"es\">"));
        assertTrue(esResp.bodyUtf8().contains("<title>Iniciar sesión en OSCAR</title>"));

        ResponseCapture frResp = new ResponseCapture();
        HttpServletRequest frReq = createRequest(
            "POST",
            "/login",
            Map.of("username", "operator"),
            null,
            Map.of("Accept", "text/html", "Accept-Language", "de-DE,fr-FR;q=0.9,en;q=0.8"));
        auth.validateRequest(frReq, frResp.proxy(), true);
        assertEquals(401, frResp.status);
        assertTrue(frResp.bodyUtf8().contains("<html lang=\"fr\">"));
        assertTrue(frResp.bodyUtf8().contains("Nom d’utilisateur ou mot de passe incorrect."));
    }


    @Test
    public void persistentZeroTimeoutSessionSurvivesRestartUntilExplicitLogout() throws Exception
    {
        Path storePath = tempFolder.newFile("oscar-identity.json").toPath();
        Files.deleteIfExists(storePath);

        InMemoryUserRegistry registry = new InMemoryUserRegistry();
        registry.addAdmin("admin", "StrongAdminPassword!23");

        BasicSessionConfig cfg = new BasicSessionConfig();
        cfg.idleTimeoutSeconds = 0;
        cfg.absoluteTimeoutSeconds = 0;
        cfg.persistSessions = true;
        cfg.allowKeepSignedIn = true;

        BasicSessionAuthenticator auth1 = createAuthenticator(cfg, storePath, registry);

        // Form login with Keep Me Signed In
        ResponseCapture loginResp = new ResponseCapture();
        HttpServletRequest loginReq = createRequest(
            "POST",
            "/login",
            Map.of("username", "admin", "password", "StrongAdminPassword!23", "keepSignedIn", "true"),
            null,
            Map.of("Accept", "text/html"));
        auth1.validateRequest(loginReq, loginResp.proxy(), true);
        assertEquals("/", loginResp.redirectLocation);
        String cookieHeader = loginResp.setCookies.get(0);
        assertTrue("Expected persistent Max-Age cookie", cookieHeader.contains("Max-Age=31536000"));
        String sessionToken = extractCookieValue(cookieHeader, "OSCAR_SESSION");
        assertNotNull(sessionToken);

        // Ensure raw session token is NEVER written to disk — only its SHA-256 digest
        String onDiskJson = Files.readString(storePath, StandardCharsets.UTF_8);
        assertFalse("Raw session token must never appear on disk", onDiskJson.contains(sessionToken));
        assertTrue(onDiskJson.contains(PasswordHasher.sha256Hex(sessionToken)));

        auth1.close();

        // Simulate OSCAR container restart / upgrade with a brand new authenticator instance
        BasicSessionAuthenticator auth2 = createAuthenticator(cfg, storePath, registry);
        ResponseCapture sessResp = new ResponseCapture();
        HttpServletRequest sessReq = createRequest(
            "GET",
            "/sensorhub/session",
            Map.of(),
            new Cookie[] {new Cookie("OSCAR_SESSION", sessionToken)},
            Map.of("Host", "oscar.local"));
        auth2.validateRequest(sessReq, sessResp.proxy(), true);
        assertEquals(200, sessResp.status);
        JsonObject sessJson = JsonParser.parseString(sessResp.bodyUtf8()).getAsJsonObject();
        assertEquals("admin", sessJson.get("username").getAsString());
        assertTrue(sessJson.get("isAdmin").getAsBoolean());
        assertTrue(sessJson.get("keepSignedIn").getAsBoolean());

        // Explicit logout immediately invalidates the persisted session
        ResponseCapture logoutResp = new ResponseCapture();
        HttpServletRequest logoutReq = createRequest(
            "POST",
            "/logout",
            Map.of(),
            new Cookie[] {new Cookie("OSCAR_SESSION", sessionToken)},
            Map.of());
        auth2.validateRequest(logoutReq, logoutResp.proxy(), true);
        assertEquals(204, logoutResp.status);

        ResponseCapture afterLogoutResp = new ResponseCapture();
        auth2.validateRequest(sessReq, afterLogoutResp.proxy(), true);
        assertEquals(401, afterLogoutResp.status);
    }


    @Test
    public void registersAndAuthenticatesPasskeyAndSignals404WhenDeleted() throws Exception
    {
        Path storePath = tempFolder.newFile("oscar-passkeys.json").toPath();
        Files.deleteIfExists(storePath);

        InMemoryUserRegistry registry = new InMemoryUserRegistry();
        registry.addAdmin("admin", "StrongAdminPassword!23");

        BasicSessionConfig cfg = new BasicSessionConfig();
        BasicSessionAuthenticator auth = createAuthenticator(cfg, storePath, registry);

        // Sign in with password first
        ResponseCapture loginResp = new ResponseCapture();
        auth.validateRequest(createRequest(
            "POST", "/login",
            Map.of("username", "admin", "password", "StrongAdminPassword!23"),
            null, Map.of("Host", "oscar.local")), loginResp.proxy(), true);
        Cookie sessionCookie = new Cookie("OSCAR_SESSION",
            extractCookieValue(loginResp.setCookies.get(0), "OSCAR_SESSION"));

        // 1. Request passkey registration options
        ResponseCapture regOptsResp = new ResponseCapture();
        auth.validateRequest(createJsonRequest(
            "POST", "/sensorhub/session/account/passkeys/options", "{}",
            new Cookie[] {sessionCookie}, "oscar.local"), regOptsResp.proxy(), true);
        assertEquals(200, regOptsResp.status);
        JsonObject regOpts = JsonParser.parseString(regOptsResp.bodyUtf8()).getAsJsonObject();
        String regChallenge = regOpts.get("challenge").getAsString();
        assertEquals("oscar.local", regOpts.getAsJsonObject("rp").get("id").getAsString());
        assertTrue(PasswordHasher.isValidUnpaddedBase64Url(
            regOpts.getAsJsonObject("user").get("id").getAsString()));

        // 2. Build real ES256 attestation & verify registration
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
        kpg.initialize(new ECGenParameterSpec("secp256r1"));
        KeyPair kp = kpg.generateKeyPair();
        byte[] credIdBytes = "admin-yubikey-001".getBytes(StandardCharsets.UTF_8);
        String credId = PasswordHasher.encodeBase64Url(credIdBytes);
        byte[] coseKey = WebAuthnCodecTest.buildEc2CosePublicKey((ECPublicKey) kp.getPublic());
        byte[] regAuthData = WebAuthnCodecTest.buildAuthData(
            "oscar.local", (byte) 0x45, 1L, new byte[16], credIdBytes, coseKey);
        byte[] attObj = WebAuthnCodecTest.buildAttestationObject(regAuthData);
        String regClientJson = "{\"type\":\"webauthn.create\",\"challenge\":\"" + regChallenge
            + "\",\"origin\":\"https://oscar.local\"}";

        String regBody = "{\"challenge\":\"" + regChallenge + "\",\"name\":\"Shift Supervisor Key\","
            + "\"credential\":{\"id\":\"" + credId + "\",\"type\":\"public-key\",\"response\":{"
            + "\"clientDataJSON\":\"" + PasswordHasher.encodeBase64Url(regClientJson.getBytes(StandardCharsets.UTF_8)) + "\","
            + "\"attestationObject\":\"" + PasswordHasher.encodeBase64Url(attObj) + "\","
            + "\"transports\":[\"usb\",\"nfc\"]}}}";

        ResponseCapture regVerifyResp = new ResponseCapture();
        auth.validateRequest(createJsonRequest(
            "POST", "/sensorhub/session/account/passkeys/verify", regBody,
            new Cookie[] {sessionCookie}, "oscar.local"), regVerifyResp.proxy(), true);
        assertEquals(200, regVerifyResp.status);
        JsonObject pkList = JsonParser.parseString(regVerifyResp.bodyUtf8()).getAsJsonObject();
        assertEquals(1, pkList.getAsJsonArray("passkeys").size());
        assertEquals("Shift Supervisor Key",
            pkList.getAsJsonArray("passkeys").get(0).getAsJsonObject().get("name").getAsString());

        // 3. Unauthenticated Passkey Sign-In
        ResponseCapture authOptsResp = new ResponseCapture();
        auth.validateRequest(createJsonRequest(
            "POST", "/login/webauthn/options", "{\"continue\":\"/event-log\",\"keepSignedIn\":true}",
            null, "oscar.local"), authOptsResp.proxy(), true);
        assertEquals(200, authOptsResp.status);
        String authChallenge = JsonParser.parseString(authOptsResp.bodyUtf8())
            .getAsJsonObject().get("challenge").getAsString();

        byte[] authClientBytes = ("{\"type\":\"webauthn.get\",\"challenge\":\"" + authChallenge
            + "\",\"origin\":\"https://oscar.local\"}").getBytes(StandardCharsets.UTF_8);
        byte[] authData = WebAuthnCodecTest.buildAuthData("oscar.local", (byte) 0x05, 2L, null, null, null);
        byte[] clientHash = PasswordHasher.sha256(authClientBytes);
        byte[] signed = new byte[authData.length + clientHash.length];
        System.arraycopy(authData, 0, signed, 0, authData.length);
        System.arraycopy(clientHash, 0, signed, authData.length, clientHash.length);
        Signature sig = Signature.getInstance("SHA256withECDSA");
        sig.initSign(kp.getPrivate());
        sig.update(signed);
        byte[] sigBytes = sig.sign();

        String loginVerifyBody = "{\"challenge\":\"" + authChallenge + "\",\"continue\":\"/event-log\","
            + "\"keepSignedIn\":true,\"credential\":{\"id\":\"" + credId + "\",\"type\":\"public-key\","
            + "\"response\":{\"clientDataJSON\":\"" + PasswordHasher.encodeBase64Url(authClientBytes) + "\","
            + "\"authenticatorData\":\"" + PasswordHasher.encodeBase64Url(authData) + "\","
            + "\"signature\":\"" + PasswordHasher.encodeBase64Url(sigBytes) + "\"}}}";

        ResponseCapture loginVerifyResp = new ResponseCapture();
        auth.validateRequest(createJsonRequest(
            "POST", "/login/webauthn/verify", loginVerifyBody, null, "oscar.local"),
            loginVerifyResp.proxy(), true);
        assertEquals(200, loginVerifyResp.status);
        JsonObject loginVerifyJson = JsonParser.parseString(loginVerifyResp.bodyUtf8()).getAsJsonObject();
        assertEquals("admin", loginVerifyJson.get("username").getAsString());
        assertEquals("/event-log", loginVerifyJson.get("redirectUrl").getAsString());
        assertFalse(loginVerifyResp.setCookies.isEmpty());

        // 4. Delete the passkey, then confirm login returns HTTP 404 unknownCredential
        ResponseCapture delResp = new ResponseCapture();
        auth.validateRequest(createJsonRequest(
            "DELETE", "/sensorhub/session/account/passkeys/" + credId, "",
            new Cookie[] {sessionCookie}, "oscar.local"), delResp.proxy(), true);
        assertEquals(200, delResp.status);

        ResponseCapture authOpts2 = new ResponseCapture();
        auth.validateRequest(createJsonRequest(
            "POST", "/login/webauthn/options", "{}", null, "oscar.local"),
            authOpts2.proxy(), true);
        String challenge2 = JsonParser.parseString(authOpts2.bodyUtf8())
            .getAsJsonObject().get("challenge").getAsString();
        String deletedVerifyBody = loginVerifyBody.replace(authChallenge, challenge2);

        ResponseCapture deletedResp = new ResponseCapture();
        auth.validateRequest(createJsonRequest(
            "POST", "/login/webauthn/verify", deletedVerifyBody, null, "oscar.local"),
            deletedResp.proxy(), true);
        assertEquals(404, deletedResp.status);
        assertTrue(deletedResp.bodyUtf8().contains("\"unknownCredential\":true"));
    }


    @Test
    public void enforcesAdminUserLifecycleScopedViewsMustChangePasswordAndFederationTokens() throws Exception
    {
        Path storePath = tempFolder.newFile("oscar-admin-users.json").toPath();
        Files.deleteIfExists(storePath);

        InMemoryUserRegistry registry = new InMemoryUserRegistry();
        registry.addAdmin("admin", "StrongAdminPassword!23");

        BasicSessionConfig cfg = new BasicSessionConfig();
        BasicSessionAuthenticator auth = createAuthenticator(cfg, storePath, registry);

        // Sign in as admin
        ResponseCapture adminLogin = new ResponseCapture();
        auth.validateRequest(createRequest(
            "POST", "/login",
            Map.of("username", "admin", "password", "StrongAdminPassword!23"),
            null, Map.of("Host", "oscar.local")), adminLogin.proxy(), true);
        Cookie adminCookie = new Cookie("OSCAR_SESSION",
            extractCookieValue(adminLogin.setCookies.get(0), "OSCAR_SESSION"));

        // Cannot delete or demote the sole active admin
        ResponseCapture selfDelete = new ResponseCapture();
        auth.validateRequest(createJsonRequest(
            "DELETE", "/sensorhub/session/admin/users/admin", "",
            new Cookie[] {adminCookie}, "oscar.local"), selfDelete.proxy(), true);
        assertEquals(400, selfDelete.status);

        // Create a gate-scoped Operator with auto-generated temporary password
        ResponseCapture createOpResp = new ResponseCapture();
        auth.validateRequest(createJsonRequest(
            "POST", "/sensorhub/session/admin/users",
            "{\"username\":\"gate_op\",\"displayName\":\"East Gate Operator\",\"role\":\"operator\",\"allowedViews\":[\"east-gate\"]}",
            new Cookie[] {adminCookie}, "oscar.local"), createOpResp.proxy(), true);
        assertEquals(201, createOpResp.status);
        JsonObject createdPayload = JsonParser.parseString(createOpResp.bodyUtf8()).getAsJsonObject();
        String tempPassword = createdPayload.get("temporaryPassword").getAsString();
        assertEquals(19, tempPassword.length());

        // Sign in as gate_op -> redirected to /login/password-change because mustChangePassword=true
        ResponseCapture opLogin = new ResponseCapture();
        auth.validateRequest(createRequest(
            "POST", "/login",
            Map.of("username", "gate_op", "password", tempPassword),
            null, Map.of("Host", "oscar.local")), opLogin.proxy(), true);
        assertTrue(opLogin.redirectLocation.startsWith("/login/password-change"));
        Cookie opCookie = new Cookie("OSCAR_SESSION",
            extractCookieValue(opLogin.setCookies.get(0), "OSCAR_SESSION"));

        // Complete forced password change
        ResponseCapture pwChange = new ResponseCapture();
        auth.validateRequest(createRequest(
            "POST", "/login/password-change",
            Map.of("newPassword", "OperatorPermanent!99", "confirmPassword", "OperatorPermanent!99", "continue", "/"),
            new Cookie[] {opCookie}, Map.of("Host", "oscar.local")), pwChange.proxy(), true);
        assertEquals("/", pwChange.redirectLocation);

        // Operator cannot access /admin/overview (403 Forbidden)
        ResponseCapture opForbidden = new ResponseCapture();
        auth.validateRequest(createJsonRequest(
            "GET", "/sensorhub/session/admin/overview", "",
            new Cookie[] {opCookie}, "oscar.local"), opForbidden.proxy(), true);
        assertEquals(403, opForbidden.status);

        // Operator /session reflects role=operator, canAdjudicate=true, allowedViews=["east-gate"]
        ResponseCapture opSess = new ResponseCapture();
        auth.validateRequest(createRequest(
            "GET", "/sensorhub/session", Map.of(),
            new Cookie[] {opCookie}, Map.of("Host", "oscar.local")), opSess.proxy(), true);
        JsonObject opSessJson = JsonParser.parseString(opSess.bodyUtf8()).getAsJsonObject();
        assertEquals("operator", opSessJson.get("primaryRole").getAsString());
        assertFalse(opSessJson.get("isAdmin").getAsBoolean());
        assertTrue(opSessJson.get("canAdjudicate").getAsBoolean());
        assertEquals("east-gate", opSessJson.getAsJsonArray("allowedViews").get(0).getAsString());

        // Mint a read-only Federation Token as Admin, authenticate with Bearer, then revoke
        ResponseCapture fedCreate = new ResponseCapture();
        auth.validateRequest(createJsonRequest(
            "POST", "/sensorhub/session/admin/federation-tokens",
            "{\"label\":\"HQ National View\",\"role\":\"viewer\",\"expiresInDays\":90}",
            new Cookie[] {adminCookie}, "oscar.local"), fedCreate.proxy(), true);
        assertEquals(201, fedCreate.status);
        JsonObject fedJson = JsonParser.parseString(fedCreate.bodyUtf8()).getAsJsonObject();
        String rawBearer = fedJson.get("token").getAsString();
        String tokenId = fedJson.getAsJsonObject("record").get("id").getAsString();
        assertTrue(rawBearer.startsWith("oscar_fed_"));

        ResponseCapture bearerResp = new ResponseCapture();
        Authentication bearerAuth = auth.validateRequest(createRequest(
            "GET", "/sensorhub/api/systems", Map.of(), null,
            Map.of("Authorization", "Bearer " + rawBearer)), bearerResp.proxy(), true);
        assertTrue(bearerAuth instanceof Authentication.User);

        // Revoke token -> immediate 401
        ResponseCapture revokeResp = new ResponseCapture();
        auth.validateRequest(createJsonRequest(
            "DELETE", "/sensorhub/session/admin/federation-tokens/" + tokenId, "",
            new Cookie[] {adminCookie}, "oscar.local"), revokeResp.proxy(), true);
        assertEquals(200, revokeResp.status);

        ResponseCapture revokedBearerResp = new ResponseCapture();
        Authentication afterRevoke = auth.validateRequest(createRequest(
            "GET", "/sensorhub/api/systems", Map.of(), null,
            Map.of("Authorization", "Bearer " + rawBearer)), revokedBearerResp.proxy(), true);
        assertEquals(Authentication.SEND_FAILURE, afterRevoke);
        assertEquals(401, revokedBearerResp.status);
    }


    @Test
    public void supportsBulkUserUploadAndSingleUseQrInviteEnrollmentWithoutLinkPreviewBurn() throws Exception
    {
        Path storePath = tempFolder.newFile("oscar-bulk-enroll.json").toPath();
        Files.deleteIfExists(storePath);

        InMemoryUserRegistry registry = new InMemoryUserRegistry();
        registry.addAdmin("admin", "StrongAdminPassword!23");

        BasicSessionAuthenticator auth = createAuthenticator(new BasicSessionConfig(), storePath, registry);

        ResponseCapture adminLogin = new ResponseCapture();
        auth.validateRequest(createRequest(
            "POST", "/login",
            Map.of("username", "admin", "password", "StrongAdminPassword!23"),
            null, Map.of("Host", "oscar.local")), adminLogin.proxy(), true);
        Cookie adminCookie = new Cookie("OSCAR_SESSION",
            extractCookieValue(adminLogin.setCookies.get(0), "OSCAR_SESSION"));

        // Bulk enroll 2 new users + skip existing admin + flag 1 invalid row
        String bulkJson = "{\"reissueExisting\":false,\"inviteExpiresInHours\":168,\"users\":["
            + "{\"username\":\"maria.gatehouse\",\"role\":\"operator\",\"allowedViews\":[\"north-gate\"]},"
            + "{\"username\":\"sam.auditor\",\"displayName\":\"Sam Auditor\",\"role\":\"auditor\",\"allowedViews\":[]},"
            + "{\"username\":\"admin\",\"role\":\"admin\"},"
            + "{\"username\":\"bad user!!\",\"role\":\"operator\"}"
            + "]}";

        ResponseCapture bulkResp = new ResponseCapture();
        auth.validateRequest(createJsonRequest(
            "POST", "/sensorhub/session/admin/users/bulk", bulkJson,
            new Cookie[] {adminCookie}, "oscar.local"), bulkResp.proxy(), true);
        assertEquals(200, bulkResp.status);

        JsonObject bulkPayload = JsonParser.parseString(bulkResp.bodyUtf8()).getAsJsonObject();
        assertEquals(2, bulkPayload.get("createdCount").getAsInt());
        assertEquals(1, bulkPayload.get("skippedCount").getAsInt());
        assertEquals(1, bulkPayload.getAsJsonArray("errors").size());
        assertEquals(2, bulkPayload.getAsJsonArray("enrolled").size());

        JsonObject mariaEnrolled = bulkPayload.getAsJsonArray("enrolled").get(0).getAsJsonObject();
        assertEquals("Maria Gatehouse", mariaEnrolled.getAsJsonObject("user").get("displayName").getAsString());
        String mariaInviteToken = mariaEnrolled.get("inviteToken").getAsString();
        assertTrue(mariaInviteToken.startsWith("oscar_inv_"));

        JsonObject samEnrolled = bulkPayload.getAsJsonArray("enrolled").get(1).getAsJsonObject();
        assertEquals("viewer", samEnrolled.getAsJsonObject("user").get("role").getAsString());

        // Multiple GET previews (e.g. SMS/WhatsApp link preview + user opening browser) must NOT burn the invite
        for (int i = 0; i < 2; i++) {
            ResponseCapture getEnroll = new ResponseCapture();
            auth.validateRequest(createRequest(
                "GET", "/login/enroll", Map.of("token", mariaInviteToken),
                null, Map.of("Host", "oscar.local")), getEnroll.proxy(), true);
            assertEquals(200, getEnroll.status);
            assertTrue(getEnroll.bodyUtf8().contains("Maria Gatehouse"));
            assertTrue(getEnroll.bodyUtf8().contains("enroll-passkey-btn"));
        }

        // Complete enrollment via password -> signs user straight into their scoped Operational View and burns invite
        ResponseCapture postEnroll = new ResponseCapture();
        auth.validateRequest(createRequest(
            "POST", "/login/enroll/password",
            Map.of(
                "token", mariaInviteToken,
                "newPassword", "MariaPersonalPass!2026",
                "confirmPassword", "MariaPersonalPass!2026",
                "keepSignedIn", "true"),
            null, Map.of("Host", "oscar.local")), postEnroll.proxy(), true);
        assertEquals(302, postEnroll.status);
        assertEquals("/?view=north-gate", postEnroll.redirectLocation);
        Cookie mariaCookie = new Cookie("OSCAR_SESSION",
            extractCookieValue(postEnroll.setCookies.get(0), "OSCAR_SESSION"));

        // Maria is immediately signed in with mustChangePassword=false
        ResponseCapture mariaSess = new ResponseCapture();
        auth.validateRequest(createRequest(
            "GET", "/sensorhub/session", Map.of(),
            new Cookie[] {mariaCookie}, Map.of("Host", "oscar.local")), mariaSess.proxy(), true);
        JsonObject mariaSessJson = JsonParser.parseString(mariaSess.bodyUtf8()).getAsJsonObject();
        assertEquals("maria.gatehouse", mariaSessJson.get("username").getAsString());
        assertFalse(mariaSessJson.get("mustChangePassword").getAsBoolean());

        // Replaying the consumed invite link returns 410 Gone
        ResponseCapture replayEnroll = new ResponseCapture();
        auth.validateRequest(createRequest(
            "GET", "/login/enroll", Map.of("token", mariaInviteToken),
            null, Map.of("Host", "oscar.local")), replayEnroll.proxy(), true);
        assertEquals(410, replayEnroll.status);
    }


    @Test
    public void trustedTunnelGroupsHeaderMapsRolesAndValidatesExternalConsoleUrl() throws Exception
    {
        Path tempDir = java.nio.file.Files.createTempDirectory("oscar-tunnel-groups-test");
        Path storePath = tempDir.resolve("identity.json");
        InMemoryUserRegistry registry = new InMemoryUserRegistry();
        registry.addAdmin("admin", "AdminBootPass!2026");

        BasicSessionConfig cfg = new BasicSessionConfig();
        cfg.trustedTunnelSsoEnabled = true;
        cfg.trustedTunnelSharedSecret = "TunnelSharedSecretKey_123456";
        cfg.trustedTunnelAutoProvisionViewer = false;
        cfg.oidcAdminGroup = "oscar-admins";
        cfg.oidcOperatorGroup = "oscar-operators";
        BasicSessionAuthenticator auth = createAuthenticator(cfg, storePath, registry);

        // 1. Tunnel user with X-Octelium-Groups JSON array -> provisioned as operator even when autoProvisionViewer=false
        ResponseCapture opTunnel = new ResponseCapture();
        auth.validateRequest(createRequest(
            "GET", "/sensorhub/session", Map.of(), null,
            Map.of(
                "Host", "oscar.local",
                "X-Oscar-Tunnel-Secret", "TunnelSharedSecretKey_123456",
                "X-Octelium-User", "hub.operator",
                "X-Octelium-Groups", "[\"oscar-operators\",\"remote-staff\"]")),
            opTunnel.proxy(), true);
        assertEquals(200, opTunnel.status);
        JsonObject opJson = JsonParser.parseString(opTunnel.bodyUtf8()).getAsJsonObject();
        assertEquals("hub.operator", opJson.get("username").getAsString());
        assertEquals("operator", opJson.get("primaryRole").getAsString());
        assertTrue(opJson.get("canAdjudicate").getAsBoolean());

        // 2. Promoting that tunnel user's group in Authentik/Octelium updates role to admin on next tunnel request
        ResponseCapture promotedTunnel = new ResponseCapture();
        auth.validateRequest(createRequest(
            "GET", "/sensorhub/session", Map.of(), null,
            Map.of(
                "Host", "oscar.local",
                "X-Oscar-Tunnel-Secret", "TunnelSharedSecretKey_123456",
                "X-Octelium-User", "hub.operator",
                "X-Octelium-Groups", "oscar-admins, remote-staff")),
            promotedTunnel.proxy(), true);
        assertEquals(200, promotedTunnel.status);
        JsonObject promotedJson = JsonParser.parseString(promotedTunnel.bodyUtf8()).getAsJsonObject();
        assertEquals("admin", promotedJson.get("primaryRole").getAsString());
        assertTrue(promotedJson.get("isAdmin").getAsBoolean());

        // 3. Local admin account role is never downgraded by X-Octelium-Groups
        ResponseCapture localAdminTunnel = new ResponseCapture();
        auth.validateRequest(createRequest(
            "GET", "/sensorhub/session", Map.of(), null,
            Map.of(
                "Host", "oscar.local",
                "X-Oscar-Tunnel-Secret", "TunnelSharedSecretKey_123456",
                "X-Octelium-User", "admin",
                "X-Octelium-Groups", "unmapped-group")),
            localAdminTunnel.proxy(), true);
        assertEquals(200, localAdminTunnel.status);
        JsonObject localAdminJson = JsonParser.parseString(localAdminTunnel.bodyUtf8()).getAsJsonObject();
        assertEquals("admin", localAdminJson.get("primaryRole").getAsString());

        // 4. External SSO console URL validation accepts https:// and rejects non-https schemes
        assertEquals("https://auth.hub.example.org/if/admin/",
            BasicSessionAuthenticator.sanitizeExternalConsoleUrl("  https://auth.hub.example.org/if/admin/  "));
        assertEquals("", BasicSessionAuthenticator.sanitizeExternalConsoleUrl("   "));
        try {
            BasicSessionAuthenticator.sanitizeExternalConsoleUrl("http://auth.hub.example.org");
            throw new AssertionError("Expected http:// console URL to be rejected");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("https://"));
        }
        try {
            BasicSessionAuthenticator.sanitizeExternalConsoleUrl("javascript:alert(1)");
            throw new AssertionError("Expected javascript: console URL to be rejected");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("https://"));
        }
    }


    @Test
    public void oidcCustomCaCertificateLoaderMergesPemWithSystemTrust() throws Exception
    {
        Path tempDir = java.nio.file.Files.createTempDirectory("oscar-oidc-ca-test");
        javax.net.ssl.TrustManagerFactory tmf = javax.net.ssl.TrustManagerFactory.getInstance(
            javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm());
        tmf.init((java.security.KeyStore) null);
        javax.net.ssl.X509TrustManager sysTm = null;
        for (javax.net.ssl.TrustManager tm : tmf.getTrustManagers()) {
            if (tm instanceof javax.net.ssl.X509TrustManager x) {
                sysTm = x;
                break;
            }
        }
        assertNotNull(sysTm);
        java.security.cert.X509Certificate sampleCert = sysTm.getAcceptedIssuers()[0];
        String pem = "-----BEGIN CERTIFICATE-----\n"
            + java.util.Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII))
                .encodeToString(sampleCert.getEncoded())
            + "\n-----END CERTIFICATE-----\n";

        Path caDir = java.nio.file.Files.createDirectory(tempDir.resolve("oidc-ca"));
        Path caFile = caDir.resolve("hub-root-ca.crt");
        java.nio.file.Files.writeString(caFile, pem, StandardCharsets.US_ASCII);

        javax.net.ssl.SSLContext ctx = OidcClientHelper.buildCompositeSslContext(List.of(caFile));
        assertNotNull(ctx);
        assertEquals("TLS", ctx.getProtocol());
    }


    private static String extractCookieValue(String setCookieHeader, String name)
    {
        String prefix = name + "=";
        if (!setCookieHeader.startsWith(prefix))
            return null;
        int semi = setCookieHeader.indexOf(';');
        return semi >= 0 ? setCookieHeader.substring(prefix.length(), semi) : setCookieHeader.substring(prefix.length());
    }


    private static BasicSessionAuthenticator createAuthenticator(Path storePath, InMemoryUserRegistry registry)
    {
        return createAuthenticator(new BasicSessionConfig(), storePath, registry);
    }


    private static BasicSessionAuthenticator createAuthenticator(
            BasicSessionConfig config,
            Path storePath,
            InMemoryUserRegistry registry)
    {
        ISecurityManager securityManager = (ISecurityManager) Proxy.newProxyInstance(
            ISecurityManager.class.getClassLoader(),
            new Class<?>[] {ISecurityManager.class},
            (proxy, method, args) -> {
                if ("getUserRegistry".equals(method.getName()))
                    return registry;
                if ("getUserInfo".equals(method.getName()))
                    return registry.get(args[0]);
                if ("isAccessControlEnabled".equals(method.getName()))
                    return true;
                return null;
            });
        return new BasicSessionAuthenticator(
            config,
            null,
            securityManager,
            storePath,
            LoggerFactory.getLogger(BasicSessionAuthenticatorTest.class));
    }


    private static final class InMemoryUserRegistry extends ConcurrentSkipListMap<String, IUserInfo>
            implements IUserRegistry
    {
        void addAdmin(String username, String plainPassword)
        {
            UserConfig u = new UserConfig();
            u.userID = username;
            u.name = "Administrator";
            u.password = PasswordHasher.hashPassword(plainPassword);
            u.roles.add("admin");
            put(username, u);
        }

        @Override
        public void addAllowPermissions(String id, Collection<IPermissionPath> permissions) {}

        @Override
        public void addDenyPermissions(String id, Collection<IPermissionPath> permissions) {}

        @Override
        public void removeAllowPermissions(String id, Collection<IPermissionPath> permissions) {}

        @Override
        public void removeDenyPermissions(String id, Collection<IPermissionPath> permissions) {}
    }


    private static HttpServletRequest createJsonRequest(
            String method,
            String path,
            String jsonBody,
            Cookie[] cookies,
            String host)
    {
        byte[] bytes = (jsonBody == null ? "" : jsonBody).getBytes(StandardCharsets.UTF_8);
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Accept", "application/json");
        headers.put("Host", host);
        headers.put("Origin", "https://" + host);
        headers.put("Sec-Fetch-Site", "same-origin");
        return createRequestWithBody(method, path, Map.of(), cookies, headers, bytes);
    }


    private static HttpServletRequest createRequest(
            String method,
            String path,
            Map<String, String> params,
            Cookie[] cookies,
            Map<String, String> headers)
    {
        return createRequestWithBody(method, path, params, cookies, headers, new byte[0]);
    }


    private static HttpServletRequest createRequestWithBody(
            String method,
            String path,
            Map<String, String> params,
            Cookie[] cookies,
            Map<String, String> headers,
            byte[] bodyBytes)
    {
        return (HttpServletRequest) Proxy.newProxyInstance(
            HttpServletRequest.class.getClassLoader(),
            new Class<?>[] {HttpServletRequest.class},
            (proxy, m, args) -> {
                switch (m.getName())
                {
                    case "getMethod":
                        return method;
                    case "getContextPath":
                        return "";
                    case "getServletPath":
                    case "getRequestURI":
                        return path;
                    case "getScheme":
                        return "https";
                    case "getServerName":
                        return headers.getOrDefault("Host", "oscar.local");
                    case "getServerPort":
                        return 443;
                    case "getRemoteAddr":
                        return "127.0.0.1";
                    case "getParameter":
                        return params.get(args[0]);
                    case "getCookies":
                        return cookies;
                    case "getHeader": {
                        String key = (String) args[0];
                        for (Map.Entry<String, String> e : headers.entrySet()) {
                            if (e.getKey().equalsIgnoreCase(key))
                                return e.getValue();
                        }
                        return null;
                    }
                    case "getInputStream": {
                        ByteArrayInputStream bais = new ByteArrayInputStream(bodyBytes);
                        return new ServletInputStream() {
                            @Override public boolean isFinished() { return bais.available() == 0; }
                            @Override public boolean isReady() { return true; }
                            @Override public void setReadListener(ReadListener readListener) {}
                            @Override public int read() { return bais.read(); }
                        };
                    }
                    default:
                        return null;
                }
            });
    }


    private static final class ResponseCapture
    {
        int status = 200;
        String redirectLocation;
        final Map<String, String> headers = new HashMap<>();
        final List<String> setCookies = new ArrayList<>();
        final ByteArrayOutputStream out = new ByteArrayOutputStream();

        HttpServletResponse proxy()
        {
            ServletOutputStream servletOut = new ServletOutputStream()
            {
                @Override
                public boolean isReady() { return true; }

                @Override
                public void setWriteListener(WriteListener writeListener) {}

                @Override
                public void write(int b) { out.write(b); }
            };

            return (HttpServletResponse) Proxy.newProxyInstance(
                HttpServletResponse.class.getClassLoader(),
                new Class<?>[] {HttpServletResponse.class},
                (p, m, args) -> {
                    switch (m.getName())
                    {
                        case "setStatus":
                            status = (Integer) args[0];
                            return null;
                        case "sendError":
                            status = (Integer) args[0];
                            return null;
                        case "sendRedirect":
                            status = 302;
                            redirectLocation = (String) args[0];
                            return null;
                        case "setHeader":
                            headers.put((String) args[0], (String) args[1]);
                            return null;
                        case "addHeader":
                            if ("Set-Cookie".equalsIgnoreCase((String) args[0]))
                                setCookies.add((String) args[1]);
                            headers.put((String) args[0], (String) args[1]);
                            return null;
                        case "getOutputStream":
                            return servletOut;
                        case "encodeRedirectURL":
                            return args[0];
                        default:
                            return null;
                    }
                });
        }

        String bodyUtf8()
        {
            return out.toString(StandardCharsets.UTF_8);
        }
    }
}
