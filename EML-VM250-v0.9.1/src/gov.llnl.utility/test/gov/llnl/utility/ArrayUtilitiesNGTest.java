/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.utility;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 *
 * @author nelson85
 */
public class ArrayUtilitiesNGTest
{

  public ArrayUtilitiesNGTest()
  {
  }

  /**
   * Test of parseDoubleFromString method, of class ArrayUtilities.
   */
  @Test
  public void testParseDoubleFromString()
  {
    Assert.assertEquals(ArrayUtilities.parseDoubleFromString("foo0bar", 3, 4), 0.0);
    Assert.assertEquals(ArrayUtilities.parseDoubleFromString("foo1bar", 3, 4), 1.0);
    Assert.assertEquals(ArrayUtilities.parseDoubleFromString("foo2bar", 3, 4), 2.0);
    Assert.assertEquals(ArrayUtilities.parseDoubleFromString("foo2.bar", 3, 5), 2.0);
    Assert.assertEquals(ArrayUtilities.parseDoubleFromString("foo2e1bar", 3, 6), 20.0);
    Assert.assertEquals(ArrayUtilities.parseDoubleFromString("foo2e-1bar", 3, 7), 0.2);
  }

  /**
   * Test of pow10 method, of class ArrayUtilities.
   */
  @Test
  public void testPow10()
  {
    Assert.assertEquals(ArrayUtilities.pow10(0, false), 1.0);
    Assert.assertEquals(ArrayUtilities.pow10(1, false), 10.0);
    Assert.assertEquals(ArrayUtilities.pow10(2, false), 100.0);
    Assert.assertEquals(ArrayUtilities.pow10(3, false), 1000.0);
    Assert.assertEquals(ArrayUtilities.pow10(4, false), 10000.0);
    Assert.assertEquals(ArrayUtilities.pow10(5, false), 100000.0);

    Assert.assertEquals(ArrayUtilities.pow10(0, true), 1.0);
    Assert.assertEquals(ArrayUtilities.pow10(1, true), 0.1);
    Assert.assertEquals(ArrayUtilities.pow10(2, true), 0.01);
    Assert.assertEquals(ArrayUtilities.pow10(3, true), 0.001);
    Assert.assertEquals(ArrayUtilities.pow10(4, true), 0.0001);
    Assert.assertEquals(ArrayUtilities.pow10(5, true), 0.00001);
  }

  /**
   * Test of parseIntegerFromString method, of class ArrayUtilities.
   */
  @Test
  public void testParseIntegerFromString()
  {
  }

  /**
   * Test of parseDoubleArray method, of class ArrayUtilities.
   */
  @Test
  public void testParseDoubleArray()
  {
  }

  /**
   * Test of countDelimitor method, of class ArrayUtilities.
   */
  @Test
  public void testCountDelimitor()
  {
  }

  /**
   * Test of fastSplit method, of class ArrayUtilities.
   */
  @Test
  public void testFastSplit()
  {
  }

  /**
   * Test of parseIntArray method, of class ArrayUtilities.
   */
  @Test
  public void testParseIntArray()
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