/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2021 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.ffmpeg;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.ffmpeg.config.FFMPEGConfig;
import org.sensorhub.mpegts.MpegTsProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Sensor driver that can read video data that is compatible with FFMPEG.
 *
 *
 * @author Drew Botts
 * @since Feb. 2023
 */
public class FFMPEGSensor extends FFMPEGSensorBase<FFMPEGConfig> {

    /** Debug logger */
    private static final Logger logger = LoggerFactory.getLogger(FFMPEGSensor.class);

    static final long DEFAULT_RECONNECT_PERIOD_MS = 2_000L;
    static final long POST_CONNECT_STATUS_CONFIRMATION_DELAY_MS = 1_500L;

    private final Object reconnectLock = new Object();
    private Thread streamMonitorThread;
    private ScheduledExecutorService reconnectExecutor;
    private ScheduledFuture<?> reconnectTask;
    private ScheduledFuture<?> connectionStatusHeartbeatTask;
    private ScheduledFuture<?> postConnectStatusTask;
    private boolean reconnectEnabled;
    private int currentReconnect;
    private int heartbeatTickCount;
    long postConnectStatusDelayMs = POST_CONNECT_STATUS_CONFIRMATION_DELAY_MS;

    @Override
    protected void doInit() throws SensorHubException {
        disableReconnect();
        super.doInit();
        currentReconnect = 0;

        // We need the background thread here since we start reading the video data immediately in order to determine
        // the video size.
        setupExecutor();

    }
    
    @Override
    protected void doStart() throws SensorHubException {
        super.doStart();
        enableReconnect();
        // Start up the background thread if it's not already going. Normally doInit() will have just been called, so
        // this is redundant (but harmless). But if the user has stopped the sensor and re-started it, then this call
        // is necessary.
        setupExecutor();

        // Make sure the stream is already open. (If the sensor has been previously started, then stopped, then the
        // stream won't be open.)

        // Clear any status left by a previous run before attempting to open the
        // source. Online is published only after both openStream() and startStream()
        // complete successfully.
        publishConnectionStatus(false);
        try {
            openStream();
            configureVideoOutputAfterOpen();
            configureStreamControlsAfterOpen();
            if (streamInterfacesDeferred) {
                registerRecoveredInterfaces();
                streamInterfacesDeferred = false;
            }

            // Some preliminary data was read from the stream in doInit(), but this call makes it start processing all
            // the frames.
            startStream();
        } catch (SensorHubException e) {
            discardFailedStream();
            scheduleReconnect(e);
            return;
        }
        publishConnectionStatus(true);
        schedulePostConnectStatusConfirmation();

        currentReconnect = 0;
        startStreamMonitor(mpegTsProcessor);
    }

    void enableReconnect() {
        synchronized (reconnectLock) {
            reconnectEnabled = true;
            if (reconnectExecutor == null || reconnectExecutor.isShutdown()) {
                reconnectExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
                    Thread thread = new Thread(runnable, "ffmpeg-reconnect-" + getLocalID());
                    thread.setDaemon(true);
                    return thread;
                });
            }
            if (connectionStatusHeartbeatTask == null || connectionStatusHeartbeatTask.isDone()) {
                heartbeatTickCount = 0;
                connectionStatusHeartbeatTask = reconnectExecutor.scheduleAtFixedRate(
                        this::runConnectionStatusHeartbeat,
                        CONNECTION_STATUS_HEARTBEAT_PERIOD_SECONDS,
                        CONNECTION_STATUS_HEARTBEAT_PERIOD_SECONDS,
                        TimeUnit.SECONDS);
            }
        }
    }

    /**
     * Republishes the current status shortly after stream startup completes so
     * asynchronous datastream consumers registered by {@code DefaultSystemRegistry}
     * after a module/lane restart always persist the post-start {@code Online}
     * transition without waiting for the 15-minute background heartbeat cadence.
     */
    private void schedulePostConnectStatusConfirmation() {
        synchronized (reconnectLock) {
            if (!reconnectEnabled || reconnectExecutor == null || reconnectExecutor.isShutdown())
                return;
            if (postConnectStatusTask != null)
                postConnectStatusTask.cancel(false);
            long delayMs = Math.max(10L, postConnectStatusDelayMs);
            postConnectStatusTask = reconnectExecutor.schedule(() -> {
                synchronized (reconnectLock) {
                    postConnectStatusTask = null;
                    if (!reconnectEnabled)
                        return;
                    try {
                        publishConnectionStatusHeartbeat();
                    } catch (Exception e) {
                        logger.warn("Unable to publish post-connect FFmpeg connection-status confirmation", e);
                    }
                }
            }, delayMs, TimeUnit.MILLISECONDS);
        }
    }

    void runConnectionStatusHeartbeat() {
        synchronized (reconnectLock) {
            if (!reconnectEnabled)
                return;
            // State transitions (connect/disconnect) publish immediately via
            // publishConnectionStatus(boolean). Coalesce unchanged background
            // heartbeats to once every 30 ticks (15 minutes) so every 30-minute
            // stage partition receives a fresh status sample without writing
            // 400 redundant rows per minute across 200 cameras.
            if ((++heartbeatTickCount % 30) != 1)
                return;

            try {
                publishConnectionStatusHeartbeat();
            } catch (Exception e) {
                // Scheduled executors suppress future invocations when a task throws.
                logger.warn("Unable to publish FFmpeg connection-status heartbeat", e);
            }
        }
    }

    void scheduleReconnect(Exception cause) {
        synchronized (reconnectLock) {
            if (postConnectStatusTask != null) {
                postConnectStatusTask.cancel(false);
                postConnectStatusTask = null;
            }
        }
        publishConnectionStatus(false);
        final int attempt;
        final int maxAttempts;
        final long delayMillis;

        synchronized (reconnectLock) {
            if (!reconnectEnabled || (reconnectTask != null && !reconnectTask.isDone()))
                return;

            maxAttempts = config.connectionConfig.reconnectAttempts;
            if (maxAttempts == 0 || (maxAttempts > 0 && currentReconnect >= maxAttempts)) {
                reconnectEnabled = false;
                reportStatus("Failed to connect after " + currentReconnect + " attempts.");
                if (cause != null)
                    reportError("Video input stream is unavailable", cause);
                stopAfterReconnectFailure();
                return;
            }

            attempt = ++currentReconnect;
            delayMillis = config.connectionConfig.reconnectPeriod > 0
                    ? config.connectionConfig.reconnectPeriod
                    : DEFAULT_RECONNECT_PERIOD_MS;
            String attemptLimit = maxAttempts < 0 ? "unlimited" : Integer.toString(maxAttempts);
            reportStatus("Reconnect attempt " + attempt + "/" + attemptLimit
                    + " in " + delayMillis + " ms");

            reconnectTask = reconnectExecutor.schedule(() -> {
                synchronized (reconnectLock) {
                    reconnectTask = null;
                    if (!reconnectEnabled)
                        return;
                }

                attemptStreamReconnect();
            }, delayMillis, TimeUnit.MILLISECONDS);
        }
    }

    void attemptStreamReconnect() {
        synchronized (reconnectLock) {
            if (!reconnectEnabled)
                return;
        }

        try {
            stopStream();
            synchronized (reconnectLock) {
                if (!reconnectEnabled)
                    return;
            }
            setupExecutor();
            openStream();
            configureVideoOutputAfterOpen();
            configureStreamControlsAfterOpen();
            if (streamInterfacesDeferred) {
                registerRecoveredInterfaces();
                streamInterfacesDeferred = false;
            }
            startStream();
        } catch (Exception e) {
            discardFailedStream();
            scheduleReconnect(e);
            return;
        }

        synchronized (reconnectLock) {
            if (!reconnectEnabled) {
                discardFailedStream();
                return;
            }
            currentReconnect = 0;
        }
        publishConnectionStatus(true);
        schedulePostConnectStatusConfirmation();
        reportStatus("Connected to video input stream.");
        startStreamMonitor(mpegTsProcessor);
    }

    @Override
    protected boolean shouldRetainStreamLeaseOnFailure() {
        synchronized (reconnectLock) {
            return reconnectEnabled;
        }
    }

    private void stopAfterReconnectFailure() {
        try {
            getParentHub().getModuleRegistry().stopModuleAsync(this);
        } catch (SensorHubException e) {
            logger.error("Unable to stop FFmpeg module after reconnect attempts were exhausted", e);
        }
    }

    private void startStreamMonitor(MpegTsProcessor processor) {
        synchronized (reconnectLock) {
            if (!reconnectEnabled || processor == null)
                return;

            if (streamMonitorThread != null)
                streamMonitorThread.interrupt();

            streamMonitorThread = new Thread(() -> waitAndReconnect(processor),
                    "ffmpeg-stream-monitor-" + getLocalID());
            streamMonitorThread.setDaemon(true);
            streamMonitorThread.start();
        }
    }

    private void waitAndReconnect(MpegTsProcessor processor) {
        try {
            processor.join();
        } catch (InterruptedException e) {
            logger.debug("FFmpeg stream monitor interrupted.");
            Thread.currentThread().interrupt();
            return;
        }

        synchronized (reconnectLock) {
            if (!reconnectEnabled || processor != mpegTsProcessor)
                return;
            streamMonitorThread = null;
        }
        scheduleReconnect(null);
    }

    void disableReconnect() {
        Thread monitor;
        ScheduledExecutorService executor;
        ScheduledFuture<?> heartbeatTask;
        ScheduledFuture<?> postConnectTask;
        synchronized (reconnectLock) {
            reconnectEnabled = false;
            heartbeatTickCount = 0;
            if (reconnectTask != null) {
                reconnectTask.cancel(true);
                reconnectTask = null;
            }
            postConnectTask = postConnectStatusTask;
            postConnectStatusTask = null;
            heartbeatTask = connectionStatusHeartbeatTask;
            connectionStatusHeartbeatTask = null;
            monitor = streamMonitorThread;
            streamMonitorThread = null;
            executor = reconnectExecutor;
            reconnectExecutor = null;
        }

        if (postConnectTask != null)
            postConnectTask.cancel(true);
        if (heartbeatTask != null)
            heartbeatTask.cancel(true);
        if (monitor != null)
            monitor.interrupt();
        if (executor != null)
            executor.shutdownNow();
    }

    @Override
    public void doStop() throws SensorHubException {
        disableReconnect();
        super.doStop();
    }

    int getReconnectAttemptCount() {
        synchronized (reconnectLock) {
            return currentReconnect;
        }
    }

    boolean isReconnectPending() {
        synchronized (reconnectLock) {
            return reconnectTask != null && !reconnectTask.isDone();
        }
    }

    boolean isConnectionStatusHeartbeatPending() {
        synchronized (reconnectLock) {
            return connectionStatusHeartbeatTask != null && !connectionStatusHeartbeatTask.isDone();
        }
    }

    ScheduledFuture<?> getConnectionStatusHeartbeatTask() {
        synchronized (reconnectLock) {
            return connectionStatusHeartbeatTask;
        }
    }
}
