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

import gov.llnl.math.internal.InterpolatorBuilder;
import java.io.Serializable;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Supplier;

/**
 * Front end for interpolating 1d data.
 *
 * FIXME this does not yet deal with Matrix interpolation which is a common
 * pattern in our code.
 *
 * @author nelson85
 */
public interface Interpolator
{

  double interp(double[] x, double[] y, double q);

  /**
   * Interpolate to the nearest point.
   *
   * @param x is a set of sample points in order.
   * @param y is the corresponding values.
   * @param xq is the query points.
   * @return the values corresponding to the query points.
   * @throws gov.llnl.math.MathExceptions.MathException
   */
  double[] interp(double[] x, double[] y, double[] xq)
          throws MathExceptions.MathException;

  default public DoubleUnaryOperator interp(double[] x, double[] y)
  {
    return (double d) -> this.interp(x, y, d);
  }

  /**
   * Interpolation rule used for implementing different methods.
   */
  public interface Method extends Serializable
  {
    /**
     * Defines how the neighboring points are used to interpolate.
     *
     * @param x is the set of sample points (in order)
     * @param y is the set of corresponding values.
     * @param xq is the query point.
     * @param n is the index of the nearest sample point
     * @return
     */
    double evaluate(double[] x, double[] y, double xq, int n);

    default void cache(double[] x, double[] y)
    {
    }
  }

  /**
   * Common methods used for specifying end behaviors.
   */
  public enum Methods implements Supplier<Method>
  {
    Nearest(InterpolatorBuilder::nearestMethod),
    Linear(InterpolatorBuilder::linearMethod),
    Zero(InterpolatorBuilder::zeroMethod);
    // More methods can be added here assuming they don't require
    // parameters.

    Methods(Method method)
    {
      this.method = method;
    }

    private Method method;

    @Override
    public Method get()
    {
      return this.method;
    }
  }

//<editor-fold desc="builder">
  /**
   * Create a factory for an Interpolator.
   *
   * @return
   */
  public static Builder builder()
  {
    return new InterpolatorBuilder();
  }

  public static interface Builder
  {

    /**
     * Create a new builder with the specified parameters.
     *
     * @return a new Interpolator.
     */
    Interpolator create();

    //<editor-fold desc="end point">
    InterpolatorBuilder lower(Interpolator.Method end);

    default InterpolatorBuilder lower(Supplier<Interpolator.Method> method)
    {
      return this.lower(method.get());
    }

    InterpolatorBuilder upper(Interpolator.Method end);

    default InterpolatorBuilder upper(Supplier<Interpolator.Method> end)
    {
      return this.upper(end.get());
    }

    InterpolatorBuilder clip();

//</editor-fold>
//<editor-fold desc="method">
    /**
     * Specify an interpolation method.
     *
     * @param method
     * @return
     */
    default InterpolatorBuilder method(Supplier<Interpolator.Method> method)
    {
      return this.method(method.get());
    }

    /**
     * Specify an interpolation method.
     *
     * @param method
     * @return
     */
    InterpolatorBuilder method(Interpolator.Method method);

    /**
     * Use a kernel density estimate weighting the nearest points.
     *
     * Typical density functions for this method are Gaussian or
     * lambda/(x*x+lambda).
     *
     * @param density is the specified density function.
     * @param nearest is the number of points around the query to consider.
     * @return
     */
    InterpolatorBuilder weighted(DoubleUnaryOperator density, int nearest);
//</editor-fold>
  }
//</editor-fold>
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