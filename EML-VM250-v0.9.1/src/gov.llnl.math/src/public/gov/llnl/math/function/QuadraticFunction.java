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

/**
 *
 * @author nelson85
 */
public class QuadraticFunction implements 
        Function.Differentiable,
        Function.Invertable,
        Function.Parameterized
{
  double k[] = new double[3];

  public QuadraticFunction()
  {
  }

  public QuadraticFunction(double offset, double slope, double accel)
  {
    k[0] = offset;
    k[1] = slope;
    k[2] = accel;
  }

  @Override
  public double applyAsDouble(double x)
  {
    return k[0] + k[1] * x + k[2] * x * x;
  }

  @Override
  public double inverse(double y)
  {
    if (k[2] == 0)
    {
      double denom = k[1];
      if (denom == 0)
        denom = 1e-10;
      return (y - k[0]) / denom;
    }
    else
    {
      // restricted domain best solution around 0.5
      double denom = k[1] * 2;
      if (denom == 0)
        denom = 1e-10;
      double q = k[1] * k[1] - 4 * (k[0] - y) * k[2];
      if (q < 0)
        return 0;
      q = Math.sqrt(q);
      double a1 = (k[1] - q) / denom;
      double a2 = (k[1] + q) / denom;

      if (Math.abs(a1 - 0.5) < Math.abs(a2 - 0.5))
        return a1;
      else
        return a2;
    }
  }

  @Override
  public double derivative(double x)
  {
    return k[1] + 2 * k[2] * x;
  }

  @Override
  public double[] toArray()
  {
    return k.clone();
  }

  @Override
  public QuadraticFunction clone() throws CloneNotSupportedException
  {
    return (QuadraticFunction) super.clone();
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