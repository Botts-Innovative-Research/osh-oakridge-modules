/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.math.function;

import gov.llnl.math.MathExceptions;

/**
 *
 * @author nelson85
 */
public class LinearFunction
        implements 
        Function.Differentiable, 
        Function.Invertable, 
        Function.Parameterized
{
  double offset;
  double slope;

  public LinearFunction()
  {
  }

  public LinearFunction(double offset, double slope)
  {
    this.offset = offset;
    this.slope = slope;
  }

  @Override
  public double applyAsDouble(double x)
  {
    return offset + slope * x;
  }

  @Override
  public double inverse(double y) throws MathExceptions.RangeException
  {
    double denom = slope;
    if (denom == 0)
      throw new MathExceptions.RangeException("Slope is zero");
    return (y - offset) / denom;
  }

  @Override
  public double derivative(double y)
  {
    return slope;
  }

  @Override
  public LinearFunction clone() throws CloneNotSupportedException
  {
    return (LinearFunction) super.clone();
  }

  @Override
  public double[] toArray()
  {
    return new double[]
    {
      offset, slope
    };
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