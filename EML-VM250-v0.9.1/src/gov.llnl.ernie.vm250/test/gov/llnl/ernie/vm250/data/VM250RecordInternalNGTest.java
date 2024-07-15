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

import gov.llnl.ernie.data.VehicleClass;
import gov.llnl.ernie.internal.data.LaneImpl;
import java.time.Instant;
import java.util.ArrayList;
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
public class VM250RecordInternalNGTest
{
  
  public VM250RecordInternalNGTest()
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
   * Test of setSegmentDescription method, of class VM250RecordInternal.
   */
  @Test
  public void testSetSegmentDescription_9args()
  {
    System.out.println("setSegmentDescription");
    int dataSourceId = 0;
    long rpmId = 0L;
    Instant rpmDateTime = null;
    boolean continuation = false;
    int[] gammaBackground = null;
    double neutronBackground = 0.0;
    boolean gammaAlarm = false;
    boolean neutronAlarm = false;
    boolean realOccupancy = false;
    VM250RecordInternal instance = new VM250RecordInternal();
    instance.setSegmentDescription(dataSourceId, rpmId, rpmDateTime, continuation, gammaBackground, neutronBackground, gammaAlarm, neutronAlarm, realOccupancy);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setSetupInfo method, of class VM250RecordInternal.
   */
  @Test
  public void testSetSetupInfo()
  {
    System.out.println("setSetupInfo");
    int intervals = 0;
    int occupancyHoldin = 0;
    double nSigma = 0.0;
    VM250RecordInternal instance = new VM250RecordInternal();
    instance.setSetupInfo(intervals, occupancyHoldin, nSigma);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of duplicate method, of class VM250RecordInternal.
   */
  @Test
  public void testDuplicate()
  {
    System.out.println("duplicate");
    VM250RecordInternal record1 = null;
    VM250RecordInternal expResult = null;
    VM250RecordInternal result = VM250RecordInternal.duplicate(record1);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of anyAlarm method, of class VM250RecordInternal.
   */
  @Test
  public void testAnyAlarm()
  {
    System.out.println("anyAlarm");
    VM250RecordInternal instance = new VM250RecordInternal();
    boolean expResult = false;
    boolean result = instance.anyAlarm();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getDataSourceId method, of class VM250RecordInternal.
   */
  @Test
  public void testGetDataSourceId()
  {
    System.out.println("getDataSourceId");
    VM250RecordInternal instance = new VM250RecordInternal();
    int expResult = 0;
    int result = instance.getDataSourceId();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getGammaBackgrounds method, of class VM250RecordInternal.
   */
  @Test
  public void testGetGammaBackgrounds()
  {
    System.out.println("getGammaBackgrounds");
    VM250RecordInternal instance = new VM250RecordInternal();
    double[] expResult = null;
    double[] result = instance.getGammaBackgrounds();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getLane method, of class VM250RecordInternal.
   */
  @Test
  public void testGetLane()
  {
    System.out.println("getLane");
    VM250RecordInternal instance = new VM250RecordInternal();
    LaneImpl expResult = null;
    LaneImpl result = instance.getLane();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getLength method, of class VM250RecordInternal.
   */
  @Test
  public void testGetLength()
  {
    System.out.println("getLength");
    VM250RecordInternal instance = new VM250RecordInternal();
    int expResult = 0;
    int result = instance.getLength();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getNPanels method, of class VM250RecordInternal.
   */
  @Test
  public void testGetNPanels()
  {
    System.out.println("getNPanels");
    VM250RecordInternal instance = new VM250RecordInternal();
    int expResult = 0;
    int result = instance.getNPanels();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getNumberOfChannels method, of class VM250RecordInternal.
   */
  @Test
  public void testGetNumberOfChannels()
  {
    System.out.println("getNumberOfChannels");
    VM250RecordInternal instance = new VM250RecordInternal();
    int expResult = 0;
    int result = instance.getNumberOfChannels();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getRpmDateTime method, of class VM250RecordInternal.
   */
  @Test
  public void testGetRpmDateTime()
  {
    System.out.println("getRpmDateTime");
    VM250RecordInternal instance = new VM250RecordInternal();
    Instant expResult = null;
    Instant result = instance.getRpmDateTime();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getRpmId method, of class VM250RecordInternal.
   */
  @Test
  public void testGetRpmId()
  {
    System.out.println("getRpmId");
    VM250RecordInternal instance = new VM250RecordInternal();
    long expResult = 0L;
    long result = instance.getRpmId();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getSegmentDescriptorId method, of class VM250RecordInternal.
   */
  @Test
  public void testGetSegmentDescriptorId()
  {
    System.out.println("getSegmentDescriptorId");
    VM250RecordInternal instance = new VM250RecordInternal();
    long expResult = 0L;
    long result = instance.getSegmentDescriptorId();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of isSecondary method, of class VM250RecordInternal.
   */
  @Test
  public void testIsSecondary()
  {
    System.out.println("isSecondary");
    VM250RecordInternal instance = new VM250RecordInternal();
    boolean expResult = false;
    boolean result = instance.isSecondary();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getVehicleClass method, of class VM250RecordInternal.
   */
  @Test
  public void testGetVehicleClass()
  {
    System.out.println("getVehicleClass");
    VM250RecordInternal instance = new VM250RecordInternal();
    VehicleClass expResult = null;
    VehicleClass result = instance.getVehicleClass();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setVehicleClass method, of class VM250RecordInternal.
   */
  @Test
  public void testSetVehicleClass()
  {
    System.out.println("setVehicleClass");
    VehicleClass vehicleClass = null;
    VM250RecordInternal instance = new VM250RecordInternal();
    instance.setVehicleClass(vehicleClass);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of isInjected method, of class VM250RecordInternal.
   */
  @Test
  public void testIsInjected()
  {
    System.out.println("isInjected");
    VM250RecordInternal instance = new VM250RecordInternal();
    boolean expResult = false;
    boolean result = instance.isInjected();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setIsInjected method, of class VM250RecordInternal.
   */
  @Test
  public void testSetIsInjected()
  {
    System.out.println("setIsInjected");
    boolean isInjected = false;
    VM250RecordInternal instance = new VM250RecordInternal();
    instance.setIsInjected(isInjected);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getOccupancyId method, of class VM250RecordInternal.
   */
  @Test
  public void testGetOccupancyId()
  {
    System.out.println("getOccupancyId");
    VM250RecordInternal instance = new VM250RecordInternal();
    long expResult = 0L;
    long result = instance.getOccupancyId();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setOccupancyId method, of class VM250RecordInternal.
   */
  @Test
  public void testSetOccupancyId()
  {
    System.out.println("setOccupancyId");
    long occupancyId = 0L;
    VM250RecordInternal instance = new VM250RecordInternal();
    instance.setOccupancyId(occupancyId);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getPanelData method, of class VM250RecordInternal.
   */
  @Test
  public void testGetPanelData()
  {
    System.out.println("getPanelData");
    VM250RecordInternal instance = new VM250RecordInternal();
    VM250RecordInternal.PanelData[] expResult = null;
    VM250RecordInternal.PanelData[] result = instance.getPanelData();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getCombinedPanelData method, of class VM250RecordInternal.
   */
  @Test
  public void testGetCombinedPanelData()
  {
    System.out.println("getCombinedPanelData");
    VM250RecordInternal instance = new VM250RecordInternal();
    VM250RecordInternal.CombinedPanelData expResult = null;
    VM250RecordInternal.CombinedPanelData result = instance.getCombinedPanelData();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setLane method, of class VM250RecordInternal.
   */
  @Test
  public void testSetLane()
  {
    System.out.println("setLane");
    LaneImpl lane = null;
    VM250RecordInternal instance = new VM250RecordInternal();
    instance.setLane(lane);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getSegmentDescription method, of class VM250RecordInternal.
   */
  @Test
  public void testGetSegmentDescription()
  {
    System.out.println("getSegmentDescription");
    VM250RecordInternal instance = new VM250RecordInternal();
    VM250RecordInternal.SegmentDescription expResult = null;
    VM250RecordInternal.SegmentDescription result = instance.getSegmentDescription();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getSetupInfo method, of class VM250RecordInternal.
   */
  @Test
  public void testGetSetupInfo()
  {
    System.out.println("getSetupInfo");
    VM250RecordInternal instance = new VM250RecordInternal();
    VM250RecordInternal.SetupInfo expResult = null;
    VM250RecordInternal.SetupInfo result = instance.getSetupInfo();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setSegmentDescription method, of class VM250RecordInternal.
   */
  @Test
  public void testSetSegmentDescription_VM250RecordInternalSegmentDescription()
  {
    System.out.println("setSegmentDescription");
    VM250RecordInternal.SegmentDescription segmentDescription = null;
    VM250RecordInternal instance = new VM250RecordInternal();
    instance.setSegmentDescription(segmentDescription);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getVelocities method, of class VM250RecordInternal.
   */
  @Test
  public void testGetVelocities()
  {
    System.out.println("getVelocities");
    VM250RecordInternal instance = new VM250RecordInternal();
    ArrayList expResult = null;
    ArrayList result = instance.getVelocities();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setVelocities method, of class VM250RecordInternal.
   */
  @Test
  public void testSetVelocities()
  {
    System.out.println("setVelocities");
    ArrayList<VM250RecordInternal.VelocityReading> velocities = null;
    VM250RecordInternal instance = new VM250RecordInternal();
    instance.setVelocities(velocities);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of isGammaHighBackground method, of class VM250RecordInternal.
   */
  @Test
  public void testIsGammaHighBackground()
  {
    System.out.println("isGammaHighBackground");
    VM250RecordInternal instance = new VM250RecordInternal();
    boolean expResult = false;
    boolean result = instance.isGammaHighBackground();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of isGammaLowBackground method, of class VM250RecordInternal.
   */
  @Test
  public void testIsGammaLowBackground()
  {
    System.out.println("isGammaLowBackground");
    VM250RecordInternal instance = new VM250RecordInternal();
    boolean expResult = false;
    boolean result = instance.isGammaLowBackground();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of isNeutronHighBackground method, of class VM250RecordInternal.
   */
  @Test
  public void testIsNeutronHighBackground()
  {
    System.out.println("isNeutronHighBackground");
    VM250RecordInternal instance = new VM250RecordInternal();
    boolean expResult = false;
    boolean result = instance.isNeutronHighBackground();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setGammaHighBackground method, of class VM250RecordInternal.
   */
  @Test
  public void testSetGammaHighBackground()
  {
    System.out.println("setGammaHighBackground");
    boolean gammaHighBackground = false;
    VM250RecordInternal instance = new VM250RecordInternal();
    instance.setGammaHighBackground(gammaHighBackground);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setGammaLowBackground method, of class VM250RecordInternal.
   */
  @Test
  public void testSetGammaLowBackground()
  {
    System.out.println("setGammaLowBackground");
    boolean gammaLowBackground = false;
    VM250RecordInternal instance = new VM250RecordInternal();
    instance.setGammaLowBackground(gammaLowBackground);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setNeutronHighBackground method, of class VM250RecordInternal.
   */
  @Test
  public void testSetNeutronHighBackground()
  {
    System.out.println("setNeutronHighBackground");
    boolean neutronHighBackground = false;
    VM250RecordInternal instance = new VM250RecordInternal();
    instance.setNeutronHighBackground(neutronHighBackground);
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