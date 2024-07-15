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

import gov.llnl.ernie.data.SensorProperties;
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
public class VM250NeutronPanelBackgroundNGTest
{
  
  public VM250NeutronPanelBackgroundNGTest()
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
   * Test of getSensorProperties method, of class VM250NeutronPanelBackground.
   */
  @Test
  public void testGetSensorProperties()
  {
    System.out.println("getSensorProperties");
    VM250NeutronPanelBackground instance = null;
    SensorProperties expResult = null;
    SensorProperties result = instance.getSensorProperties();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of size method, of class VM250NeutronPanelBackground.
   */
  @Test
  public void testSize()
  {
    System.out.println("size");
    VM250NeutronPanelBackground instance = null;
    int expResult = 0;
    int result = instance.size();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getOccupancyStart method, of class VM250NeutronPanelBackground.
   */
  @Test
  public void testGetOccupancyStart()
  {
    System.out.println("getOccupancyStart");
    VM250NeutronPanelBackground instance = null;
    int expResult = 0;
    int result = instance.getOccupancyStart();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getOccupancyEnd method, of class VM250NeutronPanelBackground.
   */
  @Test
  public void testGetOccupancyEnd()
  {
    System.out.println("getOccupancyEnd");
    VM250NeutronPanelBackground instance = null;
    int expResult = 0;
    int result = instance.getOccupancyEnd();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getExpectedCountRate method, of class VM250NeutronPanelBackground.
   */
  @Test
  public void testGetExpectedCountRate()
  {
    System.out.println("getExpectedCountRate");
    VM250NeutronPanelBackground instance = null;
    double expResult = 0.0;
    double result = instance.getExpectedCountRate();
    assertEquals(result, expResult, 0.0);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getExpectedSpectrum method, of class VM250NeutronPanelBackground.
   */
  @Test
  public void testGetExpectedSpectrum()
  {
    System.out.println("getExpectedSpectrum");
    VM250NeutronPanelBackground instance = null;
    double[] expResult = null;
    double[] result = instance.getExpectedSpectrum();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getTime method, of class VM250NeutronPanelBackground.
   */
  @Test
  public void testGetTime()
  {
    System.out.println("getTime");
    int sampleIndex = 0;
    VM250NeutronPanelBackground instance = null;
    Instant expResult = null;
    Instant result = instance.getTime(sampleIndex);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getCounts method, of class VM250NeutronPanelBackground.
   */
  @Test
  public void testGetCounts()
  {
    System.out.println("getCounts");
    int sampleIndex = 0;
    VM250NeutronPanelBackground instance = null;
    double expResult = 0.0;
    double result = instance.getCounts(sampleIndex);
    assertEquals(result, expResult, 0.0);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getCountsSum method, of class VM250NeutronPanelBackground.
   */
  @Test
  public void testGetCountsSum()
  {
    System.out.println("getCountsSum");
    int sampleIndexBegin = 0;
    int sampleIndexEnd = 0;
    VM250NeutronPanelBackground instance = null;
    double expResult = 0.0;
    double result = instance.getCountsSum(sampleIndexBegin, sampleIndexEnd);
    assertEquals(result, expResult, 0.0);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getCountsRange method, of class VM250NeutronPanelBackground.
   */
  @Test
  public void testGetCountsRange()
  {
    System.out.println("getCountsRange");
    int sampleIndexBegin = 0;
    int sampleIndexEnd = 0;
    VM250NeutronPanelBackground instance = null;
    double[] expResult = null;
    double[] result = instance.getCountsRange(sampleIndexBegin, sampleIndexEnd);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getSpectrum method, of class VM250NeutronPanelBackground.
   */
  @Test
  public void testGetSpectrum()
  {
    System.out.println("getSpectrum");
    int sampleIndex = 0;
    VM250NeutronPanelBackground instance = null;
    double[] expResult = null;
    double[] result = instance.getSpectrum(sampleIndex);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getSpectrumRange method, of class VM250NeutronPanelBackground.
   */
  @Test
  public void testGetSpectrumRange()
  {
    System.out.println("getSpectrumRange");
    int sampleIndexBegin = 0;
    int sampleIndexEnd = 0;
    VM250NeutronPanelBackground instance = null;
    double[][] expResult = null;
    double[][] result = instance.getSpectrumRange(sampleIndexBegin, sampleIndexEnd);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getSpectrumSum method, of class VM250NeutronPanelBackground.
   */
  @Test
  public void testGetSpectrumSum()
  {
    System.out.println("getSpectrumSum");
    int sampleIndexBegin = 0;
    int sampleIndexEnd = 0;
    VM250NeutronPanelBackground instance = null;
    double[] expResult = null;
    double[] result = instance.getSpectrumSum(sampleIndexBegin, sampleIndexEnd);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of computeExpectedSpectrumSuppressed method, of class VM250NeutronPanelBackground.
   */
  @Test
  public void testComputeExpectedSpectrumSuppressed()
  {
    System.out.println("computeExpectedSpectrumSuppressed");
    double fraction = 0.0;
    double time = 0.0;
    VM250NeutronPanelBackground instance = null;
    double[] expResult = null;
    double[] result = instance.computeExpectedSpectrumSuppressed(fraction, time);
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