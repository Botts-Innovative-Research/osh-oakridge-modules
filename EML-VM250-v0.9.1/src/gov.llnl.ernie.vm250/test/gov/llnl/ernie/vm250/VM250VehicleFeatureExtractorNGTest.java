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

import gov.llnl.ernie.analysis.Features;
import gov.llnl.ernie.analysis.FeaturesDescription;
import gov.llnl.ernie.data.Record;
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
public class VM250VehicleFeatureExtractorNGTest
{
  
  public VM250VehicleFeatureExtractorNGTest()
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
   * Test of initialize method, of class VM250VehicleFeatureExtractor.
   */
  @Test
  public void testInitialize()
  {
    System.out.println("initialize");
    VM250VehicleFeatureExtractor instance = new VM250VehicleFeatureExtractor();
    instance.initialize();
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getDescription method, of class VM250VehicleFeatureExtractor.
   */
  @Test
  public void testGetDescription()
  {
    System.out.println("getDescription");
    VM250VehicleFeatureExtractor instance = new VM250VehicleFeatureExtractor();
    FeaturesDescription expResult = null;
    FeaturesDescription result = instance.getDescription();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of newFeatures method, of class VM250VehicleFeatureExtractor.
   */
  @Test
  public void testNewFeatures()
  {
    System.out.println("newFeatures");
    VM250VehicleFeatureExtractor instance = new VM250VehicleFeatureExtractor();
    Features expResult = null;
    Features result = instance.newFeatures();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of compute method, of class VM250VehicleFeatureExtractor.
   */
  @Test
  public void testCompute()
  {
    System.out.println("compute");
    Record record = null;
    VM250VehicleFeatureExtractor instance = new VM250VehicleFeatureExtractor();
    Features expResult = null;
    Features result = instance.compute(record);
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