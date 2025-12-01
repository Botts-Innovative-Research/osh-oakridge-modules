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

import com.botts.impl.service.bucket.BucketService;
import com.botts.impl.service.bucket.BucketServiceConfig;
import net.opengis.swe.v20.Category;
import net.opengis.swe.v20.DataChoice;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.junit.*;
import org.sensorhub.api.ISensorHub;
import org.sensorhub.api.command.CommandData;
import org.sensorhub.api.command.IStreamingControlInterface;
import org.sensorhub.api.common.BigId;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.datastore.DataStoreException;
import org.sensorhub.api.module.ModuleEvent;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.impl.module.ModuleRegistry;
import org.sensorhub.impl.process.video.transcoder.coders.Decoder;
import org.sensorhub.impl.process.video.transcoder.coders.SwScaler;
import org.sensorhub.impl.process.video.transcoder.formatters.PacketFormatter;
import org.sensorhub.impl.process.video.transcoder.formatters.RgbFormatter;
import org.sensorhub.impl.sensor.ffmpeg.config.FFMPEGConfig;
import org.sensorhub.impl.sensor.ffmpeg.controls.FileControl;
import org.sensorhub.impl.sensor.ffmpeg.controls.HLSControl;
import org.sensorhub.impl.service.HttpServerConfig;
import org.sensorhub.mpegts.DataBufferListener;
import org.sensorhub.mpegts.DataBufferRecord;
import org.sensorhub.mpegts.MpegTsProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.bytedeco.ffmpeg.global.avutil.*;

import javax.swing.*;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

public abstract class ConnectionTest {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionTest.class);
    private static FFMPEGSensor driver = null;
    static final int SLEEP_DURATION_MS = 5000;
    static final int INIT_REATTEMPTS = 5;
    private final Object syncObject = new Object();

    static ISensorHub hub = null;
    static ModuleRegistry reg = null;
    static BucketService bucketService = null;

    private static final String TEST_BUCKET_DIRECTORY = "testBucketServiceRootDir";
    private static Path tempDirectory;

    @Before
    public void init() throws Exception {
        if (hub == null || reg == null) {
            hub = new SensorHub();
            hub.start();
            reg = hub.getModuleRegistry();
            var httpModule = new HttpServerConfig();
            httpModule.autoStart = true;
            reg.loadModule(httpModule);
        }

        //var rootDir = Paths.get(TEST_BUCKET_DIRECTORY);
        try {
            if (tempDirectory == null) {
                tempDirectory = Files.createTempDirectory(TEST_BUCKET_DIRECTORY).toAbsolutePath();
            }
        } catch (FileAlreadyExistsException ignored) {}

        if (bucketService == null) {
            var bucketServiceConfig = new BucketServiceConfig();
            bucketServiceConfig.autoStart = true;
            bucketServiceConfig.id = "testBucketService";
            bucketServiceConfig.name = "testBucketService";
            bucketServiceConfig.initialBuckets = Collections.singletonList("videos");

            bucketServiceConfig.fileStoreRootDir = tempDirectory.toString();

            bucketService = (BucketService) reg.loadModule(bucketServiceConfig);

        }

        if (driver == null) {

            FFMPEGConfig config = new FFMPEGConfig();

            populateConfig(config);

            assertTrue((config.connection.connectionString != null &&
                    !config.connection.connectionString.isEmpty()) ||
                    (config.connection.transportStreamPath != null && !config.connection.transportStreamPath.isEmpty()));

            driver = (FFMPEGSensor) reg.loadModule(config);

            for (int i = 0; i < INIT_REATTEMPTS; i++) {
                driver.init();
                driver.start();
                if (driver.getCurrentState() != ModuleEvent.ModuleState.STARTED) {
                    Thread.sleep(5000);
                } else {
                    break;
                }
            }

            assertSame(ModuleEvent.ModuleState.STARTED, driver.getCurrentState());
        }
    }

    @AfterClass
    public static void cleanupClass() throws Exception {
        driver.stop();
        driver = null;

        var store = bucketService.getBucketStore();
        var files = store.listObjects("videos");

        for (var file : files) {
            store.deleteObject("videos", file);
        }
    }

    protected abstract void populateConfig(FFMPEGConfig config);

    @Test
    public void testQueryStreams() {

        MpegTsProcessor mpegTsProcessor = driver.mpegTsProcessor;

        //assertTrue(mpegTsProcessor.openStream());

        mpegTsProcessor.queryEmbeddedStreams();

        assertTrue(mpegTsProcessor.hasVideoStream());

    }

    @Test
    public void testGetVideoFrameDimensions() {


        MpegTsProcessor mpegTsProcessor = driver.mpegTsProcessor;

        //mpegTsProcessor.openStream();

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

        //mpegTsProcessor.openStream();

        mpegTsProcessor.queryEmbeddedStreams();

        if (mpegTsProcessor.hasVideoStream()) {

            var dblistener = new DataBufferListener() {

                @Override
                public void onDataBuffer(DataBufferRecord record) {
                    Assert.assertNotNull(record);
                }

                @Override
                public boolean isWriting() {
                    return true;
                }

                public String getName() {return "test";}
            };

            mpegTsProcessor.setVideoDataBufferListener(dblistener);
        }

        //mpegTsProcessor.processStream();

        try {

            Thread.sleep(SLEEP_DURATION_MS);

        } catch (Exception e) {

            System.out.println(e);

        } finally {



            //mpegTsProcessor.closeStream();
        }

        //driver.stop();
    }

    @Test
    public void testStreamProcessingDecodeVideo() throws SensorHubException {
        // Most of this code is borrowed from VideoDisplay
        var canvas = new Canvas();
        final JFrame window = new JFrame();
        BufferStrategy strategy;
        final AtomicReference<BufferedImage> bufImg = new AtomicReference<>();
        final AtomicReference<Graphics> graphicCtx = new AtomicReference<>();

        MpegTsProcessor mpegTsProcessor = driver.mpegTsProcessor;
        //mpegTsProcessor.processStream();

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

            int processorColor = AV_PIX_FMT_YUVJ420P;
            HashMap<String, Integer> options = new HashMap<>();
            options.put("width", dimensions[0]);
            options.put("height", dimensions[1]);
            options.put("pix_fmt", processorColor);
            try {
                decoder = new Decoder(mpegTsProcessor.getCodecId(), options);
            } catch (Exception e) {
                decoder = new Decoder(avcodec.AV_CODEC_ID_H264, options);
            }

            scaler = new SwScaler(processorColor, AV_PIX_FMT_RGB24,
                    dimensions[0], dimensions[1],
                    dimensions[0], dimensions[1]);


            decoder.setInQueue(inputPacketQueue);
            scaler.setInQueue(decoder.getOutQueue()); // Chain decoder -> scaler

            scaler.start();
            decoder.start();

            var dblistener = new DataBufferListener() {

                @Override
                public void onDataBuffer(DataBufferRecord record) {
                    inputPacketQueue.add(packetF.convertInput(record.getDataBuffer()));
                }

                @Override
                public boolean isWriting() {
                    return true;
                }

                public String getName() {return "test";}
            };

            driver.mpegTsProcessor.addVideoDataBufferListener(dblistener);

            scheduler.schedule(() -> {
                run.set(false);
                logger.info("Stop");
            }, SLEEP_DURATION_MS, TimeUnit.MILLISECONDS);

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
                mpegTsProcessor.removeVideoDataBufferListener(dblistener);
                //mpegTsProcessor.stopProcessingStream();
                //driver.stop();
                window.dispose();
            }
        }
    }

    @Test
    public void testMp4FileOutput() throws SensorHubException {
        //driver.start();
        MpegTsProcessor mpegTsProcessor = driver.mpegTsProcessor;
        //mpegTsProcessor.processStream();
        final long fileRecordTime = 5000;

        Optional<IStreamingControlInterface> fileControl = driver.getCommandInputs().values().stream().filter(c -> c instanceof FileControl).findFirst();
        if (fileControl.isPresent()) {
            var struct = fileControl.get().getCommandDescription().clone();
            struct.renewDataBlock();

            // --------- Open File ---------
            DataChoice fileIO = (DataChoice) struct.getComponent(0);

            fileIO.setSelectedItem(FileControl.CMD_OPEN_FILE);
            var item = fileIO.getSelectedItem();
            if (!item.hasData()) {
                item.renewDataBlock();
            }

            String fileName = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss_SSS").withLocale(Locale.US).withZone(ZoneId.systemDefault()).format(Instant.now()) + ".mp4";
            item.getData().setStringValue(fileName);

            fileControl.get().submitCommand(new CommandData.Builder()
                    .withCommandStream(BigId.NONE)
                    .withId(BigId.NONE)
                    .withParams(struct.getData())
                    .build());

            try {
                Thread.sleep(fileRecordTime);
            } catch (InterruptedException ignored) {}


            // --------- Close File ---------
            fileIO.setSelectedItem(FileControl.CMD_CLOSE_FILE);
            var fileIoItem = fileIO.getSelectedItem();

            if (fileIoItem.getData() == null) {
                fileIoItem.renewDataBlock();
            }

            fileIoItem.getData().setBooleanValue(true);

            var result = fileControl.get().submitCommand(new CommandData.Builder()
                    .withCommandStream(BigId.NONE)
                    .withId(BigId.NONE)
                    .withParams(struct.getData())
                    .build());

            var commandOutputData = result.join().getResult().getInlineRecords().stream().findFirst();

            assertTrue(commandOutputData.isPresent());

            String filePath = commandOutputData.get().getStringValue();

            assertTrue(Files.exists(tempDirectory.resolve(filePath)));
        } else {
            fail("FileControl not found");
        }
    }

    @Test
    public void testHlsFileOutput() throws SensorHubException {
        //driver.start();
        MpegTsProcessor mpegTsProcessor = driver.mpegTsProcessor;
        //mpegTsProcessor.processStream();
        final long fileRecordTime = 5000;

        Optional<IStreamingControlInterface> fileControl = driver.getCommandInputs().values().stream().filter(c -> c instanceof HLSControl<?>).findFirst();
        if (fileControl.isPresent()) {

            // --------- Start Stream ---------
            var struct = fileControl.get().getCommandDescription().clone();
            struct.renewDataBlock();
            ((Category)struct.getComponent(0)).setValue(HLSControl.CMD_START_STREAM);

            var result = fileControl.get().submitCommand(new CommandData.Builder()
                    .withCommandStream(BigId.NONE)
                    .withId(BigId.NONE)
                    .withParams(struct.getData())
                    .build());

            try {
                Thread.sleep(fileRecordTime);
            } catch (InterruptedException ignored) {}

            var commandOutputData = result.join().getResult().getInlineRecords().stream().findFirst();

            assertTrue(commandOutputData.isPresent());

            String filePath = commandOutputData.get().getStringValue();

            assertTrue(Files.exists(tempDirectory.resolve(filePath)));

            // --------- End Stream ---------
            ((Category)struct.getComponent(0)).setValue(HLSControl.CMD_START_STREAM);

            fileControl.get().submitCommand(new CommandData.Builder()
                    .withCommandStream(BigId.NONE)
                    .withId(BigId.NONE)
                    .withParams(struct.getData())
                    .build());
        } else {
            fail("HLSControl not found");
        }
    }
}
