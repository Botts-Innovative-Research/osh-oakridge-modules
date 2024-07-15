/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.math.internal.euclidean;

import gov.llnl.math.euclidean.Vector3;
import java.io.Serializable;

/**
 *
 * @author nelson85
 */
public class Vector3Impl implements Vector3, Serializable
{
  private static final long serialVersionUID = 235981610L;

  double x_, y_, z_;

  public Vector3Impl(double x, double y, double z)
  {
    this.x_ = x;
    this.y_ = y;
    this.z_ = z;
  }

  @Override
  public double getX()
  {
    return x_;
  }

  @Override
  public double getY()
  {
    return y_;
  }

  @Override
  public double getZ()
  {
    return z_;
  }

  public String toString()
  {
    return String.format("Vector3(%f,%f,%f)", x_, y_, z_);
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