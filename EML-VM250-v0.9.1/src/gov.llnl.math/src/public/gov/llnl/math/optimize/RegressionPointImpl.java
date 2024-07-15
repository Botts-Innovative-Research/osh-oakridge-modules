/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.math.optimize;

public class RegressionPointImpl implements RegressionPoint
{
  final double x;
  final double y;
  final double lambda;

  public RegressionPointImpl(double x, double y)
  {
    this.x = x;
    this.y = y;
    this.lambda = 1;
  }

  public RegressionPointImpl(double x, double y, double lambda)
  {
    this.x = x;
    this.y = y;
    this.lambda = lambda;
  }

  @Override
  public double getX()
  {
    return this.x;
  }

  @Override
  public double getY()
  {
    return this.y;
  }

  @Override
  public double getLambda()
  {
    return this.lambda;
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