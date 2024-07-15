/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gov.llnl.ernie.vm250;

import static org.testng.Assert.*;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 *
 * @author pham21
 */
public class VelocityPairsNGTest
{
  
  public VelocityPairsNGTest()
  {
  }

  @BeforeClass
  public static void setUpClass() throws Exception
  {
  }

  @AfterClass
  public static void tearDownClass() throws Exception
  {
  }

  @BeforeMethod
  public void setUpMethod() throws Exception
  {
  }

  @AfterMethod
  public void tearDownMethod() throws Exception
  {
  }

  /**
   * Test of add method, of class VelocityPairs.
   */
  @Test
  public void testAdd()
  {
    System.out.println("add");
    double t1 = 0.0;
    double v1 = 0.0;
    VelocityPairs instance = new VelocityPairs();
    instance.add(t1, v1);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getPosition method, of class VelocityPairs.
   */
  @Test
  public void testGetPosition()
  {
    System.out.println("getPosition");
    double time = 0.0;
    VelocityPairs instance = new VelocityPairs();
    double expResult = 0.0;
    double result = instance.getPosition(time);
    assertEquals(result, expResult, 0.0);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of size method, of class VelocityPairs.
   */
  @Test
  public void testSize()
  {
    System.out.println("size");
    VelocityPairs instance = new VelocityPairs();
    int expResult = 0;
    int result = instance.size();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of clear method, of class VelocityPairs.
   */
  @Test
  public void testClear()
  {
    System.out.println("clear");
    VelocityPairs instance = new VelocityPairs();
    instance.clear();
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of augmentPositions method, of class VelocityPairs.
   */
  @Test
  public void testAugmentPositions()
  {
    System.out.println("augmentPositions");
    VelocityPairs instance = new VelocityPairs();
    instance.augmentPositions();
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of get method, of class VelocityPairs.
   */
  @Test
  public void testGet()
  {
    System.out.println("get");
    int i = 0;
    VelocityPairs instance = new VelocityPairs();
    VelocityPairs.Entry expResult = null;
    VelocityPairs.Entry result = instance.get(i);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of first method, of class VelocityPairs.
   */
  @Test
  public void testFirst()
  {
    System.out.println("first");
    VelocityPairs instance = new VelocityPairs();
    VelocityPairs.Entry expResult = null;
    VelocityPairs.Entry result = instance.first();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of last method, of class VelocityPairs.
   */
  @Test
  public void testLast()
  {
    System.out.println("last");
    VelocityPairs instance = new VelocityPairs();
    VelocityPairs.Entry expResult = null;
    VelocityPairs.Entry result = instance.last();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
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