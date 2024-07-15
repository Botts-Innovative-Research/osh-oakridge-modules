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

import gov.llnl.ernie.impl.FeaturesDescriptionImpl;
import java.util.ArrayList;
import java.util.List;

/**
 * Marshaller for MotionInfo to and from the database.
 *
 * @author mattoon1
 */
public class MotionInfoDescription extends FeaturesDescriptionImpl<MotionInfo>
{
  private static final List<FeatureDescription> FEATURE_LIST = new ArrayList<>();

  static
  {
    FeatureDescriptionBuilder builder = newBuilder(FEATURE_LIST);
    builder.add("InitialVelocity", Double.class,
            (MotionInfo c) -> c.getInitialVelocity(),
            (MotionInfo c, Double val) -> c.setInitialVelocity(val));
    builder.add("InitialAcceleration", Double.class,
            (MotionInfo c) -> c.getInitialAcceleration(),
            (MotionInfo c, Double val) -> c.setInitialAcceleration(val));
    builder.add("Jerk", Double.class,
            (MotionInfo c) -> c.getJerk(),
            (MotionInfo c, Double val) -> c.setJerk(val));
  }

  public MotionInfoDescription()
  {
    super("Motion", FEATURE_LIST);
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