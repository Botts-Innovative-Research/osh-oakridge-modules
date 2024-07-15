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
public class VM250VehicleMotionNGTest
{
  
  public VM250VehicleMotionNGTest()
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
   * Test of isGood method, of class VM250VehicleMotion.
   */
  @Test
  public void testIsGood()
  {
    System.out.println("isGood");
    VM250VehicleMotion instance = new VM250VehicleMotion();
    boolean expResult = false;
    boolean result = instance.isGood();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of isReverse method, of class VM250VehicleMotion.
   */
  @Test
  public void testIsReverse()
  {
    System.out.println("isReverse");
    VM250VehicleMotion instance = new VM250VehicleMotion();
    boolean expResult = false;
    boolean result = instance.isReverse();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getPosition method, of class VM250VehicleMotion.
   */
  @Test
  public void testGetPosition_Instant()
  {
    System.out.println("getPosition");
    Instant time = null;
    VM250VehicleMotion instance = new VM250VehicleMotion();
    double expResult = 0.0;
    double result = instance.getPosition(time);
    assertEquals(result, expResult, 0.0);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getVehicleLength method, of class VM250VehicleMotion.
   */
  @Test
  public void testGetVehicleLength()
  {
    System.out.println("getVehicleLength");
    VM250VehicleMotion instance = new VM250VehicleMotion();
    double expResult = 0.0;
    double result = instance.getVehicleLength();
    assertEquals(result, expResult, 0.0);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getVelocity method, of class VM250VehicleMotion.
   */
  @Test
  public void testGetVelocity()
  {
    System.out.println("getVelocity");
    VM250VehicleMotion instance = new VM250VehicleMotion();
    double[] expResult = null;
    double[] result = instance.getVelocity();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getVelocityInitial method, of class VM250VehicleMotion.
   */
  @Test
  public void testGetVelocityInitial()
  {
    System.out.println("getVelocityInitial");
    VM250VehicleMotion instance = new VM250VehicleMotion();
    double expResult = 0.0;
    double result = instance.getVelocityInitial();
    assertEquals(result, expResult, 0.0);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getVelocityFinal method, of class VM250VehicleMotion.
   */
  @Test
  public void testGetVelocityFinal()
  {
    System.out.println("getVelocityFinal");
    VM250VehicleMotion instance = new VM250VehicleMotion();
    double expResult = 0.0;
    double result = instance.getVelocityFinal();
    assertEquals(result, expResult, 0.0);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getPosition method, of class VM250VehicleMotion.
   */
  @Test
  public void testGetPosition_0args()
  {
    System.out.println("getPosition");
    VM250VehicleMotion instance = new VM250VehicleMotion();
    double[] expResult = null;
    double[] result = instance.getPosition();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setPosition method, of class VM250VehicleMotion.
   */
  @Test
  public void testSetPosition()
  {
    System.out.println("setPosition");
    double[] position = null;
    VM250VehicleMotion instance = new VM250VehicleMotion();
    instance.setPosition(position);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setVelocity method, of class VM250VehicleMotion.
   */
  @Test
  public void testSetVelocity()
  {
    System.out.println("setVelocity");
    double[] velocity = null;
    VM250VehicleMotion instance = new VM250VehicleMotion();
    instance.setVelocity(velocity);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setVelocityInitial method, of class VM250VehicleMotion.
   */
  @Test
  public void testSetVelocityInitial()
  {
    System.out.println("setVelocityInitial");
    double v0 = 0.0;
    VM250VehicleMotion instance = new VM250VehicleMotion();
    instance.setVelocityInitial(v0);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setVelocityFinal method, of class VM250VehicleMotion.
   */
  @Test
  public void testSetVelocityFinal()
  {
    System.out.println("setVelocityFinal");
    double v0 = 0.0;
    VM250VehicleMotion instance = new VM250VehicleMotion();
    instance.setVelocityFinal(v0);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setStartTime method, of class VM250VehicleMotion.
   */
  @Test
  public void testSetStartTime()
  {
    System.out.println("setStartTime");
    double startTime = 0.0;
    VM250VehicleMotion instance = new VM250VehicleMotion();
    instance.setStartTime(startTime);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setEndTime method, of class VM250VehicleMotion.
   */
  @Test
  public void testSetEndTime()
  {
    System.out.println("setEndTime");
    double endTime = 0.0;
    VM250VehicleMotion instance = new VM250VehicleMotion();
    instance.setEndTime(endTime);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getStartTime method, of class VM250VehicleMotion.
   */
  @Test
  public void testGetStartTime()
  {
    System.out.println("getStartTime");
    VM250VehicleMotion instance = new VM250VehicleMotion();
    double expResult = 0.0;
    double result = instance.getStartTime();
    assertEquals(result, expResult, 0.0);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getEndTime method, of class VM250VehicleMotion.
   */
  @Test
  public void testGetEndTime()
  {
    System.out.println("getEndTime");
    VM250VehicleMotion instance = new VM250VehicleMotion();
    double expResult = 0.0;
    double result = instance.getEndTime();
    assertEquals(result, expResult, 0.0);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setReferenceTime method, of class VM250VehicleMotion.
   */
  @Test
  public void testSetReferenceTime()
  {
    System.out.println("setReferenceTime");
    Instant t0 = null;
    VM250VehicleMotion instance = new VM250VehicleMotion();
    instance.setReferenceTime(t0);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setVehicleLength method, of class VM250VehicleMotion.
   */
  @Test
  public void testSetVehicleLength()
  {
    System.out.println("setVehicleLength");
    double vehicleLength = 0.0;
    VM250VehicleMotion instance = new VM250VehicleMotion();
    instance.setVehicleLength(vehicleLength);
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