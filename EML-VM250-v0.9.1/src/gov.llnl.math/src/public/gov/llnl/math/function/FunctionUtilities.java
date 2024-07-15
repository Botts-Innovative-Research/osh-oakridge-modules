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

import java.util.function.DoubleUnaryOperator;

/**
 *
 * @author nelson85
 */
public class FunctionUtilities
{

  /**
   * Evaluate a function on an array of values.
   *
   * @param function is the function to evaluate.
   * @param values is the array.
   * @return is a new array evaluated for each element in values.
   */
  public static double[] evaluate(DoubleUnaryOperator function, double[] values)
  {
    return evaluateRange(function, values, 0, values.length);
  }

  /**
   * Evaluate a function on an array of values in a range.
   *
   * @param function is the function to evaluate.
   * @param values is the array.
   * @param begin is the start of the range (inclusive).
   * @param end is the end of the range (exclusive).
   * @return is a new array with the same length as the range containing the
   * evaluation for each element of values in the selected range.
   */
  public static double[] evaluateRange(DoubleUnaryOperator function, double[] values, int begin, int end)
  {
    double[] out = new double[end - begin];
    for (int i = 0; i < end - begin; ++i)
      out[i] = function.applyAsDouble(values[i + begin]);
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