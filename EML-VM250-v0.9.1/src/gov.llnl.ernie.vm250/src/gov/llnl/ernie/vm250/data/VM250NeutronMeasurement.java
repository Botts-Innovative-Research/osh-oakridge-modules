/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.ernie.vm250.data;

import gov.llnl.ernie.data.SensorMeasurement;
import gov.llnl.ernie.data.SensorProperties;
import gov.llnl.math.IntegerArray;
import gov.llnl.utility.UUIDUtilities;
import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;

/**
 *
 * @author guensche1
 */
class VM250NeutronMeasurement implements SensorMeasurement, Serializable
{
  private static final long serialVersionUID = UUIDUtilities.createLong("VM250NeutronMeasurement-v1");

  final int panelId;
// TODO move VM250RecordInternal.PanelData to its own class called VM250PanelDataInternal 
  private final VM250RecordInternal.PanelData panelData;
  private final VM250Record outer;
  private final VM250RecordInternal internalRecord;

  public VM250NeutronMeasurement(int panel, final VM250Record outer)
  {
    this.outer = outer;
    panelId = panel;
    internalRecord = (VM250RecordInternal) outer.getInternal();
    panelData = internalRecord.getPanelData()[panelId];
  }

  @Override
  public SensorProperties getSensorProperties()
  {
    return outer.getLane().getNeutronSensorProperties().get(panelId);
  }

  @Override
  public Instant getTime(int sampleIndex) throws ArrayIndexOutOfBoundsException
  {
    return internalRecord.getSegmentDescription().getRpmDateTime().plus(Duration.ofSeconds(sampleIndex));
  }

  @Override
  public int size()
  {
    return panelData.size_n_;
  }

  @Override
  public int getCounts(int sampleIndex) throws ArrayIndexOutOfBoundsException
  {
    return IntegerArray.sum(panelData.neutronData[sampleIndex]);
  }

  @Override
  public int[] getSpectrum(int sampleIndex) throws ArrayIndexOutOfBoundsException
  {
    int[] out = new int[1];
    out[0] = getCounts(sampleIndex);
    return out;
  }

  @Override
  public int[] getCountsRange(int sampleIndexBegin, int sampleIndexEnd) throws ArrayIndexOutOfBoundsException
  {
    int len = sampleIndexEnd - sampleIndexBegin;
    int[] out = new int[len];
    for (int i = 0; i < len; ++i)
    {
      out[i] = IntegerArray.sum(panelData.neutronData[i + sampleIndexBegin]);
    }
    return out;
  }

  @Override
  public int[][] getSpectrumRange(int sampleIndexBegin, int sampleIndexEnd) throws ArrayIndexOutOfBoundsException
  {
    int len = sampleIndexEnd - sampleIndexBegin;
    int[][] out = new int[len][1];
    int[] raw = getCountsRange(sampleIndexBegin, sampleIndexEnd);
    for (int i = 0; i < len; ++i)
    {
      out[i] = new int[]
      {
        raw[i]
      };
    }
    return out;
  }

  @Override
  public int[] getSpectrumSum(int sampleIndexBegin, int sampleIndexEnd) throws ArrayIndexOutOfBoundsException
  {
    int len = sampleIndexEnd - sampleIndexBegin;
    int out = 0;
    for (int i = 0; i < len; ++i)
    {
      out += IntegerArray.sum(panelData.neutronData[i + sampleIndexBegin]);
    }
    return new int[]
    {
      out
    };
  }

  @Override
  public int getOccupancyStart()
  {
    return outer.getVPSMeasurement().getNeutronOccupancyStart();
  }

  @Override
  public int getOccupancyEnd()
  {
    return outer.getVPSMeasurement().getNeutronOccupancyEnd();
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