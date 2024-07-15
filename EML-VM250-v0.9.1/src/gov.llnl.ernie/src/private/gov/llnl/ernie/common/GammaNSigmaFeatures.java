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
public class GammaNSigmaFeatures implements Features
{
  private static final long serialVersionUID = gov.llnl.utility.UUIDUtilities.createLong("GammaNSigmaFeatures-v1");
  double[] master;
  double[] slave;
  double[] allPanels;
  double[] bgRatio;
  double maxAllPanels;
  double maxMaster;
  double maxSlave;
  double maxBgRatio;

  @Override
  public FeaturesDescription getDescription()
  {
    return GammaNSigmaFeatureExtractor.DESCRIPTION;
  }
  
  public double getMaxAllPanels()
  {
    return maxAllPanels;
  }
  
  public double getMaxMaster()
  {
    return maxMaster;
  }
  
  public double getMaxSlave()
  {
    return maxSlave;
  }
  
  public double getMaxBgRatio()
  {
    return maxBgRatio;
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