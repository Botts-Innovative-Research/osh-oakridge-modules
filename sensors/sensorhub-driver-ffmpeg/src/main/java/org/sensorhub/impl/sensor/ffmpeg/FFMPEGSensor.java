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

    private final Object reconnectLock = new Object();
    private Thread streamMonitorThread;
    private ScheduledExecutorService reconnectExecutor;
    private ScheduledFuture<?> reconnectTask;
    private boolean reconnectEnabled;
    private int currentReconnect;

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

        try {
            openStream();
        } catch (SensorHubException e) {
            scheduleReconnect(e);
            return;
        }

        // Some preliminary data was read from the stream in doInit(), but this call makes it start processing all the
        // frames.
        startStream();

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
        }
    }

    void scheduleReconnect(Exception cause) {
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
            delayMillis = Math.max(0, config.connectionConfig.reconnectPeriod);
            String attemptLimit = maxAttempts < 0 ? "unlimited" : Integer.toString(maxAttempts);
            reportStatus("Reconnect attempt " + attempt + "/" + attemptLimit
                    + " in " + delayMillis + " ms");

            reconnectTask = reconnectExecutor.schedule(() -> {
                synchronized (reconnectLock) {
                    reconnectTask = null;
                    if (!reconnectEnabled)
                        return;
                }

                try {
                    getParentHub().getModuleRegistry().restartModuleAsync(this);
                } catch (SensorHubException e) {
                    logger.error("Unable to schedule FFmpeg module restart", e);
                    scheduleReconnect(e);
                }
            }, delayMillis, TimeUnit.MILLISECONDS);
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
        synchronized (reconnectLock) {
            reconnectEnabled = false;
            if (reconnectTask != null) {
                reconnectTask.cancel(true);
                reconnectTask = null;
            }
            monitor = streamMonitorThread;
            streamMonitorThread = null;
            executor = reconnectExecutor;
            reconnectExecutor = null;
        }

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
}
