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

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.eclipse.jetty.util.security.Credential;

/**
 * Generates and verifies OSCAR PBKDF2 password credentials in the exact format
 * consumed by {@code com.botts.impl.security.PBKDF2CredentialProvider}
 * ({@code PBKDF2WithHmacSHA1:16:<base64Salt>:<base64Hash>}), plus SHA-256 and
 * Base64URL token helpers.
 */
public final class PasswordHasher {
    public static final String ALGORITHM = "PBKDF2WithHmacSHA1";
    public static final int DEFAULT_STRENGTH = 16;
    public static final int HASH_BITS = 128;
    public static final int SALT_BYTES = 16;
    public static final String PREFIX = ALGORITHM + ":";

    private static final char[] UNAMBIGUOUS_ALPHABET =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() {
    }

    public static String hashPassword(String password) {
        return hashPassword(password, DEFAULT_STRENGTH);
    }

    public static String hashPassword(String password, int strength) {
        if (password == null || password.isEmpty())
            throw new IllegalArgumentException("Password must not be empty");
        if (strength < 10 || strength > 24)
            throw new IllegalArgumentException("Invalid PBKDF2 strength");
        try {
            byte[] salt = new byte[SALT_BYTES];
            RANDOM.nextBytes(salt);
            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 1 << strength, HASH_BITS);
            SecretKeyFactory skf = SecretKeyFactory.getInstance(ALGORITHM);
            byte[] hash = skf.generateSecret(spec).getEncoded();
            Base64.Encoder b64 = Base64.getEncoder();
            return PREFIX + strength + ":" + b64.encodeToString(salt) + ":" + b64.encodeToString(hash);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Cannot hash password with " + ALGORITHM, e);
        }
    }

    public static boolean verifyPassword(String storedCredential, String candidatePassword) {
        if (storedCredential == null || storedCredential.isEmpty() || candidatePassword == null)
            return false;
        if (storedCredential.startsWith(PREFIX)) {
            try {
                String rest = storedCredential.substring(PREFIX.length());
                int firstSep = rest.indexOf(':');
                if (firstSep <= 0)
                    return false;
                int strength = Integer.parseInt(rest.substring(0, firstSep));
                String saltAndHash = rest.substring(firstSep + 1);
                int secondSep = saltAndHash.indexOf(':');
                if (secondSep <= 0)
                    return false;
                byte[] salt = Base64.getDecoder().decode(saltAndHash.substring(0, secondSep));
                byte[] expectedHash = Base64.getDecoder().decode(saltAndHash.substring(secondSep + 1));
                KeySpec spec = new PBEKeySpec(
                        candidatePassword.toCharArray(), salt, 1 << strength, expectedHash.length * 8);
                SecretKeyFactory skf = SecretKeyFactory.getInstance(ALGORITHM);
                byte[] actualHash = skf.generateSecret(spec).getEncoded();
                return MessageDigest.isEqual(expectedHash, actualHash);
            } catch (RuntimeException | GeneralSecurityException e) {
                return false;
            }
        }
        try {
            Credential cred = Credential.getCredential(storedCredential);
            return cred != null && cred.check(candidatePassword);
        } catch (RuntimeException e) {
            return false;
        }
    }

    public static String sha256Hex(String input) {
        byte[] digest = sha256(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            sb.append(Character.forDigit((b >> 4) & 0x0f, 16));
            sb.append(Character.forDigit(b & 0x0f, 16));
        }
        return sb.toString();
    }

    public static byte[] sha256(byte[] input) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(input);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    public static String randomBase64Url(int numBytes) {
        byte[] bytes = new byte[numBytes];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String randomHex(int numBytes) {
        byte[] bytes = new byte[numBytes];
        RANDOM.nextBytes(bytes);
        StringBuilder sb = new StringBuilder(numBytes * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0x0f, 16));
            sb.append(Character.forDigit(b & 0x0f, 16));
        }
        return sb.toString();
    }

    /**
     * Generates a readable 19-character temporary password formatted as
     * {@code xxxx-xxxx-xxxx-xxxx} using unambiguous alphanumeric characters
     * suitable for phone-free / SCIF workstation entry.
     */
    public static String generateTemporaryPassword() {
        StringBuilder sb = new StringBuilder(19);
        for (int group = 0; group < 4; group++) {
            if (group > 0)
                sb.append('-');
            for (int i = 0; i < 4; i++) {
                sb.append(UNAMBIGUOUS_ALPHABET[RANDOM.nextInt(UNAMBIGUOUS_ALPHABET.length)]);
            }
        }
        return sb.toString();
    }

    public static boolean isValidUnpaddedBase64Url(String value) {
        if (value == null || value.isEmpty() || value.length() > 2048 || value.length() % 4 == 1)
            return false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            boolean ok = (c >= 'A' && c <= 'Z')
                    || (c >= 'a' && c <= 'z')
                    || (c >= '0' && c <= '9')
                    || c == '-' || c == '_';
            if (!ok)
                return false;
        }
        return true;
    }

    public static byte[] decodeBase64Url(String value) {
        if (!isValidUnpaddedBase64Url(value))
            throw new IllegalArgumentException("Invalid unpadded Base64URL value");
        return Base64.getUrlDecoder().decode(value);
    }

    public static String encodeBase64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
