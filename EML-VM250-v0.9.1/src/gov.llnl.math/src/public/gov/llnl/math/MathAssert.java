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

/**
 *
 * @author nelson85
 */
public class MathAssert
{
  /**
   * Assert that two vectors are equal length.
   *
   * @param a is the first operand.
   * @param b is the second operand.
   * @throws IndexOutOfBoundsException if the vectors are different lengths.
   */
  static void assertEqualLength(int[] a, int[] b)
          throws IndexOutOfBoundsException
  {
    if (a.length != b.length)
      throw new IndexOutOfBoundsException("Size mismatch, operand1=" + a.length + " operand2=" + b.length);
  }

  /**
   * Assert that two vectors are equal length.
   *
   * @param a is the first operand.
   * @param b is the second operand.
   * @throws IndexOutOfBoundsException if the vectors are different lengths.
   */
  static void assertEqualLength(int[] a, double[] b)
          throws IndexOutOfBoundsException
  {
    if (a.length != b.length)
      throw new IndexOutOfBoundsException("Size mismatch, operand1=" + a.length + " operand2=" + b.length);
  }

  /**
   * Assert that two vectors are equal length.
   *
   * @param a is the first operand.
   * @param b is the second operand.
   * @throws IndexOutOfBoundsException if the vectors are different lengths.
   */
  static public void assertEqualLength(double[] a, double[] b)
          throws IndexOutOfBoundsException
  {
    if (a == null || b == null)
      throw new NullPointerException("Null Object, operand1=" + (a == null) + " operand2=" + (b == null));
    if (a.length != b.length)
      throw new IndexOutOfBoundsException("Size mismatch, operand1=" + a.length + " operand2=" + b.length);
  }

  /**
   * Assert that the array covers the required range.
   *
   * @param array is the array to check
   * @param begin is the start of the range (inclusive)
   * @param end is the end or the range (exclusive)
   * @param msg is the name of the variable with the error (optional)
   * @throws IndexOutOfBoundsException
   */
  public static void assertRange(double[] array, int begin, int end, String... msg)
          throws IndexOutOfBoundsException
  {
    if (end > array.length || begin < 0)
    {
      throw new IndexOutOfBoundsException("Range error" + parseVariable(msg) + ", size=" + array.length + " begin=" + begin + " end=" + end);
    }
  }

  public static void assertRange(float[] array, int begin, int end, String... msg)
          throws IndexOutOfBoundsException
  {
    if (end > array.length || begin < 0)
    {
      throw new IndexOutOfBoundsException("Range error" + parseVariable(msg) + ", size=" + array.length + " begin=" + begin + " end=" + end);
    }
  }

  public static void assertLengthEqual(double[] array, int sz, String... msg)
  {
    if (array.length == sz)
      return;
    throw new IndexOutOfBoundsException("Vector length incorrect" + parseVariable(msg) + ", expected=" + sz + " size=" + array.length);
  }
  
  public static void assertLengthEqual(int[] array, int sz, String... msg)
  {
    if (array.length == sz)
      return;
    throw new IndexOutOfBoundsException("Vector length incorrect" + parseVariable(msg) + ", expected=" + sz + " size=" + array.length);
  }

  /**
   * Assert that the array covers the required range.
   *
   * @param array is the array to check
   * @param begin is the start of the range (inclusive)
   * @param end is the end or the range (exclusive)
   * @param msg is the name of the variable with the error (optional)
   * @throws IndexOutOfBoundsException
   */
  public static void assertRange(int[] array, int begin, int end, String... msg)
          throws IndexOutOfBoundsException
  {
    if (end > array.length || begin < 0)
      throw new IndexOutOfBoundsException("Range error on " + parseVariable(msg) + ", size=" + array.length + " begin=" + begin + " end=" + end);
  }

  public static void assertNotNaN(double[] x)
  {
    for (int i = 0; i < x.length; ++i)
      if (x[i] != x[i])
        throw new RuntimeException("NaN in vector");
  }

  public static void assertSortedDoubleUnique(double[] array) throws RuntimeException
  {
    for (int i = 1; i < array.length; i++)
    {
      if (array[i - 1] >= array[i])
        throw new RuntimeException("Not Sorted Unique vector");
    }
  }

  private static String parseVariable(String[] msg)
  {
    if (msg == null || msg.length == 0)
      return "";
    return " on " + msg[0];
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