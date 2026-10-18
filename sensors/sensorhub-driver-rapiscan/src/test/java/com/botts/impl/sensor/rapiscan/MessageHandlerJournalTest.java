package com.botts.impl.sensor.rapiscan;

import com.botts.impl.sensor.rapiscan.output.DailyFileOutput;
import com.botts.impl.sensor.rapiscan.output.TamperOutput;
import org.junit.Test;
import org.mockito.InOrder;
import org.sensorhub.impl.utils.rad.dailyfile.DailyFileJournalConnection;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
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
}
