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
import gov.llnl.ernie.vm250.data.VM250RecordInternal;
import gov.llnl.math.matrix.MatrixColumnTable;
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
public class VM250BackgroundEstimatorNGTest
{
  
  public VM250BackgroundEstimatorNGTest()
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
   * Test of initialize method, of class VM250BackgroundEstimator.
   */
  @Test
  public void testInitialize() throws Exception
  {
    System.out.println("initialize");
    VM250BackgroundEstimator instance = new VM250BackgroundEstimator();
    instance.initialize(null);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of compute method, of class VM250BackgroundEstimator.
   */
  @Test
  public void testCompute() throws Exception
  {
    System.out.println("compute");
    Record record = null;
    VM250BackgroundEstimator instance = new VM250BackgroundEstimator();
    instance.compute(record);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of computeDefaultBackground method, of class VM250BackgroundEstimator.
   */
  @Test
  public void testComputeDefaultBackground()
  {
    System.out.println("computeDefaultBackground");
    Record record = null;
    VM250BackgroundEstimator instance = new VM250BackgroundEstimator();
    MatrixColumnTable expResult = null;
    MatrixColumnTable result = instance.computeDefaultBackground(record);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of computeBackground method, of class VM250BackgroundEstimator.
   */
  @Test
  public void testComputeBackground()
  {
    System.out.println("computeBackground");
    Record record = null;
    int backgroundModelIndex = 0;
    VM250BackgroundEstimator instance = new VM250BackgroundEstimator();
    MatrixColumnTable expResult = null;
    MatrixColumnTable result = instance.computeBackground(record, backgroundModelIndex);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of extractPanelRates method, of class VM250BackgroundEstimator.
   */
  @Test
  public void testExtractPanelRates()
  {
    System.out.println("extractPanelRates");
    Record record = null;
    VM250BackgroundEstimator instance = new VM250BackgroundEstimator();
    double[] expResult = null;
    double[] result = instance.extractPanelRates(record);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of extractBackgroundSpectrum method, of class VM250BackgroundEstimator.
   */
  @Test
  public void testExtractBackgroundSpectrum()
  {
    System.out.println("extractBackgroundSpectrum");
    VM250RecordInternal record = null;
    int panel = 0;
    double[] expResult = null;
    double[] result = VM250BackgroundEstimator.extractBackgroundSpectrum(record, panel);
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