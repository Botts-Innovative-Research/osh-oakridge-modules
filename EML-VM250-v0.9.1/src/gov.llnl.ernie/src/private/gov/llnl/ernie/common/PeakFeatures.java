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

import gov.llnl.ernie.spectral.EnergyFeatures;
import gov.llnl.ernie.analysis.Features;
import gov.llnl.ernie.analysis.FeaturesDescription;

/**
 *
 * @author nelson85
 */
public class PeakFeatures implements Features
{
  private static final long serialVersionUID
          = gov.llnl.utility.UUIDUtilities.createLong("PeakFeatures-v1");
  private final FeaturesDescription description;

  double[] peakStatistics;
  EnergyFeatures energyFeatures;
  double[] sampleSpectrum;
  double[] backgroundSpectrum;
  double peakSignificance;

  // Audit
  transient public double ratio1;
  transient public double ratio2;
  transient public int peakStartTime;
  transient public int peakEndTime;
  transient public double[] panelData;
  transient public double[] panelDelta;
  transient public double[] panelBkg;

  PeakFeatures(FeaturesDescription description)
  {
    this.description = description;
    this.peakStatistics = new double[3];
  }

  @Override
  public FeaturesDescription getDescription()
  {
    return this.description;
  }

  /**
   * @return the peakStatistics
   */
  public double[] getPeakStatistics()
  {
    return peakStatistics;
  }

  /**
   * @return the energyFeatures
   */
  public EnergyFeatures getEnergyFeatures()
  {
    return energyFeatures;
  }

  /**
   * @return the sampleSpectrum
   */
  public double[] getSampleSpectrum()
  {
    return sampleSpectrum;
  }

  /**
   * @return the backgroundSpectrum
   */
  public double[] getBackgroundSpectrum()
  {
    return backgroundSpectrum;
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