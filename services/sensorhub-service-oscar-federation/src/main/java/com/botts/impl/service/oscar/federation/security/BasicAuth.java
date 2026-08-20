package com.botts.impl.service.oscar.federation.security;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import com.botts.impl.service.oscar.federation.credentials.FederationCredentials;

public final class BasicAuth
{
    private BasicAuth() {}

    public static String headerValue(FederationCredentials credentials)
    {
        char[] password = credentials.copyPassword();
        char[] username = credentials.getUsername().toCharArray();
        char[] combined = new char[username.length + 1 + password.length];
        System.arraycopy(username, 0, combined, 0, username.length);
        combined[username.length] = ':';
        System.arraycopy(password, 0, combined, username.length + 1, password.length);
        ByteBuffer encoded = StandardCharsets.UTF_8.encode(CharBuffer.wrap(combined));
        byte[] bytes = new byte[encoded.remaining()];
        encoded.get(bytes);
        try
        {
            return "Basic " + Base64.getEncoder().encodeToString(bytes);
        }
        finally
        {
            java.util.Arrays.fill(password, '\0');
            java.util.Arrays.fill(username, '\0');
            java.util.Arrays.fill(combined, '\0');
            java.util.Arrays.fill(bytes, (byte)0);
        }
    }
}
