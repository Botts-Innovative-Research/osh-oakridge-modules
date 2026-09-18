package com.botts.impl.sensor.rapiscan.control;

import com.botts.impl.sensor.rapiscan.RapiscanSensor;
import com.botts.impl.sensor.rapiscan.output.TamperOutput;
import net.opengis.swe.v20.Boolean;
import org.junit.Test;
import org.sensorhub.api.command.CommandData;
import org.sensorhub.api.command.ICommandStatus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ManualTamperControlTest {

    @Test
    public void publishesCommandedTamperState() throws Exception {
        var sensor = new TestSensor();
        var control = new ManualTamperControl(sensor);

        assertTrue(control.getCommandDescription() instanceof Boolean);

        var command = control.getCommandDescription().createDataBlock();
        command.setBooleanValue(true);
        var status = control.submitCommand(new CommandData(1, command)).get();
        assertEquals(ICommandStatus.CommandStatusCode.COMPLETED, status.getStatusCode());
        assertTrue(sensor.tamperOutput.lastTamperState);

        command.setBooleanValue(false);
        status = control.submitCommand(new CommandData(2, command)).get();
        assertEquals(ICommandStatus.CommandStatusCode.COMPLETED, status.getStatusCode());
        assertFalse(sensor.tamperOutput.lastTamperState);
    }

    private static class TestSensor extends RapiscanSensor {
        private final RecordingTamperOutput tamperOutput = new RecordingTamperOutput(this);

        @Override
        public TamperOutput getTamperOutput() {
            return tamperOutput;
        }
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
