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

import gov.llnl.ernie.Fault;
import gov.llnl.ernie.data.Lane;
import gov.llnl.ernie.data.ScanContextualInfo;
import gov.llnl.ernie.data.SensorBackground;
import gov.llnl.ernie.data.SensorMeasurement;
import gov.llnl.ernie.data.SensorPosition;
import gov.llnl.ernie.data.VPSMeasurement;
import gov.llnl.ernie.data.VehicleClass;
import gov.llnl.ernie.data.VehicleInfo;
import gov.llnl.ernie.data.VendorAnalysis;
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
public class VM250RecordNGTest
{
  
  public VM250RecordNGTest()
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
   * Test of bad method, of class VM250Record.
   */
  @Test
  public void testBad()
  {
    System.out.println("bad");
    VM250Record instance = null;
    boolean expResult = false;
    boolean result = instance.bad();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getBadReasons method, of class VM250Record.
   */
  @Test
  public void testGetBadReasons()
  {
    System.out.println("getBadReasons");
    VM250Record instance = null;
    List expResult = null;
    List result = instance.getBadReasons();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getLane method, of class VM250Record.
   */
  @Test
  public void testGetLane()
  {
    System.out.println("getLane");
    VM250Record instance = null;
    Lane expResult = null;
    Lane result = instance.getLane();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getGammaMeasurements method, of class VM250Record.
   */
  @Test
  public void testGetGammaMeasurements()
  {
    System.out.println("getGammaMeasurements");
    VM250Record instance = null;
    List expResult = null;
    List result = instance.getGammaMeasurements();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getGammaBackgrounds method, of class VM250Record.
   */
  @Test
  public void testGetGammaBackgrounds()
  {
    System.out.println("getGammaBackgrounds");
    VM250Record instance = null;
    List expResult = null;
    List result = instance.getGammaBackgrounds();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getCombinedGammaMeasurement method, of class VM250Record.
   */
  @Test
  public void testGetCombinedGammaMeasurement()
  {
    System.out.println("getCombinedGammaMeasurement");
    SensorPosition position = null;
    VM250Record instance = null;
    SensorMeasurement expResult = null;
    SensorMeasurement result = instance.getCombinedGammaMeasurement(position);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getCombinedGammaBackground method, of class VM250Record.
   */
  @Test
  public void testGetCombinedGammaBackground()
  {
    System.out.println("getCombinedGammaBackground");
    SensorPosition position = null;
    VM250Record instance = null;
    SensorBackground expResult = null;
    SensorBackground result = instance.getCombinedGammaBackground(position);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getNeutronMeasurements method, of class VM250Record.
   */
  @Test
  public void testGetNeutronMeasurements()
  {
    System.out.println("getNeutronMeasurements");
    VM250Record instance = null;
    List expResult = null;
    List result = instance.getNeutronMeasurements();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getNeutronBackgrounds method, of class VM250Record.
   */
  @Test
  public void testGetNeutronBackgrounds()
  {
    System.out.println("getNeutronBackgrounds");
    VM250Record instance = null;
    List expResult = null;
    List result = instance.getNeutronBackgrounds();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getCombinedNeutronMeasurement method, of class VM250Record.
   */
  @Test
  public void testGetCombinedNeutronMeasurement()
  {
    System.out.println("getCombinedNeutronMeasurement");
    SensorPosition position = null;
    VM250Record instance = null;
    SensorMeasurement expResult = null;
    SensorMeasurement result = instance.getCombinedNeutronMeasurement(position);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getCombinedNeutronBackground method, of class VM250Record.
   */
  @Test
  public void testGetCombinedNeutronBackground()
  {
    System.out.println("getCombinedNeutronBackground");
    SensorPosition position = null;
    VM250Record instance = null;
    SensorBackground expResult = null;
    SensorBackground result = instance.getCombinedNeutronBackground(position);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getVPSMeasurement method, of class VM250Record.
   */
  @Test
  public void testGetVPSMeasurement()
  {
    System.out.println("getVPSMeasurement");
    VM250Record instance = null;
    VPSMeasurement expResult = null;
    VPSMeasurement result = instance.getVPSMeasurement();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getVehicleMotion method, of class VM250Record.
   */
  @Test
  public void testGetVehicleMotion()
  {
    System.out.println("getVehicleMotion");
    VM250Record instance = null;
    VM250VehicleMotion expResult = null;
    VM250VehicleMotion result = instance.getVehicleMotion();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getVehicleInfo method, of class VM250Record.
   */
  @Test
  public void testGetVehicleInfo()
  {
    System.out.println("getVehicleInfo");
    VM250Record instance = null;
    VehicleInfo expResult = null;
    VehicleInfo result = instance.getVehicleInfo();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getVehicleClass method, of class VM250Record.
   */
  @Test
  public void testGetVehicleClass()
  {
    System.out.println("getVehicleClass");
    VM250Record instance = null;
    VehicleClass expResult = null;
    VehicleClass result = instance.getVehicleClass();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getVendorAnalysis method, of class VM250Record.
   */
  @Test
  public void testGetVendorAnalysis()
  {
    System.out.println("getVendorAnalysis");
    VM250Record instance = null;
    VendorAnalysis expResult = null;
    VendorAnalysis result = instance.getVendorAnalysis();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getInternal method, of class VM250Record.
   */
  @Test
  public void testGetInternal()
  {
    System.out.println("getInternal");
    VM250Record instance = null;
    VM250RecordInternal expResult = null;
    VM250RecordInternal result = instance.getInternal();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setMotion method, of class VM250Record.
   */
  @Test
  public void testSetMotion()
  {
    System.out.println("setMotion");
    VM250VehicleMotion extract = null;
    VM250Record instance = null;
    instance.setMotion(extract);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setVehicleType method, of class VM250Record.
   */
  @Test
  public void testSetVehicleType()
  {
    System.out.println("setVehicleType");
    VehicleClass vehicleType = null;
    VM250Record instance = null;
    instance.setVehicleType(vehicleType);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setVehicleClass method, of class VM250Record.
   */
  @Test
  public void testSetVehicleClass()
  {
    System.out.println("setVehicleClass");
    VehicleClass classify = null;
    VM250Record instance = null;
    instance.setVehicleClass(classify);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of addFault method, of class VM250Record.
   */
  @Test
  public void testAddFault()
  {
    System.out.println("addFault");
    Fault fault = null;
    VM250Record instance = null;
    instance.addFault(fault);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getContextualInfo method, of class VM250Record.
   */
  @Test
  public void testGetContextualInfo()
  {
    System.out.println("getContextualInfo");
    VM250Record instance = null;
    ScanContextualInfo expResult = null;
    ScanContextualInfo result = instance.getContextualInfo();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setVehicleInfo method, of class VM250Record.
   */
  @Test
  public void testSetVehicleInfo()
  {
    System.out.println("setVehicleInfo");
    VehicleInfo defaultVehicleInfo = null;
    VM250Record instance = null;
    instance.setVehicleInfo(defaultVehicleInfo);
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