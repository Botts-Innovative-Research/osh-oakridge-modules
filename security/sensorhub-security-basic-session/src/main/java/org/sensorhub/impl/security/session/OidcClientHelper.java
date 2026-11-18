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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * Minimal, dependency-free OpenID Connect Authorization Code + PKCE client
 * with JWKS ID-token verification (`RS256` and `ES256`), optional custom PEM
 * CA trust bundle support, and group-to-role mapping for optional enterprise
 * or Hub SSO alongside local offline OSCAR accounts.
 */
public final class OidcClientHelper {
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
    private static final long DISCOVERY_TTL_MILLIS = 15 * 60_000L;
    private static final long MAX_CA_FILE_BYTES = 1024 * 1024L;

    private final HttpClient fixedHttpClient;
    private final HttpClient defaultHttpClient;
    private volatile CachedDiscovery cachedDiscovery;
    private volatile CachedTlsClient cachedTlsClient;

    public OidcClientHelper() {
        this.fixedHttpClient = null;
        this.defaultHttpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    OidcClientHelper(HttpClient httpClient) {
        this.fixedHttpClient = httpClient;
        this.defaultHttpClient = httpClient;
    }

    private record CachedTlsClient(String pathKey, String fingerprint, HttpClient client) {}

    public static final class OidcUserClaims {
        public final String subject;
        public final String username;
        public final String displayName;
        public final List<String> groups;
        public final String mappedRole;

        OidcUserClaims(String subject, String username, String displayName,
                       List<String> groups, String mappedRole) {
            this.subject = subject;
            this.username = username;
            this.displayName = displayName;
            this.groups = groups;
            this.mappedRole = mappedRole;
        }
    }

    private record CachedDiscovery(
            String issuer,
            String authorizationEndpoint,
            String tokenEndpoint,
            String jwksUri,
            long expiresAt
    ) {}

    public String buildAuthorizationUrl(BasicSessionConfig config,
                                        String redirectUri,
                                        String state,
                                        String nonce,
                                        String codeVerifier) throws IOException, InterruptedException {
        HttpClient client = clientFor(config);
        CachedDiscovery disc = discover(client, config.oidcIssuerUrl);
        byte[] verifierChallenge = PasswordHasher.sha256(codeVerifier.getBytes(StandardCharsets.US_ASCII));
        String codeChallenge = PasswordHasher.encodeBase64Url(verifierChallenge);
        String scopes = (config.oidcScopes == null || config.oidcScopes.isBlank())
                ? "openid profile email"
                : config.oidcScopes.trim();

        return disc.authorizationEndpoint()
                + (disc.authorizationEndpoint().contains("?") ? "&" : "?")
                + "response_type=code"
                + "&client_id=" + urlEncode(config.oidcClientId)
                + "&redirect_uri=" + urlEncode(redirectUri)
                + "&scope=" + urlEncode(scopes)
                + "&state=" + urlEncode(state)
                + "&nonce=" + urlEncode(nonce)
                + "&code_challenge=" + urlEncode(codeChallenge)
                + "&code_challenge_method=S256";
    }

    public OidcUserClaims exchangeCodeAndVerify(BasicSessionConfig config,
                                                String code,
                                                String redirectUri,
                                                String expectedNonce,
                                                String codeVerifier) throws IOException, InterruptedException {
        HttpClient client = clientFor(config);
        CachedDiscovery disc = discover(client, config.oidcIssuerUrl);

        StringBuilder form = new StringBuilder();
        form.append("grant_type=authorization_code")
                .append("&code=").append(urlEncode(code))
                .append("&redirect_uri=").append(urlEncode(redirectUri))
                .append("&client_id=").append(urlEncode(config.oidcClientId))
                .append("&code_verifier=").append(urlEncode(codeVerifier));
        if (config.oidcClientSecret != null && !config.oidcClientSecret.isEmpty()) {
            form.append("&client_secret=").append(urlEncode(config.oidcClientSecret));
        }

        HttpRequest tokenReq = HttpRequest.newBuilder(URI.create(disc.tokenEndpoint()))
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(form.toString(), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> tokenResp = client.send(tokenReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (tokenResp.statusCode() != 200)
            throw new IOException("OIDC token endpoint returned HTTP " + tokenResp.statusCode());

        JsonObject tokenJson = JsonParser.parseString(tokenResp.body()).getAsJsonObject();
        String idToken = getString(tokenJson, "id_token");
        if (idToken == null || idToken.isBlank())
            throw new IOException("OIDC token response did not include an id_token");

        JsonObject payload = verifyJwtSignatureAndClaims(
                client, idToken, disc.issuer(), config.oidcClientId, expectedNonce, disc.jwksUri());

        String sub = getString(payload, "sub");
        if (sub == null || sub.isBlank())
            throw new IOException("OIDC id_token is missing sub claim");

        String rawUser = firstNonBlank(
                getString(payload, "preferred_username"),
                getString(payload, "email"),
                sub);
        String sanitizedUsername = sanitizeOidcUsername(rawUser);
        String displayName = firstNonBlank(
                getString(payload, "name"),
                getString(payload, "email"),
                sanitizedUsername);

        List<String> groups = new ArrayList<>();
        extractStringClaims(payload.get("groups"), groups);
        extractStringClaims(payload.get("roles"), groups);

        String mappedRole = resolveRole(config, groups);
        return new OidcUserClaims(sub, sanitizedUsername, displayName, groups, mappedRole);
    }

    static String resolveRole(BasicSessionConfig config, List<String> groups) {
        if (config.oidcAdminGroup != null && !config.oidcAdminGroup.isBlank()
                && containsIgnoreCase(groups, config.oidcAdminGroup.trim())) {
            return OscarIdentityStore.ROLE_ADMIN;
        }
        if (config.oidcOperatorGroup != null && !config.oidcOperatorGroup.isBlank()
                && containsIgnoreCase(groups, config.oidcOperatorGroup.trim())) {
            return OscarIdentityStore.ROLE_OPERATOR;
        }
        String def = config.oidcDefaultRole == null ? "" : config.oidcDefaultRole.trim().toLowerCase(Locale.ROOT);
        if (OscarIdentityStore.ROLE_ADMIN.equals(def) || OscarIdentityStore.ROLE_OPERATOR.equals(def))
            return def;
        return OscarIdentityStore.ROLE_VIEWER;
    }

    static String sanitizeOidcUsername(String raw) {
        if (raw == null || raw.isBlank())
            return "sso-user";
        String cleaned = raw.trim().replaceAll("[^A-Za-z0-9._@-]", "_");
        if (cleaned.length() > 64)
            cleaned = cleaned.substring(0, 64);
        if (cleaned.length() < 2)
            cleaned = "sso_" + cleaned;
        if (cleaned.toLowerCase(Locale.ROOT).startsWith("svc-fed-"))
            cleaned = "sso_" + cleaned;
        return cleaned;
    }

    static List<String> parseTunnelGroupsHeader(String rawHeader) {
        List<String> groups = new ArrayList<>();
        if (rawHeader == null || rawHeader.isBlank())
            return groups;
        String trimmed = rawHeader.trim();
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            try {
                JsonElement parsed = JsonParser.parseString(trimmed);
                extractStringClaims(parsed, groups);
                return groups;
            } catch (RuntimeException ignored) {
                // Fall back to delimited parsing below.
            }
        }
        for (String token : trimmed.split("[,;]")) {
            String cleaned = token.trim();
            if (cleaned.length() >= 2 && cleaned.startsWith("\"") && cleaned.endsWith("\"")) {
                cleaned = cleaned.substring(1, cleaned.length() - 1).trim();
            }
            if (!cleaned.isEmpty())
                groups.add(cleaned);
        }
        return groups;
    }

    private HttpClient clientFor(BasicSessionConfig config) throws IOException {
        if (fixedHttpClient != null)
            return fixedHttpClient;
        String rawCaPath = config == null ? null : config.oidcTrustedCaCertificatePath;
        if (rawCaPath == null || rawCaPath.isBlank())
            return defaultHttpClient;

        Path path = Path.of(rawCaPath.trim()).toAbsolutePath().normalize();
        List<Path> certFiles = collectPemCertificateFiles(path);
        StringBuilder fp = new StringBuilder();
        for (Path certFile : certFiles) {
            fp.append(certFile.toString())
                    .append('|').append(Files.size(certFile))
                    .append('|').append(Files.getLastModifiedTime(certFile, LinkOption.NOFOLLOW_LINKS).toMillis())
                    .append(';');
        }
        String fingerprint = fp.toString();
        CachedTlsClient existing = cachedTlsClient;
        if (existing != null && existing.pathKey().equals(path.toString()) && existing.fingerprint().equals(fingerprint)) {
            return existing.client();
        }

        SSLContext sslContext = buildCompositeSslContext(certFiles);
        HttpClient built = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NEVER)
                .sslContext(sslContext)
                .build();
        this.cachedTlsClient = new CachedTlsClient(path.toString(), fingerprint, built);
        return built;
    }

    private static List<Path> collectPemCertificateFiles(Path configuredPath) throws IOException {
        if (!Files.exists(configuredPath, LinkOption.NOFOLLOW_LINKS))
            throw new IOException("Configured OIDC CA certificate path does not exist: " + configuredPath);
        if (Files.isSymbolicLink(configuredPath))
            throw new IOException("Configured OIDC CA certificate path must not be a symbolic link: " + configuredPath);

        List<Path> files = new ArrayList<>();
        if (Files.isRegularFile(configuredPath, LinkOption.NOFOLLOW_LINKS)) {
            files.add(configuredPath);
        } else if (Files.isDirectory(configuredPath, LinkOption.NOFOLLOW_LINKS)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(configuredPath)) {
                for (Path entry : stream) {
                    if (Files.isSymbolicLink(entry) || !Files.isRegularFile(entry, LinkOption.NOFOLLOW_LINKS))
                        continue;
                    String lower = entry.getFileName().toString().toLowerCase(Locale.ROOT);
                    if (lower.endsWith(".crt") || lower.endsWith(".pem") || lower.endsWith(".cer")) {
                        files.add(entry);
                    }
                }
            }
            files.sort(Comparator.comparing(Path::toString));
            if (files.isEmpty())
                throw new IOException("Configured OIDC CA directory contains no .crt/.pem/.cer files: " + configuredPath);
        } else {
            throw new IOException("Configured OIDC CA certificate path is not a regular file or directory: " + configuredPath);
        }
        return files;
    }

    static SSLContext buildCompositeSslContext(List<Path> certFiles) throws IOException {
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            KeyStore customStore = KeyStore.getInstance(KeyStore.getDefaultType());
            customStore.load(null, null);
            int certCount = 0;
            for (Path file : certFiles) {
                long size = Files.size(file);
                if (size <= 0 || size > MAX_CA_FILE_BYTES)
                    throw new IOException("OIDC CA certificate file exceeds bounds (1B..1MiB): " + file);
                byte[] bytes = Files.readAllBytes(file);
                Collection<? extends Certificate> parsed = cf.generateCertificates(new ByteArrayInputStream(bytes));
                for (Certificate cert : parsed) {
                    if (cert instanceof X509Certificate x509) {
                        customStore.setCertificateEntry("oidc-ca-" + (++certCount), x509);
                    }
                }
            }
            if (certCount == 0)
                throw new IOException("No valid X.509 certificates found in configured OIDC CA path");

            TrustManagerFactory defaultTmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            defaultTmf.init((KeyStore) null);
            X509TrustManager defaultTm = findX509TrustManager(defaultTmf);

            TrustManagerFactory customTmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            customTmf.init(customStore);
            X509TrustManager customTm = findX509TrustManager(customTmf);

            X509TrustManager composite = new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) throws java.security.cert.CertificateException {
                    defaultTm.checkClientTrusted(chain, authType);
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) throws java.security.cert.CertificateException {
                    try {
                        customTm.checkServerTrusted(chain, authType);
                    } catch (java.security.cert.CertificateException customErr) {
                        defaultTm.checkServerTrusted(chain, authType);
                    }
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    X509Certificate[] defIssuers = defaultTm.getAcceptedIssuers();
                    X509Certificate[] custIssuers = customTm.getAcceptedIssuers();
                    X509Certificate[] merged = Arrays.copyOf(defIssuers, defIssuers.length + custIssuers.length);
                    System.arraycopy(custIssuers, 0, merged, defIssuers.length, custIssuers.length);
                    return merged;
                }
            };

            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, new TrustManager[]{composite}, null);
            return ctx;
        } catch (GeneralSecurityException e) {
            throw new IOException("Failed to initialize custom OIDC CA SSLContext", e);
        }
    }

    private static X509TrustManager findX509TrustManager(TrustManagerFactory tmf) throws GeneralSecurityException {
        for (TrustManager tm : tmf.getTrustManagers()) {
            if (tm instanceof X509TrustManager x509)
                return x509;
        }
        throw new GeneralSecurityException("No X509TrustManager available");
    }

    private CachedDiscovery discover(HttpClient client, String rawIssuer) throws IOException, InterruptedException {
        if (rawIssuer == null || rawIssuer.isBlank())
            throw new IOException("OIDC issuer URL is not configured");
        String normalizedIssuer = rawIssuer.trim().replaceAll("/+$", "");
        if (!normalizedIssuer.startsWith("https://") && !normalizedIssuer.startsWith("http://localhost"))
            throw new IOException("OIDC issuer URL must use HTTPS");

        CachedDiscovery current = cachedDiscovery;
        long now = System.currentTimeMillis();
        if (current != null && normalizedIssuer.equals(current.issuer()) && now < current.expiresAt())
            return current;

        URI discoveryUri = URI.create(normalizedIssuer + "/.well-known/openid-configuration");
        HttpRequest req = HttpRequest.newBuilder(discoveryUri)
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() != 200)
            throw new IOException("OIDC discovery failed with HTTP " + resp.statusCode());

        JsonObject doc = JsonParser.parseString(resp.body()).getAsJsonObject();
        String issuer = firstNonBlank(getString(doc, "issuer"), normalizedIssuer);
        String authEndpoint = getString(doc, "authorization_endpoint");
        String tokenEndpoint = getString(doc, "token_endpoint");
        String jwksUri = getString(doc, "jwks_uri");
        if (authEndpoint == null || tokenEndpoint == null || jwksUri == null)
            throw new IOException("OIDC discovery metadata is incomplete");

        CachedDiscovery resolved = new CachedDiscovery(
                issuer, authEndpoint, tokenEndpoint, jwksUri, now + DISCOVERY_TTL_MILLIS);
        this.cachedDiscovery = resolved;
        return resolved;
    }

    private JsonObject verifyJwtSignatureAndClaims(HttpClient client,
                                                   String jwt,
                                                   String expectedIssuer,
                                                   String expectedAudience,
                                                   String expectedNonce,
                                                   String jwksUri) throws IOException, InterruptedException {
        String[] parts = jwt.split("\\.");
        if (parts.length != 3)
            throw new IOException("Malformed OIDC JWT");

        JsonObject header = JsonParser.parseString(
                new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8)).getAsJsonObject();
        JsonObject payload = JsonParser.parseString(
                new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8)).getAsJsonObject();
        byte[] sigBytes = Base64.getUrlDecoder().decode(parts[2]);

        String alg = getString(header, "alg");
        String kid = getString(header, "kid");
        if (!"RS256".equals(alg) && !"ES256".equals(alg))
            throw new IOException("Unsupported OIDC JWT signature algorithm: " + alg);

        HttpRequest jwksReq = HttpRequest.newBuilder(URI.create(jwksUri))
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> jwksResp = client.send(jwksReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (jwksResp.statusCode() != 200)
            throw new IOException("Failed to fetch OIDC JWKS: HTTP " + jwksResp.statusCode());

        JsonObject jwks = JsonParser.parseString(jwksResp.body()).getAsJsonObject();
        JsonArray keys = jwks.getAsJsonArray("keys");
        if (keys == null || keys.isEmpty())
            throw new IOException("OIDC JWKS contains no keys");

        byte[] signedContent = (parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII);
        boolean signatureVerified = false;
        for (JsonElement elem : keys) {
            if (!elem.isJsonObject())
                continue;
            JsonObject jwk = elem.getAsJsonObject();
            String jwkKid = getString(jwk, "kid");
            if (kid != null && jwkKid != null && !kid.equals(jwkKid))
                continue;
            try {
                PublicKey pubKey = parseJwkPublicKey(jwk, alg);
                if (pubKey == null)
                    continue;
                if ("RS256".equals(alg)) {
                    Signature verifier = Signature.getInstance("SHA256withRSA");
                    verifier.initVerify(pubKey);
                    verifier.update(signedContent);
                    if (verifier.verify(sigBytes)) {
                        signatureVerified = true;
                        break;
                    }
                } else {
                    Signature verifier = Signature.getInstance("SHA256withECDSA");
                    verifier.initVerify(pubKey);
                    verifier.update(signedContent);
                    if (verifier.verify(joseP256ToDer(sigBytes))) {
                        signatureVerified = true;
                        break;
                    }
                }
            } catch (GeneralSecurityException | RuntimeException ignored) {
                // Try next matching key in JWKS rotation set.
            }
        }
        if (!signatureVerified)
            throw new IOException("OIDC id_token signature verification failed");

        String iss = getString(payload, "iss");
        if (iss == null || !iss.replaceAll("/+$", "").equals(expectedIssuer.replaceAll("/+$", "")))
            throw new IOException("OIDC id_token issuer mismatch");

        if (!audienceMatches(payload.get("aud"), expectedAudience))
            throw new IOException("OIDC id_token audience mismatch");

        long nowSec = System.currentTimeMillis() / 1000L;
        JsonElement expEl = payload.get("exp");
        if (expEl == null || !expEl.isJsonPrimitive() || expEl.getAsLong() + 60L < nowSec)
            throw new IOException("OIDC id_token has expired");

        String nonce = getString(payload, "nonce");
        if (expectedNonce != null && !expectedNonce.equals(nonce))
            throw new IOException("OIDC id_token nonce mismatch");

        return payload;
    }

    private static PublicKey parseJwkPublicKey(JsonObject jwk, String alg) throws GeneralSecurityException {
        String kty = getString(jwk, "kty");
        if ("RS256".equals(alg) && "RSA".equals(kty)) {
            String n = getString(jwk, "n");
            String e = getString(jwk, "e");
            if (n == null || e == null)
                return null;
            RSAPublicKeySpec spec = new RSAPublicKeySpec(
                    new BigInteger(1, Base64.getUrlDecoder().decode(n)),
                    new BigInteger(1, Base64.getUrlDecoder().decode(e)));
            return KeyFactory.getInstance("RSA").generatePublic(spec);
        }
        if ("ES256".equals(alg) && "EC".equals(kty)) {
            String x = getString(jwk, "x");
            String y = getString(jwk, "y");
            if (x == null || y == null)
                return null;
            AlgorithmParameters params = AlgorithmParameters.getInstance("EC");
            params.init(new ECGenParameterSpec("secp256r1"));
            ECParameterSpec ecSpec = params.getParameterSpec(ECParameterSpec.class);
            ECPoint point = new ECPoint(
                    new BigInteger(1, Base64.getUrlDecoder().decode(x)),
                    new BigInteger(1, Base64.getUrlDecoder().decode(y)));
            return KeyFactory.getInstance("EC").generatePublic(new ECPublicKeySpec(point, ecSpec));
        }
        return null;
    }

    /**
     * Converts a 64-byte JOSE P-256 signature (R || S) to ASN.1 DER for JCA SHA256withECDSA.
     */
    static byte[] joseP256ToDer(byte[] joseSig) {
        if (joseSig.length != 64)
            throw new IllegalArgumentException("Expected 64-byte ES256 signature");
        byte[] r = new BigInteger(1, Arrays.copyOfRange(joseSig, 0, 32)).toByteArray();
        byte[] s = new BigInteger(1, Arrays.copyOfRange(joseSig, 32, 64)).toByteArray();
        int seqLen = 2 + r.length + 2 + s.length;
        byte[] der = new byte[2 + seqLen];
        int p = 0;
        der[p++] = 0x30;
        der[p++] = (byte) seqLen;
        der[p++] = 0x02;
        der[p++] = (byte) r.length;
        System.arraycopy(r, 0, der, p, r.length);
        p += r.length;
        der[p++] = 0x02;
        der[p++] = (byte) s.length;
        System.arraycopy(s, 0, der, p, s.length);
        return der;
    }

    private static boolean audienceMatches(JsonElement aud, String expected) {
        if (aud == null || expected == null)
            return false;
        if (aud.isJsonPrimitive())
            return expected.equals(aud.getAsString());
        if (aud.isJsonArray()) {
            for (JsonElement item : aud.getAsJsonArray()) {
                if (item.isJsonPrimitive() && expected.equals(item.getAsString()))
                    return true;
            }
        }
        return false;
    }

    private static void extractStringClaims(JsonElement el, List<String> out) {
        if (el == null)
            return;
        if (el.isJsonPrimitive()) {
            String s = el.getAsString().trim();
            if (!s.isEmpty())
                out.add(s);
        } else if (el.isJsonArray()) {
            for (JsonElement item : el.getAsJsonArray()) {
                if (item.isJsonPrimitive()) {
                    String s = item.getAsString().trim();
                    if (!s.isEmpty())
                        out.add(s);
                }
            }
        }
    }

    private static boolean containsIgnoreCase(List<String> list, String target) {
        for (String item : list) {
            if (item.equalsIgnoreCase(target))
                return true;
        }
        return false;
    }

    private static String firstNonBlank(String... candidates) {
        for (String c : candidates) {
            if (c != null && !c.isBlank())
                return c.trim();
        }
        return null;
    }

    private static String getString(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        return (el != null && el.isJsonPrimitive()) ? el.getAsString() : null;
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}
