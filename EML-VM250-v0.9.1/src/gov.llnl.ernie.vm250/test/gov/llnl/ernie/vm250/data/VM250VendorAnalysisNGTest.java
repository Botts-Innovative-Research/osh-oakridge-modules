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
package gov.llnl.ernie.vm250.data;

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
public class VM250VendorAnalysisNGTest
{
  
  public VM250VendorAnalysisNGTest()
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
   * Test of isAlarm method, of class VM250VendorAnalysis.
   */
  @Test
  public void testIsAlarm()
  {
    System.out.println("isAlarm");
    VM250VendorAnalysis instance = null;
    boolean expResult = false;
    boolean result = instance.isAlarm();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of isGammaAlarm method, of class VM250VendorAnalysis.
   */
  @Test
  public void testIsGammaAlarm()
  {
    System.out.println("isGammaAlarm");
    VM250VendorAnalysis instance = null;
    boolean expResult = false;
    boolean result = instance.isGammaAlarm();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of isNeutronAlarm method, of class VM250VendorAnalysis.
   */
  @Test
  public void testIsNeutronAlarm()
  {
    System.out.println("isNeutronAlarm");
    VM250VendorAnalysis instance = null;
    boolean expResult = false;
    boolean result = instance.isNeutronAlarm();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of isScanError method, of class VM250VendorAnalysis.
   */
  @Test
  public void testIsScanError()
  {
    System.out.println("isScanError");
    VM250VendorAnalysis instance = null;
    boolean expResult = false;
    boolean result = instance.isScanError();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getNumSegments method, of class VM250VendorAnalysis.
   */
  @Test
  public void testGetNumSegments()
  {
    System.out.println("getNumSegments");
    VM250VendorAnalysis instance = null;
    int expResult = 0;
    int result = instance.getNumSegments();
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