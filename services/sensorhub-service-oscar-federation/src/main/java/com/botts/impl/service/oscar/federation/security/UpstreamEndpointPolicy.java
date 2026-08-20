package com.botts.impl.service.oscar.federation.security;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Locale;

/** Validates registration and re-resolves targets before each upstream connection. */
public class UpstreamEndpointPolicy
{
    private final boolean allowInsecureHttp;
    private final boolean allowPrivateNetworkTargets;

    public UpstreamEndpointPolicy(boolean allowInsecureHttp, boolean allowPrivateNetworkTargets)
    {
        this.allowInsecureHttp = allowInsecureHttp;
        this.allowPrivateNetworkTargets = allowPrivateNetworkTargets;
    }

    public URI validateAndNormalize(URI candidate, boolean nodeAllowsPrivateNetwork)
    {
        if (candidate == null || candidate.getScheme() == null || candidate.getHost() == null)
            throw new IllegalArgumentException("Upstream URL must be an absolute HTTP(S) URL");
        String scheme = candidate.getScheme().toLowerCase(Locale.ROOT);
        if (!"https".equals(scheme) && !(allowInsecureHttp && "http".equals(scheme)))
            throw new IllegalArgumentException("Upstream URL must use HTTPS");
        if (candidate.getRawUserInfo() != null || candidate.getRawQuery() != null || candidate.getRawFragment() != null)
            throw new IllegalArgumentException("Upstream URL cannot contain user info, a query, or a fragment");
        if (candidate.getRawPath() != null && !candidate.getRawPath().isBlank() && !"/".equals(candidate.getRawPath()))
            throw new IllegalArgumentException("Upstream URL must identify the node origin without a path");
        if (candidate.getPort() == 0 || candidate.getPort() < -1 || candidate.getPort() > 65535)
            throw new IllegalArgumentException("Invalid upstream port");

        boolean allowPrivate = allowPrivateNetworkTargets && nodeAllowsPrivateNetwork;
        try
        {
            InetAddress[] addresses = InetAddress.getAllByName(candidate.getHost());
            if (addresses.length == 0)
                throw new IllegalArgumentException("Upstream host did not resolve");
            for (InetAddress address : addresses)
                validateAddress(address, allowPrivate);
        }
        catch (UnknownHostException e)
        {
            throw new IllegalArgumentException("Upstream host could not be resolved", e);
        }

        try
        {
            return new URI(scheme, null, candidate.getHost(), candidate.getPort(), null, null, null);
        }
        catch (java.net.URISyntaxException e)
        {
            throw new IllegalArgumentException("Invalid upstream URL", e);
        }
    }

    private static void validateAddress(InetAddress address, boolean allowPrivate)
    {
        if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress() ||
            address.isMulticastAddress())
            throw new IllegalArgumentException("Loopback, wildcard, link-local, and multicast targets are prohibited");

        boolean privateAddress = address.isSiteLocalAddress() || isUniqueLocalIpv6(address);
        if (privateAddress && !allowPrivate)
            throw new IllegalArgumentException("Private-network target requires explicit server and node permission");
    }

    private static boolean isUniqueLocalIpv6(InetAddress address)
    {
        return address instanceof Inet6Address && (address.getAddress()[0] & 0xfe) == 0xfc;
    }
}
