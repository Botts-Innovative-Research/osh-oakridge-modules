package com.botts.impl.sensor.rs350;

import org.junit.Test;
import org.mockito.InOrder;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.utils.rad.dailyfile.DailyFileJournalConnection;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Verifies that exact RS350 source payloads are durable before normalization/parsing. */
public class MessageHandlerJournalTest {

    private static final Instant RECEIVED_AT = Instant.parse("2026-10-17T12:34:56Z");
    private static final String RAW = "<?xml version=\"1.0\"?><RadInstrumentData/>";

    @Test
    @SuppressWarnings("unchecked")
    public void journalsExactRawPayloadBeforeNormalizerRuns() throws Exception {
        DailyFileJournalConnection journal = mock(DailyFileJournalConnection.class);
        Function<String, String> normalizer = mock(Function.class);
        when(normalizer.apply(RAW)).thenReturn("<RadInstrumentData/>");

        String normalized = MessageHandler.journalBeforeParsing(RAW, RECEIVED_AT, journal,
                normalizer);

        assertEquals("<RadInstrumentData/>", normalized);
        InOrder order = inOrder(journal, normalizer);
        order.verify(journal).append(RAW, RECEIVED_AT);
        order.verify(normalizer).apply(RAW);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void requiredJournalFailurePreventsNormalizationAndParsing() throws Exception {
        DailyFileJournalConnection journal = mock(DailyFileJournalConnection.class);
        Function<String, String> normalizer = mock(Function.class);
        IOException failure = new IOException("durable append failed");
        doThrow(failure).when(journal).append(RAW, RECEIVED_AT);
        when(journal.isRequired()).thenReturn(true);

        IOException thrown = assertThrows(IOException.class,
                () -> MessageHandler.journalBeforeParsing(RAW, RECEIVED_AT, journal,
                        normalizer));

        assertSame(failure, thrown);
        verify(normalizer, never()).apply(RAW);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void optionalJournalFailureStillAllowsNormalizationAndParsing() throws Exception {
        DailyFileJournalConnection journal = mock(DailyFileJournalConnection.class);
        Function<String, String> normalizer = mock(Function.class);
        IOException failure = new IOException("optional append failed");
        doThrow(failure).when(journal).append(RAW, RECEIVED_AT);
        when(journal.isRequired()).thenReturn(false);
        when(normalizer.apply(RAW)).thenReturn("normalized");

        String normalized = MessageHandler.journalBeforeParsing(RAW, RECEIVED_AT, journal,
                normalizer);

        assertEquals("normalized", normalized);
        verify(normalizer).apply(RAW);
    }

    @Test
    public void workerJoinCompletesFinalAppendBeforeWriterClose() throws Exception {
        DailyFileJournalConnection journal = mock(DailyFileJournalConnection.class);
        CountDownLatch workerStarted = new CountDownLatch(1);
        Thread worker = new Thread(() -> {
            workerStarted.countDown();
            try {
                Thread.sleep(30_000L);
            } catch (InterruptedException expected) {
                try {
                    journal.append(RAW, RECEIVED_AT);
                } catch (IOException error) {
                    throw new RuntimeException(error);
                }
            }
        }, "rs350-final-append-test");
        worker.start();
        org.junit.Assert.assertTrue(workerStarted.await(1, TimeUnit.SECONDS));

        worker.interrupt();
        MessageHandler.awaitStopped(worker,
                System.nanoTime() + TimeUnit.SECONDS.toNanos(2), "test worker");
        journal.close();

        InOrder order = inOrder(journal);
        order.verify(journal).append(RAW, RECEIVED_AT);
        order.verify(journal).close();
    }

    @Test
    public void sensorReleasesWriterBeforeReportingAncillaryStopFailure() throws Exception {
        MessageHandler handler = mock(MessageHandler.class);
        DailyFileJournalConnection journal = mock(DailyFileJournalConnection.class);
        IOException ancillaryFailure = new IOException("deferred WebID worker stuck");
        doThrow(ancillaryFailure).when(handler).awaitAncillaryStopped();
        RS350Sensor sensor = new RS350Sensor();
        sensor.messageHandler = handler;
        sensor.dailyFileJournal = journal;

        SensorHubException thrown = assertThrows(SensorHubException.class, sensor::doStop);

        assertSame(ancillaryFailure, thrown.getCause());
        InOrder order = inOrder(handler, journal);
        order.verify(handler).requestStop();
        order.verify(handler).awaitSourceStopped();
        order.verify(journal).close();
        order.verify(handler).awaitAncillaryStopped();
        org.junit.Assert.assertNull(sensor.dailyFileJournal);
    }
}
