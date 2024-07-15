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

import gov.llnl.math.DoubleArray;
import java.io.Serializable;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import gov.llnl.ernie.data.SensorProperties;
import gov.llnl.ernie.data.SensorBackground;

/**
 * Internal view for combined panel backgrounds.
 */
public class CombinedSensorBackground implements SensorBackground, Serializable
{
  private static final long serialVersionUID = gov.llnl.utility.UUIDUtilities.createLong("CombinedSensorBackground-v1");
  SensorProperties properties;
  List<SensorBackground> measurements;

  public CombinedSensorBackground(SensorProperties properties, List<? extends SensorBackground> measurements)
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
  public double getCounts(int sampleIndex) throws ArrayIndexOutOfBoundsException
  {
    double out = 0;
    for (SensorBackground panel : measurements)
    {
      out += panel.getCounts(sampleIndex);
    }
    return out;
  }

  @Override
  public double[] getCountsRange(int sampleIndexBegin, int sampleIndexEnd) throws ArrayIndexOutOfBoundsException
  {
    double[] out = null;
    for (SensorBackground panel : measurements)
    {
      if (out == null)
      {
        out = panel.getCountsRange(sampleIndexBegin, sampleIndexEnd).clone();
      }
      else
      {
        DoubleArray.addAssign(out, panel.getCountsRange(sampleIndexBegin, sampleIndexEnd));
      }
    }
    return out;
  }

  @Override
  public double[] getSpectrum(int sampleIndex) throws ArrayIndexOutOfBoundsException
  {
    double[] out = null;
    for (SensorBackground panel : measurements)
    {
      if (out == null)
      {
        out = panel.getSpectrum(sampleIndex).clone();
      }
      else
      {
        DoubleArray.addAssign(out, panel.getSpectrum(sampleIndex));
      }
    }
    return out;
  }

  @Override
  public double[][] getSpectrumRange(int sampleIndexBegin, int sampleIndexEnd) throws ArrayIndexOutOfBoundsException
  {
    int len = sampleIndexEnd - sampleIndexBegin;
    double[][] out = new double[len][];
    for (int i = 0; i < len; ++i)
    {
      out[i] = new double[this.getSensorProperties().getNumberOfChannels()];
      for (SensorBackground panel : measurements)
      {
        DoubleArray.addAssign(out[i], panel.getSpectrum(i + sampleIndexBegin));
      }
    }
    return out;
  }

  @Override
  public double[] getSpectrumSum(int sampleIndexBegin, int sampleIndexEnd) throws ArrayIndexOutOfBoundsException
  {
    double[] out = null;
    for (SensorBackground panel : measurements)
    {
      if (out == null)
      {
        out = panel.getSpectrumSum(sampleIndexBegin, sampleIndexEnd).clone();
      }
      else
      {
        DoubleArray.addAssign(out, panel.getSpectrumSum(sampleIndexBegin, sampleIndexEnd));
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
  public double getExpectedCountRate()
  {
    double out = 0;
    for (SensorBackground panel : measurements)
    {
      out += panel.getExpectedCountRate();
    }
    return out;
  }

  @Override
  public double[] getExpectedSpectrum()
  {
    double[] out = null;
    for (SensorBackground panel : measurements)
    {
      if (out == null)
      {
        out = panel.getExpectedSpectrum().clone();
      }
      else
      {
        DoubleArray.addAssign(out, panel.getExpectedSpectrum());
      }
    }
    return out;
  }

  @Override
  public double getCountsSum(int sampleIndexBegin, int sampleIndexEnd) throws ArrayIndexOutOfBoundsException
  {
    double out = 0;
    for (SensorBackground panel : measurements)
    {
      out += panel.getCountsSum(sampleIndexBegin, sampleIndexEnd);
    }
    return out;
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

  @Override
  public double[] computeExpectedSpectrumSuppressed(double fraction, double time)
  {
    double[] out = null;
    for (SensorBackground panel : measurements)
    {
      if (out == null)
      {
        out = panel.computeExpectedSpectrumSuppressed(fraction, time);
      }
      else
      {
        DoubleArray.addAssign(out, panel.computeExpectedSpectrumSuppressed(fraction, time));
      }
    }
    return out;
  }

}
//</editor-fold>


/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */