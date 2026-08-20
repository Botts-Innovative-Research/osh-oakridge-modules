package com.botts.impl.service.oscar.federation.credentials;

import java.util.Optional;

public interface FederationCredentialStore
{
    String store(FederationCredentials credentials);
    void replace(String credentialId, FederationCredentials credentials);
    Optional<FederationCredentials> retrieve(String credentialId);
    void remove(String credentialId);
}
