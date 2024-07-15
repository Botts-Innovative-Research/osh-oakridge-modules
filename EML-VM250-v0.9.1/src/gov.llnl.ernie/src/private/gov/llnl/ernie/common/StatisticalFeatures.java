/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.ernie.common;

import gov.llnl.ernie.analysis.Features;
import gov.llnl.ernie.analysis.FeaturesDescription;

/**
 *
 * @author nelson85
 */
public class StatisticalFeatures implements Features
{
  private static final long serialVersionUID
          = gov.llnl.utility.UUIDUtilities.createLong("StatisticalFeatures2-v1");

  public StatisticalFeaturesDescription description;
  public double[] allIntensityStats = new double[5];
  public double[] peakIntensityStats = new double[5];
  public double[] peakPositionStats = new double[4];
  public PeakFeatures allPeakFeatures;

  public double[] frontIntensityStats = new double[5];
  public double[] frontPositionStats = new double[4];
  public PeakFeatures frontPeakFeatures;

  public double[] rearIntensityStats = new double[5];
  public double[] rearPositionStats = new double[4];
  public PeakFeatures rearPeakFeatures;
  public int split;
  public double splitDistance;
  public double splitIntensity;
  public double splitDeltaWidth;  // front+back-peak

  public double spreadX;
  public double spread3d;
  public double spreadI;
  public double splitDip;
  
  public double IStdDevIMeanRatio;

  public StatisticalFeatures(StatisticalFeaturesDescription description)
  {
    this.description = description;
  }

  @Override
  public FeaturesDescription getDescription()
  {
    return this.description;
  }

  public double getPeakIntensity()
  {
    return peakIntensityStats[0];
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