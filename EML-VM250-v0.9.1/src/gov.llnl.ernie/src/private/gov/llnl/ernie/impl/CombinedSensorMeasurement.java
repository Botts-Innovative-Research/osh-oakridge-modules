/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.ernie.impl;

import gov.llnl.math.IntegerArray;
import java.io.Serializable;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import gov.llnl.ernie.data.SensorProperties;
import gov.llnl.ernie.data.SensorMeasurement;

//</editor-fold>
//<editor-fold desc="combined" defaultstate="collapsed">
/**
 * Internal view for combined panel data.
 */
public class CombinedSensorMeasurement implements SensorMeasurement, Serializable
{
  private static final long serialVersionUID = gov.llnl.utility.UUIDUtilities.createLong("CombinedSensorMeasurement-v1");
  SensorProperties properties;
  List<SensorMeasurement> measurements;

  public CombinedSensorMeasurement(SensorProperties properties, List<? extends SensorMeasurement> measurements)
  {
    this.properties = properties;
    this.measurements = Collections.unmodifiableList(measurements);
  }

  @Override
  public SensorProperties getSensorProperties()
  {
    return properties;
  }

  @Override
  public Instant getTime(int sampleIndex) throws ArrayIndexOutOfBoundsException
  {
    return measurements.get(0).getTime(sampleIndex);
  }

  @Override
  public int getCounts(int sampleIndex) throws ArrayIndexOutOfBoundsException
  {
    int out = 0;
    for (SensorMeasurement panel : measurements)
    {
      out += panel.getCounts(sampleIndex);
    }
    return out;
  }

  @Override
  public int[] getCountsRange(int sampleIndexBegin, int sampleIndexEnd) throws ArrayIndexOutOfBoundsException
  {
    int[] out = null;
    for (SensorMeasurement panel : measurements)
    {
      if (out == null)
      {
        out = panel.getCountsRange(sampleIndexBegin, sampleIndexEnd).clone();
      }
      else
      {
        IntegerArray.addAssign(out, panel.getCountsRange(sampleIndexBegin, sampleIndexEnd));
      }
    }
    return out;
  }

  @Override
  public int[] getSpectrum(int sampleIndex) throws ArrayIndexOutOfBoundsException
  {
    int[] out = null;
    for (SensorMeasurement panel : measurements)
    {
      if (out == null)
      {
        out = panel.getSpectrum(sampleIndex).clone();
      }
      else
      {
        IntegerArray.addAssign(out, panel.getSpectrum(sampleIndex));
      }
    }
    return out;
  }

  @Override
  public int[][] getSpectrumRange(int sampleIndexBegin, int sampleIndexEnd) throws ArrayIndexOutOfBoundsException
  {
    int len = sampleIndexEnd - sampleIndexBegin;
    int[][] out = new int[len][];
    for (int i = 0; i < len; ++i)
    {
      out[i] = new int[this.getSensorProperties().getNumberOfChannels()];
      for (SensorMeasurement panel : measurements)
      {
        IntegerArray.addAssign(out[i], panel.getSpectrum(i + sampleIndexBegin));
      }
    }
    return out;
  }

  @Override
  public int[] getSpectrumSum(int sampleIndexBegin, int sampleIndexEnd) throws ArrayIndexOutOfBoundsException
  {
    int[] out = null;
    for (SensorMeasurement panel : measurements)
    {
      if (out == null)
      {
        out = panel.getSpectrumSum(sampleIndexBegin, sampleIndexEnd).clone();
      }
      else
      {
        IntegerArray.addAssign(out, panel.getSpectrumSum(sampleIndexBegin, sampleIndexEnd));
      }
    }
    return out;
  }

  @Override
  public int size()
  {
    return measurements.get(0).size();
  }

  @Override
  public int getOccupancyStart()
  {
    return measurements.get(0).getOccupancyStart();
  }

  @Override
  public int getOccupancyEnd()
  {
    return measurements.get(0).getOccupancyEnd();
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