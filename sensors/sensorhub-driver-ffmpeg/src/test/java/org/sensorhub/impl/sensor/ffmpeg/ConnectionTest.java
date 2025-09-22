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

import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sensorhub.api.ISensorHub;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.ModuleEvent;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.impl.module.ModuleRegistry;
import org.sensorhub.impl.process.video.transcoder.coders.Decoder;
import org.sensorhub.impl.process.video.transcoder.coders.SwScaler;
import org.sensorhub.impl.process.video.transcoder.formatters.PacketFormatter;
import org.sensorhub.impl.process.video.transcoder.formatters.RgbFormatter;
import org.sensorhub.impl.sensor.ffmpeg.config.FFMPEGConfig;
import org.sensorhub.mpegts.MpegTsProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.bytedeco.ffmpeg.global.avutil.*;;

import javax.swing.*;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.*;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

public abstract class ConnectionTest {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionTest.class);
    private FFMPEGSensor driver = null;
    static final int SLEEP_DURATION_MS = 5000;
    static final int INIT_REATTEMPTS = 5;
    private final Object syncObject = new Object();
    ISensorHub hub;
    ModuleRegistry reg;

    @Before
    public void init() throws Exception {
        hub = new SensorHub();
        hub.start();
        reg = hub.getModuleRegistry();

        FFMPEGConfig config = new FFMPEGConfig();

        populateConfig(config);

        assertTrue((config.connection.connectionString != null &&
                !config.connection.connectionString.isEmpty()) ||
                (config.connection.transportStreamPath != null && !config.connection.transportStreamPath.isEmpty()));

        driver = (FFMPEGSensor) reg.loadModule(config);

        for (int i = 0; i < INIT_REATTEMPTS; i++) {
            driver.init();
            if (driver.getCurrentState() != ModuleEvent.ModuleState.INITIALIZED) {
                Thread.sleep(5000);
            } else {
                break;
            }
        }

        assertSame(ModuleEvent.ModuleState.INITIALIZED, driver.getCurrentState());
    }

    @After
    public void cleanup() throws Exception {
        driver.stop();
    }

    protected abstract void populateConfig(FFMPEGConfig config);

    @Test
    public void testQueryStreams() {

        MpegTsProcessor mpegTsProcessor = driver.mpegTsProcessor;

        assertTrue(mpegTsProcessor.openStream());

        mpegTsProcessor.queryEmbeddedStreams();

        assertTrue(mpegTsProcessor.hasVideoStream());

    }

    @Test
    public void testGetVideoFrameDimensions() {

        MpegTsProcessor mpegTsProcessor = driver.mpegTsProcessor;

        mpegTsProcessor.openStream();

        mpegTsProcessor.queryEmbeddedStreams();

        if (mpegTsProcessor.hasVideoStream()) {

            int[] dimensions = mpegTsProcessor.getVideoStreamFrameDimensions();

            assertEquals(2, dimensions.length);

            assertTrue(dimensions[0] > 0);

            assertTrue(dimensions[1] > 0);

        } else {
            fail("Video sub-stream not present");
        }
    }

    @Test
    public void testStreamProcessing() throws SensorHubException {

        MpegTsProcessor mpegTsProcessor = driver.mpegTsProcessor;

        mpegTsProcessor.openStream();

        mpegTsProcessor.queryEmbeddedStreams();

        if (mpegTsProcessor.hasVideoStream()) {

            mpegTsProcessor.setVideoDataBufferListener(Assert::assertNotNull);
        }

        mpegTsProcessor.processStream();

        try {

            Thread.sleep(SLEEP_DURATION_MS);

        } catch (Exception e) {

            System.out.println(e);

        } finally {

            mpegTsProcessor.stopProcessingStream();

            try {

                mpegTsProcessor.join();

            } catch (InterruptedException e) {

                e.printStackTrace();
            }

            mpegTsProcessor.closeStream();
        }

        driver.stop();
    }

    @Test
    public void testStreamProcessingDecodeVideo() throws SensorHubException {
        // Most of this code is borrowed from VideoDisplay
        var canvas = new Canvas();
        final JFrame window = new JFrame();
        BufferStrategy strategy;
        final AtomicReference<BufferedImage> bufImg = new AtomicReference<>();
        final AtomicReference<Graphics> graphicCtx = new AtomicReference<>();

        driver.start();
        MpegTsProcessor mpegTsProcessor = driver.mpegTsProcessor;

        int[] dimensions = mpegTsProcessor.getVideoStreamFrameDimensions();

        window.setSize(dimensions[0], dimensions[1]);
        window.setVisible(true);
        window.setResizable(false);

        canvas.setPreferredSize(new Dimension(dimensions[0], dimensions[1]));

        window.add(canvas);
        window.pack();
        window.setVisible(true);
        window.setResizable(false);

        // create RGB buffered image
        var cs = java.awt.color.ColorSpace.getInstance(ColorSpace.CS_sRGB);
        var colorModel = new ComponentColorModel(
                cs, new int[] {8,8,8},
                false, false,
                Transparency.OPAQUE,
                DataBuffer.TYPE_BYTE);
        var raster = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE,
                dimensions[0], dimensions[1],
                dimensions[0]*3, 3,
                new int[] {0, 1, 2}, null);
        bufImg.set(new BufferedImage(colorModel, raster, false, null));
        //bufImg.set(new BufferedImage(dimensions[0], dimensions[1],BufferedImage.TYPE_3BYTE_BGR));
        canvas.createBufferStrategy(2);
        strategy = canvas.getBufferStrategy();

        if (mpegTsProcessor.hasVideoStream()) {
            final Queue<AVPacket> inputPacketQueue = new ArrayDeque<>();
            Decoder decoder;
            SwScaler scaler; // Convert pixel format to rgb
            RgbFormatter rgbF = new RgbFormatter(dimensions[0], dimensions[1]);
            PacketFormatter packetF = new PacketFormatter();
            AtomicBoolean run = new AtomicBoolean(true);
            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

            int processorColor = mpegTsProcessor.getColorSpace();
            processorColor = AV_PIX_FMT_YUVJ420P;
            HashMap<String, Integer> options = new HashMap<>();
            options.put("width", dimensions[0]);
            options.put("height", dimensions[1]);
            options.put("pix_fmt", processorColor);
            decoder = new Decoder(mpegTsProcessor.getCodecId(), options);

            scaler = new SwScaler(processorColor, AV_PIX_FMT_RGB24,
                    dimensions[0], dimensions[1],
                    dimensions[0], dimensions[1]);


            decoder.setInQueue(inputPacketQueue);
            scaler.setInQueue(decoder.getOutQueue()); // Chain decoder -> scaler

            scaler.start();
            decoder.start();

            mpegTsProcessor.setVideoDataBufferListener(dataBufferRecord -> {
                inputPacketQueue.add(packetF.convertInput(dataBufferRecord.getDataBuffer()));
            });

            scheduler.schedule(() -> {
                run.set(false);
                logger.info("Stop");
            }, SLEEP_DURATION_MS, TimeUnit.MILLISECONDS);

            int i = 0;
            logger.info("BEFORE FRAMES");
            var outQueue = scaler.getOutQueue();
            //var outQueue = decoder.getOutQueue();
            while (run.get()) {
                while (!outQueue.isEmpty()) {
                    var frame = outQueue.poll();
                    if (!run.get())
                        break;

                    //logger.info("Frame {}", ++i);
                    var imgData = ((DataBufferByte) bufImg.get().getRaster().getDataBuffer()).getData();
                    var frameData = rgbF.convertOutput(frame);
                    System.arraycopy(frameData, 0, imgData, 0, frameData.length);

                    graphicCtx.set(strategy.getDrawGraphics());
                    graphicCtx.get().setColor(Color.YELLOW);
                    graphicCtx.get().drawImage(bufImg.get(), 0, 0, null);
                    strategy.show();
                    graphicCtx.get().dispose();
                    av_frame_unref(frame);
                    av_frame_free(frame);
                }
            }

            decoder.doRun.set(false);
            scaler.doRun.set(false);
            try {
                decoder.join();
                scaler.join();
            } catch (InterruptedException ignored) {
                logger.error("Error: ", ignored);
            } finally {
                driver.stop();
                window.dispose();
            }
        }
    }
}
