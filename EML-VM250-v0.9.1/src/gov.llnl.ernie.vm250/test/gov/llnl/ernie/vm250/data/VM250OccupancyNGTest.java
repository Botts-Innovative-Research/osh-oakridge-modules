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

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.javatuples.Triplet;
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
public class VM250OccupancyNGTest
{
  
  public VM250OccupancyNGTest()
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
   * Test of of method, of class VM250Occupancy.
   */
  @Test
  public void testOf_List()
  {
    System.out.println("of");
    List<Triplet<String, List<String>, Instant>> parsedOccupancyDataList = null;
    VM250Occupancy expResult = null;
    VM250Occupancy result = VM250Occupancy.of(parsedOccupancyDataList);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of of method, of class VM250Occupancy.
   */
  @Test
  public void testOf_List_boolean()
  {
    System.out.println("of");
    List<Triplet<String, List<String>, Instant>> parsedOccupancyDataList = null;
    boolean continuation = false;
    VM250Occupancy expResult = null;
    VM250Occupancy result = VM250Occupancy.of(parsedOccupancyDataList, continuation);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of isContinuation method, of class VM250Occupancy.
   */
  @Test
  public void testIsContinuation()
  {
    System.out.println("isContinuation");
    VM250Occupancy instance = null;
    boolean expResult = false;
    boolean result = instance.isContinuation();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of isGammaAlarm method, of class VM250Occupancy.
   */
  @Test
  public void testIsGammaAlarm()
  {
    System.out.println("isGammaAlarm");
    VM250Occupancy instance = null;
    boolean expResult = false;
    boolean result = instance.isGammaAlarm();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of isNeutronAlarm method, of class VM250Occupancy.
   */
  @Test
  public void testIsNeutronAlarm()
  {
    System.out.println("isNeutronAlarm");
    VM250Occupancy instance = null;
    boolean expResult = false;
    boolean result = instance.isNeutronAlarm();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of isRealOccupancy method, of class VM250Occupancy.
   */
  @Test
  public void testIsRealOccupancy()
  {
    System.out.println("isRealOccupancy");
    VM250Occupancy instance = null;
    boolean expResult = false;
    boolean result = instance.isRealOccupancy();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of isTamper method, of class VM250Occupancy.
   */
  @Test
  public void testIsTamper()
  {
    System.out.println("isTamper");
    VM250Occupancy instance = null;
    boolean expResult = false;
    boolean result = instance.isTamper();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getIndex method, of class VM250Occupancy.
   */
  @Test
  public void testGetIndex()
  {
    System.out.println("getIndex");
    VM250Occupancy instance = null;
    int expResult = 0;
    int result = instance.getIndex();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getRailCarIndex method, of class VM250Occupancy.
   */
  @Test
  public void testGetRailCarIndex()
  {
    System.out.println("getRailCarIndex");
    VM250Occupancy instance = null;
    int expResult = 0;
    int result = instance.getRailCarIndex();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getNeutronBackground method, of class VM250Occupancy.
   */
  @Test
  public void testGetNeutronBackground()
  {
    System.out.println("getNeutronBackground");
    VM250Occupancy instance = null;
    double expResult = 0.0;
    double result = instance.getNeutronBackground();
    assertEquals(result, expResult, 0.0);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getDuration method, of class VM250Occupancy.
   */
  @Test
  public void testGetDuration()
  {
    System.out.println("getDuration");
    VM250Occupancy instance = null;
    Duration expResult = null;
    Duration result = instance.getDuration();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getStartTime method, of class VM250Occupancy.
   */
  @Test
  public void testGetStartTime()
  {
    System.out.println("getStartTime");
    VM250Occupancy instance = null;
    Instant expResult = null;
    Instant result = instance.getStartTime();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getEndTime method, of class VM250Occupancy.
   */
  @Test
  public void testGetEndTime()
  {
    System.out.println("getEndTime");
    VM250Occupancy instance = null;
    Instant expResult = null;
    Instant result = instance.getEndTime();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getGammaDataList method, of class VM250Occupancy.
   */
  @Test
  public void testGetGammaDataList()
  {
    System.out.println("getGammaDataList");
    VM250Occupancy instance = null;
    List expResult = null;
    List result = instance.getGammaDataList();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getNeutronDataList method, of class VM250Occupancy.
   */
  @Test
  public void testGetNeutronDataList()
  {
    System.out.println("getNeutronDataList");
    VM250Occupancy instance = null;
    List expResult = null;
    List result = instance.getNeutronDataList();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getLastGammaBackgroundData method, of class VM250Occupancy.
   */
  @Test
  public void testGetLastGammaBackgroundData()
  {
    System.out.println("getLastGammaBackgroundData");
    VM250Occupancy instance = null;
    Triplet expResult = null;
    Triplet result = instance.getLastGammaBackgroundData();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getVelocityData method, of class VM250Occupancy.
   */
  @Test
  public void testGetVelocityData()
  {
    System.out.println("getVelocityData");
    VM250Occupancy instance = null;
    List expResult = null;
    List result = instance.getVelocityData();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of size method, of class VM250Occupancy.
   */
  @Test
  public void testSize()
  {
    System.out.println("size");
    VM250Occupancy instance = null;
    int expResult = 0;
    int result = instance.size();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setLastGammaBackgroundData method, of class VM250Occupancy.
   */
  @Test
  public void testSetLastGammaBackgroundData()
  {
    System.out.println("setLastGammaBackgroundData");
    Triplet<Instant, List<Integer>, String> lastGammaBackgroundData = null;
    VM250Occupancy instance = null;
    instance.setLastGammaBackgroundData(lastGammaBackgroundData);
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