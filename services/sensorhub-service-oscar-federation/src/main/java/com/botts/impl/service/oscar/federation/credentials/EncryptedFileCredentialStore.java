package com.botts.impl.service.oscar.federation.credentials;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/** AES-256-GCM credential storage. The encryption key is loaded from a separate file. */
public class EncryptedFileCredentialStore implements FederationCredentialStore
{
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final Gson GSON = new Gson();

    private static final class Envelope
    {
        String iv;
        String ciphertext;
    }

    private final Path storeFile;
    private final SecretKey key;
    private final SecureRandom random = new SecureRandom();
    private final Map<String, Envelope> entries = new HashMap<>();

    public EncryptedFileCredentialStore(Path storeFile, Path keyFile) throws IOException
    {
        this.storeFile = storeFile;
        byte[] keyBytes = Base64.getDecoder().decode(Files.readString(keyFile, StandardCharsets.US_ASCII).trim());
        if (keyBytes.length != 32)
            throw new IllegalArgumentException("Federation credential key must decode to exactly 32 bytes");
        this.key = new SecretKeySpec(keyBytes, "AES");
        java.util.Arrays.fill(keyBytes, (byte)0);
        load();
    }

    @Override
    public synchronized String store(FederationCredentials credentials)
    {
        String id = UUID.randomUUID().toString();
        entries.put(id, encrypt(id, credentials));
        try
        {
            persist();
        }
        catch (RuntimeException e)
        {
            entries.remove(id);
            throw e;
        }
        return id;
    }

    @Override
    public synchronized void replace(String credentialId, FederationCredentials credentials)
    {
        if (!entries.containsKey(credentialId))
            throw new IllegalArgumentException("Unknown credential reference");
        Envelope previous = entries.put(credentialId, encrypt(credentialId, credentials));
        try
        {
            persist();
        }
        catch (RuntimeException e)
        {
            entries.put(credentialId, previous);
            throw e;
        }
    }

    @Override
    public synchronized Optional<FederationCredentials> retrieve(String credentialId)
    {
        Envelope envelope = entries.get(credentialId);
        return envelope == null ? Optional.empty() : Optional.of(decrypt(credentialId, envelope));
    }

    @Override
    public synchronized void remove(String credentialId)
    {
        if (credentialId == null)
            return;
        Envelope removed = entries.remove(credentialId);
        if (removed != null)
        {
            try
            {
                persist();
            }
            catch (RuntimeException e)
            {
                entries.put(credentialId, removed);
                throw e;
            }
        }
    }

    private Envelope encrypt(String id, FederationCredentials credentials)
    {
        byte[] plain = encode(credentials);
        byte[] iv = new byte[IV_BYTES];
        random.nextBytes(iv);
        try
        {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            cipher.updateAAD(id.getBytes(StandardCharsets.UTF_8));
            Envelope result = new Envelope();
            result.iv = Base64.getEncoder().encodeToString(iv);
            result.ciphertext = Base64.getEncoder().encodeToString(cipher.doFinal(plain));
            return result;
        }
        catch (GeneralSecurityException e)
        {
            throw new IllegalStateException("Cannot encrypt federation credentials", e);
        }
        finally
        {
            java.util.Arrays.fill(plain, (byte)0);
            java.util.Arrays.fill(iv, (byte)0);
        }
    }

    private FederationCredentials decrypt(String id, Envelope envelope)
    {
        byte[] iv = Base64.getDecoder().decode(envelope.iv);
        byte[] encrypted = Base64.getDecoder().decode(envelope.ciphertext);
        byte[] plain = null;
        try
        {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            cipher.updateAAD(id.getBytes(StandardCharsets.UTF_8));
            plain = cipher.doFinal(encrypted);
            return decode(plain);
        }
        catch (GeneralSecurityException | IOException e)
        {
            throw new IllegalStateException("Cannot decrypt federation credentials", e);
        }
        finally
        {
            java.util.Arrays.fill(iv, (byte)0);
            java.util.Arrays.fill(encrypted, (byte)0);
            if (plain != null)
                java.util.Arrays.fill(plain, (byte)0);
        }
    }

    private static byte[] encode(FederationCredentials credentials)
    {
        byte[] user = credentials.getUsername().getBytes(StandardCharsets.UTF_8);
        char[] passwordChars = credentials.copyPassword();
        ByteBuffer encodedPassword = StandardCharsets.UTF_8.encode(java.nio.CharBuffer.wrap(passwordChars));
        byte[] password = new byte[encodedPassword.remaining()];
        encodedPassword.get(password);
        ByteBuffer buffer = ByteBuffer.allocate(8 + user.length + password.length);
        buffer.putInt(user.length).put(user).putInt(password.length).put(password);
        java.util.Arrays.fill(passwordChars, '\0');
        java.util.Arrays.fill(password, (byte)0);
        return buffer.array();
    }

    private static FederationCredentials decode(byte[] plain) throws IOException
    {
        try (var in = new DataInputStream(new java.io.ByteArrayInputStream(plain)))
        {
            int userLength = in.readInt();
            if (userLength < 0 || userLength > plain.length)
                throw new IOException("Invalid encrypted credential payload");
            byte[] user = in.readNBytes(userLength);
            int passwordLength = in.readInt();
            if (passwordLength < 0 || passwordLength > plain.length)
                throw new IOException("Invalid encrypted credential payload");
            byte[] password = in.readNBytes(passwordLength);
            java.nio.CharBuffer decodedPassword = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(password));
            char[] chars = new char[decodedPassword.remaining()];
            decodedPassword.get(chars);
            FederationCredentials result = new FederationCredentials(
                new String(user, StandardCharsets.UTF_8), chars);
            java.util.Arrays.fill(user, (byte)0);
            java.util.Arrays.fill(password, (byte)0);
            java.util.Arrays.fill(chars, '\0');
            return result;
        }
    }

    private void load() throws IOException
    {
        if (!Files.exists(storeFile))
            return;
        String json = Files.readString(storeFile, StandardCharsets.UTF_8);
        Map<String, Envelope> loaded = GSON.fromJson(json, new TypeToken<Map<String, Envelope>>(){}.getType());
        if (loaded != null)
            entries.putAll(loaded);
    }

    private void persist()
    {
        try
        {
            Files.createDirectories(storeFile.toAbsolutePath().getParent());
            Path temp = Files.createTempFile(storeFile.toAbsolutePath().getParent(), "federation-credentials-", ".tmp");
            Files.writeString(temp, GSON.toJson(entries), StandardCharsets.UTF_8);
            try
            {
                Files.setPosixFilePermissions(temp, PosixFilePermissions.fromString("rw-------"));
            }
            catch (UnsupportedOperationException ignored) {}
            try
            {
                Files.move(temp, storeFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            }
            catch (AtomicMoveNotSupportedException e)
            {
                Files.move(temp, storeFile, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        catch (IOException e)
        {
            throw new IllegalStateException("Cannot persist encrypted federation credentials", e);
        }
    }
}
