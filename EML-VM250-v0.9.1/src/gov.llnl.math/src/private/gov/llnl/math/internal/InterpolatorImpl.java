/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.math.internal;

import gov.llnl.math.Interpolator;
import gov.llnl.math.MathExceptions;
import gov.llnl.utility.Exceptions;
import gov.llnl.utility.annotation.Internal;
import java.util.Arrays;

/**
 *
 * @author nelson85
 */
@Internal
public class InterpolatorImpl implements Interpolator
{
  private final Method lower;
  private final Method inner;
  private final Method upper;

  public InterpolatorImpl(Method lower, Method inner, Method upper)
  {
    this.lower = lower;
    this.inner = inner;
    this.upper = upper;
  }
  
  public double interp(double[]x , double[]y, double q)
  {
          // Check lower limits
      if (q < x[0])
      {
        return this.lower.evaluate(x, y, q, 0);
      }

      // Check upper limits
    int m = x.length;
      if (q >= x[m - 1])
      {
        return this.upper.evaluate(x, y, q, m - 1);
      }

      // Search inner O = n*log(n)
      return this.inner.evaluate(x, y, q, nearest(x, q));
  }

  public double[] interp(double[] x, double[] y, double[] xq)
          throws MathExceptions.MathException
  {
    int n = xq.length;
    int m = x.length;
    double out[] = new double[n];

    if (y.length != x.length)
      throw new MathExceptions.SizeException("Size mismatch");

    // FIXME this seems like a waste of time, if the user is willing
    // to stimpulate that they have ordered the samples.
    for (int i = 0; i < m - 1; ++i)
      if (x[i] > x[i + 1])
        throw new MathExceptions.MathException("Inputs out of order");

    for (int i = 0; i < n; ++i)
    {
      // Check lower limits
      double q = xq[i];
      if (q < x[0])
      {
        out[i] = this.lower.evaluate(x, y, q, 0);
        continue;
      }

      // Check upper limits
      if (q >= x[m - 1])
      {
        out[i] = this.upper.evaluate(x, y, q, m - 1);
        continue;
      }

      // Search inner O = n*log(n)
      out[i] = this.inner.evaluate(x, y, q, nearest(x, q));
      if (Double.isNaN(out[i]))
      {
        throw new RuntimeException("NaN in output");
      }
    }
    return out;
  }

  public int nearest(double[] x, double q)
  {
    int k = Arrays.binarySearch(x, q);

    // If we hit an exact point then call it directly
    if (k >= 0)
      return k;

    // Otherwise find the nearest point
    k = -k - 1;
    if (k <= 0)
      return 0;
    if (k >= x.length)
      k = x.length - 1;
    if (q - x[k - 1] < x[k] - q)
      return k - 1;
    return k;
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