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

import gov.llnl.ernie.Analysis;
import gov.llnl.ernie.data.Record;
import gov.llnl.ernie.data.SensorMeasurement;
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
public class VM250GammaPanelFixNGTest
{
  
  public VM250GammaPanelFixNGTest()
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
   * Test of compute method, of class VM250GammaPanelFix.
   */
  @Test
  public void testCompute()
  {
    System.out.println("compute");
    Record record = null;
    VM250GammaPanelFix instance = new VM250GammaPanelFix();
    instance.compute(record);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of initialize method, of class VM250GammaPanelFix.
   */
  @Test
  public void testInitialize()
  {
    System.out.println("initialize");
    Analysis par0 = null;
    VM250GammaPanelFix instance = new VM250GammaPanelFix();
    instance.initialize(par0);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setTTBR method, of class VM250GammaPanelFix.
   */
  @Test
  public void testSetTTBR()
  {
    System.out.println("setTTBR");
    double TTBR = 0.0;
    VM250GammaPanelFix instance = new VM250GammaPanelFix();
    instance.setTTBR(TTBR);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of replaceOnePanel method, of class VM250GammaPanelFix.
   */
  @Test
  public void testReplaceOnePanel()
  {
    System.out.println("replaceOnePanel");
    SensorMeasurement panelA = null;
    SensorMeasurement panelB = null;
    double coef1 = 0.0;
    double coef2 = 0.0;
    int[][] destination = null;
    VM250GammaPanelFix instance = new VM250GammaPanelFix();
    instance.replaceOnePanel(panelA, panelB, coef1, coef2, destination);
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