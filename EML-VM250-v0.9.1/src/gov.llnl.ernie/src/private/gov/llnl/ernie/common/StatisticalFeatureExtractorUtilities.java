/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.ernie.common;

import gov.llnl.math.DoubleArray;

/**
 *
 * @author nelson85
 */
public class StatisticalFeatureExtractorUtilities
{
  public static double[] computeWeightedStatistics(double[] v1, double[] v2, int start, int end)
  {
    double N = DoubleArray.sumRange(v2, start, end);
    if (N == 0)
    {
      N = 1;
    }
    // Compute the moments
    double m1 = DoubleArray.multiplyInnerRange(v1, v2, start, end) / N;

    //System.out.println(m1);
    double m2 = (1.0 / N) * momentWeighted(v1, v2, m1, 2, start, end);
    double m3 = (1.0 / N) * momentWeighted(v1, v2, m1, 3, start, end);
    double m4 = (1.0 / N) * momentWeighted(v1, v2, m1, 4, start, end);
    // Calculates data count intensity average:
    double Average = m1;
    // Calculates data count intensity variance (unbiased)
    double Variance = m2;
    // return Sandard Deviation
    double Stddev = Math.sqrt(Variance);
    if (m2 == 0)
    {
      m2 = 1;
    }
    // Calculates data count intensity skew (unbiased)
    double g1 = m3 / Math.pow(m2, 3.0 / 2.0);
    double Skew = g1;
    // Calculates data count intensity Kurtosis
    double Kurtosis = m4 / Math.pow(m2, 2) - 3.0;
    double[] out = new double[4];
    out[0] = Average;
    out[1] = Stddev;
    out[2] = Skew;
    out[3] = Kurtosis;
    return out;
  }

  /**
   * Specialized algorithm for finding the average counts point in the time
   * trace.
   *
   * This handles the case were we under subtracted by updating a lower base so
   * that we are guaranteed to have some positive counts.
   */
  public static double computeSplit(double[] v2, int start, int end)
  {
    int n = 0;
    double base = 0;
    for (int i = start; i < end; ++i)
    {
      if (v2[i] < 0)
      {
        base += v2[i];
        n++;
      }
    }
    if (n > 0)
      base /= n;

    double q1 = 0;
    double q2 = 0;
    for (int i = start; i < end; ++i)
    {
      q1 += i * (v2[i] - base);
      q2 += (v2[i] - base);
    }
    if (q2 == 0)
      return (start + end) / 2;
    double split = q1 / q2;
    if (split<start)
      split=start;
    if (split>= (end - 1))
      split=end-1;
    return split;
  }

  public static double momentWeighted(double[] data, double[] weights, double m1, int p, int start, int end)
  {
    double sum = 0;
    for (int i = start; i < end; ++i)
    {
      sum += weights[i] * Math.pow(data[i] - m1, p);
    }
    return sum;
  }

  public static double[] computeIntensityStatistics(double[] data, int start, int end, double dt)
  {
    double[] out = new double[5];
    int N = end - start - 1;
    if (N < 1)
    {
      return out;
    }

    // Compute the moments
    double m1 = (1.0 / N) * DoubleArray.sumRange(data, start, end);
    double m2 = (1.0 / N) * moment(data, m1, 2, start, end);
    double m3 = (1.0 / N) * moment(data, m1, 3, start, end);
    double m4 = (1.0 / N) * moment(data, m1, 4, start, end);
    // Calculates data count intensity average:
    double mean = m1;
    // Calculates data count intensity variance (unbiased)
    double Variance = (N > 1 ? (N / (N - 1.0)) * m2 : 0);
    // return Sandard Deviation
    double stddev = Math.sqrt(Variance);
    // Calculates data count intensity skew (unbiased)
    double g1 = m3 / Math.pow(m2, 3.0 / 2.0);
    double skew = (N > 2 ? Math.sqrt(N * (N - 1.0)) / (N - 2.0) * g1 : 0);
    // Calculates data count intensity Kurtosis
    double kurtosis = (N > 3
            ? (N - 1.0) / ((N - 2.0) * (N - 3.0)) * ((N - 1.0) * m4 / Math.pow(m2, 2) - 3.0 * (N - 1))
            : 0);
    out[0] = mean / dt;
    out[1] = stddev / dt;
    out[2] = skew;
    out[3] = kurtosis;

    out[4] = 0;
    if (out[0] > 1)
    {
      out[4] = out[1] / out[0];
    }

    return out;
  }

  public static double moment(double[] data, double m1, int p, int start, int end)
  {
    double sum = 0;
    for (int i = start; i < end; ++i)
    {
      sum += Math.pow(data[i] - m1, p);
    }
    return sum;
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