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
    public void readinessIsWithdrawnBeforeBaseShutdown() throws SensorHubException {
        List<String> lifecycle = new ArrayList<>();
        OSCARServiceModule module = new OSCARServiceModule() {
            @Override void stopApplicationReadiness() { lifecycle.add("readiness"); }
            @Override void stopBaseModule() { lifecycle.add("base"); }
        };

        module.doStop();

        assertEquals(List.of("readiness", "base"), lifecycle);
    }

    @Test
    public void everyWorkerIsSignalledBeforeTheFirstAwait() throws SensorHubException {
        List<String> lifecycle = new ArrayList<>();
        OSCARServiceModule module = new OSCARServiceModule() {
            @Override void stopBaseModule() { lifecycle.add("base"); }
            @Override void requestStatisticsOutputStop() { lifecycle.add("request-statistics"); }
            @Override void requestDatabasePurgerStop() { lifecycle.add("request-purger"); }
            @Override void requestVideoRetentionStop() { lifecycle.add("request-video"); }
            @Override void requestStoragePressureRetentionStop() { lifecycle.add("request-pressure"); }

            @Override
            void awaitStatisticsOutputStop(long deadline) {
                assertEquals(List.of("base", "request-statistics", "request-purger",
                        "request-video", "request-pressure"), lifecycle);
                lifecycle.add("await-statistics");
            }

            @Override void awaitDatabasePurgerStop(long deadline) { lifecycle.add("await-purger"); }
            @Override void awaitVideoRetentionStop(long deadline) { lifecycle.add("await-video"); }
            @Override void awaitStoragePressureRetentionStop(long deadline) { lifecycle.add("await-pressure"); }
            @Override void unregisterWebIdHandler() { lifecycle.add("web-id"); }
            @Override void unregisterMediaHandler() { lifecycle.add("media"); }
        };

        module.doStop();

        assertEquals(List.of("base", "request-statistics", "request-purger",
                "request-video", "request-pressure", "await-statistics", "await-purger",
                "await-video", "await-pressure", "web-id", "media"), lifecycle);
    }

    @Test
    public void firstAwaitFailureCannotSkipIndependentServiceCleanup() {
        List<String> cleanup = new ArrayList<>();
        OSCARServiceModule module = new OSCARServiceModule() {
            @Override void stopBaseModule() { cleanup.add("base"); }
            @Override void requestStatisticsOutputStop() { cleanup.add("request-statistics"); }
            @Override void requestDatabasePurgerStop() { cleanup.add("request-purger"); }
            @Override void requestVideoRetentionStop() { cleanup.add("request-video"); }
            @Override void requestStoragePressureRetentionStop() { cleanup.add("request-pressure"); }

            @Override
            void awaitStatisticsOutputStop(long deadline) {
                cleanup.add("await-statistics");
                throw new IllegalStateException("fixture statistics timeout");
            }

            @Override void awaitDatabasePurgerStop(long deadline) { cleanup.add("await-purger"); }
            @Override void awaitVideoRetentionStop(long deadline) { cleanup.add("await-video"); }
            @Override void awaitStoragePressureRetentionStop(long deadline) { cleanup.add("await-pressure"); }
            @Override void unregisterWebIdHandler() { cleanup.add("web-id"); }
            @Override void unregisterMediaHandler() { cleanup.add("media"); }
        };

        SensorHubException failure = assertThrows(SensorHubException.class, module::doStop);

        assertEquals(List.of("base", "request-statistics", "request-purger", "request-video",
                "request-pressure", "await-statistics", "await-purger", "await-video",
                "await-pressure", "web-id", "media"), cleanup);
        assertTrue(failure.getMessage().contains("statistics output"));
    }

    @Test
    public void cleanupFailuresAreAggregatedAfterEveryStepRuns() {
        List<String> cleanup = new ArrayList<>();
        OSCARServiceModule module = new OSCARServiceModule() {
            @Override void stopBaseModule() { cleanup.add("base"); }
            @Override void requestStatisticsOutputStop() { cleanup.add("request-statistics"); }
            @Override void requestDatabasePurgerStop() { cleanup.add("request-purger"); }
            @Override void requestVideoRetentionStop() { cleanup.add("request-video"); }
            @Override void requestStoragePressureRetentionStop() { cleanup.add("request-pressure"); }
            @Override void awaitStatisticsOutputStop(long deadline) { cleanup.add("await-statistics"); }

            @Override
            void awaitDatabasePurgerStop(long deadline) throws SensorHubException {
                cleanup.add("await-purger");
                throw new SensorHubException("first");
            }

            @Override
            void awaitVideoRetentionStop(long deadline) {
                cleanup.add("await-video");
                throw new AssertionError("second");
            }

            @Override void awaitStoragePressureRetentionStop(long deadline) { cleanup.add("await-pressure"); }
            @Override void unregisterWebIdHandler() { cleanup.add("web-id"); }
            @Override void unregisterMediaHandler() { cleanup.add("media"); }
        };

        SensorHubException failure = assertThrows(SensorHubException.class, module::doStop);

        assertEquals(1, failure.getSuppressed().length);
        assertTrue(failure.getSuppressed()[0].getMessage().contains("video retention"));
        assertEquals(11, cleanup.size());
    }

    @Test
    public void everyAwaitReceivesOneAbsoluteModuleDeadline() throws SensorHubException {
        List<Long> deadlines = new ArrayList<>();
        OSCARServiceModule module = new OSCARServiceModule() {
            @Override void stopBaseModule() { }
            @Override void requestStatisticsOutputStop() { }
            @Override void requestDatabasePurgerStop() { }
            @Override void requestVideoRetentionStop() { }
            @Override void requestStoragePressureRetentionStop() { }
            @Override void awaitStatisticsOutputStop(long deadline) { deadlines.add(deadline); }
            @Override void awaitDatabasePurgerStop(long deadline) { deadlines.add(deadline); }
            @Override void awaitVideoRetentionStop(long deadline) { deadlines.add(deadline); }
            @Override void awaitStoragePressureRetentionStop(long deadline) { deadlines.add(deadline); }
            @Override void unregisterWebIdHandler() { }
            @Override void unregisterMediaHandler() { }
        };

        module.doStop();

        assertEquals(4, deadlines.size());
        assertTrue(deadlines.stream().allMatch(deadline -> deadline.equals(deadlines.get(0))));
        assertTrue("OSCAR must retain headroom inside the core ten-second stop timeout",
                OSCARServiceModule.STOP_BUDGET_NANOS <
                        java.util.concurrent.TimeUnit.SECONDS.toNanos(10));
    }
}
