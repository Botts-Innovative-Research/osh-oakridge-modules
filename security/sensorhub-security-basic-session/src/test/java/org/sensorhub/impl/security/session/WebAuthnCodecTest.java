package org.sensorhub.impl.security.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.Arrays;
import java.util.Set;
import org.junit.Test;

public class WebAuthnCodecTest {

    @Test
    public void verifiesEs256RegistrationAndAssertionAndDetectsTampering() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
        kpg.initialize(new ECGenParameterSpec("secp256r1"));
        KeyPair kp = kpg.generateKeyPair();
        ECPublicKey pub = (ECPublicKey) kp.getPublic();

        byte[] credIdBytes = new byte[24];
        for (int i = 0; i < credIdBytes.length; i++) credIdBytes[i] = (byte) (i + 7);
        String credId = PasswordHasher.encodeBase64Url(credIdBytes);

        byte[] coseKey = buildEc2CosePublicKey(pub);
        String rpId = "oscar.local";
        String challengeReg = PasswordHasher.randomBase64Url(32);
        byte[] aaguid = new byte[] {
                (byte) 0xfb, (byte) 0xfc, 0x30, 0x07, 0x15, 0x4e, 0x4e, (byte) 0xcc,
                (byte) 0x8c, 0x0b, 0x6e, 0x02, 0x05, 0x57, (byte) 0xd7, (byte) 0xbd
        }; // iCloud Keychain
        byte[] regAuthData = buildAuthData(rpId, (byte) 0x5d, 1L, aaguid, credIdBytes, coseKey);
        byte[] attObj = buildAttestationObject(regAuthData);

        String regClientData = "{\"type\":\"webauthn.create\",\"challenge\":\"" + challengeReg
                + "\",\"origin\":\"https://oscar.local\",\"crossOrigin\":false}";

        WebAuthnCodec.VerifiedRegistration reg = WebAuthnCodec.verifyRegistration(
                credId,
                PasswordHasher.encodeBase64Url(regClientData.getBytes(StandardCharsets.UTF_8)),
                PasswordHasher.encodeBase64Url(attObj),
                challengeReg,
                rpId,
                Set.of("https://oscar.local"),
                true);

        assertEquals(credId, reg.credentialId);
        assertEquals(WebAuthnCodec.COSE_ALG_ES256, reg.coseAlgorithm);
        assertEquals("fbfc3007-154e-4ecc-8c0b-6e020557d7bd", reg.aaguid);
        assertEquals("iCloud Keychain", reg.authenticatorLabel);
        assertTrue(reg.backupEligible);
        assertTrue(reg.backupState);

        // Now verify an assertion signed with the private key
        String challengeAuth = PasswordHasher.randomBase64Url(32);
        String authClientData = "{\"type\":\"webauthn.get\",\"challenge\":\"" + challengeAuth
                + "\",\"origin\":\"https://oscar.local\"}";
        byte[] authClientBytes = authClientData.getBytes(StandardCharsets.UTF_8);
        byte[] authData = buildAuthData(rpId, (byte) 0x1d, 2L, null, null, null);

        byte[] clientHash = PasswordHasher.sha256(authClientBytes);
        byte[] toSign = new byte[authData.length + clientHash.length];
        System.arraycopy(authData, 0, toSign, 0, authData.length);
        System.arraycopy(clientHash, 0, toSign, authData.length, clientHash.length);

        Signature sig = Signature.getInstance("SHA256withECDSA");
        sig.initSign(kp.getPrivate());
        sig.update(toSign);
        byte[] sigBytes = sig.sign();

        WebAuthnCodec.VerifiedAssertion verified = WebAuthnCodec.verifyAssertion(
                PasswordHasher.encodeBase64Url(authClientBytes),
                PasswordHasher.encodeBase64Url(authData),
                PasswordHasher.encodeBase64Url(sigBytes),
                reg.publicKeyCoseB64Url,
                1L,
                challengeAuth,
                rpId,
                Set.of("https://oscar.local"),
                true);

        assertEquals(2L, verified.signCount);
        assertTrue(verified.userVerified);

        // Replay with non-advancing signCount must fail
        try {
            WebAuthnCodec.verifyAssertion(
                    PasswordHasher.encodeBase64Url(authClientBytes),
                    PasswordHasher.encodeBase64Url(authData),
                    PasswordHasher.encodeBase64Url(sigBytes),
                    reg.publicKeyCoseB64Url,
                    2L,
                    challengeAuth,
                    rpId,
                    Set.of("https://oscar.local"),
                    false);
            fail("Expected replay signCount rejection");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("counter"));
        }

        // Wrong origin must fail
        try {
            WebAuthnCodec.verifyAssertion(
                    PasswordHasher.encodeBase64Url(authClientBytes),
                    PasswordHasher.encodeBase64Url(authData),
                    PasswordHasher.encodeBase64Url(sigBytes),
                    reg.publicKeyCoseB64Url,
                    1L,
                    challengeAuth,
                    rpId,
                    Set.of("https://evil.example"),
                    false);
            fail("Expected origin mismatch rejection");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("origin"));
        }
    }

    @Test
    public void verifiesRs256RegistrationAndAssertion() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();
        RSAPublicKey pub = (RSAPublicKey) kp.getPublic();

        byte[] credIdBytes = "rsa-yubikey-cred-01".getBytes(StandardCharsets.UTF_8);
        String credId = PasswordHasher.encodeBase64Url(credIdBytes);
        byte[] coseKey = buildRsaCosePublicKey(pub);
        String rpId = "oscar.local";
        String challengeReg = PasswordHasher.randomBase64Url(32);
        byte[] aaguid = new byte[16];
        byte[] regAuthData = buildAuthData(rpId, (byte) 0x45, 0L, aaguid, credIdBytes, coseKey);
        byte[] attObj = buildAttestationObject(regAuthData);
        String regClientData = "{\"type\":\"webauthn.create\",\"challenge\":\"" + challengeReg
                + "\",\"origin\":\"https://oscar.local\"}";

        WebAuthnCodec.VerifiedRegistration reg = WebAuthnCodec.verifyRegistration(
                credId,
                PasswordHasher.encodeBase64Url(regClientData.getBytes(StandardCharsets.UTF_8)),
                PasswordHasher.encodeBase64Url(attObj),
                challengeReg,
                rpId,
                Set.of("https://oscar.local"),
                false);
        assertEquals(WebAuthnCodec.COSE_ALG_RS256, reg.coseAlgorithm);

        String challengeAuth = PasswordHasher.randomBase64Url(32);
        byte[] authClientBytes = ("{\"type\":\"webauthn.get\",\"challenge\":\"" + challengeAuth
                + "\",\"origin\":\"https://oscar.local\"}").getBytes(StandardCharsets.UTF_8);
        byte[] authData = buildAuthData(rpId, (byte) 0x05, 0L, null, null, null);
        byte[] clientHash = PasswordHasher.sha256(authClientBytes);
        byte[] toSign = new byte[authData.length + clientHash.length];
        System.arraycopy(authData, 0, toSign, 0, authData.length);
        System.arraycopy(clientHash, 0, toSign, authData.length, clientHash.length);

        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initSign(kp.getPrivate());
        sig.update(toSign);
        byte[] sigBytes = sig.sign();

        WebAuthnCodec.VerifiedAssertion verified = WebAuthnCodec.verifyAssertion(
                PasswordHasher.encodeBase64Url(authClientBytes),
                PasswordHasher.encodeBase64Url(authData),
                PasswordHasher.encodeBase64Url(sigBytes),
                reg.publicKeyCoseB64Url,
                0L,
                challengeAuth,
                rpId,
                Set.of("https://oscar.local"),
                false);
        assertEquals(0L, verified.signCount);
    }

    static byte[] buildAuthData(String rpId, byte flags, long signCount,
                                byte[] aaguid, byte[] credId, byte[] coseKey) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(PasswordHasher.sha256(rpId.getBytes(StandardCharsets.UTF_8)));
        out.write(flags);
        out.write(ByteBuffer.allocate(4).putInt((int) signCount).array());
        if (aaguid != null && credId != null && coseKey != null) {
            out.write(aaguid);
            out.write((credId.length >>> 8) & 0xff);
            out.write(credId.length & 0xff);
            out.write(credId);
            out.write(coseKey);
        }
        return out.toByteArray();
    }

    static byte[] buildAttestationObject(byte[] authData) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(0xa2); // map(2)
        writeCborText(out, "fmt");
        writeCborText(out, "none");
        writeCborText(out, "authData");
        writeCborBytes(out, authData);
        return out.toByteArray();
    }

    static byte[] buildEc2CosePublicKey(ECPublicKey pub) throws Exception {
        byte[] x = toFixedUnsigned(pub.getW().getAffineX(), 32);
        byte[] y = toFixedUnsigned(pub.getW().getAffineY(), 32);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(0xa5); // map(5)
        out.write(0x01); out.write(0x02); // 1: 2 (EC2)
        out.write(0x03); out.write(0x26); // 3: -7 (ES256)
        out.write(0x20); out.write(0x01); // -1: 1 (P-256)
        out.write(0x21); writeCborBytes(out, x); // -2: x
        out.write(0x22); writeCborBytes(out, y); // -3: y
        return out.toByteArray();
    }

    static byte[] buildRsaCosePublicKey(RSAPublicKey pub) throws Exception {
        byte[] n = toUnsigned(pub.getModulus());
        byte[] e = toUnsigned(pub.getPublicExponent());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(0xa4); // map(4)
        out.write(0x01); out.write(0x03); // 1: 3 (RSA)
        out.write(0x03); out.write(0x39); out.write(0x01); out.write(0x00); // 3: -257 (RS256)
        out.write(0x20); writeCborBytes(out, n); // -1: n
        out.write(0x21); writeCborBytes(out, e); // -2: e
        return out.toByteArray();
    }

    private static void writeCborText(ByteArrayOutputStream out, String s) throws Exception {
        byte[] b = s.getBytes(StandardCharsets.UTF_8);
        writeCborHeader(out, 3, b.length);
        out.write(b);
    }

    private static void writeCborBytes(ByteArrayOutputStream out, byte[] b) throws Exception {
        writeCborHeader(out, 2, b.length);
        out.write(b);
    }

    private static void writeCborHeader(ByteArrayOutputStream out, int major, int len) {
        int m = major << 5;
        if (len < 24) {
            out.write(m | len);
        } else if (len < 256) {
            out.write(m | 24);
            out.write(len);
        } else {
            out.write(m | 25);
            out.write((len >>> 8) & 0xff);
            out.write(len & 0xff);
        }
    }

    private static byte[] toUnsigned(BigInteger bi) {
        byte[] raw = bi.toByteArray();
        return (raw.length > 1 && raw[0] == 0) ? Arrays.copyOfRange(raw, 1, raw.length) : raw;
    }

    private static byte[] toFixedUnsigned(BigInteger bi, int size) {
        byte[] u = toUnsigned(bi);
        if (u.length == size) return u;
        if (u.length > size) return Arrays.copyOfRange(u, u.length - size, u.length);
        byte[] padded = new byte[size];
        System.arraycopy(u, 0, padded, size - u.length, u.length);
        return padded;
    }
}
