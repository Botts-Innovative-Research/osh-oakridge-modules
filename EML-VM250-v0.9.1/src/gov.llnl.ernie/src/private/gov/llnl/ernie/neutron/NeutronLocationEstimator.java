/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gov.llnl.ernie.neutron;

import gov.llnl.math.DoubleArray;
import java.time.Instant;
import gov.llnl.ernie.data.Record;
import java.util.List;
import gov.llnl.ernie.data.SensorMeasurement;
import gov.llnl.ernie.data.SensorPosition;
import gov.llnl.ernie.math.Utility;
import gov.llnl.math.euclidean.Vector3;

/**
 *
 * FIXME these values are computed but never used in the code.
 * 
 * @author mattoon1
 */
public class NeutronLocationEstimator
{
  /**
   * Estimate x,y,z location of neutron source.
   *
   * x location estimate is just the max neutron counts, converted to position
   *
   * logic for getting y/z location is similar to extracting MeanY and MeanZ in
   * FeatureExtractorStatistical, using sum of all counts rather than just the
   * peak portion
   *
   * @param record
   * @param result
   */
  public static Vector3 compute(Record record)
  {
    int nsensors = record.getLane().getNeutronSensorProperties().size();
    double[] panelData = new double[nsensors];
    List<SensorMeasurement> nm = record.getNeutronMeasurements();
    int[] allPanels = new int[nm.get(0).size()];

    for (int i = 0; i < nsensors; i++)
    {
      SensorMeasurement ndata = nm.get(i);
      for (int j = 0; j < ndata.size(); j++)
      {
        int neutronSum = ndata.getCounts(j);
        panelData[i] += neutronSum;
        allPanels[j] += neutronSum;
      }
    }
    // x location from allPanels
    int maxIndex = 0;
    int maxVal = 0;
    for (int idx = 0; idx < allPanels.length; idx++)
    {
      if (allPanels[idx] > maxVal)
      {
        maxVal = allPanels[idx];
        maxIndex = idx;
      }
    }
    Instant time = nm.get(0).getTime(maxIndex);
    double resultX = record.getVehicleMotion().getPosition(time);

    // FIXME assumes 4 panels with 2 on the left and 2 on the right
    double sumPanelData = DoubleArray.sum(panelData);
    double sumBottom = Utility.sum(panelData, record.getLane().getNeutronPanels(SensorPosition.BOTTOM));
    double sumTop = Utility.sum(panelData, record.getLane().getNeutronPanels(SensorPosition.TOP));
    double sumRight = Utility.sum(panelData, record.getLane().getNeutronPanels(SensorPosition.RIGHT));
    double sumLeft = Utility.sum(panelData, record.getLane().getNeutronPanels(SensorPosition.LEFT));
    double ratio1 = (sumBottom - sumTop) / (sumPanelData + 1);
    double ratio2 = (sumRight - sumLeft) / (sumPanelData + 1);
    Vector3 lp = record.getLane().convertNeutronRatios(ratio1, ratio2);
    double resultY = lp.getY();
    double resultZ = lp.getZ();
    return Vector3.of(resultX, resultY, resultZ);
  }

}


/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */