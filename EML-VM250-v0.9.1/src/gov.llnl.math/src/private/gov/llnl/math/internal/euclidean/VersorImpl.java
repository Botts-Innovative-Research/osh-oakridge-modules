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
import gov.llnl.math.euclidean.Versor;

/**
 *
 * @author nelson85
 */
public final class VersorImpl implements Versor
{
  double u_, i_, j_, k_;

  public VersorImpl(Vector3 axis, double angle)
  {
    double x = axis.getX();
    double y = axis.getY();
    double z = axis.getZ();
    double k = Math.sin(angle / 2) / axis.norm();

    this.u_ = Math.cos(angle / 2);
    this.i_ = x * k;
    this.j_ = y * k;
    this.k_ = z * k;
  }

  public VersorImpl(double u, double i, double j, double k)
  {
    this.u_ = u;
    this.i_ = i;
    this.j_ = j;
    this.k_ = k;
  }

  @Override
  public double getI()
  {
    return i_;
  }

  @Override
  public double getJ()
  {
    return j_;
  }

  @Override
  public double getK()
  {
    return k_;
  }

  @Override
  public double getU()
  {
    return u_;
  }

  @Override
  public String toString()
  {
    return String.format("Versor(%f,%f,%f,%f)", u_, i_, j_, k_);
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