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

import gov.llnl.math.DoubleArray;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

/**
 * Equivalent of Matlab polyval. For use with 3rd order or above.
 *
 * @author nelson85
 */
public class PolynomialFunction implements Function.Parameterized
{
  // WARNING the two functions here implement the order of the polynomial coefficients 
  // differently,  This could lead to problems in implementation.  
  // Matlab uses A(0)*X^n+ A(1)*X^(n-1) + ... + A(N-1)
  // should try to get all versions of the code to use the same definition

  double[] v;

  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("polynomial(");
    sb.append(DoubleStream.of(v).mapToObj(Double::toString).collect(Collectors.joining(", ")));
    sb.append(")");
    return sb.toString();
  }

  public PolynomialFunction(double[] v)
  {
    this.v = DoubleArray.copyOf(v);
  }

  @Override
  public double applyAsDouble(double x)
  {
    int n = v.length;
    double out = v[n - 1];
    double y = x;
    for (int i = 2; i < n; ++i)
    {
      out += y * v[n - i];
      y = y * x;
    }
    out += y * v[0];
    return out;
  }

  @Override
  public double[] toArray()
  {
    return v;
  }

  /**
   * Implementation of MATLAB polyval function.
   *
   * @param x
   * @param coef
   * @return the evaluation of the polynomial at x.
   */
  public static double polyval(double x, double[] coef)
  {
    double xp = x;
    double out = coef[0];
    for (int i = 1; i < coef.length; ++i)
    {
      out += coef[i] * xp;
      xp *= x;
    }
    return out;
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