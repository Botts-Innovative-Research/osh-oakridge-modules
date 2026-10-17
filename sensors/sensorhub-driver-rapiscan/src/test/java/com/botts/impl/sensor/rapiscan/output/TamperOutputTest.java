package com.botts.impl.sensor.rapiscan.output;

import com.botts.impl.sensor.rapiscan.RapiscanSensor;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TamperOutputTest {

    @Test
    public void newConnectionPublishesClearBaseline() {
        var output = new RecordingTamperOutput(new RapiscanSensor());

        output.onNewMessage(true);
        assertTrue(output.lastTamperState);

        output.onConnected();
        assertFalse(output.lastTamperState);
    }

    private static class RecordingTamperOutput extends TamperOutput {
        private boolean lastTamperState;

        private RecordingTamperOutput(RapiscanSensor parentSensor) {
            super(parentSensor);
        }

        @Override
        public void onNewMessage(boolean tamperState) {
            lastTamperState = tamperState;
        }
    }
}
