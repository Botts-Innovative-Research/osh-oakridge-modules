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
import org.sensorhub.api.module.ModuleEvent;
import org.sensorhub.impl.module.ModuleRegistry;
import org.sensorhub.impl.sensor.ffmpeg.config.FFMPEGConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private Thread reconnectThread;

    private int currentReconnect;

    @Override
    protected void doInit() throws SensorHubException {
        super.doInit();
        currentReconnect = 0;

        // We need the background thread here since we start reading the video data immediately in order to determine
        // the video size.
        setupExecutor();

        // Open up the stream so that we can get the video output.
        openStream();

    }
    
    @Override
    protected void doStart() throws SensorHubException {
    	super.doStart();
    	// Start up the background thread if it's not already going. Normally doInit() will have just been called, so
    	// this is redundant (but harmless). But if the user has stopped the sensor and re-started it, then this call
    	// is necessary.
        setupExecutor();

        // Make sure the stream is already open. (If the sensor has been previously started, then stopped, then the
        // stream won't be open.)
        try {
            openStream();
        } catch (SensorHubException e) {
            handleReconnect();
            return;
        }

        // Some preliminary data was read from the stream in doInit(), but this call makes it start processing all the
        // frames.
        startStream();

        currentReconnect = 0;
        reconnectThread = new Thread(this::waitAndReconnect);
        if (!reconnectThread.isAlive()) {
            reconnectThread.start();
        }
    }

    private void handleReconnect() {
        if (currentReconnect < config.connectionConfig.reconnectAttempts) {
            try {
                reportStatus("Reconnect attempt " + currentReconnect + 1);
                this.getParentHub().getModuleRegistry().restartModuleAsync(this);
                currentReconnect++;
            } catch (SensorHubException e) {
                throw new RuntimeException(e);
            }
        } else {
            reportStatus("Failed to connect after " + currentReconnect + " attempts.");
            currentReconnect = 0;
            try {
                this.getParentHub().getModuleRegistry().stopModuleAsync(this);
            } catch (SensorHubException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void waitAndReconnect() {
        // Wait for the mpegTsProcessor to finish (video stream end)
        // and reconnect.
        try {
            if(mpegTsProcessor != null)
                mpegTsProcessor.join();
        } catch (InterruptedException e) {
            // If join is interrupted, this means doStop was called
            // and the function should immediately return to avoid
            // starting the reconnect loop.
            logger.debug("Mpeg process interrupted.");
            currentReconnect = 0;
            return;
        }
        handleReconnect();
    }

    @Override
    public void doStop() throws SensorHubException {
        if (reconnectThread != null) {
            try {
                reconnectThread.interrupt();
            } catch (SecurityException e) {
                logger.debug("Reconnect thread could not be interrupted.");
            }
            reconnectThread = null;
        }
        super.doStop();
    }
}
