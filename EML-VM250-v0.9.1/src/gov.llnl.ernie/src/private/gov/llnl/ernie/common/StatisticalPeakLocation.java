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
package gov.llnl.ernie.common;

import gov.llnl.ernie.data.Record;
import gov.llnl.ernie.data.SensorBackground;
import gov.llnl.ernie.data.SensorMeasurement;

/**
 * Helper function for determining the region to extract the peak.
 *
 * @author nelson85
 */
public class StatisticalPeakLocation
{
  final int peakTime;
  final int peakPanel;
  final double max;
  public int peakStartTime;
  public int peakEndTime;

  public static StatisticalPeakLocation findPeakLocation(Record record,
          int occupancyStart, int occupancyEnd, int minimumSamples)
  {
    int nsensors = record.getGammaBackgrounds().size();
    double max = 0;
    int peakTime = occupancyStart;
    int peakPanel = 0;
    for (int i0 = 0; i0 < nsensors; ++i0)
    {
      SensorMeasurement pd = record.getGammaMeasurements().get(i0);
      SensorBackground bd = record.getGammaBackgrounds().get(i0);
      int[] gross = pd.getCountsRange(0, pd.size());
      for (int i1 = occupancyStart; i1 < occupancyEnd; ++i1)
      {
        // Compute the counts above background
        double delta = gross[i1] - bd.getCounts(i1);
        if (max < delta)
        {
          max = delta;
          peakTime = i1;
          peakPanel = i0;
        }
      }
    }
    StatisticalPeakLocation out = new StatisticalPeakLocation(max, peakTime, peakPanel);
    out.getPeakExtent(record, occupancyStart, occupancyEnd, minimumSamples);
    return out;
  }

  public StatisticalPeakLocation(double max, int peakTime, int peakPanel)
  {
    this.max = max;
    this.peakTime = peakTime;
    this.peakPanel = peakPanel;
  }

  private void getPeakExtent(Record record, int start, int end, int minimumSamples)
  {
    int gammaLength = record.getGammaMeasurements().get(0).size();
    int lpeakStartTime = Math.max(this.peakTime - 4, start);
    int lpeakEndTime = Math.min(this.peakTime + 5, end);
    // Locate the start and end of the peak region
    {
      SensorMeasurement pd = record.getGammaMeasurements().get(this.peakPanel);
      SensorBackground bd = record.getGammaBackgrounds().get(this.peakPanel);
      int[] gross = pd.getCountsRange(0, pd.size());
      while (lpeakStartTime > start && gross[lpeakStartTime] - bd.getCounts(lpeakStartTime) > this.max / 4)
      {
        lpeakStartTime--;
      }
      while (lpeakEndTime < end && gross[lpeakEndTime - 1] - bd.getCounts(lpeakEndTime - 1) > this.max / 4)
      {
        lpeakEndTime++;
      }
    }
    
    // Impose a minimum.   
    // Trying to analyze a region with too little signal produces 
    // and indeterminate result, so try to pad out the required length.
    if (lpeakEndTime - lpeakStartTime < minimumSamples)
    {
      int violation = minimumSamples - (lpeakEndTime - lpeakStartTime);
      lpeakEndTime += violation / 2;
      lpeakStartTime -= (violation - violation / 2);
    }
    
    if (lpeakStartTime < 0)
    {
      lpeakStartTime = 0;
    }
    
    if (lpeakEndTime > gammaLength)
    {
      lpeakEndTime = gammaLength;
    }

    this.peakStartTime = lpeakStartTime;
    this.peakEndTime = lpeakEndTime;
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