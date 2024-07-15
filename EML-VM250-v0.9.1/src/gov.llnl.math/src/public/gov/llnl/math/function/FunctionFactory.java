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
 * Factory for commonly used functions. Primarily here to make pythons life
 * simpler.
 *
 * @author nelson85
 */
public class FunctionFactory
{

  /**
   * Linear function of the form y=k*x
   *
   * @param k is the slope
   * @return
   */
  static public LinearFunction newLinear(double k)
  {
    return new LinearFunction(0, k);
  }

  static public QuadraticFunction newQuadratic(double offset, double slope, double accel)
  {
    return new QuadraticFunction(offset, slope, accel);
  }

  /**
   * Polynomial function of the form y=\Sum_i a_i x^i
   *
   * @param a
   * @return
   */
  static public PolynomialFunction newPolynomial(double[] a)
  {
    return new PolynomialFunction(a);
  }

  /**
   * Power function given as y=k x^p.
   *
   * @param k
   * @param p
   * @return
   */
  static public PowerFunction newPower(double k, double p)
  {
    return new PowerFunction(k, p);
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