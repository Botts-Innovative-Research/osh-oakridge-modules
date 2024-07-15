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

import gov.llnl.ernie.data.SensorBackground;
import gov.llnl.ernie.data.SensorProperties;
import gov.llnl.utility.UUIDUtilities;
import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;

/**
 *
 * @author mattoon1
 */
public class VM250NeutronPanelBackground implements SensorBackground, Serializable
{
  private static final long serialVersionUID = UUIDUtilities.createLong("VM250NeutronPanelBackground-v1");
  
  int panel;
  private final VM250Record outer;
  private final VM250RecordInternal recordInternal;

  VM250NeutronPanelBackground(int i, final VM250Record outer)
  {
    this.outer = outer;
    this.panel = i;
    recordInternal = (VM250RecordInternal) outer.getInternal();
  }

  @Override
  public SensorProperties getSensorProperties()
  {
    return recordInternal.getLane().getNeutronSensorProperties().get(panel);
  }

  @Override
  public int size()
  {
    return recordInternal.getPanelData()[0].size();
  }

  @Override
  public int getOccupancyStart()
  {
    return outer.getVPSMeasurement().getGammaOccupancyStart();
  }

  @Override
  public int getOccupancyEnd()
  {
    return outer.getVPSMeasurement().getGammaOccupancyEnd();
  }

  @Override
  public double getExpectedCountRate()
  {
    // FIXME this is copied from RPM8NeutronPanelBackground. Not sure what this should be
//    VM250SegmentResultInternal sr = outer.internal.combinedSegmentResult;
//    return sr.neutronBackground1 / 4;
    return -1.0;
  }

  @Override
  public double[] getExpectedSpectrum()
  {
    return new double[]{getExpectedCountRate()};
  }

  @Override
  public Instant getTime(int sampleIndex) throws ArrayIndexOutOfBoundsException
  {
    return recordInternal.getSegmentDescription().getRpmDateTime().plus(Duration.ofMillis(100 * sampleIndex));
  }

  @Override
  public double getCounts(int sampleIndex) throws ArrayIndexOutOfBoundsException
  {
    return this.getExpectedCountRate() * this.getSensorProperties().getSamplePeriodSeconds();
  }

  @Override
  public double getCountsSum(int sampleIndexBegin, int sampleIndexEnd) throws ArrayIndexOutOfBoundsException
  {
    return this.getExpectedCountRate() * this.getSensorProperties().getSamplePeriodSeconds() * (sampleIndexEnd - sampleIndexEnd);
  }

  @Override
  public double[] getCountsRange(int sampleIndexBegin, int sampleIndexEnd) throws ArrayIndexOutOfBoundsException
  {
    double[] out = new double[sampleIndexEnd - sampleIndexBegin];
    double counts = getExpectedCountRate() * this.getSensorProperties().getSamplePeriodSeconds();
    for (int i = 0; i < out.length; ++i)
    {
      out[i] = counts;
    }
    return out;
  }

  @Override
  public double[] getSpectrum(int sampleIndex) throws ArrayIndexOutOfBoundsException
  {
    return new double[]{getExpectedCountRate() * this.getSensorProperties().getSamplePeriodSeconds()};
  }

  @Override
  public double[][] getSpectrumRange(int sampleIndexBegin, int sampleIndexEnd) throws ArrayIndexOutOfBoundsException
  {
    double[][] out = new double[sampleIndexEnd - sampleIndexBegin][];
    double[] c = getSpectrum(0);
    for (int i = 0; i < out.length; ++i)
    {
      out[i] = c.clone();
    }
    return out;
  }

  @Override
  public double[] getSpectrumSum(int sampleIndexBegin, int sampleIndexEnd) throws ArrayIndexOutOfBoundsException
  {
    double[] out = getSpectrum(0);
    for (int i = 0; i < out.length; ++i)
    {
      out[i] *= sampleIndexEnd - sampleIndexBegin;
    }
    return out;
  }

  @Override
  public double[] computeExpectedSpectrumSuppressed(double fraction, double time)
  {
    throw new UnsupportedOperationException("computeExpectedSpectrumSuppressed: not supported yet.");
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