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

import gov.llnl.ernie.data.Lane;
import gov.llnl.ernie.data.Record;
import gov.llnl.ernie.internal.manipulator.ShieldingModel;
import gov.llnl.ernie.manipulator.InjectionSourceLibrary;
import gov.llnl.ernie.manipulator.Manipulation;
import gov.llnl.ernie.manipulator.ManipulationDescription;
import gov.llnl.ernie.manipulator.RecordManipulator;
import gov.llnl.math.matrix.Matrix;
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
public class VM250RecordManipulatorNGTest
{
  
  public VM250RecordManipulatorNGTest()
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
   * Test of setShieldingModel method, of class VM250RecordManipulator.
   */
  @Test
  public void testSetShieldingModel()
  {
    System.out.println("setShieldingModel");
    ShieldingModel sm = null;
    VM250RecordManipulator instance = new VM250RecordManipulator();
    instance.setShieldingModel(sm);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setInjectionSourceLibrary method, of class VM250RecordManipulator.
   */
  @Test
  public void testSetInjectionSourceLibrary()
  {
    System.out.println("setInjectionSourceLibrary");
    InjectionSourceLibrary isl = null;
    VM250RecordManipulator instance = new VM250RecordManipulator();
    instance.setInjectionSourceLibrary(isl);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of initialize method, of class VM250RecordManipulator.
   */
  @Test
  public void testInitialize()
  {
    System.out.println("initialize");
    VM250RecordManipulator instance = new VM250RecordManipulator();
    instance.initialize();
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of createManipulation method, of class VM250RecordManipulator.
   */
  @Test
  public void testCreateManipulation_String()
  {
    System.out.println("createManipulation");
    String prefix = "";
    VM250RecordManipulator instance = new VM250RecordManipulator();
    Manipulation expResult = null;
    Manipulation result = instance.createManipulation(prefix);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of createManipulation method, of class VM250RecordManipulator.
   */
  @Test
  public void testCreateManipulation_ManipulationDescription()
  {
    System.out.println("createManipulation");
    ManipulationDescription description = null;
    VM250RecordManipulator instance = new VM250RecordManipulator();
    Manipulation expResult = null;
    Manipulation result = instance.createManipulation(description);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of applyManipulation method, of class VM250RecordManipulator.
   */
  @Test
  public void testApplyManipulation() throws Exception
  {
    System.out.println("applyManipulation");
    Record record = null;
    Manipulation manipulation = null;
    VM250RecordManipulator instance = new VM250RecordManipulator();
    RecordManipulator.Output expResult = null;
    RecordManipulator.Output result = instance.applyManipulation(record, manipulation);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of createPanels method, of class VM250RecordManipulator.
   */
  @Test
  public void testCreatePanels()
  {
    System.out.println("createPanels");
    Lane lane = null;
    List expResult = null;
    List result = VM250RecordManipulator.createPanels(lane);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getLibrary method, of class VM250RecordManipulator.
   */
  @Test
  public void testGetLibrary()
  {
    System.out.println("getLibrary");
    VM250RecordManipulator instance = new VM250RecordManipulator();
    InjectionSourceLibrary expResult = null;
    InjectionSourceLibrary result = instance.getLibrary();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of injectGamma method, of class VM250RecordManipulator.
   */
  @Test
  public void testInjectGamma()
  {
    System.out.println("injectGamma");
    Record record = null;
    double[] position = null;
    Matrix[] gammaData = null;
    VM250RecordManipulator instance = new VM250RecordManipulator();
    instance.injectGamma(record, position, gammaData);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getNominalLaneWidth method, of class VM250RecordManipulator.
   */
  @Test
  public void testGetNominalLaneWidth()
  {
    System.out.println("getNominalLaneWidth");
    VM250RecordManipulator instance = new VM250RecordManipulator();
    double expResult = 0.0;
    double result = instance.getNominalLaneWidth();
    assertEquals(result, expResult, 0.0);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setNominalLaneWidth method, of class VM250RecordManipulator.
   */
  @Test
  public void testSetNominalLaneWidth()
  {
    System.out.println("setNominalLaneWidth");
    double nominalLaneWidth = 0.0;
    VM250RecordManipulator instance = new VM250RecordManipulator();
    instance.setNominalLaneWidth(nominalLaneWidth);
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