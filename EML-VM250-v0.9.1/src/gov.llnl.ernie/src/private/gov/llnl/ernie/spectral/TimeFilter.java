/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.ernie.spectral;

//import gov.llnl.ernie.math.DoubleArray;
//import gov.llnl.ernie.math.IntegerArray;
import gov.llnl.math.DoubleArray;
import gov.llnl.math.IntegerArray;

/**
 *
 * @author nelson85
 */
public class TimeFilter
{
  /**
   * Returns the rolling time windowed output for a block of spectral data. The
   * filtering is centered unlike MATLAB.
   *
   * @param data
   * @param window
   * @return
   */
  public static double[][] filter(double[][] data, int window)
  {
    double[][] out = new double[data.length][];
    double[] accumulator = new double[data[0].length];
    int j = 0;
    for (int i = 0; i < data.length + window + 1; i++)
    {
      if (j == data.length)
      {
        break;
      }
      if (i < data.length)
      {
        DoubleArray.addAssign(accumulator, data[i]);
      }
      if (i - window >= 0)
      {
        DoubleArray.subtractAssign(accumulator, data[i - window]);
      }
      if (i > window / 2 - 1)
      {
        out[j++] = accumulator.clone();
      }
    }
    return out;
  }

  static double[][] filter(int[][] data, int window)
  {
    double[][] out = new double[data.length][];
    int[] accumulator = new int[data[0].length];
    int j = 0;
    for (int i = 0; i < data.length + window + 1; i++)
    {
      if (j == data.length)
      {
        break;
      }
      if (i < data.length)
      {
        IntegerArray.addAssign(accumulator, data[i]);
      }
      if (i - window >= 0)
      {
        IntegerArray.subtractAssign(accumulator, data[i - window]);
      }
      if (i > window / 2 - 1)
      {
        out[j++] = IntegerArray.promoteToDoubles(accumulator);
      }
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