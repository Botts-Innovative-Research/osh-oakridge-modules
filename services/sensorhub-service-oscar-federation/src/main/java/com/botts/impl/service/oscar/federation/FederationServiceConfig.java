package com.botts.impl.service.oscar.federation;

import java.util.ArrayList;
import java.util.List;
import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.security.SecurityConfig;
import org.sensorhub.api.service.HttpServiceConfig;
import com.botts.impl.service.oscar.federation.model.RemoteNodeConfig;

public class FederationServiceConfig extends HttpServiceConfig
{
    @DisplayInfo(desc = "Security options for federation routes")
    public SecurityConfig security = new SecurityConfig();

    @DisplayInfo(label = "Remote Nodes", desc = "Non-secret connection metadata for registered OSCAR nodes")
    public List<RemoteNodeConfig> remoteNodes = new ArrayList<>();

    @DisplayInfo.Required
    @DisplayInfo(label = "Credential Encryption Key File", desc = "File containing a base64-encoded 256-bit AES key; keep it separate from module state")
    public String credentialKeyFile = "/run/secrets/oscar_federation_key";

    @DisplayInfo(label = "Administrator Role", desc = "OSH role allowed to register, update, and remove federation targets")
    public String administratorRole = "admin";

    @DisplayInfo(label = "Allow Private Network Targets", desc = "Allow RFC1918/ULA targets after loopback, link-local, multicast, and wildcard addresses are rejected")
    public boolean allowPrivateNetworkTargets = false;

    @DisplayInfo(label = "Allow Insecure HTTP", desc = "Allow unencrypted upstream HTTP and WS connections")
    public boolean allowInsecureHttp = false;

    @DisplayInfo(label = "Connect Timeout (seconds)")
    public int connectTimeoutSeconds = 10;

    @DisplayInfo(label = "Request Timeout (seconds)")
    public int requestTimeoutSeconds = 120;

    @DisplayInfo(label = "Maximum Registration Body (bytes)")
    public int maxRegistrationBodyBytes = 65536;

    public FederationServiceConfig()
    {
        moduleClass = FederationService.class.getCanonicalName();
        endPoint = "/federation";
        security.requireAuth = true;
    }
}
