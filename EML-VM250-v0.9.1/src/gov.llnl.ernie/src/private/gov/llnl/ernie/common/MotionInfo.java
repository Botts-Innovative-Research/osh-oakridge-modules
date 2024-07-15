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
 * Data stored for training machine learning containing truth information about
 * a record.
 *
 * @author mattoon1
 */
public class MotionInfo implements Features, Cloneable
{
  public final static MotionInfoDescription DESCRIPTION = new MotionInfoDescription();

  private double initialVelocity;
  private double initialAcceleration;
  private double jerk;

  public MotionInfo()
  {
  }

  /**
   * Copy constructor.
   *
   * @param classification
   */
  public MotionInfo(MotionInfo classification)
  {
    this.initialVelocity = classification.initialVelocity;
    this.initialAcceleration = classification.initialAcceleration;
    this.jerk = classification.jerk;
  }

  @Override
  public FeaturesDescription getDescription()
  {
    return DESCRIPTION;
  }

  /**
   * @return the initialVelocity
   */
  public double getInitialVelocity()
  {
    return initialVelocity;
  }

  /**
   * @param initialVelocity the initialVelocity to set
   */
  public void setInitialVelocity(double initialVelocity)
  {
    this.initialVelocity = initialVelocity;
  }

  /**
   * @return the initialAcceleration
   */
  public double getInitialAcceleration()
  {
    return initialAcceleration;
  }

  /**
   * @param initialAcceleration the initialAcceleration to set
   */
  public void setInitialAcceleration(double initialAcceleration)
  {
    this.initialAcceleration = initialAcceleration;
  }

  /**
   * @return the jerk
   */
  public double getJerk()
  {
    return jerk;
  }

  /**
   * @param jerk the jerk to set
   */
  public void setJerk(double jerk)
  {
    this.jerk = jerk;
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