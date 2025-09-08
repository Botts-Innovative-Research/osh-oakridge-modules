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

import net.opengis.swe.v20.DataBlock;
import org.eclipse.jetty.io.ByteBufferOutputStream;
import org.jcodec.codecs.h264.H264Decoder;
import org.jcodec.codecs.h264.MappedH264ES;
import org.jcodec.codecs.mjpeg.JpegDecoder;
import org.jcodec.codecs.mjpeg.JpegConst;
import org.jcodec.common.NIOUtils;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Picture;
import org.jcodec.scale.AWTUtil;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.api.data.IStreamingDataInterface;
import org.sensorhub.api.event.IEventListener;
import org.sensorhub.impl.sensor.ffmpeg.config.FFMPEGConfig;
import org.sensorhub.mpegts.MpegTsProcessor;
import org.vast.data.DataBlockMixed;
import org.vast.swe.SWEHelper;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public abstract class ConnectionTest {

    private FFMPEGSensor driver = null;
    static final int SLEEP_DURATION_MS = 1000;
    private final Object syncObject = new Object();

    @Before
    public void init() throws Exception {

        FFMPEGConfig config = new FFMPEGConfig();

        populateConfig(config);

        assertTrue((config.connection.connectionString != null &&
                !config.connection.connectionString.isEmpty()) ||
                (config.connection.transportStreamPath != null && !config.connection.transportStreamPath.isEmpty()));

        driver = new FFMPEGSensor();
        driver.setConfiguration(config);
        driver.init();
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

            System.out.println(e.toString());

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

        driver.start();

        MpegTsProcessor mpegTsProcessor = driver.mpegTsProcessor;

        int[] dimensions = mpegTsProcessor.getVideoStreamFrameDimensions();

        final JFrame window = new JFrame();
        window.setSize(dimensions[0], dimensions[1]);
        window.setVisible(true);
        window.setResizable(false);

        if (mpegTsProcessor.hasVideoStream()) {

            mpegTsProcessor.setVideoDataBufferListener(dataBufferRecord -> {

                try {
                    if (mpegTsProcessor.getCodecName().equalsIgnoreCase("H264")) {
                        MappedH264ES es = new MappedH264ES(NIOUtils.from(ByteBuffer.wrap(dataBufferRecord.getDataBuffer()), 0));
                        Picture out = Picture.create(dimensions[0], dimensions[1], ColorSpace.YUV420);
                        H264Decoder decoder = new H264Decoder();
                        Packet packet;

                        while (null != (packet = es.nextFrame())) {

                            ByteBuffer data = packet.getData();
                            Picture res = decoder.decodeFrame(data, out.getData());
                            BufferedImage bi = AWTUtil.toBufferedImage(res);
                            window.getContentPane().getGraphics().drawImage(bi, 0, 0, null);
                        }
                    } else { // TODO This did not work when tested last
                        Picture out = Picture.create(dimensions[0], dimensions[1], ColorSpace.YUV420);
                        JpegDecoder decoder = new JpegDecoder();

                        ByteBuffer data = NIOUtils.from(ByteBuffer.wrap(dataBufferRecord.getDataBuffer()), 0);
                        Picture res = decoder.decodeFrame(data, out.getData());
                        BufferedImage bi = AWTUtil.toBufferedImage(res);
                        window.getContentPane().getGraphics().drawImage(bi, 0, 0, null);
                    }

                } catch (Exception ignored) {

                }
            });
        }

        //mpegTsProcessor.processStream();

        try {

            Thread.sleep(SLEEP_DURATION_MS*10);

        } catch (Exception e) {

            System.out.println(e.toString());

        } finally {

            driver.stop();

            window.dispose();
        }
    }
}
