package com.botts.impl.sensor.rapiscan;

import com.botts.impl.sensor.rapiscan.output.TamperOutput;
import org.junit.Test;
import org.sensorhub.impl.module.RobustConnection;
import org.sensorhub.impl.module.RobustConnectionConfig;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class RapiscanSensorConnectionLifecycleTest {

    @Test
    public void publishesClearBeforeStartingNewConnectionReader() throws Exception {
        var sensor = new RecordingSensor();
        sensor.connection = new RobustConnection(sensor, new RobustConnectionConfig(), "test RPM") {
            @Override
            public boolean tryConnect() {
                return true;
            }

            @Override
            public void waitForConnection() {
                sensor.events.add("connected");
            }
        };

        sensor.doStart();
        sensor.doStart();

        assertEquals(List.of(
                "connected", "tamper:false", "reader-started",
                "connected", "tamper:false", "reader-started"
        ), sensor.events);
    }

    private static class RecordingSensor extends RapiscanSensor {
        private final List<String> events = new ArrayList<>();
        private final RecordingTamperOutput tamperOutput = new RecordingTamperOutput(this, events);

        @Override
        public TamperOutput getTamperOutput() {
            return tamperOutput;
        }

        @Override
        public void initMsgHandler() {
            events.add("reader-started");
        }
    }

    private static class RecordingTamperOutput extends TamperOutput {
        private final List<String> events;

        private RecordingTamperOutput(RapiscanSensor parentSensor, List<String> events) {
            super(parentSensor);
            this.events = events;
        }

        @Override
        public void onNewMessage(boolean tamperState) {
            events.add("tamper:" + tamperState);
        }
    }
}
