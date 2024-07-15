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
import gov.llnl.math.DoubleArray;
import gov.llnl.math.matrix.MatrixViews;
import gov.llnl.utility.UUIDUtilities;
import gov.llnl.utility.annotation.Debug;
import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;

//</editor-fold>
public class VM250GammaPanelBackground implements SensorBackground, Serializable
{
  private static final long serialVersionUID = UUIDUtilities.createLong("VM250GammaPanelBackground-v1");

  final int panel;
  private final VM250Record outer;
  private final VM250RecordInternal internalRecord;

  public VM250GammaPanelBackground(int panel, final VM250Record outer)
  {
    this.outer = outer;
    this.panel = panel;
    this.internalRecord = (VM250RecordInternal) outer.getInternal();
  }

  @Override
  public SensorProperties getSensorProperties()
  {
    return internalRecord.getLane().getGammaSensorProperties().get(panel);
  }

  @Override
  public int size()
  {
    assertComputed();
    return outer.gammaBackgroundGross.rows();
  }

  @Override
  public int getOccupancyStart()
  {
    return 0;
  }

  @Override
  public int getOccupancyEnd()
  {
    return outer.getInternal().panelData[0].size_g_;
  }

  @Override
  public double getExpectedCountRate()
  {
    return outer.gammaBackgroundPanelRate[panel];
  }

  @Override
  public double[] getExpectedSpectrum()
  {
    return outer.gammaBackgroundSpectrum.copyColumn(panel);
  }

  @Override
  public Instant getTime(int sampleIndex) throws ArrayIndexOutOfBoundsException
  {
    return internalRecord.getSegmentDescription().getRpmDateTime().plus(Duration.ofMillis(100 * sampleIndex));
  }

  @Override
  public double getCounts(int sampleIndex) throws ArrayIndexOutOfBoundsException
  {
    return outer.gammaBackgroundGross.get(sampleIndex, panel);
  }

  @Override
  public double[] getCountsRange(int sampleIndexBegin, int sampleIndexEnd) throws ArrayIndexOutOfBoundsException
  {
    assertComputed();
    return MatrixViews.select(outer.gammaBackgroundGross, sampleIndexBegin, sampleIndexEnd, panel, panel + 1).flatten();
  }

  @Override
  public double getCountsSum(int sampleIndexBegin, int sampleIndexEnd) throws ArrayIndexOutOfBoundsException
  {
    assertComputed();
    double c = 0;
    for (int i = sampleIndexBegin; i < sampleIndexEnd; ++i)
    {
      c += outer.gammaBackgroundGross.get(i, panel);
    }
    return c;
  }

  @Debug
  public double computeSurpression(int sampleIndex)
  {
    double baseline = outer.gammaBackgroundPanelRate[panel];
    double expected = outer.gammaBackgroundGross.get(sampleIndex, panel);
    return expected / baseline; // FIXME check units
  }

  @Override
  public double[] getSpectrum(int sampleIndex) throws ArrayIndexOutOfBoundsException
  {
    assertComputed();
    // Each portal should have its own suppression model that defines how
    // the energy in the panel changes as a function of the observed suppression.
    // Thus this should be computed from factors that define the lane.
    double fraction = computeSurpression(sampleIndex);
    return computeExpectedSpectrumSuppressed(fraction, this.getSensorProperties().getSamplePeriodSeconds());
  }

  @Override
  public double[] computeExpectedSpectrumSuppressed(double fraction, double time)
  {
    double[] backgroundRateEstimate = getExpectedSpectrum();
    double[] energyFactors = getSensorProperties().getEnergyFactors();
    double dt = this.getSensorProperties().getSamplePeriodSeconds();
    double s0 = 0;
    double s1 = 0;
    for (int i = 0; i < 9; ++i)
    {
      s0 += backgroundRateEstimate[i];
      backgroundRateEstimate[i] = ((energyFactors[i] * (fraction - 1) + 1) * backgroundRateEstimate[i]);
      s1 += backgroundRateEstimate[i];
    }
    DoubleArray.multiplyAssign(backgroundRateEstimate, time * s0 * fraction / s1); // FIXME uncomment once diffs understood
    return backgroundRateEstimate;
  }

  @Override
  public double[][] getSpectrumRange(int sampleIndexBegin, int sampleIndexEnd) throws ArrayIndexOutOfBoundsException
  {
    assertComputed();
    int len = sampleIndexEnd - sampleIndexBegin;
    double[][] out = new double[len][];
    for (int i = 0; i < len; ++i)
    {
      out[i] = getSpectrum(i + sampleIndexBegin);
    }
    return out;
  }

  @Override
  public double[] getSpectrumSum(int sampleIndexBegin, int sampleIndexEnd) throws ArrayIndexOutOfBoundsException
  {
    assertComputed();
    int len = sampleIndexEnd - sampleIndexBegin;
    double[] out = new double[9];
    for (int i = 0; i < len; ++i)
    {
      DoubleArray.addAssign(out, getSpectrum(i + sampleIndexBegin));
    }
    return out;
  }

  private void assertComputed()
  {
    if (outer.gammaBackgroundGross == null)
    {
      throw new RuntimeException("Background is not computed");
    }
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