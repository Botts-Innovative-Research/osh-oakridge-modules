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

import java.lang.reflect.Method;
import java.util.Arrays;
import org.testng.Assert;
import org.testng.annotations.Test;
import support.MatrixTestGenerator;

/**
 *
 * @author nelson85
 */
public class DoubleArrayNGTest
{
  MatrixTestGenerator tg = new MatrixTestGenerator();

  public DoubleArrayNGTest()
  {
  }

  /**
   * Test of assign method, of class DoubleArray.
   */
  @Test
  public void testAssign_doubleArr_doubleArr()
  {
    double[] out = new double[10];
    double[] in = tg.newArray(10);
    DoubleArray.assign(out, in);
    Assert.assertTrue(DoubleArray.equivalent(in, out));
  }

  @Test(expectedExceptions = IndexOutOfBoundsException.class)
  public void testAssign_doubleArr_doubleArr_IndexOutOfBoundsException()
  {
    double[] out = new double[9];
    double[] in = tg.newArray(10);
    DoubleArray.assign(out, in);
  }

  /**
   * Test of assign method, of class DoubleArray.
   */
  @Test
  public void testAssign_5args()
  {
    double[] out = new double[10];
    double[] in = tg.newArray(10);
    DoubleArray.assign(out, 2, in, 3, 6);
    Assert.assertTrue(DoubleArray.equivalent(out, 2, in, 3, 6));
    for (int i = 0; i < 2; i++)
      Assert.assertEquals(out[i], 0.0);
    for (int i = 8; i < 10; i++)
      Assert.assertEquals(out[i], 0.0);
  }

  @Test(expectedExceptions = IndexOutOfBoundsException.class)
  public void testAssign_5args_IndexOutOfBoundsException1()
  {
    double[] out = new double[5];
    double[] in = tg.newArray(10);
    DoubleArray.assign(out, 2, in, 3, 6);
  }

  @Test(expectedExceptions = IndexOutOfBoundsException.class)
  public void testAssign_5args_IndexOutOfBoundsException2()
  {
    double[] out = new double[10];
    double[] in = tg.newArray(5);
    DoubleArray.assign(out, 2, in, 3, 6);
    // TODO complete test
  }

  /**
   * Test of assignMaxOf method, of class DoubleArray.
   */
  @Test
  public void testAssignMaxOf()
  {
  }

  /**
   * Test of assignMinOf method, of class DoubleArray.
   */
  @Test
  public void testAssignMinOf()
  {
  }

  /**
   * Test of copyOf method, of class DoubleArray.
   */
  @Test
  public void testCopyOf()
  {
    double[] in = tg.newArray(10);
    double[] out = DoubleArray.copyOf(in);
    Assert.assertTrue(DoubleArray.equivalent(in, out));
    Assert.assertTrue(in != out);
  }

  /**
   * Test of copyOfRange method, of class DoubleArray.
   */
  @Test
  public void testCopyOfRange()
  {
    double[] in = tg.newArray(10);
    double[] out = DoubleArray.copyOfRange(in, 2, 8);
    Assert.assertTrue(DoubleArray.equivalent(in, 2, out, 0, 8 - 2));
    Assert.assertTrue(in != out);
  }

  /**
   * Test of fill method, of class DoubleArray.
   */
  @Test
  public void testFill()
  {
    double[] in = new double[5];
    DoubleArray.fill(in, 2);
    int index = DoubleArray.findIndexOfFirstRange(in, 0, in.length,
            new DoubleConditional.NotEqual(2));
    Assert.assertEquals(index, -1);
  }

  /**
   * Test of fillRange method, of class DoubleArray.
   */
  @Test
  public void testFillRange()
  {
    double[] in = new double[10];
    DoubleArray.fillRange(in, 4, 8, 2.0);
    int index = DoubleArray.findIndexOfFirstRange(in, 4, 8,
            new DoubleConditional.NotEqual(2));
    Assert.assertEquals(index, -1);
  }

  /**
   * Test of addAssign method, of class DoubleArray.
   */
  @Test
  public void testAddAssign_doubleArr_double()
  {
    double[] in = tg.newArray(10);
    double[] orig = DoubleArray.copyOf(in);
    DoubleArray.addAssign(in, 4.0);
    for (int i = 0; i < 10; ++i)
      Assert.assertEquals(in[i], orig[i] + 4.0);
  }

  /**
   * Test of addAssignRange method, of class DoubleArray.
   */
  @Test
  public void testAddAssignRange()
  {
    double[] in = tg.newArray(10);
    double[] orig = DoubleArray.copyOf(in);
    DoubleArray.addAssignRange(in, 2, 8, 4.0);
    for (int i = 0; i < 10; ++i)
      Assert.assertEquals(in[i], orig[i] + ((i >= 2 && i < 8) ? 4.0 : 0));
  }

  /**
   * Test of addAssign method, of class DoubleArray.
   */
  @Test
  public void testAddAssign_doubleArr_doubleArr()
  {
    double[] in = tg.newArray(10);
    double[] orig = DoubleArray.copyOf(in);
    double[] op = tg.newArray(10);
    DoubleArray.addAssign(in, op);
    for (int i = 0; i < 10; ++i)
      Assert.assertEquals(in[i], orig[i] + op[i]);
  }

  /**
   * Test of addAssign method, of class DoubleArray.
   */
  @Test
  public void testAddAssign_5args()
  {
    double[] in = tg.newArray(10);
    double[] orig = DoubleArray.copyOf(in);
    double[] op = tg.newArray(6);
    DoubleArray.addAssign(in, 2, op, 0, 6);
    for (int i = 0; i < 10; ++i)
      Assert.assertEquals(in[i], orig[i] + ((i >= 2 && i < 8) ? op[i - 2] : 0));
  }

  /**
   * Test of addAssignScaled method, of class DoubleArray.
   */
  @Test
  public void testAddAssignScaled_3args()
  {
    double[] in = tg.newArray(10);
    double[] orig = DoubleArray.copyOf(in);
    double[] op = tg.newArray(10);
    DoubleArray.addAssignScaled(in, op, 1.25);
    for (int i = 0; i < 10; ++i)
      Assert.assertEquals(in[i], orig[i] + op[i] * 1.25);
  }

  /**
   * Test of addAssignScaled method, of class DoubleArray.
   */
  @Test
  public void testAddAssignScaled_6args()
  {
    double[] in = tg.newArray(10);
    double[] orig = DoubleArray.copyOf(in);
    double[] op = tg.newArray(6);
    DoubleArray.addAssignScaled(in, 2, op, 0, 6, 1.25);
    for (int i = 0; i < 10; ++i)
      Assert.assertEquals(in[i], orig[i] + ((i >= 2 && i < 8) ? op[i - 2] * 1.25 : 0));
  }

  /**
   * Test of subtractAssign method, of class DoubleArray.
   */
  @Test
  public void testSubtractAssign_doubleArr_doubleArr()
  {
    double[] in = tg.newArray(10);
    double[] orig = DoubleArray.copyOf(in);
    double[] op = tg.newArray(10);
    DoubleArray.subtractAssign(in, op);
    for (int i = 0; i < 10; ++i)
      Assert.assertEquals(in[i], orig[i] - op[i]);
  }

  /**
   * Test of subtractAssign method, of class DoubleArray.
   */
  @Test
  public void testSubtractAssign_5args()
  {
    double[] in = tg.newArray(10);
    double[] orig = DoubleArray.copyOf(in);
    double[] op = tg.newArray(6);
    DoubleArray.subtractAssign(in, 2, op, 0, 6);
    for (int i = 0; i < 10; ++i)
      Assert.assertEquals(in[i], orig[i] - ((i >= 2 && i < 8) ? op[i - 2] : 0));
  }
  
  @Test
  public void testSubtract()
  {
    double[] u = {6, 4};
    double[] v = {2, 2};
    double[] actual = DoubleArray.subtract(u, v);
    double[] expected = {4, 2};
    for (int i = 0; i < u.length; i++)
      Assert.assertEquals(actual[i], expected[i], 0.1);
  }

  /**
   * Test of negateAssign method, of class DoubleArray.
   */
  @Test
  public void testNegateAssign()
  {
    double[] in = tg.newArray(10);
    double[] orig = DoubleArray.copyOf(in);
    DoubleArray.negateAssign(in);
    for (int i = 0; i < 10; ++i)
      Assert.assertEquals(in[i], -orig[i]);
  }

  /**
   * Test of negateAssignRange method, of class DoubleArray.
   */
  @Test
  public void testNegateAssignRange()
  {
    double[] in = tg.newArray(10);
    double[] orig = DoubleArray.copyOf(in);
    DoubleArray.negateAssignRange(in, 2, 8);
    for (int i = 0; i < 10; ++i)
      Assert.assertEquals(in[i], orig[i] * ((i >= 2 && i < 8) ? -1 : 1));
  }

  /**
   * Test of multiply method, of class DoubleArray.
   */
  @Test
  public void testMultiply_doubleArr_double()
  {
    double[] in = tg.newArray(10);
    double[] out = DoubleArray.multiply(in, 4.223);
    for (int i = 0; i < 10; ++i)
      Assert.assertEquals(out[i], in[i] * 4.223);
  }

  /**
   * Test of multiply method, of class DoubleArray.
   */
  @Test
  public void testMultiply_doubleArr_doubleArr()
  {
    double[] in1 = tg.newArray(10);
    double[] in2 = tg.newArray(10);
    double[] out = DoubleArray.multiply(in1, in2);
    for (int i = 0; i < 10; ++i)
      Assert.assertEquals(out[i], in1[i] * in2[i]);
  }

  /**
   * Test of multiply method, of class DoubleArray.
   */
  @Test
  public void testMultiply_7args()
  {
    double[] in1 = tg.newArray(10);
    double[] in2 = tg.newArray(10);
    double[] out = new double[10];
    DoubleArray.multiply(out, 1, in1, 2, in2, 3, 4);
    for (int i = 0; i < 10; ++i)
      Assert.assertEquals(out[i], ((i >= 1) && (i < 5)) ? (in1[i + 1] * in2[i + 2]) : 0);
  }

  /**
   * Test of multiplyAssign method, of class DoubleArray.
   */
  @Test
  public void testMultiplyAssign_doubleArr_double()
  {
    double[] in = tg.newArray(10);
    double[] orig = DoubleArray.copyOf(in);
    DoubleArray.multiplyAssign(in, 4.231);
    for (int i = 0; i < 10; ++i)
      Assert.assertEquals(in[i], orig[i] * 4.231);
  }

  /**
   * Test of multiplyAssign method, of class DoubleArray.
   */
  @Test
  public void testMultiplyAssign_doubleArr_doubleArr()
  {
    double[] in = tg.newArray(10);
    double[] orig = DoubleArray.copyOf(in);
    double[] p = tg.newArray(10);
    DoubleArray.multiplyAssign(in, p);
    for (int i = 0; i < 10; ++i)
      Assert.assertEquals(in[i], orig[i] * p[i]);
  }

  /**
   * Test of multiplyAssign method, of class DoubleArray.
   */
  @Test
  public void testMultiplyAssign_5args()
  {
  }

  /**
   * Test of multiplyAssignRange method, of class DoubleArray.
   */
  @Test
  public void testMultiplyAssignRange()
  {
  }

  /**
   * Test of multiplyInner method, of class DoubleArray.
   */
  @Test
  public void testMultiplyInner_doubleArr_doubleArr()
  {
  }

  /**
   * Test of multiplyInner method, of class DoubleArray.
   */
  @Test
  public void testMultiplyInner_5args()
  {
  }

  /**
   * Test of divideAssign method, of class DoubleArray.
   */
  @Test
  public void testDivideAssign()
  {
    double[] in = tg.newArray(10);
    double[] orig = DoubleArray.copyOf(in);
    DoubleArray.divideAssign(in, 4.231);
    for (int i = 0; i < 10; ++i)
      Assert.assertEquals(in[i], orig[i] / 4.231);
  }

  /**
   * Test of divideAssignRange method, of class DoubleArray.
   */
  @Test
  public void testDivideAssignRange()
  {
  }

  /**
   * Test of fromString method, of class DoubleArray.
   */
  @Test
  public void testFromString()
  {
  }

  /**
   * Test of toString method, of class DoubleArray.
   */
  @Test
  public void testToString_doubleArr()
  {
  }

  /**
   * Test of toString method, of class DoubleArray.
   */
  @Test
  public void testToString_doubleArr_String()
  {
  }

  /**
   * Test of normColumns1 method, of class DoubleArray.
   */
  @Test
  public void testNormColumns1()
  {
    double[] in = tg.newArray(10);
    DoubleArray.normColumns1(in);
    double sum = 0;
    for (int i = 0; i < in.length; ++i)
      sum += Math.abs(in[i]);
    Assert.assertTrue(DoubleUtilities.equivalent(sum, 1.0));
  }

  /**
   * Test of normColumns2 method, of class DoubleArray.
   */
  @Test
  public void testNormColumns2()
  {
    double[] in = tg.newArray(10);
    DoubleArray.normColumns2(in);
    double d = DoubleArray.sumSqr(in);
    Assert.assertTrue(DoubleUtilities.equivalent(d, 1.0));
  }

  /**
   * Test of normColumns1Range method, of class DoubleArray.
   */
  @Test
  public void testNormColumns1Range()
  {
    double[] in = tg.newArray(10);
    DoubleArray.normColumns1Range(in, 2, 8);
    double sum = 0;
    for (int i = 2; i < 8; ++i)
      sum += Math.abs(in[i]);
    Assert.assertTrue(DoubleUtilities.equivalent(sum, 1.0));
  }

  /**
   * Test of normColumns2Range method, of class DoubleArray.
   */
  @Test
  public void testNormColumns2Range()
  {
    double[] in = tg.newArray(10);
    DoubleArray.normColumns2Range(in, 2, 8);
    Assert.assertTrue(DoubleUtilities.equivalent(
            DoubleArray.sumSqrRange(in, 2, 8), 1.0));
  }

  /**
   * Test of sum method, of class DoubleArray.
   */
  @Test
  public void testSum()
  {
    double[] m = tg.newArray(10);
    double sum = 0;
    for (int i = 0; i < 10; i++)
      sum += m[i];
    Assert.assertTrue(sum == DoubleArray.sum(m));
  }

  /**
   * Test of sumRange method, of class DoubleArray.
   */
  @Test
  public void testSumRange()
  {
  }

  /**
   * Test of sumSqr method, of class DoubleArray.
   */
  @Test
  public void testSumSqr()
  {
    double[] in = tg.newArray(10);
    double sum = 0;
    for (int i = 0; i < in.length; ++i)
      sum += in[i] * in[i];
    Assert.assertTrue(DoubleUtilities.equivalent(sum, DoubleArray.sumSqr(in)));
  }

  /**
   * Test of sumSqrRange method, of class DoubleArray.
   */
  @Test
  public void testSumSqrRange()
  {
    double[] in = tg.newArray(10);
    double sum = 0;
    for (int i = 2; i < 8; ++i)
      sum += in[i] * in[i];
    Assert.assertTrue(sum == DoubleArray.sumSqrRange(in, 2, 8));
  }

  @Test
  public void testProject()
  {
    double[] u = {4, 2};
    double[] v = {2, 4};
    double[] actual = DoubleArray.project(u, v);
    double[] expected = {1.6, 3.2};
    for (int i = 0; i < u.length; i++)
      Assert.assertEquals(actual[i], expected[i], 0.1);
  }
  
  @Test
  public void testEuclideanDistance()
  {
    double[] u = {0, 0, 0};
    double[] v = {5, 0, -6};
    Assert.assertEquals(7.81, DoubleArray.computeEuclideanDistance(u, v), 0.1);
  }
  
  @Test
  public void testIsInElementWiseRange1()
  {
    double[] u = {4, 5};
    double[] v = {7, 9};
    double[] x = {5, 7};
    Assert.assertTrue(DoubleArray.isInElementWiseRange(x, u, v));
  }
  
  @Test
  public void testIsInElementWiseRange2()
  {
    double[] u = {4, 5};
    double[] v = {7, 9};
    double[] x = {2, 7}; // 1st coordinate out of range.
    Assert.assertFalse(DoubleArray.isInElementWiseRange(x, u, v));
  }
  
  @Test
  public void testIsInElementWiseRange3()
  {
    double[] u = {4, 5};
    double[] v = {7, 9};
    double[] x = {5, 14}; // 2nd coordinate out of range.
    Assert.assertFalse(DoubleArray.isInElementWiseRange(x, u, v));
  }
  
  /**
   * Test of findIndexOfMaximum method, of class DoubleArray.
   */
  @Test
  public void testFindIndexOfMaximum()
  {
    double[] in = tg.newArray(10);
    int index = DoubleArray.findIndexOfMaximum(in);
    for (int i = 0; i < in.length; ++i)
    {
      Assert.assertTrue(in[index] >= in[i]);
    }
  }

  /**
   * Test of findIndexOfMaximumRange method, of class DoubleArray.
   */
  @Test
  public void testFindIndexOfMaximumRange()
  {
  }

  /**
   * Test of findMaximumAbsolute method, of class DoubleArray.
   */
  @Test
  public void testFindMaximumAbsolute()
  {
    double[] in = tg.newArray(10);
    double v = DoubleArray.findMaximumAbsolute(in);
    for (int i = 0; i < in.length; ++i)
    {
      Assert.assertTrue(v >= Math.abs(in[i]));
    }
  }

  /**
   * Test of findIndexOfMinimum method, of class DoubleArray.
   */
  @Test
  public void testFindIndexOfMinimum()
  {
    double[] in = tg.newArray(10);
    int index = DoubleArray.findIndexOfMinimum(in);
    for (int i = 0; i < in.length; ++i)
    {
      Assert.assertTrue(in[index] <= in[i]);
    }
  }

  /**
   * Test of findIndexOfMinimumRange method, of class DoubleArray.
   */
  @Test
  public void testFindIndexOfMinimumRange()
  {
    double[] in = tg.newArray(10);
    int index = DoubleArray.findIndexOfMinimumRange(in, 2, 8);
    for (int i = 2; i < 8; ++i)
    {
      Assert.assertTrue(in[index] <= in[i]);
    }
  }

  /**
   * Test of findIndexOfExtrema method, of class DoubleArray.
   */
  @Test
  public void testFindIndexOfExtrema()
  {
    double[] in = tg.newArray(10);
    DoubleArray.findIndexOfExtrema(in);
    // TODO complete test
  }

  /**
   * Test of findIndexOfExtremaRange method, of class DoubleArray.
   */
  @Test
  public void testFindIndexOfExtremaRange()
  {
    double[] in = tg.newArray(10);
    DoubleArray.findIndexOfExtremaRange(in, 2, 8);
    // TODO complete test
  }

  /**
   * Test of findIndexOfFirstRange method, of class DoubleArray.
   */
  @Test
  public void testFindIndexOfFirstRange()
  {
    double[] in = tg.newArray(10);
    Arrays.sort(in);
    // Not a great test because assumes no repeats
    Assert.assertTrue(DoubleArray.findIndexOfFirstRange(in, 0, in.length,
            new DoubleConditional.Equal(in[5])) == 5);
  }

  /**
   * Test of findIndexOfLastRange method, of class DoubleArray.
   */
  @Test
  public void testFindIndexOfLastRange()
  {
    double[] in = tg.newArray(10);
    Arrays.sort(in);
    // Not a great test because assumes no repeats
    Assert.assertTrue(DoubleArray.findIndexOfFirstRange(in, 0, in.length,
            new DoubleConditional.Equal(in[5])) == 5);
  }

  /**
   * Test of findIndexOfRange method, of class DoubleArray.
   */
  @Test
  public void testFindIndexOfRange()
  {
    double[] in = tg.newArray(10);
    int max = DoubleArray.findIndexOfRange(in, 0, in.length, new DoubleComparator.Positive());
    int min = DoubleArray.findIndexOfRange(in, 0, in.length, new DoubleComparator.Negative());
    for (int i = 0; i < in.length; ++i)
    {
      Assert.assertTrue(in[max] >= in[i]);
    }
    for (int i = 0; i < in.length; ++i)
    {
      Assert.assertTrue(in[min] <= in[i]);
    }
  }

  /**
   * Test of findIndexOfMedian method, of class DoubleArray.
   */
  @Test
  public void testFindIndexOfMedian()
  {
  }

  /**
   * Test of equivalent method, of class DoubleArray.
   */
  @Test
  public void testEquals_doubleArr_doubleArr()
  {
    double[] m1 = tg.newArray(10);
    double[] m2 = DoubleArray.copyOf(m1);
    for (int i = 0; i < 10; ++i)
    {
      m2[i] = m2[i] + 1;
      Assert.assertTrue(!DoubleArray.equivalent(m1, m2));
      m2[i] = m2[i] - 1;
      Assert.assertTrue(DoubleArray.equivalent(m1, m2));
    }
  }

  /**
   * Test of equivalent method, of class DoubleArray.
   */
  @Test
  public void testEquals_5args()
  {
    double[] m1 = tg.newArray(10);
    double[] m2 = DoubleArray.copyOf(m1);
    for (int i = 0; i < 10; ++i)
    {
      boolean b = (i >= 2 && i < 8);
      m2[i] = m2[i] + 1;
      Assert.assertTrue(DoubleArray.equivalent(m1, 2, m2, 2, 6) != b);
      m2[i] = m2[i] - 1;
      Assert.assertTrue(DoubleArray.equivalent(m1, 2, m2, 2, 6));
    }
  }

  /**
   * Test of isNaN method, of class DoubleArray.
   */
  @Test
  public void testIsNaN()
  {
    double[] in = tg.newArray(5);
    Assert.assertTrue(!DoubleArray.isNaN(in));
    in[4] = Double.NaN;
    Assert.assertTrue(DoubleArray.isNaN(in));
  }

  /**
   * Test of isNaNRange method, of class DoubleArray.
   */
  @Test
  public void testIsNaNRange()
  {
    double[] in = tg.newArray(5);
    Assert.assertTrue(!DoubleArray.isNaNRange(in, 0, 5));
    in[4] = Double.NaN;
    Assert.assertTrue(DoubleArray.isNaNRange(in, 0, 5));
    Assert.assertTrue(!DoubleArray.isNaNRange(in, 0, 4));
  }

  /**
   * Test of toPrimitives method, of class DoubleArray.
   */
  @Test
  public void testToPrimitives_DoubleArr()
  {
  }

  /**
   * Test of newArray method, of class DoubleArray.
   */
  @Test
  public void testNewArray()
  {
    double[] a = DoubleArray.newArray(0, 1, 2, 3, 4);
    for (int i = 0; i < 5; ++i)
      Assert.assertEquals(a[i], i * 1.0);
  }

  /**
   * Test of toPrimitives method, of class DoubleArray.
   */
  @Test
  public void testToPrimitives_Collection()
  {
  }

  /**
   * Test of toObjects method, of class DoubleArray.
   */
  @Test
  public void testToObjects()
  {
  }

  /**
   * Test of exp method, of class DoubleArray.
   */
  @Test
  public void testExp()
  {
    double[] m1 = tg.newArray(10);
    double[] m2 = DoubleArray.exp(m1);
    for (int i = 0; i < 10; ++i)
    {
      Assert.assertTrue(Math.exp(m1[i]) == m2[i]);
    }
  }

//  /**
//   * Test of test method, of class DoubleArray.
//   */
//  @Test
//  public void testEvaluate() throws NoSuchMethodException
//  {
//    Method expm = Math.class.getMethod("exp", Double.TYPE);
//    double[] m1 = tg.newArray(10);
//    double[] m2 = DoubleArray.test(expm, m1);
//    for (int i = 0; i < 10; ++i)
//    {
//      Assert.assertTrue(Math.exp(m1[i]) == m2[i]);
//    }
//  }

  /**
   * Test of getField method, of class DoubleArray.
   */
  @Test
  public void testGetField_GenericType_Field() throws Exception
  {
  }

  /**
   * Test of getField method, of class DoubleArray.
   */
  @Test
  public void testGetField_Collection_Field() throws Exception
  {
  }

  /**
   * Test of sortPairs method, of class DoubleArray.
   */
  @Test
  public void testSortPairs()
  {
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