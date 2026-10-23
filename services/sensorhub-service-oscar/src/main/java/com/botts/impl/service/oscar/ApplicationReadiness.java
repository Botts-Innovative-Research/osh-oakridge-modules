/*******************************************************************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 The Initial Developer is Botts Innovative Research Inc. Portions created by the Initial
 Developer are Copyright (C) 2025 the Initial Developer. All Rights Reserved.

 ******************************************************************************/

package com.botts.impl.service.oscar;

import com.botts.impl.system.lane.IAutoStartDispatchStatus;
import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.ModuleEvent.ModuleState;
import org.sensorhub.api.system.ISystemGroupDriver;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Publishes OSCAR application readiness independently of HTTP availability.
 *
 * <p>The HTTP server is intentionally able to start before the lane modules. A
 * container health check that probes HTTP alone can therefore release a second
 * administrative operation while modules from the first start are still being
 * initialized. This monitor keeps a container-local marker present only while
 * every configured top-level auto-start module is fully started and each of
 * its auto-start members has completed its startup attempt. An unavailable
 * device can settle in {@code STOPPED}, or in {@code LOADED} after its containing
 * lane explicitly reports that the child auto-start pass completed.
 * Connectivity is reported by OSCAR's status-of-health data and must not make
 * the application itself unavailable.</p>
 */
final class ApplicationReadiness implements AutoCloseable {
    static final Path DEFAULT_MARKER = Path.of("/tmp/oscar-ready");
    static final long DEFAULT_POLL_MILLIS = 500L;

    private final Object lifecycleLock = new Object();
    private final Supplier<? extends Collection<? extends IModule<?>>> modulesSupplier;
    private final Path marker;
    private final Path temporaryMarker;
    private final long pollMillis;
    private final Logger logger;

    private volatile boolean running;
    private boolean failureLogged;
    private ScheduledExecutorService executor;
    private ScheduledFuture<?> refreshTask;

    ApplicationReadiness(Supplier<? extends Collection<? extends IModule<?>>> modulesSupplier,
                         Path marker,
                         long pollMillis,
                         Logger logger) {
        this.modulesSupplier = Objects.requireNonNull(modulesSupplier, "modulesSupplier");
        this.marker = Objects.requireNonNull(marker, "marker");
        this.temporaryMarker = marker.resolveSibling(marker.getFileName() + ".tmp");
        if (pollMillis <= 0L)
            throw new IllegalArgumentException("pollMillis must be positive");
        this.pollMillis = pollMillis;
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    void start() throws IOException {
        synchronized (lifecycleLock) {
            if (running)
                return;

            removeMarker();
            running = true;
            executor = Executors.newSingleThreadScheduledExecutor(readinessThreadFactory());
            try {
                refreshTask = executor.scheduleWithFixedDelay(
                        this::refreshSafely, 0L, pollMillis, TimeUnit.MILLISECONDS);
            } catch (RuntimeException error) {
                running = false;
                executor.shutdownNow();
                executor = null;
                throw error;
            }
        }
    }

    private ThreadFactory readinessThreadFactory() {
        return task -> {
            Thread thread = new Thread(task, "oscar-application-readiness");
            thread.setDaemon(true);
            return thread;
        };
    }

    private void refreshSafely() {
        if (!running)
            return;

        try {
            boolean ready = allAutoStartModuleTreesSettled(modulesSupplier.get());
            synchronized (lifecycleLock) {
                // close() can run while the module tree is being traversed. The
                // second check prevents an interrupted or delayed poll from
                // republishing readiness after shutdown has withdrawn it.
                if (!running)
                    return;
                publish(ready);
            }
            failureLogged = false;
        } catch (IOException | RuntimeException error) {
            synchronized (lifecycleLock) {
                if (!running)
                    return;
                removeMarkerAfterRefreshFailure();
                if (!failureLogged) {
                    logger.error("Could not refresh OSCAR application readiness", error);
                    failureLogged = true;
                }
            }
        }
    }

    static boolean allAutoStartModuleTreesSettled(Collection<? extends IModule<?>> modules) {
        if (modules == null)
            return false;

        boolean[] foundAutoStartModule = {false};
        for (IModule<?> module : modules) {
            // Keep traversal identity local to each registry root. If a malformed
            // graph also exposes a nested object as a top-level module, that root
            // must still satisfy the stricter top-level STARTED requirement.
            Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
            if (!autoStartTreeSettled(module, visited, foundAutoStartModule, true, false))
                return false;
        }
        return foundAutoStartModule[0];
    }

    /**
     * SensorSystem publishes its own STARTED state immediately before starting
     * its auto-start children. Readiness therefore has to inspect nested system
     * members rather than treating the parent state as sufficient.
     */
    private static boolean autoStartTreeSettled(IModule<?> module,
                                                Set<Object> visited,
                                                boolean[] foundAutoStartModule,
                                                boolean topLevel,
                                                boolean parentAutoStartDispatchComplete) {
        if (module == null || !visited.add(module))
            return true;

        var configuration = module.getConfiguration();
        if (configuration == null || !configuration.autoStart)
            return true;

        foundAutoStartModule[0] = true;
        ModuleState state = module.getCurrentState();
        if (state != ModuleState.STARTED) {
            // A nested physical sensor is allowed to finish a failed connection
            // attempt in STOPPED. Its top-level lane remains STARTED so the UI can
            // report that disconnected state. Top-level application services,
            // however, must always be STARTED.
            if (!topLevel) {
                if (state == ModuleState.STOPPED)
                    return true;
                // An offline RS350 leaves its dependent occupancy process LOADED
                // without an error even after SensorSystem has attempted every
                // child. Only the containing group's explicit completion boundary
                // can distinguish that terminal state from not-yet-attempted.
                if (state == ModuleState.LOADED && parentAutoStartDispatchComplete)
                    return true;
            }
            return false;
        }

        if (!(module instanceof ISystemGroupDriver<?> group))
            return true;

        boolean autoStartDispatchComplete = false;
        if (module instanceof IAutoStartDispatchStatus dispatchStatus) {
            autoStartDispatchComplete = dispatchStatus.isAutoStartDispatchComplete();
            if (!autoStartDispatchComplete)
                return false;
        }

        var members = group.getMembers();
        if (members == null)
            return true;
        for (var member : members.values()) {
            if (member instanceof IModule<?> memberModule &&
                    !autoStartTreeSettled(memberModule, visited, foundAutoStartModule,
                            false, autoStartDispatchComplete))
                return false;
        }
        return true;
    }

    private void publish(boolean ready) throws IOException {
        if (!ready) {
            removeMarker();
            return;
        }

        if (Files.isRegularFile(marker))
            return;

        Files.writeString(temporaryMarker, "ready\n", StandardCharsets.US_ASCII,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE);
        try {
            Files.move(temporaryMarker, marker, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException error) {
            Files.move(temporaryMarker, marker, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void removeMarkerAfterRefreshFailure() {
        try {
            removeMarker();
        } catch (IOException removalError) {
            if (!failureLogged)
                logger.error("Could not withdraw OSCAR application readiness", removalError);
        }
    }

    private void removeMarker() throws IOException {
        Files.deleteIfExists(marker);
        Files.deleteIfExists(temporaryMarker);
    }

    @Override
    public void close() throws IOException {
        ScheduledExecutorService executorToStop;
        IOException markerFailure = null;
        synchronized (lifecycleLock) {
            running = false;
            if (refreshTask != null) {
                refreshTask.cancel(true);
                refreshTask = null;
            }
            executorToStop = executor;
            executor = null;
            try {
                removeMarker();
            } catch (IOException error) {
                markerFailure = error;
            }
        }

        if (executorToStop != null) {
            executorToStop.shutdownNow();
            try {
                if (!executorToStop.awaitTermination(250L, TimeUnit.MILLISECONDS))
                    logger.warn("OSCAR application-readiness worker did not stop within 250 ms");
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                logger.warn("Interrupted while stopping OSCAR application-readiness worker", error);
            }
        }

        if (markerFailure != null)
            throw markerFailure;
    }
}
