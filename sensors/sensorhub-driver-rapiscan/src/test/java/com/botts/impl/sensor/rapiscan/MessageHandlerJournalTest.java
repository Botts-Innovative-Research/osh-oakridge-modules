package com.botts.impl.sensor.rapiscan;

import com.botts.impl.sensor.rapiscan.output.DailyFileOutput;
import com.botts.impl.sensor.rapiscan.output.TamperOutput;
import org.junit.Test;
import org.mockito.InOrder;
import org.sensorhub.impl.utils.rad.dailyfile.DailyFileJournalConnection;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Verifies that audit capture precedes parsing and compatibility publication. */
public class MessageHandlerJournalTest {

    @Test
    public void journalsEveryNonBlankRawLineBeforeParsing() throws Exception {
        String malformed = "\"unterminated,quote";
        String valid = "TC,111111,111111,111111,111111";
        String input = malformed + "\r\n\r\n" + valid + "\r\n";

        RapiscanSensor sensor = mock(RapiscanSensor.class);
        RapiscanConfig config = new RapiscanConfig();
        DailyFileJournalConnection journal = mock(DailyFileJournalConnection.class);
        DailyFileOutput dailyFileOutput = mock(DailyFileOutput.class);
        TamperOutput tamperOutput = mock(TamperOutput.class);
        when(sensor.getLogger()).thenReturn(LoggerFactory.getLogger(MessageHandlerJournalTest.class));
        when(sensor.getUniqueIdentifier()).thenReturn("urn:test:rapiscan:journal");
        when(sensor.getConfiguration()).thenReturn(config);
        when(sensor.getDailyFileJournal()).thenReturn(journal);
        when(sensor.getDailyFileOutput()).thenReturn(dailyFileOutput);
        when(sensor.getTamperOutput()).thenReturn(tamperOutput);
        when(journal.shouldPublishLegacyObservation()).thenReturn(true);

        MessageHandler handler = new MessageHandler(new ByteArrayInputStream(
                input.getBytes(StandardCharsets.UTF_8)), sensor);
        verify(journal, timeout(5_000).times(2)).append(anyString(), any(Instant.class));
        verify(tamperOutput, timeout(5_000)).onNewMessage(false);
        handler.stop();

        InOrder order = inOrder(journal, dailyFileOutput);
        order.verify(journal).append(eq(malformed), any(Instant.class));
        order.verify(journal).append(eq(valid), any(Instant.class));
        order.verify(dailyFileOutput).onNewMessage(eq(valid), any(Instant.class));
    }

    @Test
    public void stopWaitsPastWarningForFinalJournalAppendBeforeWriterClose() throws Exception {
        String input = "TC,111111,111111,111111,111111\r\n";
        RapiscanSensor sensor = mock(RapiscanSensor.class);
        DailyFileJournalConnection journal = mock(DailyFileJournalConnection.class);
        DailyFileOutput dailyFileOutput = mock(DailyFileOutput.class);
        TamperOutput tamperOutput = mock(TamperOutput.class);
        when(sensor.getLogger()).thenReturn(LoggerFactory.getLogger(MessageHandlerJournalTest.class));
        when(sensor.getUniqueIdentifier()).thenReturn("urn:test:rapiscan:shutdown");
        when(sensor.getConfiguration()).thenReturn(new RapiscanConfig());
        when(sensor.getDailyFileJournal()).thenReturn(journal);
        when(sensor.getDailyFileOutput()).thenReturn(dailyFileOutput);
        when(sensor.getTamperOutput()).thenReturn(tamperOutput);
        when(journal.shouldPublishLegacyObservation()).thenReturn(true);
        CountDownLatch appendEntered = new CountDownLatch(1);
        CountDownLatch releaseAppend = new CountDownLatch(1);
        doAnswer(invocation -> {
            appendEntered.countDown();
            boolean released = false;
            while (!released) {
                try {
                    released = releaseAppend.await(30, TimeUnit.SECONDS);
                } catch (InterruptedException ignored) {
                    // Model an append already inside non-interruptible durable I/O.
                }
            }
            return null;
        }).when(journal).append(anyString(), any(Instant.class));

        MessageHandler handler = new MessageHandler(new ByteArrayInputStream(
                input.getBytes(StandardCharsets.UTF_8)), sensor);
        org.junit.Assert.assertTrue(appendEntered.await(1, TimeUnit.SECONDS));
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> stop = executor.submit(() -> {
            try {
                handler.requestStop();
                handler.awaitStop(100L);
            } catch (java.io.IOException error) {
                throw new RuntimeException(error);
            }
        });
        try {
            Thread.sleep(200L);
            org.junit.Assert.assertFalse(
                    "stop must keep waiting after the warning threshold", stop.isDone());
            releaseAppend.countDown();
            stop.get(2, TimeUnit.SECONDS);
            journal.close();
        } finally {
            releaseAppend.countDown();
            executor.shutdownNow();
        }

        InOrder order = inOrder(journal);
        order.verify(journal).append(eq(input.trim()), any(Instant.class));
        order.verify(journal).close();
    }

    @Test
    public void completedLineDuringStopIsJournaledButNotPublished() throws Exception {
        String line = "TC,111111,111111,111111,111111";
        CloseGatedLineInputStream input = new CloseGatedLineInputStream(line + "\r\n");
        RapiscanSensor sensor = mock(RapiscanSensor.class);
        DailyFileJournalConnection journal = mock(DailyFileJournalConnection.class);
        DailyFileOutput dailyFileOutput = mock(DailyFileOutput.class);
        when(sensor.getLogger()).thenReturn(LoggerFactory.getLogger(MessageHandlerJournalTest.class));
        when(sensor.getUniqueIdentifier()).thenReturn("urn:test:rapiscan:stop-line");
        when(sensor.getConfiguration()).thenReturn(new RapiscanConfig());
        when(sensor.getDailyFileJournal()).thenReturn(journal);
        when(sensor.getDailyFileOutput()).thenReturn(dailyFileOutput);

        MessageHandler handler = new MessageHandler(input, sensor);
        org.junit.Assert.assertTrue(input.readEntered.await(1, TimeUnit.SECONDS));
        handler.requestStop();
        handler.awaitStop();

        verify(journal).append(eq(line), any(Instant.class));
        verify(dailyFileOutput, never()).onNewMessage(anyString(), any(Instant.class));
    }

    @Test
    public void heartbeatTracksReceiptBeforeSlowJournalAppendCompletes() throws Exception {
        String line = "TC,111111,111111,111111,111111";
        RapiscanSensor sensor = mock(RapiscanSensor.class);
        DailyFileJournalConnection journal = mock(DailyFileJournalConnection.class);
        DailyFileOutput dailyFileOutput = mock(DailyFileOutput.class);
        when(sensor.getLogger()).thenReturn(LoggerFactory.getLogger(MessageHandlerJournalTest.class));
        when(sensor.getUniqueIdentifier()).thenReturn("urn:test:rapiscan:receipt-heartbeat");
        when(sensor.getConfiguration()).thenReturn(new RapiscanConfig());
        when(sensor.getDailyFileJournal()).thenReturn(journal);
        when(sensor.getDailyFileOutput()).thenReturn(dailyFileOutput);
        when(journal.shouldPublishLegacyObservation()).thenReturn(true);
        CountDownLatch appendEntered = new CountDownLatch(1);
        CountDownLatch releaseAppend = new CountDownLatch(1);
        doAnswer(invocation -> {
            appendEntered.countDown();
            releaseAppend.await(5, TimeUnit.SECONDS);
            return null;
        }).when(journal).append(eq(line), any(Instant.class));

        PipedInputStream input = new PipedInputStream();
        PipedOutputStream rpm = new PipedOutputStream(input);
        MessageHandler handler = new MessageHandler(input, sensor);
        try {
            var lastMessage = MessageHandler.class.getDeclaredField("timeSinceLastMessage");
            lastMessage.setAccessible(true);
            lastMessage.setLong(handler, 0L);
            rpm.write((line + "\n").getBytes(StandardCharsets.UTF_8));
            rpm.flush();
            org.junit.Assert.assertTrue(appendEntered.await(1, TimeUnit.SECONDS));
            org.junit.Assert.assertTrue(
                    "receipt must update the heartbeat timestamp before journal append returns",
                    lastMessage.getLong(handler) > 0L);
            org.junit.Assert.assertTrue(
                    "heartbeat must observe the newly received line while its journal append drains",
                    handler.getTimeSinceLastMessage() < 1_000L);
        } finally {
            releaseAppend.countDown();
            handler.stop();
            rpm.close();
        }
    }

    @Test
    public void stopInterruptsAnActiveReaderWithoutCancellingItsCompletionSignal() throws Exception {
        String line = "TC,111111,111111,111111,111111";
        RapiscanSensor sensor = mock(RapiscanSensor.class);
        DailyFileJournalConnection journal = mock(DailyFileJournalConnection.class);
        when(sensor.getLogger()).thenReturn(LoggerFactory.getLogger(MessageHandlerJournalTest.class));
        when(sensor.getUniqueIdentifier()).thenReturn("urn:test:rapiscan:interrupt-stop");
        when(sensor.getConfiguration()).thenReturn(new RapiscanConfig());
        when(sensor.getDailyFileJournal()).thenReturn(journal);
        CountDownLatch appendEntered = new CountDownLatch(1);
        CountDownLatch readerInterrupted = new CountDownLatch(1);
        doAnswer(invocation -> {
            appendEntered.countDown();
            try {
                new CountDownLatch(1).await();
            } catch (InterruptedException expected) {
                readerInterrupted.countDown();
                Thread.currentThread().interrupt();
            }
            return null;
        }).when(journal).append(eq(line), any(Instant.class));

        MessageHandler handler = new MessageHandler(new ByteArrayInputStream(
                (line + "\r\n").getBytes(StandardCharsets.UTF_8)), sensor);
        org.junit.Assert.assertTrue(appendEntered.await(1, TimeUnit.SECONDS));
        handler.requestStop();
        handler.awaitStop(100L);

        org.junit.Assert.assertTrue(
                "stop must interrupt a reader that remains active past the graceful interval",
                readerInterrupted.await(1, TimeUnit.SECONDS));
    }

    private static final class CloseGatedLineInputStream extends InputStream {
        private final byte[] contents;
        private final CountDownLatch readEntered = new CountDownLatch(1);
        private final CountDownLatch closeCalled = new CountDownLatch(1);
        private int offset;

        private CloseGatedLineInputStream(String contents) {
            this.contents = contents.getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public int read(byte[] target, int targetOffset, int length) throws IOException {
            readEntered.countDown();
            try {
                closeCalled.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while awaiting test close", e);
            }
            if (offset == contents.length)
                return -1;
            int count = Math.min(length, contents.length - offset);
            System.arraycopy(contents, offset, target, targetOffset, count);
            offset += count;
            return count;
        }

        @Override
        public int read() throws IOException {
            byte[] one = new byte[1];
            int count = read(one, 0, 1);
            return count < 0 ? -1 : one[0] & 0xff;
        }

        @Override
        public void close() {
            closeCalled.countDown();
        }
    }
}
