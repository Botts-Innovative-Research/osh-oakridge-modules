package com.botts.impl.service.oscar.federation.http;

import java.util.Optional;

public final class FederationRoute
{
    public enum Kind { NODE_MANAGEMENT, HTTP_PROXY, MQTT_WEBSOCKET, INVALID }

    private final Kind kind;
    private final String targetUid;
    private final String upstreamPath;

    private FederationRoute(Kind kind, String targetUid, String upstreamPath)
    {
        this.kind = kind;
        this.targetUid = targetUid;
        this.upstreamPath = upstreamPath;
    }

    public static FederationRoute parse(String pathInfo)
    {
        if (pathInfo == null || pathInfo.isBlank() || "/".equals(pathInfo) || "/nodes".equals(pathInfo))
            return new FederationRoute(Kind.NODE_MANAGEMENT, null, null);

        String normalized = pathInfo.startsWith("/") ? pathInfo.substring(1) : pathInfo;
        String[] segments = normalized.split("/", 2);
        if (segments[0].equals("nodes"))
            return segments.length == 2 && !segments[1].isBlank() && !segments[1].contains("/")
                ? new FederationRoute(Kind.NODE_MANAGEMENT, segments[1], null)
                : new FederationRoute(Kind.INVALID, null, null);
        if (segments.length < 2 || segments[0].isBlank())
            return new FederationRoute(Kind.INVALID, null, null);

        String remainder = segments[1];
        if ("mqtt".equals(remainder))
            return new FederationRoute(Kind.MQTT_WEBSOCKET, segments[0], null);
        if (remainder.isBlank() || remainder.contains("..") || remainder.indexOf('\\') >= 0)
            return new FederationRoute(Kind.INVALID, null, null);

        return new FederationRoute(Kind.HTTP_PROXY, segments[0], "/" + remainder);
    }

    public Kind getKind() { return kind; }
    public Optional<String> getTargetUid() { return Optional.ofNullable(targetUid); }
    public Optional<String> getUpstreamPath() { return Optional.ofNullable(upstreamPath); }
}
