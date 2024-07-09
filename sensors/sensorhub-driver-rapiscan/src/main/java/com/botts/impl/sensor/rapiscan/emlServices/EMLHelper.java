package com.botts.impl.sensor.rapiscan.emlServices;

import com.botts.impl.sensor.rapiscan.MessageHandler;
import com.botts.impl.sensor.rapiscan.emlServices.types.AlgorithmType;

import java.util.Arrays;
import java.util.EnumSet;

public class EMLHelper {

    MessageHandler messageHandler;

    public EMLHelper(MessageHandler messageHandler) {
        this.messageHandler = messageHandler;


    }

    /**
     * Calculation of gamma threshold for VM250 RPM Gamma Alarm
     * @return
     */
    public double getGammaThreshold() {
        // TODO: Have as occupancy output
        // Get NSigma value from gamma setup 1 data record
        float nSigma = messageHandler.getGammaSetUp1Output().getLatestRecord().getFloatValue(6);
        // Get Algorithm value from gamma setup 2 data record
        String algorithmData = messageHandler.getGammaSetUp2Output().getLatestRecord().getStringValue(6);
        // Algorithm EnumSet includes algorithm type (example: 1010 = SUM, VERTICAL)
        EnumSet<AlgorithmType> algorithmSet = EnumSet.noneOf(AlgorithmType.class);

        for (int i = 0; i < 4; i++) {
            if(algorithmData.charAt(i) == '1') {
                algorithmSet.add(AlgorithmType.values()[i]);
            }
        }

        // TODO: Only sum # of detectors based on algorithm AND whether all 4 detectors are on
        int[] gammaCounts = new int[4];

        // TODO: Somehow ensure that we get latest background counts
        if(messageHandler.getGammaOutput().getLatestRecord().getStringValue(2) == "Background") {
            for(int i = 0; i < 4; i++) {
                gammaCounts[i] = messageHandler.getGammaOutput().getLatestRecord().getIntValue(i+3);
            }
        }

//        double averageGB = Arrays.stream(gammaCounts).average().orElse(Double.NaN);
        int sumGB = Arrays.stream(gammaCounts).sum();
        double sqrtSumGB = Math.sqrt(sumGB);

        // Return equation for threshold calculation
        return sumGB + (nSigma * sqrtSumGB);
    }

}
