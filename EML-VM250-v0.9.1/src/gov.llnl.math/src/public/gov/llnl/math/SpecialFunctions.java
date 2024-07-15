/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.math;

import gov.llnl.math.internal.function.LogGammaFunction;
import gov.llnl.math.internal.function.RegularizedGammaFunction;
import gov.llnl.utility.annotation.Matlab;
import java.lang.reflect.Method;

/**
 * Implementations of special functions we have required. Does not include exp,
 * expm1, log, log1p, sinh, cosh, tanh, sqrt, cbrt, pow, asin, and acos which
 * are all part of java.lang.Math.
 *
 * @author nelson85
 */
public class SpecialFunctions
{
  /**
   * Quick and dirty version of erfinv function.
   *
   * Taken from https://people.maths.ox.ac.uk/gilesm/files/gems_erfinv.pdf
   *
   * @param x
   * @return the err function inverse.
   */
  public static double erfinv(double x)
  {
    // FIXME the quality of this method is very poor especially near 0 and 1
    double p;
    double w = -Math.log((1 - x) * (1 + x));

    if (w < 5)
    {
      w = w - 2.5;
      p = 2.81022636e-08;
      p = 3.43273939e-07 + p * w;
      p = -3.5233877e-06 + p * w;
      p = -4.39150654e-06 + p * w;
      p = 0.00021858087 + p * w;
      p = -0.00125372503 + p * w;
      p = -0.00417768164 + p * w;
      p = 0.246640727 + p * w;
      p = 1.50140941 + p * w;
    }
    else
    {
      w = Math.sqrt(w) - 3.000000;
      p = -0.000200214257;
      p = 0.000100950558 + p * w;
      p = 0.00134934322 + p * w;
      p = -0.00367342844 + p * w;
      p = 0.00573950773 + p * w;
      p = -0.0076224613 + p * w;
      p = 0.00943887047 + p * w;
      p = 1.00167406 + p * w;
      p = 2.83297682 + p * w;
    }
    return p * x;
  }

  public static double gammaln(double x)
  {
    return LogGammaFunction.evaluate(x);
  }

  public static double atanh(double x)
  {
    return 0.5 * Math.log((1.0 + x) / (1.0 - x));
  }

  /**
   * This function maps all real numbers to the positive domain. Accurate from
   * -745 to infinity.
   *
   * @param x
   * @return \( \displaystyle U(x)=\left\{\begin{matrix} x-log\left (
   * 1-\frac{1}{1+e^x} \right ) &amp;&amp; x\geqslant 0 \\ log(e^x+1) &amp;&amp;
   * x&lt;0 \end{matrix}\right. \)
   */
  public static double positiveAsymptotic(double x)
  {
    if (x < 0)
      return Math.log1p(Math.exp(x));
    return x - Math.log(1 - 1 / (1 + Math.exp(x)));
    // alternatives were 0.5*(x+sqrt(1+x^2))
  }

  /**
   * Error function.
   *
   * @param x
   * @return \( \displaystyle erf(x)= \frac{2}{\sqrt{\pi}}
   * \int_{0}^{x}{e^{-t^2}dt} \)
   */
  public static double erf(double x)
  {
    // From https://en.wikipedia.org/wiki/Abramowitz_and_Stegun
    // constants
    final double a1 = 0.254829592;
    final double a2 = -0.284496736;
    final double a3 = 1.421413741;
    final double a4 = -1.453152027;
    final double a5 = 1.061405429;
    final double p = 0.3275911;
    // Save the sign of x
    int sign = 1;
    if (x < 0)
      sign = -1;
    x = Math.abs(x);
    double t = 1.0 / (1.0 + p * x);
    double y = 1.0 - (((((a5 * t + a4) * t) + a3) * t + a2) * t + a1) * t * Math.exp(-x * x);
    return sign * y;
  }

  /**
   * Complementary error function.
   *
   * @param x is the integration limit.
   * @return \( \displaystyle erfc(x)= \frac{2}{\sqrt{\pi}}
   * \int_{x}^{\infty}{e^{-t^2}dt} = 1-erf(x) \)
   */
  public static double erfc(double x)
  {
    if (x < 0)
      return 1 + erf(-x);
    // constants
    final double a1 = 0.254829592;
    final double a2 = -0.284496736;
    final double a3 = 1.421413741;
    final double a4 = -1.453152027;
    final double a5 = 1.061405429;
    final double p = 0.3275911;
    // Save the sign of x
    double sign = Math.signum(x);
    double t = 1.0 / (1.0 + p * x);
    double y = (((((a5 * t + a4) * t) + a3) * t + a2) * t + a1) * t * Math.exp(-x * x);
    return sign * y + (1 - sign);
  }

  public static double logerfc(double x)
  {
    if (x > 20)
    {
      double u = x * x;
      // Log of continued fraction expansion of erfc.
      // Cuyt, Annie A. M.; Petersen, Vigdis B.; Verdonk, Brigitte; Waadeland, Haakon; Jones, William B. (2008). 
      // Handbook of Continued Fractions for Special Functions.
      return -u + Math.log(x / MathConstants.SQRT_PI / (u + 0.5 / (1 + 1 / (u + 1.5 / (1 + 2 / (u + 2.5))))));
    }
    return Math.log(erfc(x));
  }

  // FIXME the quality of this is very poor.
  public static double erfcinv(double x)
  {
    return erfinv(1 - x);
  }

  /**
   * Gamma incomplete function for upper incomplete gamma function.
   *
   * @param a is the gamma factorial.
   * @param x is the upper limit.
   * @return \( \displaystyle P(a,x)=\frac{1}{\Gamma (x)} \int_{-\infty}^{x}
   * t^{a-1}e^{-t} dt \)
   */
  public static double gammaP(double a, double x)
  {
    RegularizedGammaFunction rgf = new RegularizedGammaFunction();
    rgf.evaluate(a, x);
    return rgf.getP();
  }

  /**
   * Gamma incomplete function for lower regularized incomplete gamma function.
   *
   * @param a is the gamma factorial.
   * @param x is the lower limit.
   * @return \( \displaystyle Q(a,x)=\frac{1}{\Gamma (x)} \int_{x}^{\infty}
   * t^{a-1}e^{-t} dt \)
   *
   */
  public static double gammaQ(double a, double x)
  {
    RegularizedGammaFunction rgf = new RegularizedGammaFunction();
    rgf.evaluate(a, x);
    return rgf.getQ();
  }

  /**
   * Logistic function
   *
   * @param x is the parameter.
   * @param x0 is the midpoint.
   * @param k is the the steepness of the curve.
   * @return
   */
  public static double logistic(double x, double x0, double k)
  {
    if (x > x0)
      return 1 / (1 + Math.exp(-k * (x - x0)));
    double t = Math.exp(k * (x - x0));
    return t / (t + 1);
  }

  /**
   * Convenience function for Matlab plotting. This method is used with {@link DoubleArray#evaluate(java.lang.reflect.Method, double[])
   * }
   *
   * @param methodName is the name of a special function
   * @return the method if it exists or null otherwise.
   */
  @Matlab static public Method getFunction(String methodName)
  {
    try
    {
      Class cls = SpecialFunctions.class;
      Method out = cls.getDeclaredMethod(methodName, double.class);
      return out;
    }
    catch (NoSuchMethodException | SecurityException ex)
    {
      throw null;
    }
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