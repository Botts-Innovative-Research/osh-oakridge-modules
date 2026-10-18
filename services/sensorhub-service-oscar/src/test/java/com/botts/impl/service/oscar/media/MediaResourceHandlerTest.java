package com.botts.impl.service.oscar.media;

import com.botts.impl.service.bucket.BucketService;
import com.botts.impl.service.bucket.BucketServiceConfig;
import com.botts.impl.service.bucket.filesystem.FileSystemBucketStore;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sensorhub.api.ISensorHub;
import org.sensorhub.api.module.ModuleEvent;
import org.sensorhub.api.service.IHttpServer;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.impl.module.ModuleRegistry;
import org.sensorhub.impl.security.BasicSecurityRealmConfig;
import org.sensorhub.impl.service.HttpServer;
import org.sensorhub.impl.service.HttpServerConfig;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;

public class MediaResourceHandlerTest {
    private static final String USER = "media-viewer";
    private static final String PASSWORD = "test-password";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private ISensorHub hub;
    private BucketService bucketService;
    private IHttpServer<?> httpServer;
    private MediaSetStore mediaStore;
    private String endpoint;

    @Before
    public void setUp() throws Exception {
        hub = new SensorHub();
        hub.start();
        ModuleRegistry registry = hub.getModuleRegistry();

        BasicSecurityRealmConfig realmConfig = new BasicSecurityRealmConfig();
        realmConfig.id = UUID.randomUUID().toString();
        realmConfig.autoStart = true;
        BasicSecurityRealmConfig.UserConfig user = new BasicSecurityRealmConfig.UserConfig();
        user.userID = USER;
        user.name = USER;
        user.password = PASSWORD;
        realmConfig.users.add(user);
        registry.loadModule(realmConfig).waitForState(ModuleEvent.ModuleState.STARTED, 5_000);

        HttpServerConfig httpConfig = new HttpServerConfig();
        httpConfig.id = UUID.randomUUID().toString();
        httpConfig.autoStart = true;
        httpConfig.httpPort = freePort();
        httpConfig.authMethod = HttpServerConfig.AuthMethod.BASIC;
        httpServer = (HttpServer) registry.loadModule(httpConfig);
        assertTrue(httpServer.waitForState(ModuleEvent.ModuleState.STARTED, 5_000));

        BucketServiceConfig bucketConfig = new BucketServiceConfig();
        bucketConfig.id = UUID.randomUUID().toString();
        bucketConfig.autoStart = true;
        bucketConfig.fileStoreRootDir = temporaryFolder.newFolder("bucket-root").getAbsolutePath();
        bucketConfig.initialBuckets = List.of(MediaSetStore.VIDEO_BUCKET);
        bucketConfig.security.requireAuth = true;
        bucketConfig.security.enableAccessControl = false;
        bucketService = (BucketService) registry.loadModule(bucketConfig);
        assertTrue(bucketService.waitForState(ModuleEvent.ModuleState.STARTED, 5_000));

        mediaStore = new MediaSetStore(bucketService.getBucketStore());
        bucketService.registerObjectHandler(new MediaResourceHandler(mediaStore));
        endpoint = httpServer.getPublicEndpointUrl(bucketConfig.endPoint);
    }

    @After
    public void tearDown() {
        if (hub != null)
            hub.stop();
    }

    @Test
    public void eventMediaApiRequiresAuthenticationAndOnlyServesWhitelistedFinalObjects()
            throws Exception {
        MediaSetStore.Identity identity = createMediaSet("event-api", "camera-api");
        String cameraToken = MediaSetStore.apiCameraToken(identity.cameraUid());
        HttpClient client = HttpClient.newHttpClient();

        HttpResponse<String> unauthenticated = client.send(HttpRequest.newBuilder()
                        .uri(URI.create(endpoint + "/videos/media/" + identity.eventId()))
                        .build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(401, unauthenticated.statusCode());

        HttpResponse<String> event = sendAuthenticated(client, "GET",
                "/videos/media/" + identity.eventId());
        assertEquals(200, event.statusCode());
        assertTrue(event.headers().firstValue("Content-Type").orElse("")
                .startsWith("application/json"));
        assertTrue(event.body().contains("\"eventId\":\"event-api\""));
        assertTrue(event.body().contains("frame-01.jpg"));

        HttpResponse<String> manifest = sendAuthenticated(client, "GET",
                "/videos/media/" + identity.eventId() + "/" + cameraToken + "/manifest.json");
        assertEquals(200, manifest.statusCode());
        assertTrue(manifest.body().contains("\"schemaVersion\":1"));

        HttpResponse<byte[]> frame = sendAuthenticatedBytes(client,
                "/videos/media/" + identity.eventId() + "/" + cameraToken + "/frame-01.jpg");
        assertEquals(200, frame.statusCode());
        assertTrue(frame.headers().firstValue("Content-Type").orElse("")
                .startsWith("image/jpeg"));
        assertNotNull(ImageIO.read(new java.io.ByteArrayInputStream(frame.body())));

        assertEquals(404, sendAuthenticated(client, "GET",
                "/videos/media/legacy-event-without-manifest").statusCode());
        assertEquals(404, sendAuthenticated(client, "GET",
                "/videos/" + mediaStore.manifestKey(identity)).statusCode());
        assertEquals(405, sendAuthenticated(client, "POST",
                "/videos/media/" + identity.eventId()).statusCode());
    }

    @Test
    public void eventCatalogAndServedFrameBytesAreStrictlyBounded() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        String boundedEvent = "event-camera-bound";
        for (int index = 0; index <= MediaResourceHandler.MAX_MEDIA_SETS_PER_EVENT; index++)
            createMediaSet(boundedEvent, "camera-bound-" + index);
        assertEquals("A ninth camera must be rejected instead of building an unbounded response",
                400, sendAuthenticated(client, "GET", "/videos/media/" + boundedEvent).statusCode());

        MediaSetStore.Identity identity = createMediaSet("event-mutated-frame", "camera-mutated");
        MediaManifest.Asset asset = mediaStore.findManifest(identity).orElseThrow().assets().get(0);
        Path frame = Path.of(mediaStore.bucketStore().getResourceURI(
                MediaSetStore.VIDEO_BUCKET, asset.objectKey()));
        Files.write(frame, new byte[]{0x00}, java.nio.file.StandardOpenOption.APPEND);
        String cameraToken = MediaSetStore.apiCameraToken(identity.cameraUid());
        assertEquals("Actual bytes larger than the manifest must be rejected before response output",
                404, sendAuthenticated(client, "GET", "/videos/media/" + identity.eventId()
                        + "/" + cameraToken + "/frame-01.jpg").statusCode());
    }

    private MediaSetStore.Identity createMediaSet(String eventId, String cameraUid) throws Exception {
        FileSystemBucketStore store = (FileSystemBucketStore) bucketService.getBucketStore();
        MediaSetStore.Identity identity = new MediaSetStore.Identity(eventId, "lane-api", cameraUid);
        String sourceKey = "clips/" + cameraUid + "/source.mp4";
        Path source = Path.of(store.getWritableResourceURI(MediaSetStore.VIDEO_BUCKET, sourceKey));
        Files.writeString(source, "source", StandardCharsets.UTF_8);

        List<MediaManifest.Asset> assets = new ArrayList<>();
        for (int index = 0; index < MediaManifest.RETAINED_FRAME_COUNT; index++) {
            BufferedImage image = new BufferedImage(4, 4, BufferedImage.TYPE_3BYTE_BGR);
            image.setRGB(index % 4, index % 4, 0xff101010 * (index + 1));
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            assertTrue(ImageIO.write(image, "jpeg", bytes));
            MediaSetStore.PublishedAsset published = mediaStore.publishFrame(
                    identity, index + 1, bytes.toByteArray());
            assets.add(new MediaManifest.Asset(MediaManifest.FramePosition.values()[index],
                    published.objectKey(), "image/jpeg", published.sha256(), published.bytes(),
                    published.width(), published.height(), index, index * 10L, index,
                    MediaManifest.AssetState.PUBLISHED));
        }

        Instant now = Instant.now();
        mediaStore.publishManifest(identity, new MediaManifest(
                MediaManifest.CURRENT_SCHEMA_VERSION, identity.mediaSetId(), identity.eventId(),
                identity.laneUid(), identity.cameraUid(), now, 85,
                MediaManifest.LifecycleState.AVAILABLE, MediaManifest.Hold.none(),
                new MediaManifest.Source("videos/" + sourceKey, MediaSetStore.sha256(source),
                        Files.size(source), "h264", 1_000_000, now.plusSeconds(3_600),
                        MediaManifest.SourceState.PRESERVED_FOR_ROLLBACK), assets));
        return identity;
    }

    private HttpResponse<String> sendAuthenticated(HttpClient client, String method, String path)
            throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint + path))
                .header("Authorization", basicAuthorization());
        if ("POST".equals(method))
            request.POST(HttpRequest.BodyPublishers.noBody());
        else
            request.GET();
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<byte[]> sendAuthenticatedBytes(HttpClient client, String path)
            throws Exception {
        return client.send(HttpRequest.newBuilder()
                        .uri(URI.create(endpoint + path))
                        .header("Authorization", basicAuthorization())
                        .GET().build(), HttpResponse.BodyHandlers.ofByteArray());
    }

    private static String basicAuthorization() {
        return "Basic " + Base64.getEncoder().encodeToString(
                (USER + ":" + PASSWORD).getBytes(StandardCharsets.UTF_8));
    }

    private static int freePort() throws Exception {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
