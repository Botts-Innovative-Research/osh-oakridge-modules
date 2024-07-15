/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.math.internal.wavelet;

import java.util.Arrays;

/**
 * Small statistical utility for 1D array
 */
public class Statistics1D
{
  double[] data;
  int size;
  double minValue;
  double maxValue;

  public Statistics1D(double[] data)
  {
    this.data = data;
    size = data.length;
    maxValue = data[0];
    minValue = data[0];
    for (int it = 0; it < size; it++)
    {
      if (maxValue < data[it])
      {
        maxValue = data[it];
      }
      if (minValue > data[it])
      {
        minValue = data[it];
      }
    }
  }

  double getMean()
  {
    double sum = 0.0;
    for (double a : data)
      sum += a;
    return sum / size;
  }

  double getVariance()
  {
    double mean = getMean();
    double temp = 0;
    for (double a : data)
      temp += (a - mean) * (a - mean);
    return temp / (size - 1);
  }

  double getStdDev()
  {
    return Math.sqrt(getVariance());
  }

  public double median()
  {
    Arrays.sort(data);
    if (data.length % 2 == 0)
      return (data[(data.length / 2) - 1] + data[data.length / 2]) / 2.0;
    return data[data.length / 2];
  }

  public double getMin()
  {
    return minValue;
  }

  public double getMax()
  {
    return maxValue;
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