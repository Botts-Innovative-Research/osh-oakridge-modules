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
 * @author seilhan3
 */
public class PowerFunction implements Function.Invertable,
        Function.Parameterized
{
  double k;
  double p;

  public PowerFunction(double k, double p)
  {
    this.k = k;
    this.p = p;
  }

  @Override
  public double inverse(double y) throws MathExceptions.RangeException
  {
    if (y == 0)
      return Double.NaN;
    return Math.pow(y / k, 1 / p);
  }

  @Override
  public double applyAsDouble(double x) throws MathExceptions.DomainException
  {
    return k * Math.pow(x, p);
  }
  
  public double[] evaluate(double[] x) throws MathExceptions.DomainException
  {
    double[] out = new double[x.length];
    for (int i = 0; i < x.length; i++)
    {
      out[i] = this.applyAsDouble(x[i]);
    }
    return out;
  }
  
  @Override
  public double[] toArray()
  {
    return new double[]
    {
      k, p
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