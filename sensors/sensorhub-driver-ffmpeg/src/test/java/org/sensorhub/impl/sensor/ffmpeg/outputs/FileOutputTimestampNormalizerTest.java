package org.sensorhub.impl.sensor.ffmpeg.outputs;

import org.junit.Test;

import java.io.IOException;

import static org.bytedeco.ffmpeg.global.avutil.AV_NOPTS_VALUE;
import static org.junit.Assert.*;

public class FileOutputTimestampNormalizerTest {

    @Test
    public void preservesCompositionOffsetsAndRebasesTimestampReset() throws Exception {
        var normalizer = new FileOutput.TimestampNormalizer();

        var first = normalizer.normalize(300, 100, 40);
        assertEquals(200, first.pts());
        assertEquals(0, first.dts());
        assertEquals(200, first.pts() - first.dts());
        assertFalse(first.discontinuity());

        var bFrame = normalizer.normalize(260, 140, 40);
        assertEquals(120, bFrame.pts() - bFrame.dts());
        assertTrue(bFrame.dts() > first.dts());
        assertFalse(bFrame.discontinuity());

        // Simulate a camera reconnect that starts a new timestamp epoch below the last DTS.
        var reset = normalizer.normalize(25, -15, 40);
        assertTrue(reset.discontinuity());
        assertTrue(reset.dts() > bFrame.dts());
        assertEquals(40, reset.pts() - reset.dts());

        var afterReset = normalizer.normalize(65, 25, 40);
        assertFalse(afterReset.discontinuity());
        assertTrue(afterReset.dts() > reset.dts());
        assertEquals(40, afterReset.pts() - afterReset.dts());
    }

    @Test
    public void usesPtsWhenDtsIsUnavailable() throws Exception {
        var normalizer = new FileOutput.TimestampNormalizer();

        var first = normalizer.normalize(900, AV_NOPTS_VALUE, 30);
        assertEquals(0, first.pts());
        assertEquals(AV_NOPTS_VALUE, first.dts());

        var reset = normalizer.normalize(100, AV_NOPTS_VALUE, 30);
        assertTrue(reset.discontinuity());
        assertEquals(30, reset.pts());
        assertEquals(AV_NOPTS_VALUE, reset.dts());
    }

    @Test(expected = IOException.class)
    public void rejectsPacketWithoutAnyTimestamp() throws Exception {
        new FileOutput.TimestampNormalizer().normalize(AV_NOPTS_VALUE, AV_NOPTS_VALUE, 0);
    }
}
