/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.ernie.spectral;

import gov.llnl.ernie.analysis.Features;
import gov.llnl.ernie.analysis.FeaturesDescription;

/**
 *
 * @author nelson85
 */
public class EnergyFeatures implements Features
{
  private static final long serialVersionUID 
          = gov.llnl.utility.UUIDUtilities.createLong("EnergyFeatures-v1");

  EnergyFeaturesDescription description;
  double[] pcaFeatures;
  double[] ratioFeatures;
  double[] hypothesisFeatures;
  double k;
  double background;

  EnergyFeatures(EnergyFeaturesDescription description)
  {
    this.description = description;
    this.pcaFeatures = new double[description.nPcaFeatures];
    this.ratioFeatures = new double[description.nRatioFeatures];
    this.hypothesisFeatures = new double[description.nHypothesisFeatures];
  }

  @Override
  public FeaturesDescription getDescription()
  {
    return description;
  }

  /**
   * @return the pcaFeatures
   */
  public double[] getPcaFeatures()
  {
    return pcaFeatures;
  }

  /**
   * @return the ratioFeatures
   */
  public double[] getRatioFeatures()
  {
    return ratioFeatures;
  }

  /**
   * @return the hypothesisFeatures
   */
  public double[] getHypothesisFeatures()
  {
    return hypothesisFeatures;
  }

  /**
   * @return the k
   */
  public double getK()
  {
    return k;
  }

  /**
   * @return the background
   */
  public double getBackground()
  {
    return background;
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