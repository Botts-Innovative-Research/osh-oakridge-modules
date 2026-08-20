package com.botts.impl.service.oscar.federation.credentials;

public final class FederationCredentials implements AutoCloseable
{
    private final String username;
    private final char[] password;

    public FederationCredentials(String username, char[] password)
    {
        this.username = username;
        this.password = password == null ? new char[0] : password.clone();
    }

    public String getUsername() { return username; }
    public char[] copyPassword() { return password.clone(); }

    @Override
    public void close()
    {
        java.util.Arrays.fill(password, '\0');
    }
}
