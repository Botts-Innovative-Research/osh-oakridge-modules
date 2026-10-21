package org.sensorhub.impl.utils.rad.dailyfile;

import org.junit.Test;
import org.sensorhub.api.ISensorHub;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.ModuleEvent;
import org.sensorhub.impl.module.ModuleRegistry;
import org.slf4j.Logger;

import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

public class DailyFileJournalConnectionTest {

    @Test
    public void registryShutdownEndsRequiredStartupWaitImmediately() throws Exception {
        Fixture fixture = fixture(ModuleEvent.ModuleState.STOPPING);
        when(fixture.registry.isShuttingDown()).thenReturn(true);
        DailyFileJournalClientConfig policy = requiredPolicy(300);

        SensorHubException error = assertThrows(SensorHubException.class, () ->
                DailyFileJournalConnection.connect(fixture.hub, "urn:test:lane:shutdown",
                        policy, fixture.log));

        assertTrue(error.getMessage().contains("Required daily-file journal is not available"));
        assertTrue(error.getCause().getMessage().contains("module-registry shutdown"));
        verify(fixture.service, never()).openWriter(any());
    }

    @Test
    public void ordinaryStoppedJournalIsNotTreatedAsReadyOrTerminal() {
        Fixture fixture = fixture(ModuleEvent.ModuleState.STOPPED);
        assertFalse(DailyFileJournalConnection.isStartedJournal(
                (IModule<?>) fixture.service));
        when(fixture.registry.isShuttingDown()).thenReturn(false);
        assertFalse(fixture.registry.isShuttingDown());
    }

    @Test
    public void optionalJournalStoppingAfterWaitResolutionFallsBackCleanly() throws Exception {
        Fixture fixture = fixture(ModuleEvent.ModuleState.STOPPING);
        DailyFileJournalClientConfig policy = requiredPolicy(300);
        policy.required = false;

        DailyFileJournalConnection connection = DailyFileJournalConnection.connect(
                fixture.hub, "urn:test:lane:optional-restart", policy, fixture.log);

        assertFalse(connection.isAttached());
        assertFalse(connection.isRequired());
        verify(fixture.service, never()).openWriter(any());
    }

    @Test
    public void startedJournalStillAttachesWriter() throws Exception {
        Fixture fixture = fixture(ModuleEvent.ModuleState.STARTED);
        DailyFileWriterHandle writer = mock(DailyFileWriterHandle.class);
        when(fixture.service.openWriter("urn:test:lane:ready")).thenReturn(writer);
        when(fixture.service.getObservationMode()).thenReturn(DailyFileObservationMode.NONE);

        DailyFileJournalConnection connection = DailyFileJournalConnection.connect(
                fixture.hub, "urn:test:lane:ready", requiredPolicy(300), fixture.log);

        assertTrue(connection.isAttached());
        assertTrue(connection.isRequired());
        assertFalse(connection.shouldPublishLegacyObservation());
        connection.close();
        verify(writer).close();
    }

    @Test
    public void onlyStartedJournalIsUsable() {
        assertTrue(DailyFileJournalConnection.isStartedJournal(
                moduleInState(ModuleEvent.ModuleState.STARTED)));
        assertFalse(DailyFileJournalConnection.isStartedJournal(
                moduleInState(ModuleEvent.ModuleState.STOPPING)));
        assertFalse(DailyFileJournalConnection.isStartedJournal(
                moduleInState(ModuleEvent.ModuleState.STOPPED)));
        assertFalse(DailyFileJournalConnection.isStartedJournal(
                moduleInState(ModuleEvent.ModuleState.STARTING)));
    }

    private static DailyFileJournalClientConfig requiredPolicy(int timeoutSeconds) {
        DailyFileJournalClientConfig policy = new DailyFileJournalClientConfig();
        policy.required = true;
        policy.startupTimeoutSeconds = timeoutSeconds;
        return policy;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Fixture fixture(ModuleEvent.ModuleState state) {
        ISensorHub hub = mock(ISensorHub.class);
        ModuleRegistry registry = mock(ModuleRegistry.class);
        Logger log = mock(Logger.class);
        IDailyFileJournalService service = mock(IDailyFileJournalService.class,
                withSettings().extraInterfaces(IModule.class));
        IModule<?> module = (IModule<?>) service;
        when(hub.getModuleRegistry()).thenReturn(registry);
        when(registry.getModuleByType(IDailyFileJournalService.class)).thenReturn(service);
        when(module.isStarted()).thenReturn(false);
        when(module.getCurrentState()).thenReturn(state);
        when(registry.waitForModule(any())).thenReturn(
                CompletableFuture.completedFuture((IModule) module));
        return new Fixture(hub, registry, log, service);
    }

    @SuppressWarnings("unchecked")
    private static IModule<?> moduleInState(ModuleEvent.ModuleState state) {
        IDailyFileJournalService service = mock(IDailyFileJournalService.class,
                withSettings().extraInterfaces(IModule.class));
        IModule<?> module = (IModule<?>) service;
        when(module.getCurrentState()).thenReturn(state);
        return module;
    }

    private record Fixture(ISensorHub hub, ModuleRegistry registry, Logger log,
                           IDailyFileJournalService service) {
    }
}
