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

import gov.llnl.ernie.data.VPSProperties;
import java.time.Instant;
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
public class VM250VPSMeasurementNGTest
{
  
  public VM250VPSMeasurementNGTest()
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
   * Test of getVPSProperties method, of class VM250VPSMeasurement.
   */
  @Test
  public void testGetVPSProperties()
  {
    System.out.println("getVPSProperties");
    VM250VPSMeasurement instance = null;
    VPSProperties expResult = null;
    VPSProperties result = instance.getVPSProperties();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getDirectionTraveled method, of class VM250VPSMeasurement.
   */
  @Test
  public void testGetDirectionTraveled()
  {
    System.out.println("getDirectionTraveled");
    VM250VPSMeasurement instance = null;
    boolean expResult = false;
    boolean result = instance.getDirectionTraveled();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getGammaOccupancyStart method, of class VM250VPSMeasurement.
   */
  @Test
  public void testGetGammaOccupancyStart()
  {
    System.out.println("getGammaOccupancyStart");
    VM250VPSMeasurement instance = null;
    int expResult = 0;
    int result = instance.getGammaOccupancyStart();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getGammaOccupancyEnd method, of class VM250VPSMeasurement.
   */
  @Test
  public void testGetGammaOccupancyEnd()
  {
    System.out.println("getGammaOccupancyEnd");
    VM250VPSMeasurement instance = null;
    int expResult = 0;
    int result = instance.getGammaOccupancyEnd();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getNeutronOccupancyStart method, of class VM250VPSMeasurement.
   */
  @Test
  public void testGetNeutronOccupancyStart()
  {
    System.out.println("getNeutronOccupancyStart");
    VM250VPSMeasurement instance = null;
    int expResult = 0;
    int result = instance.getNeutronOccupancyStart();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getNeutronOccupancyEnd method, of class VM250VPSMeasurement.
   */
  @Test
  public void testGetNeutronOccupancyEnd()
  {
    System.out.println("getNeutronOccupancyEnd");
    VM250VPSMeasurement instance = null;
    int expResult = 0;
    int result = instance.getNeutronOccupancyEnd();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getOccupancyStart method, of class VM250VPSMeasurement.
   */
  @Test
  public void testGetOccupancyStart()
  {
    System.out.println("getOccupancyStart");
    VM250VPSMeasurement instance = null;
    Instant expResult = null;
    Instant result = instance.getOccupancyStart();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getOccupancyEnd method, of class VM250VPSMeasurement.
   */
  @Test
  public void testGetOccupancyEnd()
  {
    System.out.println("getOccupancyEnd");
    VM250VPSMeasurement instance = null;
    Instant expResult = null;
    Instant result = instance.getOccupancyEnd();
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