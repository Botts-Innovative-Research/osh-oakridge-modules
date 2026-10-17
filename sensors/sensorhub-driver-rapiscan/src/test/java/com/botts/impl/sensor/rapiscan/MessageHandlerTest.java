package com.botts.impl.sensor.rapiscan;

import com.botts.impl.sensor.rapiscan.output.DailyFileOutput;
import com.botts.impl.sensor.rapiscan.output.TamperOutput;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MessageHandlerTest {

    @Test
    public void startsOneReaderAndStopsBlockedRead() throws Exception {
        var input = new BlockingInputStream();
        var handler = new MessageHandler(input, configuredSensor());

        assertTrue("reader did not start", input.readStarted.await(2, TimeUnit.SECONDS));

        handler.start();
        handler.stop();

        assertTrue(input.closed.getCount() == 0);
        assertTrue("reader did not exit", input.readStopped.await(2, TimeUnit.SECONDS));
        assertEquals(1, input.readCalls.get());
        assertEquals(1, input.maxConcurrentReaders.get());
        assertEquals(0, input.activeReaders.get());
    }

    @Test
    public void rpmTamperMessageOverridesConnectionClearBaseline() throws Exception {
        var sensor = new RecordingSensor();
        sensor.setConfiguration(new RapiscanConfig());
        sensor.tamperOutput.onConnected();

        // The deliberately incomplete SP message fails in the recording
        // sensor, but must not prevent the following tamper transition.
        var input = new ByteArrayInputStream("SP\nTT\n".getBytes(StandardCharsets.UTF_8));
        var handler = new MessageHandler(input, sensor);

        assertTrue("TT message was not processed", sensor.tamperOutput.tamperReceived.await(2, TimeUnit.SECONDS));
        handler.stop();

        assertEquals(List.of(false, true), sensor.tamperOutput.states);
    }

    @Test
    public void endOfStreamStopsWithoutRestartingReader() throws Exception {
        var input = new CountingEofInputStream();
        var handler = new MessageHandler(input, configuredSensor());

        assertTrue("EOF was not read", input.eofRead.await(2, TimeUnit.SECONDS));
        Thread.sleep(50);
        handler.stop();

        assertEquals(1, input.readCalls.get());
    }

    @Test
    public void occupancySummaryAllowsConnectionBeforeFirstNeutronSample() {
        assertEquals(0, MessageHandler.getNeutronMax(List.of()));
        assertEquals(17, MessageHandler.getNeutronMax(List.of(4, 17, 9)));
    }

    private RapiscanSensor configuredSensor() {
        var sensor = new RapiscanSensor();
        sensor.setConfiguration(new RapiscanConfig());
        return sensor;
    }

    private static class RecordingSensor extends RapiscanSensor {
        private final RecordingTamperOutput tamperOutput = new RecordingTamperOutput(this);
        private final RecordingDailyFileOutput dailyFileOutput = new RecordingDailyFileOutput(this);

        @Override
        public TamperOutput getTamperOutput() {
            return tamperOutput;
        }

        @Override
        public DailyFileOutput getDailyFileOutput() {
            return dailyFileOutput;
        }
    }

    private static class RecordingTamperOutput extends TamperOutput {
        private final List<java.lang.Boolean> states = new CopyOnWriteArrayList<>();
        private final CountDownLatch tamperReceived = new CountDownLatch(1);

        private RecordingTamperOutput(RapiscanSensor parentSensor) {
            super(parentSensor);
        }

        @Override
        public void onNewMessage(boolean tamperState) {
            states.add(tamperState);
            if (tamperState) {
                tamperReceived.countDown();
            }
        }
    }

    private static class RecordingDailyFileOutput extends DailyFileOutput {
        private RecordingDailyFileOutput(RapiscanSensor parentSensor) {
            super(parentSensor);
        }

        @Override
        public void onNewMessage(String csvString) {
            // The routing behavior is under test, not persistence.
        }
    }

    private static class BlockingInputStream extends InputStream {
        private final CountDownLatch readStarted = new CountDownLatch(1);
        private final CountDownLatch readStopped = new CountDownLatch(1);
        private final CountDownLatch closed = new CountDownLatch(1);
        private final AtomicInteger readCalls = new AtomicInteger();
        private final AtomicInteger activeReaders = new AtomicInteger();
        private final AtomicInteger maxConcurrentReaders = new AtomicInteger();

        @Override
        public int read() throws IOException {
            readCalls.incrementAndGet();
            int active = activeReaders.incrementAndGet();
            maxConcurrentReaders.accumulateAndGet(active, Math::max);
            readStarted.countDown();

            try {
                if (!closed.await(5, TimeUnit.SECONDS)) {
                    throw new IOException("test input was not closed");
                }
                return -1;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("reader interrupted", e);
            } finally {
                activeReaders.decrementAndGet();
                readStopped.countDown();
            }
        }

        @Override
        public void close() {
            closed.countDown();
        }
    }

    private static class CountingEofInputStream extends InputStream {
        private final AtomicInteger readCalls = new AtomicInteger();
        private final CountDownLatch eofRead = new CountDownLatch(1);

        @Override
        public int read() {
            readCalls.incrementAndGet();
            eofRead.countDown();
            return -1;
        }
    }
}
