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

import support.IntegerTestGenerator;
import static org.testng.Assert.assertEquals;
import org.testng.annotations.Test;

/**
 *
 * @author nelson85
 */
public class IntegerArrayNGTest
{
  IntegerTestGenerator tg = new IntegerTestGenerator();

  public IntegerArrayNGTest()
  {
  }

  /**
   * Test of assign method, of class IntegerArray.
   */
  @Test
  public void testAssign()
  {
  }

  /**
   * Test of copyOf method, of class IntegerArray.
   */
  @Test
  public void testCopyOf()
  {
  }

  /**
   * Test of copyOfRange method, of class IntegerArray.
   */
  @Test
  public void testCopyOfRange()
  {
  }

  /**
   * Test of fill method, of class IntegerArray.
   */
  @Test
  public void testFill()
  {
  }

  /**
   * Test of fillRange method, of class IntegerArray.
   */
  @Test
  public void testFillRange()
  {
  }

  /**
   * Test of colon method, of class IntegerArray.
   */
  @Test
  public void testColon()
  {
    int[] v = IntegerArray.colon(10, 15);
    int j = 0;
    for (int i = 10; i < 15; ++i, ++j)
    {
      assertEquals(v[j], i);
    }
  }

  /**
   * Test of add method, of class IntegerArray.
   */
  @Test
  public void testAdd()
  {
  }

  /**
   * Test of addAssign method, of class IntegerArray.
   */
  @Test
  public void testAddAssign()
  {
    for (int i = 0; i < 10; ++i)
    {
      int len = tg.uniformRand(1, 65);
      int[] a1 = tg.newArray(len);
      int[] a2 = tg.newArray(len);
      int[] a3 = new int[len];
      for (int j = 0; j < len; ++j)
      {
        a3[j] = a1[j] + a2[j];
      }
      IntegerArray.addAssign(a1, a2);

      for (int j = 0; j < len; ++j)
      {
        assertEquals(a3[j], a1[j]);
      }
    }
  }

  /**
   * Test of subtractAssign method, of class IntegerArray.
   */
  @Test
  public void testSubtractAssign()
  {
  }

  /**
   * Test of fromString method, of class IntegerArray.
   */
  @Test
  public void testFromString()
  {
  }

  /**
   * Test of toString method, of class IntegerArray.
   */
  @Test
  public void testToString()
  {
  }

  /**
   * Test of sum method, of class IntegerArray.
   */
  @Test
  public void testSum()
  {
  }

  /**
   * Test of sumRange method, of class IntegerArray.
   */
  @Test
  public void testSumRange()
  {
  }

  /**
   * Test of findIndexOfMinimum method, of class IntegerArray.
   */
  @Test
  public void testFindIndexOfMinimum()
  {
  }

  /**
   * Test of findIndexOfMaximum method, of class IntegerArray.
   */
  @Test
  public void testFindIndexOfMaximum()
  {
  }

  /**
   * Test of findIndexOfMinimumRange method, of class IntegerArray.
   */
  @Test
  public void testFindIndexOfMinimumRange()
  {
  }

  /**
   * Test of findIndexOfMaximumRange method, of class IntegerArray.
   */
  @Test
  public void testFindIndexOfMaximumRange()
  {
  }

  /**
   * Test of findIndexOfExtrema method, of class IntegerArray.
   */
  @Test
  public void testFindIndexOfExtrema()
  {
  }

  /**
   * Test of findIndexOfExtremaRange method, of class IntegerArray.
   */
  @Test
  public void testFindIndexOfExtremaRange()
  {
  }

  /**
   * Test of findIndexOfFirstRange method, of class IntegerArray.
   */
  @Test
  public void testFindIndexOfFirstRange()
  {
    int[] v = IntegerArray.colon(0, 5);
    int[] ans = new int[]
    {
      0, 0, -1, -1,
      1, 0, -1, 0,
      2, 1, 0, 0,
      3, 2, 0, 0,
      4, 3, 0, 0,
      -1, 4, 0, 0,
      -1, -1, 0, 0
    };

    int j = 0;
    for (int i = -1; i < 6; i++)
    {
      int u0 = IntegerArray.findIndexOfFirstRange(v, 0, v.length, new IntegerConditional.GreaterThan(i));
      int u1 = IntegerArray.findIndexOfFirstRange(v, 0, v.length, new IntegerConditional.GreaterThanEqual(i));
      int u2 = IntegerArray.findIndexOfFirstRange(v, 0, v.length, new IntegerConditional.LessThan(i));
      int u3 = IntegerArray.findIndexOfFirstRange(v, 0, v.length, new IntegerConditional.LessThanEqual(i));

      assertEquals(u0, ans[j++]);
      assertEquals(u1, ans[j++]);
      assertEquals(u2, ans[j++]);
      assertEquals(u3, ans[j++]);
    }
  }

  /**
   * Test of findIndexOfLastRange method, of class IntegerArray.
   */
  @Test
  public void testFindIndexOfLastRange()
  {
    int[] v = IntegerArray.colon(0, 5);
    int[] ans = new int[]
    {
      4, 4, -1, -1, // -1
      4, 4, -1, 0, //   0
      4, 4, 0, 1, //    1 
      4, 4, 1, 2, //    2
      4, 4, 2, 3, //    3
      -1, 4, 3, 4, //   4
      -1, -1, 4, 4  //  5
    };

    int j = 0;
    for (int i = -1; i < 6; i++)
    {
      int u0 = IntegerArray.findIndexOfLastRange(v, 0, v.length, new IntegerConditional.GreaterThan(i));
      int u1 = IntegerArray.findIndexOfLastRange(v, 0, v.length, new IntegerConditional.GreaterThanEqual(i));
      int u2 = IntegerArray.findIndexOfLastRange(v, 0, v.length, new IntegerConditional.LessThan(i));
      int u3 = IntegerArray.findIndexOfLastRange(v, 0, v.length, new IntegerConditional.LessThanEqual(i));

      assertEquals(u0, ans[j++]);
      assertEquals(u1, ans[j++]);
      assertEquals(u2, ans[j++]);
      assertEquals(u3, ans[j++]);
    }
  }

  /**
   * Test of anyEquals method, of class IntegerArray.
   */
  @Test
  public void testAnyEquals()
  {
  }

  /**
   * Test of promoteToDoubles method, of class IntegerArray.
   */
  @Test
  public void testPromoteToDoubles()
  {
  }

  /**
   * Test of toPrimitives method, of class IntegerArray.
   */
  @Test
  public void testToPrimitives_IntegerArr()
  {
  }

  /**
   * Test of toPrimitives method, of class IntegerArray.
   */
  @Test
  public void testToPrimitives_Collection()
  {
  }

  /**
   * Test of toObjects method, of class IntegerArray.
   */
  @Test
  public void testToObjects()
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