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

import gov.llnl.ernie.data.VehicleClass;
import gov.llnl.ernie.vehicle.VehicleInfoImpl;
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
public class VM250VehicleClassNGTest
{
  
  public VM250VehicleClassNGTest()
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
   * Test of getScale method, of class VM250VehicleClass.
   */
  @Test
  public void testGetScale()
  {
    System.out.println("getScale");
    VM250VehicleClass instance = new VM250VehicleClass();
    double expResult = 0.0;
    double result = instance.getScale();
    assertEquals(result, expResult, 0.0);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getInfo method, of class VM250VehicleClass.
   */
  @Test
  public void testGetInfo()
  {
    System.out.println("getInfo");
    VM250VehicleClass instance = new VM250VehicleClass();
    VehicleInfoImpl expResult = null;
    VehicleInfoImpl result = instance.getInfo();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getBackgroundModels method, of class VM250VehicleClass.
   */
  @Test
  public void testGetBackgroundModels()
  {
    System.out.println("getBackgroundModels");
    VM250VehicleClass instance = new VM250VehicleClass();
    List expResult = null;
    List result = instance.getBackgroundModels();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of addBackgroundModel method, of class VM250VehicleClass.
   */
  @Test
  public void testAddBackgroundModel()
  {
    System.out.println("addBackgroundModel");
    VehicleClass.BackgroundModel bm = null;
    VM250VehicleClass instance = new VM250VehicleClass();
    instance.addBackgroundModel(bm);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setTemplate method, of class VM250VehicleClass.
   */
  @Test
  public void testSetTemplate()
  {
    System.out.println("setTemplate");
    double[] template = null;
    VM250VehicleClass instance = new VM250VehicleClass();
    instance.setTemplate(template);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getTemplate method, of class VM250VehicleClass.
   */
  @Test
  public void testGetTemplate()
  {
    System.out.println("getTemplate");
    VM250VehicleClass instance = new VM250VehicleClass();
    double[] expResult = null;
    double[] result = instance.getTemplate();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of renormalizeTemplate method, of class VM250VehicleClass.
   */
  @Test
  public void testRenormalizeTemplate()
  {
    System.out.println("renormalizeTemplate");
    VM250VehicleClass instance = new VM250VehicleClass();
    instance.renormalizeTemplate();
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getRepresentatives method, of class VM250VehicleClass.
   */
  @Test
  public void testGetRepresentatives()
  {
    System.out.println("getRepresentatives");
    VM250VehicleClass instance = new VM250VehicleClass();
    List expResult = null;
    List result = instance.getRepresentatives();
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