/*******************************************************************************
 * Copyright (c) 2026 Botts Innovative Research, Inc.
 * All rights reserved.
 *******************************************************************************/
package com.botts.impl.process.ocr.pipeline;

import org.sensorhub.impl.utils.rad.model.VehicleOcrResult;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Asynchronous background OCR job descriptor for one completed lane occupancy.
 */
public record OcrJob(
        String laneUid,
        String initialOccupancyObsId,
        Supplier<String> occupancyObsIdResolver,
        double occupancyStartTime,
        double occupancyEndTime,
        double resultTimestamp,
        List<CameraClip> clips,
        OcrSettings settings,
        Consumer<VehicleOcrResult> resultConsumer,
        Runnable cleanupCallback
) {
    private static final Runnable NOOP_CLEANUP = () -> {};

    public OcrJob {
        if (cleanupCallback == null) {
            cleanupCallback = NOOP_CLEANUP;
        }
    }

    public OcrJob(
            String laneUid,
            String initialOccupancyObsId,
            Supplier<String> occupancyObsIdResolver,
            double occupancyStartTime,
            double occupancyEndTime,
            double resultTimestamp,
            List<CameraClip> clips,
            OcrSettings settings,
            Consumer<VehicleOcrResult> resultConsumer
    ) {
        this(laneUid, initialOccupancyObsId, occupancyObsIdResolver,
                occupancyStartTime, occupancyEndTime, resultTimestamp,
                clips, settings, resultConsumer, NOOP_CLEANUP);
    }

    public record CameraClip(String cameraUid, Path videoPath) {}
}
