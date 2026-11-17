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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Pure-Java WebAuthn Level 2/3 Relying Party verifier supporting CBOR attestation
 * objects, COSE ES256 ({@code -7}) and RS256 ({@code -257}) public keys,
 * authenticator data flags, AAGUID metadata, origin/RP-ID validation, and
 * assertion signature verification without external runtime dependencies.
 */
public final class WebAuthnCodec {
    public static final int COSE_ALG_ES256 = -7;
    public static final int COSE_ALG_RS256 = -257;

    private static final int FLAG_USER_PRESENT = 0x01;
    private static final int FLAG_USER_VERIFIED = 0x04;
    private static final int FLAG_BACKUP_ELIGIBLE = 0x08;
    private static final int FLAG_BACKUP_STATE = 0x10;
    private static final int FLAG_ATTESTED_CRED_DATA = 0x40;

    private static final Map<String, String> KNOWN_AAGUID_NAMES = Map.ofEntries(
            Map.entry("fbfc3007-154e-4ecc-8c0b-6e020557d7bd", "iCloud Keychain"),
            Map.entry("00000000-0000-0000-0000-000000000000", "Passkey / Security Key"),
            Map.entry("ea9b8d66-4d01-1d21-3ce4-b6b48cb575d4", "Google Password Manager"),
            Map.entry("adce0002-35bc-c60a-648b-0b25f1f05503", "Chrome on macOS"),
            Map.entry("08987058-cadc-4b81-b6e1-30de50dcbe96", "Windows Hello"),
            Map.entry("9ddd1817-af5a-4672-a2b9-3e3dd95000a9", "Windows Hello"),
            Map.entry("bada5566-a7aa-401f-bd96-45619a55120d", "1Password"),
            Map.entry("d548826e-79b4-db40-a3d8-11116f7e8349", "Bitwarden"),
            Map.entry("2fc0579f-8113-47ea-b116-bb5a8db9202a", "YubiKey 5 Series"),
            Map.entry("ee882879-721c-4913-9775-3dfcce97072a", "YubiKey 5 Series"),
            Map.entry("cb69481e-8ff7-4039-93ec-0a2729a154a8", "YubiKey 5 Series"),
            Map.entry("531126d6-e717-415c-9320-3d9aa6981239", "Dashlane")
    );

    private WebAuthnCodec() {
    }

    public static final class VerifiedRegistration {
        public final String credentialId;
        public final String publicKeyCoseB64Url;
        public final int coseAlgorithm;
        public final String aaguid;
        public final String authenticatorLabel;
        public final long signCount;
        public final boolean userVerified;
        public final boolean backupEligible;
        public final boolean backupState;

        VerifiedRegistration(String credentialId,
                             String publicKeyCoseB64Url,
                             int coseAlgorithm,
                             String aaguid,
                             String authenticatorLabel,
                             long signCount,
                             boolean userVerified,
                             boolean backupEligible,
                             boolean backupState) {
            this.credentialId = credentialId;
            this.publicKeyCoseB64Url = publicKeyCoseB64Url;
            this.coseAlgorithm = coseAlgorithm;
            this.aaguid = aaguid;
            this.authenticatorLabel = authenticatorLabel;
            this.signCount = signCount;
            this.userVerified = userVerified;
            this.backupEligible = backupEligible;
            this.backupState = backupState;
        }
    }

    public static final class VerifiedAssertion {
        public final long signCount;
        public final boolean userVerified;
        public final boolean backupState;

        VerifiedAssertion(long signCount, boolean userVerified, boolean backupState) {
            this.signCount = signCount;
            this.userVerified = userVerified;
            this.backupState = backupState;
        }
    }

    public static String friendlyAuthenticatorName(String aaguid) {
        if (aaguid == null)
            return "Passkey";
        return KNOWN_AAGUID_NAMES.getOrDefault(aaguid.toLowerCase(Locale.ROOT), "Passkey");
    }

    public static VerifiedRegistration verifyRegistration(
            String rawCredentialId,
            String clientDataJsonB64Url,
            String attestationObjectB64Url,
            String expectedChallenge,
            String expectedRpId,
            Set<String> allowedOrigins,
            boolean requireUserVerification) {
        if (!PasswordHasher.isValidUnpaddedBase64Url(rawCredentialId))
            throw new IllegalArgumentException("Credential ID must be unpadded Base64URL");

        byte[] clientDataBytes = PasswordHasher.decodeBase64Url(clientDataJsonB64Url);
        verifyClientData(clientDataBytes, "webauthn.create", expectedChallenge, allowedOrigins);

        byte[] attestationBytes = PasswordHasher.decodeBase64Url(attestationObjectB64Url);
        Object decodedAttestation = CborDecoder.decodeSingleItem(attestationBytes);
        if (!(decodedAttestation instanceof Map<?, ?> attMap))
            throw new IllegalArgumentException("Attestation object must be a CBOR map");

        Object authDataObj = attMap.get("authData");
        if (!(authDataObj instanceof byte[] authData))
            throw new IllegalArgumentException("Attestation object is missing authData");

        ParsedAuthData parsed = parseAuthData(authData, true);
        byte[] expectedRpIdHash = PasswordHasher.sha256(expectedRpId.getBytes(StandardCharsets.UTF_8));
        if (!MessageDigest.isEqual(expectedRpIdHash, parsed.rpIdHash))
            throw new IllegalArgumentException("RP ID hash mismatch");

        if ((parsed.flags & FLAG_USER_PRESENT) == 0)
            throw new IllegalArgumentException("Authenticator did not assert User Presence (UP)");

        boolean uv = (parsed.flags & FLAG_USER_VERIFIED) != 0;
        if (requireUserVerification && !uv)
            throw new IllegalArgumentException("Authenticator did not assert User Verification (UV)");

        String attestedCredId = PasswordHasher.encodeBase64Url(parsed.credentialIdBytes);
        if (!attestedCredId.equals(rawCredentialId))
            throw new IllegalArgumentException("Credential ID mismatch between outer response and authData");

        DecodedCoseKey coseKey = decodeCosePublicKey(parsed.cosePublicKeyBytes);
        boolean backupEligible = (parsed.flags & FLAG_BACKUP_ELIGIBLE) != 0;
        boolean backupState = (parsed.flags & FLAG_BACKUP_STATE) != 0;

        return new VerifiedRegistration(
                attestedCredId,
                PasswordHasher.encodeBase64Url(parsed.cosePublicKeyBytes),
                coseKey.algorithm,
                parsed.aaguid,
                friendlyAuthenticatorName(parsed.aaguid),
                parsed.signCount,
                uv,
                backupEligible,
                backupState);
    }

    public static VerifiedAssertion verifyAssertion(
            String clientDataJsonB64Url,
            String authenticatorDataB64Url,
            String signatureB64Url,
            String storedPublicKeyCoseB64Url,
            long storedSignCount,
            String expectedChallenge,
            String expectedRpId,
            Set<String> allowedOrigins,
            boolean requireUserVerification) {
        byte[] clientDataBytes = PasswordHasher.decodeBase64Url(clientDataJsonB64Url);
        verifyClientData(clientDataBytes, "webauthn.get", expectedChallenge, allowedOrigins);

        byte[] authData = PasswordHasher.decodeBase64Url(authenticatorDataB64Url);
        ParsedAuthData parsed = parseAuthData(authData, false);

        byte[] expectedRpIdHash = PasswordHasher.sha256(expectedRpId.getBytes(StandardCharsets.UTF_8));
        if (!MessageDigest.isEqual(expectedRpIdHash, parsed.rpIdHash))
            throw new IllegalArgumentException("RP ID hash mismatch");

        if ((parsed.flags & FLAG_USER_PRESENT) == 0)
            throw new IllegalArgumentException("Authenticator did not assert User Presence (UP)");

        boolean uv = (parsed.flags & FLAG_USER_VERIFIED) != 0;
        if (requireUserVerification && !uv)
            throw new IllegalArgumentException("Authenticator did not assert User Verification (UV)");

        byte[] signatureBytes = PasswordHasher.decodeBase64Url(signatureB64Url);
        byte[] coseKeyBytes = PasswordHasher.decodeBase64Url(storedPublicKeyCoseB64Url);
        DecodedCoseKey coseKey = decodeCosePublicKey(coseKeyBytes);

        byte[] clientDataHash = PasswordHasher.sha256(clientDataBytes);
        byte[] signedPayload = new byte[authData.length + clientDataHash.length];
        System.arraycopy(authData, 0, signedPayload, 0, authData.length);
        System.arraycopy(clientDataHash, 0, signedPayload, authData.length, clientDataHash.length);

        String jcaAlg = coseKey.algorithm == COSE_ALG_ES256 ? "SHA256withECDSA" : "SHA256withRSA";
        try {
            Signature verifier = Signature.getInstance(jcaAlg);
            verifier.initVerify(coseKey.publicKey);
            verifier.update(signedPayload);
            if (!verifier.verify(signatureBytes))
                throw new IllegalArgumentException("Invalid WebAuthn assertion signature");
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException("WebAuthn signature verification failed", e);
        }

        if (storedSignCount > 0 && parsed.signCount > 0 && parsed.signCount <= storedSignCount)
            throw new IllegalArgumentException("Authenticator signature counter did not advance");

        long updatedCount = Math.max(storedSignCount, parsed.signCount);
        boolean backupState = (parsed.flags & FLAG_BACKUP_STATE) != 0;
        return new VerifiedAssertion(updatedCount, uv, backupState);
    }

    private static void verifyClientData(byte[] clientDataBytes,
                                         String expectedType,
                                         String expectedChallenge,
                                         Set<String> allowedOrigins) {
        if (clientDataBytes == null || clientDataBytes.length == 0 || clientDataBytes.length > 16_384)
            throw new IllegalArgumentException("Invalid clientDataJSON size");
        JsonObject json;
        try {
            json = JsonParser.parseString(new String(clientDataBytes, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Malformed clientDataJSON", e);
        }
        String type = getString(json, "type");
        if (!expectedType.equals(type))
            throw new IllegalArgumentException("Unexpected clientDataJSON type: " + type);

        String challenge = getString(json, "challenge");
        if (expectedChallenge == null || !expectedChallenge.equals(challenge))
            throw new IllegalArgumentException("WebAuthn challenge mismatch");

        String origin = getString(json, "origin");
        if (origin == null || !allowedOrigins.contains(origin))
            throw new IllegalArgumentException("Unexpected WebAuthn origin: " + origin);

        JsonElement crossOrigin = json.get("crossOrigin");
        if (crossOrigin != null && crossOrigin.isJsonPrimitive()
                && crossOrigin.getAsJsonPrimitive().isBoolean()
                && crossOrigin.getAsBoolean()) {
            throw new IllegalArgumentException("Cross-origin WebAuthn ceremonies are not allowed");
        }
    }

    private static String getString(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        return (el != null && el.isJsonPrimitive()) ? el.getAsString() : null;
    }

    private static final class ParsedAuthData {
        final byte[] rpIdHash;
        final int flags;
        final long signCount;
        final String aaguid;
        final byte[] credentialIdBytes;
        final byte[] cosePublicKeyBytes;

        ParsedAuthData(byte[] rpIdHash, int flags, long signCount,
                       String aaguid, byte[] credentialIdBytes, byte[] cosePublicKeyBytes) {
            this.rpIdHash = rpIdHash;
            this.flags = flags;
            this.signCount = signCount;
            this.aaguid = aaguid;
            this.credentialIdBytes = credentialIdBytes;
            this.cosePublicKeyBytes = cosePublicKeyBytes;
        }
    }

    private static ParsedAuthData parseAuthData(byte[] authData, boolean requireAttestedCredentialData) {
        if (authData == null || authData.length < 37)
            throw new IllegalArgumentException("Authenticator data is shorter than 37 bytes");

        byte[] rpIdHash = Arrays.copyOfRange(authData, 0, 32);
        int flags = authData[32] & 0xff;
        long signCount = ByteBuffer.wrap(authData, 33, 4).getInt() & 0xffffffffL;

        boolean hasAttested = (flags & FLAG_ATTESTED_CRED_DATA) != 0;
        if (requireAttestedCredentialData && !hasAttested)
            throw new IllegalArgumentException("Authenticator data does not include attestedCredentialData");

        if (!requireAttestedCredentialData)
            return new ParsedAuthData(rpIdHash, flags, signCount, null, null, null);

        if (authData.length < 55)
            throw new IllegalArgumentException("Truncated attestedCredentialData");

        byte[] aaguidBytes = Arrays.copyOfRange(authData, 37, 53);
        String aaguid = formatUuid(aaguidBytes);
        int credIdLen = ((authData[53] & 0xff) << 8) | (authData[54] & 0xff);
        if (credIdLen <= 0 || credIdLen > 1023 || 55 + credIdLen >= authData.length)
            throw new IllegalArgumentException("Invalid attested credential ID length");

        byte[] credId = Arrays.copyOfRange(authData, 55, 55 + credIdLen);
        int coseStart = 55 + credIdLen;
        CborDecoder decoder = new CborDecoder(authData, coseStart);
        decoder.readItem();
        int coseEnd = decoder.offset();
        byte[] cosePublicKeyBytes = Arrays.copyOfRange(authData, coseStart, coseEnd);

        return new ParsedAuthData(rpIdHash, flags, signCount, aaguid, credId, cosePublicKeyBytes);
    }

    private static String formatUuid(byte[] bytes) {
        StringBuilder sb = new StringBuilder(36);
        for (int i = 0; i < 16; i++) {
            if (i == 4 || i == 6 || i == 8 || i == 10)
                sb.append('-');
            sb.append(Character.forDigit((bytes[i] >> 4) & 0x0f, 16));
            sb.append(Character.forDigit(bytes[i] & 0x0f, 16));
        }
        return sb.toString();
    }

    static final class DecodedCoseKey {
        final int algorithm;
        final PublicKey publicKey;

        DecodedCoseKey(int algorithm, PublicKey publicKey) {
            this.algorithm = algorithm;
            this.publicKey = publicKey;
        }
    }

    static DecodedCoseKey decodeCosePublicKey(byte[] coseBytes) {
        Object decoded = CborDecoder.decodeSingleItem(coseBytes);
        if (!(decoded instanceof Map<?, ?> map))
            throw new IllegalArgumentException("COSE_Key must be a CBOR map");

        int kty = toInt(map.get(1L), "kty");
        int alg = toInt(map.get(3L), "alg");

        try {
            if (kty == 2 && alg == COSE_ALG_ES256) {
                int crv = toInt(map.get(-1L), "crv");
                if (crv != 1)
                    throw new IllegalArgumentException("Unsupported EC curve in COSE_Key: " + crv);
                byte[] xBytes = toBytes(map.get(-2L), "x");
                byte[] yBytes = toBytes(map.get(-3L), "y");
                if (xBytes.length != 32 || yBytes.length != 32)
                    throw new IllegalArgumentException("Invalid P-256 coordinate length");

                AlgorithmParameters params = AlgorithmParameters.getInstance("EC");
                params.init(new ECGenParameterSpec("secp256r1"));
                ECParameterSpec ecSpec = params.getParameterSpec(ECParameterSpec.class);
                ECPoint point = new ECPoint(new BigInteger(1, xBytes), new BigInteger(1, yBytes));
                PublicKey pub = KeyFactory.getInstance("EC").generatePublic(new ECPublicKeySpec(point, ecSpec));
                return new DecodedCoseKey(COSE_ALG_ES256, pub);
            }
            if (kty == 3 && alg == COSE_ALG_RS256) {
                byte[] nBytes = toBytes(map.get(-1L), "n");
                byte[] eBytes = toBytes(map.get(-2L), "e");
                if (nBytes.length < 256 || eBytes.length == 0)
                    throw new IllegalArgumentException("RSA key modulus must be at least 2048 bits");
                RSAPublicKeySpec rsaSpec = new RSAPublicKeySpec(
                        new BigInteger(1, nBytes), new BigInteger(1, eBytes));
                PublicKey pub = KeyFactory.getInstance("RSA").generatePublic(rsaSpec);
                return new DecodedCoseKey(COSE_ALG_RS256, pub);
            }
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException("Failed to reconstruct COSE public key", e);
        }
        throw new IllegalArgumentException("Unsupported COSE key type/algorithm: kty=" + kty + ", alg=" + alg);
    }

    private static int toInt(Object value, String field) {
        if (value instanceof Number num)
            return num.intValue();
        throw new IllegalArgumentException("Missing or invalid integer COSE field: " + field);
    }

    private static byte[] toBytes(Object value, String field) {
        if (value instanceof byte[] b)
            return b;
        throw new IllegalArgumentException("Missing or invalid byte-string COSE field: " + field);
    }

    /**
     * Minimal, bounded CBOR (RFC 8949) reader for WebAuthn attestation objects and COSE keys.
     */
    static final class CborDecoder {
        private static final int MAX_DEPTH = 8;
        private static final int MAX_COLLECTION_ITEMS = 256;
        private final byte[] buf;
        private int pos;

        CborDecoder(byte[] buf, int startOffset) {
            this.buf = buf;
            this.pos = startOffset;
        }

        static Object decodeSingleItem(byte[] bytes) {
            if (bytes == null || bytes.length == 0 || bytes.length > 65_536)
                throw new IllegalArgumentException("Invalid CBOR payload size");
            CborDecoder dec = new CborDecoder(bytes, 0);
            return dec.readItem(0);
        }

        int offset() {
            return pos;
        }

        Object readItem() {
            return readItem(0);
        }

        private Object readItem(int depth) {
            if (depth > MAX_DEPTH)
                throw new IllegalArgumentException("CBOR nesting exceeds maximum depth");
            if (pos >= buf.length)
                throw new IllegalArgumentException("Unexpected end of CBOR input");

            int initial = buf[pos++] & 0xff;
            int major = initial >>> 5;
            int info = initial & 0x1f;
            long arg = readArgument(info);

            switch (major) {
                case 0: // Unsigned integer
                    if (arg < 0)
                        throw new IllegalArgumentException("Unsupported 64-bit unsigned CBOR integer");
                    return arg;
                case 1: // Negative integer (-1 - arg)
                    if (arg < 0)
                        throw new IllegalArgumentException("Unsupported 64-bit negative CBOR integer");
                    return -1L - arg;
                case 2: { // Byte string
                    int len = checkedLength(arg);
                    byte[] out = Arrays.copyOfRange(buf, pos, pos + len);
                    pos += len;
                    return out;
                }
                case 3: { // Text string
                    int len = checkedLength(arg);
                    String s = new String(buf, pos, len, StandardCharsets.UTF_8);
                    pos += len;
                    return s;
                }
                case 4: { // Array
                    if (arg > MAX_COLLECTION_ITEMS)
                        throw new IllegalArgumentException("CBOR array too large");
                    int count = (int) arg;
                    List<Object> list = new ArrayList<>(count);
                    for (int i = 0; i < count; i++)
                        list.add(readItem(depth + 1));
                    return list;
                }
                case 5: { // Map
                    if (arg > MAX_COLLECTION_ITEMS)
                        throw new IllegalArgumentException("CBOR map too large");
                    int count = (int) arg;
                    Map<Object, Object> map = new LinkedHashMap<>(count);
                    for (int i = 0; i < count; i++) {
                        Object k = readItem(depth + 1);
                        Object v = readItem(depth + 1);
                        map.put(k, v);
                    }
                    return map;
                }
                case 6: // Semantic tag — unwrap tagged value
                    return readItem(depth + 1);
                case 7: // Simple / float
                    if (info == 20) return Boolean.FALSE;
                    if (info == 21) return Boolean.TRUE;
                    if (info == 22 || info == 23) return null;
                    throw new IllegalArgumentException("Unsupported CBOR simple/float value: " + info);
                default:
                    throw new IllegalArgumentException("Unsupported CBOR major type: " + major);
            }
        }

        private long readArgument(int info) {
            if (info < 24)
                return info;
            if (info == 24) {
                ensureAvailable(1);
                return buf[pos++] & 0xffL;
            }
            if (info == 25) {
                ensureAvailable(2);
                int v = ((buf[pos] & 0xff) << 8) | (buf[pos + 1] & 0xff);
                pos += 2;
                return v;
            }
            if (info == 26) {
                ensureAvailable(4);
                long v = ByteBuffer.wrap(buf, pos, 4).getInt() & 0xffffffffL;
                pos += 4;
                return v;
            }
            if (info == 27) {
                ensureAvailable(8);
                long v = ByteBuffer.wrap(buf, pos, 8).getLong();
                pos += 8;
                return v;
            }
            throw new IllegalArgumentException("Indefinite-length CBOR items are not allowed");
        }

        private int checkedLength(long len) {
            if (len < 0 || len > buf.length - pos)
                throw new IllegalArgumentException("CBOR byte/text length out of bounds");
            return (int) len;
        }

        private void ensureAvailable(int n) {
            if (pos + n > buf.length)
                throw new IllegalArgumentException("Truncated CBOR argument");
        }
    }
}
