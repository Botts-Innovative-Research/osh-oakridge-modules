/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.ernie.timeprofiler;

import gov.llnl.ernie.analysis.Features;
import gov.llnl.ernie.analysis.FeaturesDescription;
import gov.llnl.math.euclidean.Vector3;
import gov.llnl.utility.UUIDUtilities;


/**
 *
 * @author chandrasekar1
 */
public class TimeProfilerSource implements Features
{
  private static final long serialVersionUID = UUIDUtilities.createLong("TimeProfilerSource-v1");
  
  private FeaturesDescription description;

  // List of all the features associated with one source
  // intensity
  // x  delta_x, std_x, skew_x kert_x
  // y
  // audit
  //  time profile by panel
  double x1, x2;
  public Vector3 position; // where is the source located (m)
  double intensity;

  @Override
  public FeaturesDescription getDescription()
  {
    return this.description;
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