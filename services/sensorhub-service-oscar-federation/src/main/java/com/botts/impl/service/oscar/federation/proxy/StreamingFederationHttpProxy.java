package com.botts.impl.service.oscar.federation.proxy;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.botts.impl.service.oscar.federation.credentials.FederationCredentialStore;
import com.botts.impl.service.oscar.federation.model.RemoteNodeConnection;
import com.botts.impl.service.oscar.federation.security.BasicAuth;
import com.botts.impl.service.oscar.federation.security.UpstreamEndpointPolicy;

public class StreamingFederationHttpProxy implements FederationHttpProxy
{
    private static final Set<String> REQUEST_HEADERS = Set.of(
        "accept", "accept-encoding", "cache-control", "content-type", "content-encoding",
        "if-match", "if-none-match", "if-modified-since", "if-unmodified-since", "range");
    private static final Set<String> RESPONSE_HEADERS = Set.of(
        "accept-ranges", "cache-control", "content-disposition", "content-encoding", "content-language",
        "content-length", "content-range", "content-type", "etag", "expires", "last-modified", "location", "vary");

    private final HttpClient client;
    private final FederationCredentialStore credentialStore;
    private final UpstreamEndpointPolicy endpointPolicy;
    private final Duration requestTimeout;

    public StreamingFederationHttpProxy(HttpClient client, FederationCredentialStore credentialStore,
            UpstreamEndpointPolicy endpointPolicy, Duration requestTimeout)
    {
        this.client = client;
        this.credentialStore = credentialStore;
        this.endpointPolicy = endpointPolicy;
        this.requestTimeout = requestTimeout;
    }

    @Override
    public void proxy(RemoteNodeConnection node, String upstreamPath,
            HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        try
        {
            URI base = endpointPolicy.validateAndNormalize(node.getUpstreamBaseUri(), node.isPrivateNetworkAllowed());
            URI target = buildTarget(base, upstreamPath, request.getQueryString());
            var credentials = credentialStore.retrieve(node.getCredentialId())
                .orElseThrow(() -> new IllegalStateException("Federation credentials are unavailable"));

            HttpRequest.Builder builder = HttpRequest.newBuilder(target).timeout(requestTimeout);
            java.util.Collections.list(request.getHeaderNames()).forEach(name -> {
                if (REQUEST_HEADERS.contains(name.toLowerCase(Locale.ROOT)))
                    java.util.Collections.list(request.getHeaders(name)).forEach(value -> builder.header(name, value));
            });
            try (credentials)
            {
                builder.header("Authorization", BasicAuth.headerValue(credentials));
            }

            HttpRequest.BodyPublisher body = hasRequestBody(request)
                ? HttpRequest.BodyPublishers.ofInputStream(() -> inputStream(request))
                : HttpRequest.BodyPublishers.noBody();
            builder.method(request.getMethod(), body);

            HttpResponse<InputStream> upstream = client.send(builder.build(), HttpResponse.BodyHandlers.ofInputStream());
            response.setStatus(upstream.statusCode());
            upstream.headers().map().forEach((name, values) -> copyResponseHeader(
                name, values, node, request, response));

            if (!"HEAD".equalsIgnoreCase(request.getMethod()))
            {
                try (InputStream input = upstream.body())
                {
                    input.transferTo(response.getOutputStream());
                }
            }
        }
        catch (java.net.http.HttpTimeoutException e)
        {
            response.sendError(HttpServletResponse.SC_GATEWAY_TIMEOUT, "Federation upstream timed out");
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            response.sendError(HttpServletResponse.SC_BAD_GATEWAY, "Federation request was interrupted");
        }
        catch (IllegalArgumentException | IllegalStateException e)
        {
            response.sendError(HttpServletResponse.SC_BAD_GATEWAY, e.getMessage());
        }
        catch (IOException e)
        {
            if (!response.isCommitted())
                response.sendError(HttpServletResponse.SC_BAD_GATEWAY, "Cannot connect to federation upstream");
            else
                throw e;
        }
    }

    private static URI buildTarget(URI base, String path, String rawQuery)
    {
        if (path == null || !path.startsWith("/") || path.indexOf('\\') >= 0)
            throw new IllegalArgumentException("Invalid upstream path");
        URI pathUri = URI.create(path);
        if (!pathUri.normalize().getRawPath().equals(pathUri.getRawPath()))
            throw new IllegalArgumentException("Path traversal is prohibited");
        try
        {
            return new URI(base.getScheme(), null, base.getHost(), base.getPort(),
                pathUri.getRawPath(), rawQuery, null);
        }
        catch (java.net.URISyntaxException e)
        {
            throw new IllegalArgumentException("Invalid upstream request URI", e);
        }
    }

    private static boolean hasRequestBody(HttpServletRequest request)
    {
        String method = request.getMethod().toUpperCase(Locale.ROOT);
        return Set.of("POST", "PUT", "PATCH").contains(method) || request.getContentLengthLong() > 0;
    }

    private static InputStream inputStream(HttpServletRequest request)
    {
        try
        {
            return request.getInputStream();
        }
        catch (IOException e)
        {
            throw new java.io.UncheckedIOException(e);
        }
    }

    private static void copyResponseHeader(String name, List<String> values, RemoteNodeConnection node,
            HttpServletRequest request, HttpServletResponse response)
    {
        String lower = name.toLowerCase(Locale.ROOT);
        if (!RESPONSE_HEADERS.contains(lower))
            return;
        for (String value : values)
        {
            String output = "location".equals(lower) ? rewriteLocation(value, node, request) : value;
            if (output != null)
                response.addHeader(name, output);
        }
    }

    private static String rewriteLocation(String value, RemoteNodeConnection node, HttpServletRequest request)
    {
        try
        {
            URI location = URI.create(value);
            if (!location.isAbsolute())
            {
                if (location.getRawAuthority() != null)
                    return null;

                String gatewayPrefix = request.getContextPath() + "/" + node.getOscarSystemUid();
                if (location.getRawPath().startsWith("/"))
                    return gatewayPrefix + location.getRawPath() + suffix(location);

                URI currentGatewayUri = URI.create(request.getRequestURI());
                URI resolved = currentGatewayUri.resolve(location);
                if (!resolved.getRawPath().startsWith(gatewayPrefix + "/") &&
                    !resolved.getRawPath().equals(gatewayPrefix))
                    return null;
                return resolved.getRawPath() + suffix(resolved);
            }
            URI base = node.getUpstreamBaseUri();
            int locationPort = effectivePort(location);
            int basePort = effectivePort(base);
            if (!location.getScheme().equalsIgnoreCase(base.getScheme()) ||
                !location.getHost().equalsIgnoreCase(base.getHost()) || locationPort != basePort)
                return null;
            String context = request.getContextPath();
            return context + "/" + node.getOscarSystemUid() + location.getRawPath() +
                suffix(location);
        }
        catch (RuntimeException e)
        {
            return null;
        }
    }

    private static String suffix(URI uri)
    {
        return (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery()) +
            (uri.getRawFragment() == null ? "" : "#" + uri.getRawFragment());
    }

    private static int effectivePort(URI uri)
    {
        if (uri.getPort() >= 0)
            return uri.getPort();
        return "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
    }
}
