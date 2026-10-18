package com.botts.impl.sensor.aspect;

import com.botts.impl.sensor.aspect.output.DailyFileOutput;
import com.botts.impl.sensor.aspect.registers.MonitorRegisters;
import org.junit.Test;
import org.mockito.InOrder;
import org.sensorhub.impl.utils.rad.dailyfile.DailyFileJournalConnection;

import java.io.IOException;
import java.time.Instant;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Verifies the durability boundary for generated Aspect compatibility lines. */
public class MessageHandlerJournalTest {

    private static final Instant RECEIVED_AT = Instant.parse("2026-10-17T12:34:56Z");
    private static final String GENERATED_LINE =
            "1,2,3,4,5.0,6.0,7,8,9.0,10.0,11,12,13,14";

    @Test
    public void journalsGeneratedLineBeforeLegacyPublication() throws Exception {
        MonitorRegisters registers = registers();
        DailyFileJournalConnection journal = mock(DailyFileJournalConnection.class);
        DailyFileOutput legacyOutput = mock(DailyFileOutput.class);
        when(journal.shouldPublishLegacyObservation()).thenReturn(true);

        MessageHandler.publishDailyFile(registers, RECEIVED_AT, journal, legacyOutput);

        InOrder order = inOrder(journal, legacyOutput);
        order.verify(journal).append(GENERATED_LINE, RECEIVED_AT);
        order.verify(journal).shouldPublishLegacyObservation();
        order.verify(legacyOutput).onNewMessage(GENERATED_LINE, RECEIVED_AT);
    }

    @Test
    public void requiredJournalFailurePreventsLegacyPublication() throws Exception {
        DailyFileJournalConnection journal = mock(DailyFileJournalConnection.class);
        DailyFileOutput legacyOutput = mock(DailyFileOutput.class);
        IOException failure = new IOException("durable append failed");
        org.mockito.Mockito.doThrow(failure).when(journal).append(anyString(),
                org.mockito.ArgumentMatchers.any(Instant.class));
        when(journal.isRequired()).thenReturn(true);

        IOException thrown = assertThrows(IOException.class,
                () -> MessageHandler.publishDailyFile(registers(), RECEIVED_AT, journal,
                        legacyOutput));

        org.junit.Assert.assertSame(failure, thrown);
        verify(legacyOutput, never()).onNewMessage(anyString(),
                org.mockito.ArgumentMatchers.any(Instant.class));
    }

    @Test
    public void optionalJournalFailureContinuesLegacyPublication() throws Exception {
        DailyFileJournalConnection journal = mock(DailyFileJournalConnection.class);
        DailyFileOutput legacyOutput = mock(DailyFileOutput.class);
        org.mockito.Mockito.doThrow(new IOException("optional append failed"))
                .when(journal).append(anyString(), org.mockito.ArgumentMatchers.any(Instant.class));
        when(journal.isRequired()).thenReturn(false);
        when(journal.shouldPublishLegacyObservation()).thenReturn(true);

        MessageHandler.publishDailyFile(registers(), RECEIVED_AT, journal, legacyOutput);

        verify(legacyOutput).onNewMessage(GENERATED_LINE, RECEIVED_AT);
    }

    @Test
    public void dailyObservationAndOccupancyUseSameMillisecondCorrelationClock() throws Exception {
        DailyFileJournalConnection journal = mock(DailyFileJournalConnection.class);
        DailyFileOutput legacyOutput = mock(DailyFileOutput.class);
        when(journal.shouldPublishLegacyObservation()).thenReturn(true);
        Instant captured = Instant.parse("2026-10-17T12:34:56.123456789Z");

        MessageHandler.SampleClock clock = MessageHandler.sampleClock(captured);
        MessageHandler.publishDailyFile(registers(), clock.observationTime(), journal,
                legacyOutput);

        verify(legacyOutput).onNewMessage(GENERATED_LINE, clock.observationTime());
        assertEquals(clock.observationTime().toEpochMilli(),
                Math.round(clock.epochSeconds() * 1_000.0));
        assertEquals(captured.toEpochMilli(), clock.observationTime().toEpochMilli());

        // The same captured clock that timestamps the daily sample is also consumed by the
        // Aspect occupancy state machine when its object counter changes. This is the exact key
        // later used to associate the completed Occupancy with its camera generation.
        MessageHandler handler = new MessageHandler(mock(AspectSensor.class));
        MonitorRegisters baseline = mock(MonitorRegisters.class);
        when(baseline.getObjectCounter()).thenReturn(10);
        MonitorRegisters occupied = mock(MonitorRegisters.class);
        when(occupied.getObjectCounter()).thenReturn(11);
        when(occupied.isOccupied()).thenReturn(true);

        org.junit.Assert.assertFalse(handler.checkOccupancyRecord(baseline, 1.0));
        org.junit.Assert.assertFalse(handler.checkOccupancyRecord(
                occupied, clock.epochSeconds()));
        assertEquals(clock.observationTime().toEpochMilli(),
                Math.round(handler.startTime * 1_000.0));
        org.junit.Assert.assertTrue("An occupancy start between cadence ticks must publish now",
                MessageHandler.shouldPublishDailyFile(true, 1L, true));
        org.junit.Assert.assertFalse("Ordinary scan samples remain cadence-limited",
                MessageHandler.shouldPublishDailyFile(true, 1L, false));
    }

    private static MonitorRegisters registers() {
        MonitorRegisters registers = mock(MonitorRegisters.class);
        when(registers.getTimeElapsed()).thenReturn(1);
        when(registers.getInputSignals()).thenReturn(2);
        when(registers.getGammaChannelStatus()).thenReturn(3);
        when(registers.getGammaChannelCount()).thenReturn(4);
        when(registers.getGammaChannelBackground()).thenReturn(5.0f);
        when(registers.getGammaChannelVariance()).thenReturn(6.0);
        when(registers.getNeutronChannelStatus()).thenReturn(7);
        when(registers.getNeutronChannelCount()).thenReturn(8);
        when(registers.getNeutronChannelBackground()).thenReturn(9.0f);
        when(registers.getNeutronChannelVariance()).thenReturn(10.0f);
        when(registers.getObjectCounter()).thenReturn(11);
        when(registers.getObjectMark()).thenReturn(12);
        when(registers.getObjectSpeed()).thenReturn(13);
        when(registers.getOutputSignals()).thenReturn(14);
        return registers;
    }
}
