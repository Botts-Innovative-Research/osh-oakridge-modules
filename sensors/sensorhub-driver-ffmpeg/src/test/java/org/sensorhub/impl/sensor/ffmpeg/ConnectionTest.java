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
import net.opengis.swe.v20.DataComponent;
import org.bytedeco.ffmpeg.avcodec.AVCodec;
import org.bytedeco.ffmpeg.avcodec.AVCodecContext;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avutil.AVFrame;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacpp.PointerPointer;
import org.junit.*;
import org.sensorhub.api.ISensorHub;
import org.sensorhub.api.command.CommandData;
import org.sensorhub.api.command.ICommandStatus;
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
import static org.bytedeco.ffmpeg.global.avcodec.*;
import static org.bytedeco.ffmpeg.global.avformat.*;

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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
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
        if (driver != null) {
            driver.stop();
        }
        driver = null;

        if (bucketService == null) {
            return;
        }

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
        DataBufferListener testListener = null;

        //mpegTsProcessor.openStream();

        mpegTsProcessor.queryEmbeddedStreams();

        if (mpegTsProcessor.hasVideoStream()) {

            testListener = new DataBufferListener() {

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

            // Do not replace the sensor's registered FileOutput/HLS listeners. Clearing those
            // shared listeners makes later recording tests depend on JUnit method order.
            mpegTsProcessor.addVideoDataBufferListener(testListener);
        }

        //mpegTsProcessor.processStream();

        try {

            Thread.sleep(SLEEP_DURATION_MS);

        } catch (Exception e) {

            System.out.println(e);

        } finally {
            if (testListener != null) {
                mpegTsProcessor.removeVideoDataBufferListener(testListener);
            }
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
            final BlockingQueue<AVPacket> inputPacketQueue = new LinkedBlockingQueue<>();
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
    public void testDuplicateMp4StartIsSerialized() {
        Optional<IStreamingControlInterface> fileControl = driver.getCommandInputs().values().stream()
                .filter(c -> c instanceof FileControl).findFirst();
        assertTrue("FileControl not found", fileControl.isPresent());

        var struct = fileControl.get().getCommandDescription().clone();
        struct.renewDataBlock();
        DataChoice fileIO = (DataChoice) struct.getComponent(0);
        fileIO.setSelectedItem(FileControl.CMD_OPEN_FILE);
        var item = fileIO.getSelectedItem();
        if (!item.hasData())
            item.renewDataBlock();
        item.getData().setStringValue("duplicate-start.mp4");

        var first = fileControl.get().submitCommand(new CommandData.Builder()
                .withCommandStream(BigId.NONE)
                .withId(BigId.NONE)
                .withParams(struct.getData().clone())
                .build());
        var second = fileControl.get().submitCommand(new CommandData.Builder()
                .withCommandStream(BigId.NONE)
                .withId(BigId.NONE)
                .withParams(struct.getData().clone())
                .build());

        var statuses = java.util.List.of(first.join(), second.join());
        assertEquals(1, statuses.stream()
                .filter(status -> status.getStatusCode() == ICommandStatus.CommandStatusCode.ACCEPTED)
                .count());
        assertEquals(1, statuses.stream()
                .filter(status -> status.getStatusCode() == ICommandStatus.CommandStatusCode.FAILED)
                .count());

        fileIO.setSelectedItem(FileControl.CMD_CLOSE_FILE);
        var closeItem = fileIO.getSelectedItem();
        if (!closeItem.hasData())
            closeItem.renewDataBlock();
        closeItem.getData().setBooleanValue(false);
        fileControl.get().submitCommand(new CommandData.Builder()
                .withCommandStream(BigId.NONE)
                .withId(BigId.NONE)
                .withParams(struct.getData())
                .build()).join();
    }

    @Test
    public void testDuplicateHlsStartIsIdempotent() {
        Optional<IStreamingControlInterface> hlsControl = driver.getCommandInputs().values().stream()
                .filter(c -> c instanceof HLSControl<?>).findFirst();
        assertTrue("HLSControl not found", hlsControl.isPresent());

        var startCommand = hlsControl.get().getCommandDescription().clone();
        startCommand.renewDataBlock();
        ((Category) startCommand.getComponent(0)).setValue(HLSControl.CMD_START_STREAM);

        var first = hlsControl.get().submitCommand(new CommandData.Builder()
                .withCommandStream(BigId.NONE)
                .withId(BigId.NONE)
                .withParams(startCommand.getData().clone())
                .build());
        var second = hlsControl.get().submitCommand(new CommandData.Builder()
                .withCommandStream(BigId.NONE)
                .withId(BigId.NONE)
                .withParams(startCommand.getData().clone())
                .build());

        var firstStatus = first.join();
        var secondStatus = second.join();
        assertEquals(ICommandStatus.CommandStatusCode.ACCEPTED, firstStatus.getStatusCode());
        assertEquals(ICommandStatus.CommandStatusCode.ACCEPTED, secondStatus.getStatusCode());
        assertEquals(
                firstStatus.getResult().getInlineRecords().iterator().next().getStringValue(),
                secondStatus.getResult().getInlineRecords().iterator().next().getStringValue());

        var stopCommand = hlsControl.get().getCommandDescription().clone();
        stopCommand.renewDataBlock();
        ((Category) stopCommand.getComponent(0)).setValue(HLSControl.CMD_END_STREAM);
        var stopStatus = hlsControl.get().submitCommand(new CommandData.Builder()
                .withCommandStream(BigId.NONE)
                .withId(BigId.NONE)
                .withParams(stopCommand.getData())
                .build()).join();
        assertEquals("Immediate HLS stop must accept cancellation before the first segment: "
                        + stopStatus.getMessage(),
                ICommandStatus.CommandStatusCode.ACCEPTED, stopStatus.getStatusCode());
    }

    @Test
    public void failedHlsOutputIsReportedAndRemoved() {
        Optional<IStreamingControlInterface> control = driver.getCommandInputs().values().stream()
                .filter(c -> c instanceof HLSControl<?>).findFirst();
        assertTrue("HLSControl not found", control.isPresent());
        HLSControl<?> hlsControl = (HLSControl<?>) control.get();

        var startCommand = hlsControl.getCommandDescription().clone();
        startCommand.renewDataBlock();
        ((Category) startCommand.getComponent(0)).setValue(HLSControl.CMD_START_STREAM);
        var startStatus = hlsControl.submitCommand(new CommandData.Builder()
                .withCommandStream(BigId.NONE)
                .withId(BigId.NONE)
                .withParams(startCommand.getData())
                .build()).join();
        assertEquals("HLS start command failed: " + startStatus.getMessage(),
                ICommandStatus.CommandStatusCode.ACCEPTED, startStatus.getStatusCode());

        String streamPath = startStatus.getResult().getInlineRecords().iterator().next()
                .getStringValue();
        hlsControl.getFileOutput().onDataBuffer(
                new DataBufferRecord(Double.NaN, new byte[0], false));

        var stopCommand = hlsControl.getCommandDescription().clone();
        stopCommand.renewDataBlock();
        ((Category) stopCommand.getComponent(0)).setValue(HLSControl.CMD_END_STREAM);
        var stopStatus = hlsControl.submitCommand(new CommandData.Builder()
                .withCommandStream(BigId.NONE)
                .withId(BigId.NONE)
                .withParams(stopCommand.getData())
                .build()).join();

        assertEquals("A real HLS packet-write failure must not be reported as a cancellation",
                ICommandStatus.CommandStatusCode.FAILED, stopStatus.getStatusCode());
        assertFalse("Failed HLS output was retained: " + streamPath,
                Files.exists(tempDirectory.resolve(streamPath)));
    }

    @Test
    public void testMp4FileOutput() throws SensorHubException {
        MpegTsProcessor processor = driver.mpegTsProcessor;
        // Deliberately disable pre-roll. OPEN must remain pending until a new, muxable keyframe
        // arrives from the live processor rather than succeeding from an already buffered GOP.
        processor.setFrameBuffer(0);

        try {
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

                var openStatus = fileControl.get().submitCommand(new CommandData.Builder()
                        .withCommandStream(BigId.NONE)
                        .withId(BigId.NONE)
                        .withParams(struct.getData())
                        .build()).join();
                assertEquals("MP4 open command failed: " + openStatus.getMessage(),
                        ICommandStatus.CommandStatusCode.ACCEPTED, openStatus.getStatusCode());
                assertTrue("An accepted MP4 open must already contain a muxed keyframe",
                        ((FileControl<?>) fileControl.get()).getFileOutput().hasWrittenPacket());

                // Close immediately after OPEN is acknowledged. This is the production race that
                // previously produced a header-only, zero-packet MP4 when an occupancy ended as
                // the camera processor started.
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

                var closeStatus = result.join();
                assertEquals("MP4 close command failed: " + closeStatus.getMessage(),
                        ICommandStatus.CommandStatusCode.ACCEPTED, closeStatus.getStatusCode());
                assertNotNull("MP4 close command did not return a file result", closeStatus.getResult());
                var commandOutputData = closeStatus.getResult().getInlineRecords().stream().findFirst();

                assertTrue(commandOutputData.isPresent());

                String filePath = commandOutputData.get().getStringValue();
                Path outputPath = tempDirectory.resolve(filePath);

                assertTrue("MP4 output does not exist: " + outputPath, Files.exists(outputPath));
                try {
                    assertTrue("MP4 output is empty: " + outputPath, Files.size(outputPath) > 0);
                } catch (IOException e) {
                    throw new AssertionError("Could not inspect MP4 output " + outputPath, e);
                }
                assertValidVideoFile(outputPath, true);
            } else {
                fail("FileControl not found");
            }
        } finally {
            processor.setFrameBuffer(0);
        }
    }

    @Test
    public void failedMp4OutputIsRemoved() {
        Optional<IStreamingControlInterface> control = driver.getCommandInputs().values().stream()
                .filter(c -> c instanceof FileControl)
                .findFirst();
        assertTrue("FileControl not found", control.isPresent());

        FileControl<?> fileControl = (FileControl<?>) control.get();
        var command = fileControl.getCommandDescription().clone();
        command.renewDataBlock();
        DataChoice fileIO = (DataChoice) command.getComponent(0);

        fileIO.setSelectedItem(FileControl.CMD_OPEN_FILE);
        var openItem = fileIO.getSelectedItem();
        if (!openItem.hasData()) {
            openItem.renewDataBlock();
        }
        String leafName = "forced-write-failure-" + System.nanoTime() + ".mp4";
        openItem.getData().setStringValue(leafName);

        var openStatus = fileControl.submitCommand(new CommandData.Builder()
                .withCommandStream(BigId.NONE)
                .withId(BigId.NONE)
                .withParams(command.getData())
                .build()).join();
        assertEquals("MP4 open command failed: " + openStatus.getMessage(),
                ICommandStatus.CommandStatusCode.ACCEPTED, openStatus.getStatusCode());

        // Simulate an asynchronous packet-path failure after the header and placeholder have
        // already been created. The close command must report failure and remove that artifact.
        fileControl.getFileOutput().onDataBuffer(new DataBufferRecord(Double.NaN, new byte[0], false));

        fileIO.setSelectedItem(FileControl.CMD_CLOSE_FILE);
        var closeItem = fileIO.getSelectedItem();
        if (!closeItem.hasData()) {
            closeItem.renewDataBlock();
        }
        closeItem.getData().setBooleanValue(true);

        var closeStatus = fileControl.submitCommand(new CommandData.Builder()
                .withCommandStream(BigId.NONE)
                .withId(BigId.NONE)
                .withParams(command.getData())
                .build()).join();
        assertEquals("A failed MP4 recording must not be reported as saved",
                ICommandStatus.CommandStatusCode.FAILED, closeStatus.getStatusCode());

        Path failedOutput = tempDirectory.resolve("videos").resolve(Paths.get(
                "clips", driver.getUniqueIdentifier().replace(':', '-'), leafName));
        assertFalse("Incomplete MP4 output was retained: " + failedOutput, Files.exists(failedOutput));
    }

    /**
     * Opens the completed MP4 with FFmpeg and requires valid video metadata, a keyframe as the
     * first video packet, and an actually decoded video frame. File existence or packet parsing
     * alone is insufficient: a muxer can create a header-only or mid-GOP file that no player can
     * decode.
     */
    private static void assertValidVideoFile(Path outputPath, boolean requireContainerDimensions) {
        AVFormatContext context = new AVFormatContext(null);
        int openRet = avformat_open_input(context, outputPath.toString(), null, null);
        try {
            assertTrue("FFmpeg could not open video output (error " + openRet + "): " + outputPath,
                    openRet >= 0);
            int infoRet = avformat_find_stream_info(context, (PointerPointer<?>) null);
            assertTrue("FFmpeg could not read video stream metadata (error " + infoRet + ")",
                    infoRet >= 0);

            int videoStreamIndex = -1;
            for (int i = 0; i < context.nb_streams(); i++) {
                var codecParameters = context.streams(i).codecpar();
                if (codecParameters.codec_type() == AVMEDIA_TYPE_VIDEO) {
                    if (requireContainerDimensions) {
                        assertTrue("Video width is missing", codecParameters.width() > 0);
                        assertTrue("Video height is missing", codecParameters.height() > 0);
                    }
                    videoStreamIndex = i;
                    break;
                }
            }
            assertTrue("Output contains no video stream", videoStreamIndex >= 0);

            var codecParameters = context.streams(videoStreamIndex).codecpar();
            AVCodec decoder = avcodec_find_decoder(codecParameters.codec_id());
            assertNotNull("No decoder is available for the MP4 video codec", decoder);
            assertFalse("No decoder is available for the MP4 video codec", decoder.isNull());
            AVCodecContext decoderContext = avcodec_alloc_context3(decoder);
            assertNotNull("FFmpeg could not allocate an MP4 decoder", decoderContext);
            assertFalse("FFmpeg could not allocate an MP4 decoder", decoderContext.isNull());
            assertTrue("Could not copy MP4 codec parameters into decoder",
                    avcodec_parameters_to_context(decoderContext, codecParameters) >= 0);
            assertTrue("Could not open MP4 video decoder",
                    avcodec_open2(decoderContext, decoder, (PointerPointer<?>) null) >= 0);

            AVPacket packet = av_packet_alloc();
            AVFrame frame = av_frame_alloc();
            assertTrue("FFmpeg could not allocate a validation packet",
                    packet != null && !packet.isNull());
            assertTrue("FFmpeg could not allocate a validation frame",
                    frame != null && !frame.isNull());
            try {
                boolean foundVideoPacket = false;
                boolean decodedFrame = false;
                long previousDts = AV_NOPTS_VALUE;
                int videoPacketsRead = 0;
                while (!decodedFrame && videoPacketsRead < 30
                        && av_read_frame(context, packet) >= 0) {
                    if (packet.stream_index() == videoStreamIndex) {
                        assertTrue("Output contains an empty video packet", packet.size() > 0);
                        if (!foundVideoPacket) {
                            assertTrue("The first MP4 video packet is not a keyframe",
                                    (packet.flags() & AV_PKT_FLAG_KEY) != 0);
                        }
                        if (packet.dts() != AV_NOPTS_VALUE && previousDts != AV_NOPTS_VALUE) {
                            assertTrue("Output video DTS is not monotonic",
                                    packet.dts() > previousDts);
                        }
                        if (packet.dts() != AV_NOPTS_VALUE) {
                            previousDts = packet.dts();
                        }
                        foundVideoPacket = true;
                        videoPacketsRead++;

                        int sendRet = avcodec_send_packet(decoderContext, packet);
                        assertTrue("MP4 decoder rejected a packet (error " + sendRet + ")",
                                sendRet >= 0);
                        int receiveRet = avcodec_receive_frame(decoderContext, frame);
                        if (receiveRet >= 0) {
                            assertTrue("Decoded MP4 frame width is missing", frame.width() > 0);
                            assertTrue("Decoded MP4 frame height is missing", frame.height() > 0);
                            decodedFrame = true;
                        } else {
                            assertTrue("MP4 decoder failed before producing a frame (error "
                                            + receiveRet + ")",
                                    receiveRet == AVERROR_EAGAIN() || receiveRet == AVERROR_EOF);
                        }
                    }
                    av_packet_unref(packet);
                }
                assertTrue("Output contains no readable video packets", foundVideoPacket);
                assertTrue("MP4 output did not decode a video frame", decodedFrame);
            } finally {
                av_frame_free(frame);
                av_packet_free(packet);
                avcodec_free_context(decoderContext);
            }
        } finally {
            if (context != null && !context.isNull()) {
                avformat_close_input(context);
            }
        }
    }

    @Test
    public void testHlsFileOutput() throws Exception {
        MpegTsProcessor mpegTsProcessor = driver.mpegTsProcessor;

        Optional<IStreamingControlInterface> fileControl = driver.getCommandInputs().values().stream().filter(c -> c instanceof HLSControl<?>).findFirst();
        if (fileControl.isPresent()) {
            HLSControl<?> hlsControl = (HLSControl<?>) fileControl.get();

            // --------- Start Stream ---------
            var struct = hlsControl.getCommandDescription().clone();
            struct.renewDataBlock();
            ((Category)struct.getComponent(0)).setValue(HLSControl.CMD_START_STREAM);

            // Submit and finish the start command from an inline packet observer immediately
            // after the first non-key packet following an IDR. The demuxer cannot advance to the
            // next packet until openFile returns, making this a deterministic mid-GOP start even
            // for an unpaced test source.
            var startStatus = startHlsMidGop(mpegTsProcessor, hlsControl, struct);
            assertEquals("HLS start command failed: " + startStatus.getMessage(),
                    ICommandStatus.CommandStatusCode.ACCEPTED, startStatus.getStatusCode());
            assertNotNull("HLS start command did not return a manifest", startStatus.getResult());
            try {
                var commandOutputData = startStatus.getResult().getInlineRecords().stream().findFirst();
                assertTrue(commandOutputData.isPresent());

                String filePath = commandOutputData.get().getStringValue();
                Path manifestPath = tempDirectory.resolve(filePath);
                if (Files.exists(manifestPath)) {
                    assertTrue("HLS start exposed an empty placeholder manifest",
                            Files.size(manifestPath) > 0);
                }
                Path segmentPath = waitForHlsSegment(manifestPath, 15_000L);
                assertTrue("HLS did not drop the mid-GOP packets preceding the next keyframe",
                        hlsControl.getFileOutput().getPacketsDroppedBeforeInitialKeyFrame() > 0);
                assertTrue("HLS manifest does not declare independent media segments",
                        Files.readString(manifestPath).contains("#EXT-X-INDEPENDENT-SEGMENTS"));
                assertIndependentlyDecodableHlsSegment(segmentPath);
            } finally {
                // --------- End Stream ---------
                ((Category)struct.getComponent(0)).setValue(HLSControl.CMD_END_STREAM);
                var stopStatus = hlsControl.submitCommand(new CommandData.Builder()
                        .withCommandStream(BigId.NONE)
                        .withId(BigId.NONE)
                        .withParams(struct.getData())
                        .build()).join();
                assertEquals("HLS stop command failed: " + stopStatus.getMessage(),
                        ICommandStatus.CommandStatusCode.ACCEPTED, stopStatus.getStatusCode());
            }
        } else {
            fail("HLSControl not found");
        }
    }

    private static ICommandStatus startHlsMidGop(
            MpegTsProcessor processor, HLSControl<?> hlsControl, DataComponent startCommand) {
        CountDownLatch startFinished = new CountDownLatch(1);
        AtomicBoolean keySeen = new AtomicBoolean(false);
        AtomicBoolean startSubmitted = new AtomicBoolean(false);
        AtomicReference<ICommandStatus> startStatus = new AtomicReference<>();
        AtomicReference<Throwable> startFailure = new AtomicReference<>();
        DataBufferListener observer = new DataBufferListener() {
            @Override
            public void onDataBuffer(DataBufferRecord record) {
                if (record.isKeyFrame()) {
                    keySeen.set(true);
                } else if (keySeen.get() && startSubmitted.compareAndSet(false, true)) {
                    try {
                        startStatus.set(hlsControl.submitCommand(new CommandData.Builder()
                                .withCommandStream(BigId.NONE)
                                .withId(BigId.NONE)
                                .withParams(startCommand.getData().clone())
                                .build()).join());
                    } catch (Throwable e) {
                        startFailure.set(e);
                    } finally {
                        startFinished.countDown();
                    }
                }
            }

            @Override
            public boolean isWriting() {
                return true;
            }

            @Override
            public String getName() {
                return "HlsMidGopObserver";
            }
        };

        processor.addVideoDataBufferListener(observer, false);
        try {
            assertTrue("Timed out starting HLS from a non-key packet after an IDR",
                    startFinished.await(5, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while synchronizing the HLS mid-GOP start", e);
        } finally {
            processor.removeVideoDataBufferListener(observer);
        }

        if (startFailure.get() != null) {
            throw new AssertionError("HLS mid-GOP start failed", startFailure.get());
        }
        assertNotNull("HLS mid-GOP start returned no status", startStatus.get());
        return startStatus.get();
    }

    /**
     * A standalone HLS fragment must carry dimensions/codec configuration, begin with a keyframe,
     * and produce a decoded frame. Merely parsing transport packets is not sufficient: browsers
     * can parse a mid-GOP fragment but remain permanently at 0:00 because it is not decodable.
     */
    private static void assertIndependentlyDecodableHlsSegment(Path segmentPath) {
        AVFormatContext formatContext = new AVFormatContext(null);
        AVCodecContext decoderContext = null;
        AVPacket packet = null;
        AVFrame frame = null;
        int openRet = avformat_open_input(formatContext, segmentPath.toString(), null, null);
        try {
            assertTrue("FFmpeg could not open HLS segment (error " + openRet + "): " + segmentPath,
                    openRet >= 0);
            int infoRet = avformat_find_stream_info(formatContext, (PointerPointer<?>) null);
            assertTrue("FFmpeg could not read HLS stream metadata (error " + infoRet + ")",
                    infoRet >= 0);

            int videoStreamIndex = -1;
            for (int i = 0; i < formatContext.nb_streams(); i++) {
                var codecParameters = formatContext.streams(i).codecpar();
                if (codecParameters.codec_type() == AVMEDIA_TYPE_VIDEO) {
                    assertTrue("HLS video width is missing", codecParameters.width() > 0);
                    assertTrue("HLS video height is missing", codecParameters.height() > 0);
                    assertTrue("HLS video codec is missing", codecParameters.codec_id() != AV_CODEC_ID_NONE);
                    videoStreamIndex = i;

                    AVCodec decoder = avcodec_find_decoder(codecParameters.codec_id());
                    assertNotNull("No decoder is available for the HLS video codec", decoder);
                    assertFalse("No decoder is available for the HLS video codec", decoder.isNull());
                    decoderContext = avcodec_alloc_context3(decoder);
                    assertNotNull("FFmpeg could not allocate an HLS decoder", decoderContext);
                    assertFalse("FFmpeg could not allocate an HLS decoder", decoderContext.isNull());
                    assertTrue("Could not copy HLS codec parameters into decoder",
                            avcodec_parameters_to_context(decoderContext, codecParameters) >= 0);
                    assertTrue("Could not open HLS video decoder",
                            avcodec_open2(decoderContext, decoder, (PointerPointer<?>) null) >= 0);
                    break;
                }
            }
            assertTrue("HLS segment contains no video stream", videoStreamIndex >= 0);

            packet = av_packet_alloc();
            frame = av_frame_alloc();
            assertNotNull("FFmpeg could not allocate an HLS validation packet", packet);
            assertFalse("FFmpeg could not allocate an HLS validation packet", packet.isNull());
            assertNotNull("FFmpeg could not allocate an HLS validation frame", frame);
            assertFalse("FFmpeg could not allocate an HLS validation frame", frame.isNull());

            boolean firstVideoPacket = true;
            boolean decodedFrame = false;
            while (!decodedFrame && av_read_frame(formatContext, packet) >= 0) {
                if (packet.stream_index() == videoStreamIndex) {
                    if (firstVideoPacket) {
                        assertTrue("The first HLS video packet is not a keyframe",
                                (packet.flags() & AV_PKT_FLAG_KEY) != 0);
                        firstVideoPacket = false;
                    }

                    int sendRet = avcodec_send_packet(decoderContext, packet);
                    assertTrue("HLS decoder rejected a packet (error " + sendRet + ")", sendRet >= 0);
                    int receiveRet = avcodec_receive_frame(decoderContext, frame);
                    if (receiveRet >= 0) {
                        assertTrue("Decoded HLS frame width is missing", frame.width() > 0);
                        assertTrue("Decoded HLS frame height is missing", frame.height() > 0);
                        decodedFrame = true;
                    } else {
                        assertTrue("HLS decoder failed before producing a frame (error "
                                        + receiveRet + ")",
                                receiveRet == AVERROR_EAGAIN() || receiveRet == AVERROR_EOF);
                    }
                }
                av_packet_unref(packet);
            }

            assertFalse("HLS segment contains no video packets", firstVideoPacket);
            assertTrue("HLS segment did not decode a video frame", decodedFrame);
        } finally {
            if (frame != null && !frame.isNull()) {
                av_frame_free(frame);
            }
            if (packet != null && !packet.isNull()) {
                av_packet_free(packet);
            }
            if (decoderContext != null && !decoderContext.isNull()) {
                avcodec_free_context(decoderContext);
            }
            if (formatContext != null && !formatContext.isNull()) {
                avformat_close_input(formatContext);
            }
        }
    }

    private static Path waitForHlsSegment(Path manifestPath, long timeoutMillis) {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
        IOException lastReadFailure = null;
        while (System.nanoTime() < deadline) {
            try {
                if (Files.exists(manifestPath)) {
                    Optional<String> segmentName = Files.readAllLines(manifestPath).stream()
                            .map(String::trim)
                            .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                            .findFirst();
                    if (segmentName.isPresent()) {
                        Path segmentPath = manifestPath.getParent().resolve(segmentName.get()).normalize();
                        if (Files.exists(segmentPath) && Files.size(segmentPath) > 0) {
                            return segmentPath;
                        }
                    }
                }
            } catch (IOException e) {
                // The HLS muxer atomically replaces the manifest; retry a transient read race.
                lastReadFailure = e;
            }

            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError("Interrupted while waiting for an HLS segment", e);
            }
        }

        AssertionError failure = new AssertionError(
                "HLS manifest contains no completed media segment after " + timeoutMillis
                        + " ms: " + manifestPath);
        if (lastReadFailure != null) {
            failure.addSuppressed(lastReadFailure);
        }
        throw failure;
    }
}
