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

import java.time.Duration;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalUnit;
import java.util.List;
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
public class MutableDurationNGTest
{
  
  public MutableDurationNGTest()
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
   * Test of get method, of class MutableDuration.
   */
  @Test
  public void testGet()
  {
    System.out.println("get");
    TemporalUnit unit = null;
    MutableDuration instance = new MutableDuration();
    long expResult = 0L;
    long result = instance.get(unit);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getUnits method, of class MutableDuration.
   */
  @Test
  public void testGetUnits()
  {
    System.out.println("getUnits");
    MutableDuration instance = new MutableDuration();
    List expResult = null;
    List result = instance.getUnits();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of addTo method, of class MutableDuration.
   */
  @Test
  public void testAddTo()
  {
    System.out.println("addTo");
    Temporal temporal = null;
    MutableDuration instance = new MutableDuration();
    Temporal expResult = null;
    Temporal result = instance.addTo(temporal);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of subtractFrom method, of class MutableDuration.
   */
  @Test
  public void testSubtractFrom()
  {
    System.out.println("subtractFrom");
    Temporal temporal = null;
    MutableDuration instance = new MutableDuration();
    Temporal expResult = null;
    Temporal result = instance.subtractFrom(temporal);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of addAssign method, of class MutableDuration.
   */
  @Test
  public void testAddAssign()
  {
    System.out.println("addAssign");
    Duration d = null;
    MutableDuration instance = new MutableDuration();
    instance.addAssign(d);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of toDuration method, of class MutableDuration.
   */
  @Test
  public void testToDuration()
  {
    System.out.println("toDuration");
    MutableDuration instance = new MutableDuration();
    Duration expResult = null;
    Duration result = instance.toDuration();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of compareTo method, of class MutableDuration.
   */
  @Test
  public void testCompareTo()
  {
    System.out.println("compareTo");
    TemporalAmount o = null;
    MutableDuration instance = new MutableDuration();
    int expResult = 0;
    int result = instance.compareTo(o);
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