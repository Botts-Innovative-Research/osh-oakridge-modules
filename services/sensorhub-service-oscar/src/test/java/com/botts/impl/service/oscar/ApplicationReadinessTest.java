package com.botts.impl.service.oscar;

import com.botts.impl.system.lane.IAutoStartDispatchStatus;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.api.module.ModuleEvent.ModuleState;
import org.sensorhub.api.system.ISystemDriver;
import org.sensorhub.api.system.ISystemGroupDriver;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ApplicationReadinessTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void nestedInFlightMemberMustSettle() {
        AtomicReference<ModuleState> childState = new AtomicReference<>(ModuleState.STARTING);
        IModule<?> child = module(true, childState);
        IModule<?> lane = group(true, new AtomicReference<>(ModuleState.STARTED),
                Map.of("rpm", (ISystemDriver) child));

        assertFalse(ApplicationReadiness.allAutoStartModuleTreesSettled(List.of(lane)));

        childState.set(ModuleState.STARTED);
        assertTrue(ApplicationReadiness.allAutoStartModuleTreesSettled(List.of(lane)));
    }

    @Test
    public void everyNestedInFlightStateBlocksReadiness() {
        AtomicReference<ModuleState> childState = new AtomicReference<>();
        IModule<?> child = module(true, childState);
        IModule<?> lane = group(true, new AtomicReference<>(ModuleState.STARTED),
                Map.of("rpm", (ISystemDriver) child));

        for (ModuleState state : List.of(ModuleState.LOADED, ModuleState.INITIALIZING,
                ModuleState.INITIALIZED, ModuleState.STARTING, ModuleState.STOPPING)) {
            childState.set(state);
            assertFalse("nested " + state + " must not release application readiness",
                    ApplicationReadiness.allAutoStartModuleTreesSettled(List.of(lane)));
        }
    }

    @Test
    public void offlineNestedMemberCanSettleStopped() {
        IModule<?> offlineRpm = module(true, new AtomicReference<>(ModuleState.STOPPED));
        IModule<?> lane = group(true, new AtomicReference<>(ModuleState.STARTED),
                Map.of("rpm", (ISystemDriver) offlineRpm));

        assertTrue(ApplicationReadiness.allAutoStartModuleTreesSettled(List.of(lane)));
    }

    @Test
    public void loadedMemberCannotSettleWithoutExplicitParentDispatchBoundary() {
        IModule<?> offlineRpm = module(true, new AtomicReference<>(ModuleState.LOADED),
                new IllegalStateException("fixture connection refused"));
        IModule<?> lane = group(true, new AtomicReference<>(ModuleState.STARTED),
                Map.of("rpm", (ISystemDriver) offlineRpm));

        assertFalse(ApplicationReadiness.allAutoStartModuleTreesSettled(List.of(lane)));
    }

    @Test
    public void rs350DependentLoadedMemberSettlesOnlyAfterLaneDispatchCompletes() {
        IModule<?> offlineRpm = module(true, new AtomicReference<>(ModuleState.LOADED),
                new IllegalStateException("fixture connection refused"));
        IModule<?> dependentProcess = module(true, new AtomicReference<>(ModuleState.LOADED));
        AtomicBoolean dispatchComplete = new AtomicBoolean(false);
        IModule<?> lane = dispatchAwareGroup(true, new AtomicReference<>(ModuleState.STARTED),
                dispatchComplete, Map.of(
                        "rpm", (ISystemDriver) offlineRpm,
                        "occupancy", (ISystemDriver) dependentProcess));

        assertFalse(ApplicationReadiness.allAutoStartModuleTreesSettled(List.of(lane)));

        dispatchComplete.set(true);
        assertTrue(ApplicationReadiness.allAutoStartModuleTreesSettled(List.of(lane)));
    }

    @Test
    public void stoppedTopLevelModuleStillBlocksReadiness() {
        IModule<?> service = module(true, new AtomicReference<>(ModuleState.STOPPED));

        assertFalse(ApplicationReadiness.allAutoStartModuleTreesSettled(List.of(service)));
    }

    @Test
    public void nestedAliasCannotHideStoppedTopLevelModule() {
        IModule<?> shared = module(true, new AtomicReference<>(ModuleState.STOPPED));
        IModule<?> group = group(true, new AtomicReference<>(ModuleState.STARTED),
                Map.of("shared", (ISystemDriver) shared));

        assertFalse(ApplicationReadiness.allAutoStartModuleTreesSettled(List.of(group, shared)));
    }

    @Test
    public void disabledNestedMemberDoesNotBlockReadiness() {
        IModule<?> disabledCamera = module(false, new AtomicReference<>(ModuleState.INITIALIZED));
        IModule<?> lane = group(true, new AtomicReference<>(ModuleState.STARTED),
                Map.of("disabled-camera", (ISystemDriver) disabledCamera));

        assertTrue(ApplicationReadiness.allAutoStartModuleTreesSettled(List.of(lane)));
    }

    @Test
    public void identityCycleCannotRecurseForever() {
        AtomicReference<IModule<?>> groupReference = new AtomicReference<>();
        IModule<?> group = group(true, new AtomicReference<>(ModuleState.STARTED),
                () -> Map.of("self", (ISystemDriver) groupReference.get()));
        groupReference.set(group);

        assertTrue(ApplicationReadiness.allAutoStartModuleTreesSettled(List.of(group)));
    }

    @Test
    public void markerTracksReadinessAndIsWithdrawnOnClose() throws Exception {
        Path marker = temporaryFolder.getRoot().toPath().resolve("oscar-ready");
        Files.writeString(marker, "stale\n");
        AtomicReference<ModuleState> state = new AtomicReference<>(ModuleState.STARTING);
        IModule<?> module = module(true, state);
        ApplicationReadiness readiness = new ApplicationReadiness(
                () -> List.of(module), marker, 10L,
                LoggerFactory.getLogger(ApplicationReadinessTest.class));

        try {
            readiness.start();
            await(() -> !Files.exists(marker));

            state.set(ModuleState.STARTED);
            await(() -> Files.isRegularFile(marker));

            state.set(ModuleState.STOPPING);
            await(() -> !Files.exists(marker));
        } finally {
            readiness.close();
        }

        assertFalse(Files.exists(marker));
    }

    private static IModule<?> module(boolean autoStart, AtomicReference<ModuleState> state) {
        return module(autoStart, state, null);
    }

    private static IModule<?> module(boolean autoStart,
                                     AtomicReference<ModuleState> state,
                                     Throwable error) {
        return proxy(autoStart, state, new AtomicReference<>(error), null);
    }

    private static IModule<?> group(boolean autoStart,
                                    AtomicReference<ModuleState> state,
                                    Map<String, ? extends ISystemDriver> members) {
        return group(autoStart, state, () -> members);
    }

    private static IModule<?> group(boolean autoStart,
                                    AtomicReference<ModuleState> state,
                                    java.util.function.Supplier<Map<String, ? extends ISystemDriver>> members) {
        return proxy(autoStart, state, new AtomicReference<>(), members);
    }

    private static IModule<?> dispatchAwareGroup(boolean autoStart,
                                                 AtomicReference<ModuleState> state,
                                                 AtomicBoolean dispatchComplete,
                                                 Map<String, ? extends ISystemDriver> members) {
        return proxy(autoStart, state, new AtomicReference<>(), () -> members,
                dispatchComplete);
    }

    private static IModule<?> proxy(boolean autoStart,
                                    AtomicReference<ModuleState> state,
                                    AtomicReference<Throwable> error,
                                    java.util.function.Supplier<Map<String, ? extends ISystemDriver>> members) {
        return proxy(autoStart, state, error, members, null);
    }

    private static IModule<?> proxy(boolean autoStart,
                                    AtomicReference<ModuleState> state,
                                    AtomicReference<Throwable> error,
                                    java.util.function.Supplier<Map<String, ? extends ISystemDriver>> members,
                                    AtomicBoolean dispatchComplete) {
        ModuleConfig config = new ModuleConfig();
        config.autoStart = autoStart;
        Class<?>[] interfaces = members == null
                ? new Class<?>[]{IModule.class, ISystemDriver.class}
                : dispatchComplete == null
                    ? new Class<?>[]{IModule.class, ISystemGroupDriver.class}
                    : new Class<?>[]{IModule.class, ISystemGroupDriver.class,
                            IAutoStartDispatchStatus.class};
        return (IModule<?>) Proxy.newProxyInstance(
                ApplicationReadinessTest.class.getClassLoader(), interfaces,
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "getConfiguration" -> config;
                    case "getCurrentState" -> state.get();
                    case "getCurrentError" -> error.get();
                    case "getMembers" -> members.get();
                    case "isAutoStartDispatchComplete" -> dispatchComplete.get();
                    case "getName", "getDescription", "getLocalID", "getUniqueIdentifier" -> "fixture";
                    case "isStarted", "isEnabled" -> state.get() == ModuleState.STARTED;
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == arguments[0];
                    case "toString" -> "fixture-module";
                    default -> defaultValue(method.getReturnType());
                });
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive())
            return null;
        if (type == boolean.class)
            return false;
        if (type == char.class)
            return '\0';
        if (type == byte.class)
            return (byte) 0;
        if (type == short.class)
            return (short) 0;
        if (type == int.class)
            return 0;
        if (type == long.class)
            return 0L;
        if (type == float.class)
            return 0F;
        if (type == double.class)
            return 0D;
        throw new AssertionError("Unknown primitive " + type);
    }

    private static void await(BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2L);
        while (!condition.getAsBoolean() && System.nanoTime() < deadline)
            Thread.sleep(10L);
        assertTrue("condition was not met before timeout", condition.getAsBoolean());
    }
}
