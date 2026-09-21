package com.botts.impl.sensor.rapiscan;

import com.botts.api.service.bucket.IBucketStore;
import com.botts.impl.sensor.rapiscan.output.DailyFileOutput;
import com.botts.impl.sensor.rapiscan.output.TamperOutput;
import com.botts.impl.service.bucket.filesystem.FileSystemBucketStore;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sensorhub.impl.utils.rad.dailyfile.DailyFileAppender;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

/**
 * Verifies that the Rapiscan message reader records every raw line in the daily file before parsing,
 * and that a malformed line does not stop processing of the lines that follow.
 */
public class MessageHandlerTest {

    private Path root;
    private IBucketStore store;
    private RapiscanSensor sensor;
    private TamperOutput tamperOutput;
    private DailyFileOutput dailyFileOutput;

    @Before
    public void setUp() throws Exception {
        root = Files.createTempDirectory("rapiscan-handler-test-");
        store = new FileSystemBucketStore(root);

        sensor = mock(RapiscanSensor.class);
        when(sensor.getLogger()).thenReturn(LoggerFactory.getLogger(MessageHandlerTest.class));
        when(sensor.getUniqueIdentifier()).thenReturn("urn:osh:sensor:rapiscan:test001");
        when(sensor.getConfiguration()).thenReturn(new RapiscanConfig());

        tamperOutput = mock(TamperOutput.class);
        dailyFileOutput = mock(DailyFileOutput.class);
        when(sensor.getTamperOutput()).thenReturn(tamperOutput);
        when(sensor.getDailyFileOutput()).thenReturn(dailyFileOutput);
    }

    @After
    public void tearDown() throws Exception {
        if (root != null && Files.exists(root)) {
            try (Stream<Path> paths = Files.walk(root)) {
                paths.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
            }
        }
    }

    @Test
    public void badLineIsRecordedAndDoesNotStopTheReader() throws Exception {
        String stream = "GX,000001\r\n"                                    // too few fields: parse fails
                + "\r\n"                                                  // stray blank line
                + "\"unterminated,quote\r\n"                              // opencsv error
                + "TC,111111,111111,111111,111111\r\n";                   // valid, must still be dispatched
        InputStream in = new ByteArrayInputStream(stream.getBytes(StandardCharsets.UTF_8));

        DailyFileAppender appender = new DailyFileAppender(
                CompletableFuture.completedFuture(store), "test001", Clock.systemDefaultZone(),
                LoggerFactory.getLogger(MessageHandlerTest.class));

        MessageHandler handler = new MessageHandler(in, sensor, appender);

        // The valid line after the bad ones is still dispatched
        verify(tamperOutput, timeout(5000)).onNewMessage(false);
        handler.stop();
        appender.close();

        // Every non-blank raw line (including the unparseable ones) reached the daily file with
        // local and UTC CAS timestamps.
        String key = "test001_" + LocalDate.now() + ".csv";
        try (InputStream file = store.getObject(DailyFileAppender.BUCKET, key)) {
            String[] lines = new String(file.readAllBytes(), StandardCharsets.UTF_8).split("\\r\\n");
            assertEquals(3, lines.length);
            assertEquals("GX,000001", lines[0].substring(0, "GX,000001".length()));
            assertEquals("\"unterminated,quote", lines[1].substring(0, "\"unterminated,quote".length()));
            assertEquals("TC,111111,111111,111111,111111",
                    lines[2].substring(0, "TC,111111,111111,111111,111111".length()));
            for (String line : lines)
                org.junit.Assert.assertTrue(line.matches(".*\\d{2}-\\d{2}-\\d{2}\\.\\d{3},"
                        + "\\d{2}-\\d{2}-\\d{2}\\.\\d{3}"));
        }

        // The live dailyFile output saw the same three lines
        verify(dailyFileOutput, times(3)).onNewMessage(anyString());
    }
}
