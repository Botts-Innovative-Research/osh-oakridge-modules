/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.ernie.vm250;

import gov.llnl.ernie.analysis.StandardFeatures;
import gov.llnl.ernie.analysis.FeaturesDescription;

/**
 *
 * @author nelson85
 */
public class VM250StandardFeatures implements StandardFeatures
{
  private static final long serialVersionUID = gov.llnl.utility.UUIDUtilities.createLong("VM250StandardFeatures-v1");
  private final VM250StandardFeaturesDescription description;

  public double[] grossCountMetric;
  public double maxGrossCountMetric;
  public double[] maxCounts;
  public int N1;

  public VM250StandardFeatures(VM250StandardFeaturesDescription description)
  {
    int nsensors = description.getPanels();
    maxCounts = new double[nsensors + 1];
    grossCountMetric = new double[nsensors + 1];
    this.description = description;
  }

  @Override
  public FeaturesDescription getDescription()
  {
    return description;
  }

  @Override
  public double getMaxEnergyRatioMetric()
  {
    throw new UnsupportedOperationException("VM250 does not support energy ratios.");
  }

  @Override
  public void setMaxEnergyRatioMetric(double maxEnergyRatioMetric)
  {
    throw new UnsupportedOperationException("VM250 does not support energy ratios.");
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