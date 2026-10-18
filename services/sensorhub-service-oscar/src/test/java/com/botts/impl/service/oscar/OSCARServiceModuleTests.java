package com.botts.impl.service.oscar;

import org.junit.Test;
import org.sensorhub.api.common.SensorHubException;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class OSCARServiceModuleTests {

    @Test
    public void purgerFailureCannotSkipIndependentServiceCleanup() {
        List<String> cleanup = new ArrayList<>();
        OSCARServiceModule module = new OSCARServiceModule() {
            @Override void stopBaseModule() { cleanup.add("base"); }
            @Override void stopStatisticsOutput(long deadline) { cleanup.add("statistics"); }

            @Override
            void stopDatabasePurger(long deadline) throws SensorHubException {
                cleanup.add("purger");
                throw new SensorHubException("fixture purger failure");
            }

            @Override void stopVideoRetention(long deadline) { cleanup.add("video"); }
            @Override void stopStoragePressureRetention(long deadline) { cleanup.add("pressure"); }
            @Override void unregisterWebIdHandler() { cleanup.add("web-id"); }
            @Override void unregisterMediaHandler() { cleanup.add("media"); }
        };

        SensorHubException failure = assertThrows(SensorHubException.class, module::doStop);

        assertEquals(List.of("base", "statistics", "purger", "video", "pressure",
                "web-id", "media"), cleanup);
        assertTrue(failure.getMessage().contains("database purger"));
    }

    @Test
    public void cleanupFailuresAreAggregatedAfterEveryStepRuns() {
        List<String> cleanup = new ArrayList<>();
        OSCARServiceModule module = new OSCARServiceModule() {
            @Override void stopBaseModule() { cleanup.add("base"); }
            @Override void stopStatisticsOutput(long deadline) { cleanup.add("statistics"); }

            @Override
            void stopDatabasePurger(long deadline) throws SensorHubException {
                cleanup.add("purger");
                throw new SensorHubException("first");
            }

            @Override
            void stopVideoRetention(long deadline) {
                cleanup.add("video");
                throw new AssertionError("second");
            }

            @Override void stopStoragePressureRetention(long deadline) { cleanup.add("pressure"); }
            @Override void unregisterWebIdHandler() { cleanup.add("web-id"); }
            @Override void unregisterMediaHandler() { cleanup.add("media"); }
        };

        SensorHubException failure = assertThrows(SensorHubException.class, module::doStop);

        assertEquals(1, failure.getSuppressed().length);
        assertTrue(failure.getSuppressed()[0].getMessage().contains("video retention"));
        assertEquals(7, cleanup.size());
    }
}
