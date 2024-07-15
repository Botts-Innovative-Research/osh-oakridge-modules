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

import gov.llnl.ernie.data.Record;
import gov.llnl.ernie.data.VehicleClass;
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
public class VM250VehicleClassifierNGTest
{
  
  public VM250VehicleClassifierNGTest()
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
   * Test of getCorrelation method, of class VM250VehicleClassifier.
   */
  @Test
  public void testGetCorrelation()
  {
    System.out.println("getCorrelation");
    Record record = null;
    VehicleClass cls = null;
    VM250VehicleClassifier instance = new VM250VehicleClassifier();
    double expResult = 0.0;
    double result = instance.getCorrelation(record, cls);
    assertEquals(result, expResult, 0.0);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of compute method, of class VM250VehicleClassifier.
   */
  @Test
  public void testCompute() throws Exception
  {
    System.out.println("compute");
    Record record = null;
    VM250VehicleClassifier instance = new VM250VehicleClassifier();
    instance.compute(record);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of classify method, of class VM250VehicleClassifier.
   */
  @Test
  public void testClassify() throws Exception
  {
    System.out.println("classify");
    Record record = null;
    VM250VehicleClassifier instance = new VM250VehicleClassifier();
    VehicleClass expResult = null;
    VehicleClass result = instance.classify(record);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getVehicleClass method, of class VM250VehicleClassifier.
   */
  @Test
  public void testGetVehicleClass()
  {
    System.out.println("getVehicleClass");
    int i = 0;
    VM250VehicleClassifier instance = new VM250VehicleClassifier();
    VehicleClass expResult = null;
    VehicleClass result = instance.getVehicleClass(i);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getNumVehicleClasses method, of class VM250VehicleClassifier.
   */
  @Test
  public void testGetNumVehicleClasses()
  {
    System.out.println("getNumVehicleClasses");
    VM250VehicleClassifier instance = new VM250VehicleClassifier();
    int expResult = 0;
    int result = instance.getNumVehicleClasses();
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