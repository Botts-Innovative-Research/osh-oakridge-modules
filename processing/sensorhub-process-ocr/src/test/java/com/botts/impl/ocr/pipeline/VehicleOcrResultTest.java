package com.botts.impl.ocr.pipeline;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataRecord;
import org.junit.Test;
import org.sensorhub.impl.utils.rad.RADHelper;
import org.sensorhub.impl.utils.rad.model.VehicleOcrResult;

import java.time.Instant;

import static org.junit.Assert.*;

public class VehicleOcrResultTest {

    @Test
    public void roundTripsThroughDataBlock() {
        Instant sampleTime = Instant.parse("2026-07-11T12:34:56Z");
        Instant frameTime = Instant.parse("2026-07-11T12:34:42Z");

        VehicleOcrResult original = new VehicleOcrResult.Builder()
                .sampleTime(sampleTime)
                .occupancyObsId("dGVzdC1vY2N1cGFuY3k")
                .idType(VehicleOcrResult.ID_TYPE_CONTAINER)
                .value("CSQU 305438 3")
                .normalizedValue("CSQU3054383")
                .checksumValid(true)
                .confidence(0.93f)
                .readCount(5)
                .cameraUid("urn:osh:sensor:ffmpeg:lane:L1:0")
                .frameTime(frameTime)
                .evidenceImagePath("ocr/crops/lane-L1/2026-07-11_12-34-40_container_0.jpg")
                .build();

        DataBlock dataBlock = VehicleOcrResult.fromVehicleOcrResult(original);
        VehicleOcrResult parsed = VehicleOcrResult.toVehicleOcrResult(dataBlock);

        assertEquals(sampleTime, parsed.getSampleTime());
        assertEquals(original.getOccupancyObsId(), parsed.getOccupancyObsId());
        assertEquals(original.getIdType(), parsed.getIdType());
        assertEquals(original.getValue(), parsed.getValue());
        assertEquals(original.getNormalizedValue(), parsed.getNormalizedValue());
        assertTrue(parsed.isChecksumValid());
        assertEquals(original.getConfidence(), parsed.getConfidence(), 1e-6);
        assertEquals(original.getReadCount(), parsed.getReadCount());
        assertEquals(original.getCameraUid(), parsed.getCameraUid());
        assertEquals(frameTime, parsed.getFrameTime());
        assertEquals(original.getEvidenceImagePath(), parsed.getEvidenceImagePath());
    }

    @Test
    public void dataBlockAtomCountMatchesRecordSchema() {
        // the PostGIS JSON serializer walks the record structure over the block's
        // atoms, so a size mismatch corrupts stored observations
        DataRecord record = new RADHelper().createVehicleOcrRecord();
        DataBlock schemaBlock = record.createDataBlock();

        DataBlock dataBlock = VehicleOcrResult.fromVehicleOcrResult(new VehicleOcrResult.Builder()
                .sampleTime(Instant.now())
                .occupancyObsId("abc")
                .idType(VehicleOcrResult.ID_TYPE_PLATE)
                .value("XYZ 7890")
                .normalizedValue("XYZ7890")
                .checksumValid(false)
                .confidence(0.7f)
                .readCount(2)
                .cameraUid("cam")
                .frameTime(Instant.now())
                .evidenceImagePath("")
                .build());

        assertEquals(schemaBlock.getAtomCount(), dataBlock.getAtomCount());
    }

    @Test
    public void toleratesNullOptionalStrings() {
        DataBlock dataBlock = VehicleOcrResult.fromVehicleOcrResult(new VehicleOcrResult.Builder()
                .sampleTime(Instant.now())
                .idType(VehicleOcrResult.ID_TYPE_PLATE)
                .confidence(0.5f)
                .readCount(1)
                .build());

        VehicleOcrResult parsed = VehicleOcrResult.toVehicleOcrResult(dataBlock);
        assertEquals("", parsed.getOccupancyObsId());
        assertEquals("", parsed.getValue());
        assertEquals("", parsed.getCameraUid());
        assertEquals("", parsed.getEvidenceImagePath());
    }
}
