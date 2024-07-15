/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.ernie.manipulator;

import gov.llnl.ernie.analysis.FeaturesDescription;
import gov.llnl.ernie.rtk.DoubleSpectrum;
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
public class ManipulationNGTest
{
  
  public ManipulationNGTest()
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
   * Test of getVehicleClass method, of class Manipulation.
   */
  @Test
  public void testGetVehicleClass()
  {
    System.out.println("getVehicleClass");
    Manipulation instance = null;
    int expResult = 0;
    int result = instance.getVehicleClass();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setVehicleClass method, of class Manipulation.
   */
  @Test
  public void testSetVehicleClass()
  {
    System.out.println("setVehicleClass");
    int vehicleClass = 0;
    Manipulation instance = null;
    instance.setVehicleClass(vehicleClass);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getDescription method, of class Manipulation.
   */
  @Test
  public void testGetDescription()
  {
    System.out.println("getDescription");
    Manipulation instance = null;
    FeaturesDescription expResult = null;
    FeaturesDescription result = instance.getDescription();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setGroupDescription method, of class Manipulation.
   */
  @Test
  public void testSetGroupDescription()
  {
    System.out.println("setGroupDescription");
    FeaturesDescription description = null;
    Manipulation instance = null;
    instance.setGroupDescription(description);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setExternalCompactSource method, of class Manipulation.
   */
  @Test
  public void testSetExternalCompactSource()
  {
    System.out.println("setExternalCompactSource");
    DoubleSpectrum source = null;
    Manipulation instance = null;
    instance.setExternalCompactSource(source);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setExternalDistributedSource method, of class Manipulation.
   */
  @Test
  public void testSetExternalDistributedSource()
  {
    System.out.println("setExternalDistributedSource");
    DoubleSpectrum source = null;
    Manipulation instance = null;
    instance.setExternalDistributedSource(source);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setCompactLocation method, of class Manipulation.
   */
  @Test
  public void testSetCompactLocation()
  {
    System.out.println("setCompactLocation");
    double x = 0.0;
    double y = 0.0;
    double z = 0.0;
    Manipulation instance = null;
    instance.setCompactLocation(x, y, z);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of dump method, of class Manipulation.
   */
  @Test
  public void testDump()
  {
    System.out.println("dump");
    Manipulation instance = null;
    instance.dump();
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getSerialVersionUID method, of class Manipulation.
   */
  @Test
  public void testGetSerialVersionUID()
  {
    System.out.println("getSerialVersionUID");
    long expResult = 0L;
    long result = Manipulation.getSerialVersionUID();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of isAlterVelocity method, of class Manipulation.
   */
  @Test
  public void testIsAlterVelocity()
  {
    System.out.println("isAlterVelocity");
    Manipulation instance = null;
    boolean expResult = false;
    boolean result = instance.isAlterVelocity();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of isInjectCompact method, of class Manipulation.
   */
  @Test
  public void testIsInjectCompact()
  {
    System.out.println("isInjectCompact");
    Manipulation instance = null;
    boolean expResult = false;
    boolean result = instance.isInjectCompact();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of isInjectDistributed method, of class Manipulation.
   */
  @Test
  public void testIsInjectDistributed()
  {
    System.out.println("isInjectDistributed");
    Manipulation instance = null;
    boolean expResult = false;
    boolean result = instance.isInjectDistributed();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getSpeedStart method, of class Manipulation.
   */
  @Test
  public void testGetSpeedStart()
  {
    System.out.println("getSpeedStart");
    Manipulation instance = null;
    double expResult = 0.0;
    double result = instance.getSpeedStart();
    assertEquals(result, expResult, 0.0);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getSpeedEnd method, of class Manipulation.
   */
  @Test
  public void testGetSpeedEnd()
  {
    System.out.println("getSpeedEnd");
    Manipulation instance = null;
    double expResult = 0.0;
    double result = instance.getSpeedEnd();
    assertEquals(result, expResult, 0.0);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getCompactIntensity method, of class Manipulation.
   */
  @Test
  public void testGetCompactIntensity()
  {
    System.out.println("getCompactIntensity");
    Manipulation instance = null;
    double expResult = 0.0;
    double result = instance.getCompactIntensity();
    assertEquals(result, expResult, 0.0);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getCompactPx method, of class Manipulation.
   */
  @Test
  public void testGetCompactPx()
  {
    System.out.println("getCompactPx");
    Manipulation instance = null;
    double expResult = 0.0;
    double result = instance.getCompactPx();
    assertEquals(result, expResult, 0.0);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getCompactPy method, of class Manipulation.
   */
  @Test
  public void testGetCompactPy()
  {
    System.out.println("getCompactPy");
    Manipulation instance = null;
    double expResult = 0.0;
    double result = instance.getCompactPy();
    assertEquals(result, expResult, 0.0);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getCompactPz method, of class Manipulation.
   */
  @Test
  public void testGetCompactPz()
  {
    System.out.println("getCompactPz");
    Manipulation instance = null;
    double expResult = 0.0;
    double result = instance.getCompactPz();
    assertEquals(result, expResult, 0.0);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getCompactSourceId method, of class Manipulation.
   */
  @Test
  public void testGetCompactSourceId()
  {
    System.out.println("getCompactSourceId");
    Manipulation instance = null;
    String expResult = "";
    String result = instance.getCompactSourceId();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getDistributedIntensity1 method, of class Manipulation.
   */
  @Test
  public void testGetDistributedIntensity1()
  {
    System.out.println("getDistributedIntensity1");
    Manipulation instance = null;
    double expResult = 0.0;
    double result = instance.getDistributedIntensity1();
    assertEquals(result, expResult, 0.0);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getDistributedIntensity2 method, of class Manipulation.
   */
  @Test
  public void testGetDistributedIntensity2()
  {
    System.out.println("getDistributedIntensity2");
    Manipulation instance = null;
    double expResult = 0.0;
    double result = instance.getDistributedIntensity2();
    assertEquals(result, expResult, 0.0);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getDistributedPx1 method, of class Manipulation.
   */
  @Test
  public void testGetDistributedPx1()
  {
    System.out.println("getDistributedPx1");
    Manipulation instance = null;
    double expResult = 0.0;
    double result = instance.getDistributedPx1();
    assertEquals(result, expResult, 0.0);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getDistributedPx2 method, of class Manipulation.
   */
  @Test
  public void testGetDistributedPx2()
  {
    System.out.println("getDistributedPx2");
    Manipulation instance = null;
    double expResult = 0.0;
    double result = instance.getDistributedPx2();
    assertEquals(result, expResult, 0.0);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getDistributedPy method, of class Manipulation.
   */
  @Test
  public void testGetDistributedPy()
  {
    System.out.println("getDistributedPy");
    Manipulation instance = null;
    double expResult = 0.0;
    double result = instance.getDistributedPy();
    assertEquals(result, expResult, 0.0);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getDistributedPz method, of class Manipulation.
   */
  @Test
  public void testGetDistributedPz()
  {
    System.out.println("getDistributedPz");
    Manipulation instance = null;
    double expResult = 0.0;
    double result = instance.getDistributedPz();
    assertEquals(result, expResult, 0.0);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getDistributedSourceId method, of class Manipulation.
   */
  @Test
  public void testGetDistributedSourceId()
  {
    System.out.println("getDistributedSourceId");
    Manipulation instance = null;
    String expResult = "";
    String result = instance.getDistributedSourceId();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getCargoModelId method, of class Manipulation.
   */
  @Test
  public void testGetCargoModelId()
  {
    System.out.println("getCargoModelId");
    Manipulation instance = null;
    int expResult = 0;
    int result = instance.getCargoModelId();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getSourceType method, of class Manipulation.
   */
  @Test
  public void testGetSourceType()
  {
    System.out.println("getSourceType");
    Manipulation instance = null;
    int expResult = 0;
    int result = instance.getSourceType();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getGammaLeakage method, of class Manipulation.
   */
  @Test
  public void testGetGammaLeakage()
  {
    System.out.println("getGammaLeakage");
    Manipulation instance = null;
    double expResult = 0.0;
    double result = instance.getGammaLeakage();
    assertEquals(result, expResult, 0.0);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getNeutronLeakage method, of class Manipulation.
   */
  @Test
  public void testGetNeutronLeakage()
  {
    System.out.println("getNeutronLeakage");
    Manipulation instance = null;
    double expResult = 0.0;
    double result = instance.getNeutronLeakage();
    assertEquals(result, expResult, 0.0);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setAlterVelocity method, of class Manipulation.
   */
  @Test
  public void testSetAlterVelocity()
  {
    System.out.println("setAlterVelocity");
    boolean alterVelocity = false;
    Manipulation instance = null;
    instance.setAlterVelocity(alterVelocity);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setInjectCompact method, of class Manipulation.
   */
  @Test
  public void testSetInjectCompact()
  {
    System.out.println("setInjectCompact");
    boolean injectCompact = false;
    Manipulation instance = null;
    instance.setInjectCompact(injectCompact);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setInjectDistributed method, of class Manipulation.
   */
  @Test
  public void testSetInjectDistributed()
  {
    System.out.println("setInjectDistributed");
    boolean injectDistributed = false;
    Manipulation instance = null;
    instance.setInjectDistributed(injectDistributed);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setSpeedStart method, of class Manipulation.
   */
  @Test
  public void testSetSpeedStart()
  {
    System.out.println("setSpeedStart");
    double speedStart = 0.0;
    Manipulation instance = null;
    instance.setSpeedStart(speedStart);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setCompactIntensity method, of class Manipulation.
   */
  @Test
  public void testSetCompactIntensity()
  {
    System.out.println("setCompactIntensity");
    double compactIntensity = 0.0;
    Manipulation instance = null;
    instance.setCompactIntensity(compactIntensity);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setCompactPx method, of class Manipulation.
   */
  @Test
  public void testSetCompactPx()
  {
    System.out.println("setCompactPx");
    double compactPx = 0.0;
    Manipulation instance = null;
    instance.setCompactPx(compactPx);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setCompactPy method, of class Manipulation.
   */
  @Test
  public void testSetCompactPy()
  {
    System.out.println("setCompactPy");
    double compactPy = 0.0;
    Manipulation instance = null;
    instance.setCompactPy(compactPy);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setCompactPz method, of class Manipulation.
   */
  @Test
  public void testSetCompactPz()
  {
    System.out.println("setCompactPz");
    double compactPz = 0.0;
    Manipulation instance = null;
    instance.setCompactPz(compactPz);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setCompactSourceId method, of class Manipulation.
   */
  @Test
  public void testSetCompactSourceId()
  {
    System.out.println("setCompactSourceId");
    String compactSourceId = "";
    Manipulation instance = null;
    instance.setCompactSourceId(compactSourceId);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setDistributedIntensity1 method, of class Manipulation.
   */
  @Test
  public void testSetDistributedIntensity1()
  {
    System.out.println("setDistributedIntensity1");
    double distributedIntensity1 = 0.0;
    Manipulation instance = null;
    instance.setDistributedIntensity1(distributedIntensity1);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setDistributedIntensity2 method, of class Manipulation.
   */
  @Test
  public void testSetDistributedIntensity2()
  {
    System.out.println("setDistributedIntensity2");
    double distributedIntensity2 = 0.0;
    Manipulation instance = null;
    instance.setDistributedIntensity2(distributedIntensity2);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setDistributedPx1 method, of class Manipulation.
   */
  @Test
  public void testSetDistributedPx1()
  {
    System.out.println("setDistributedPx1");
    double distributedPx1 = 0.0;
    Manipulation instance = null;
    instance.setDistributedPx1(distributedPx1);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setDistributedPx2 method, of class Manipulation.
   */
  @Test
  public void testSetDistributedPx2()
  {
    System.out.println("setDistributedPx2");
    double distributedPx2 = 0.0;
    Manipulation instance = null;
    instance.setDistributedPx2(distributedPx2);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setDistributedPy method, of class Manipulation.
   */
  @Test
  public void testSetDistributedPy()
  {
    System.out.println("setDistributedPy");
    double distributedPy = 0.0;
    Manipulation instance = null;
    instance.setDistributedPy(distributedPy);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setDistributedPz method, of class Manipulation.
   */
  @Test
  public void testSetDistributedPz()
  {
    System.out.println("setDistributedPz");
    double distributedPz = 0.0;
    Manipulation instance = null;
    instance.setDistributedPz(distributedPz);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setDistributedSourceId method, of class Manipulation.
   */
  @Test
  public void testSetDistributedSourceId()
  {
    System.out.println("setDistributedSourceId");
    String distributedSourceId = "";
    Manipulation instance = null;
    instance.setDistributedSourceId(distributedSourceId);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setCargoModelId method, of class Manipulation.
   */
  @Test
  public void testSetCargoModelId()
  {
    System.out.println("setCargoModelId");
    int cargoModelId = 0;
    Manipulation instance = null;
    instance.setCargoModelId(cargoModelId);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setSourceType method, of class Manipulation.
   */
  @Test
  public void testSetSourceType()
  {
    System.out.println("setSourceType");
    int sourceType = 0;
    Manipulation instance = null;
    instance.setSourceType(sourceType);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setGammaLeakage method, of class Manipulation.
   */
  @Test
  public void testSetGammaLeakage()
  {
    System.out.println("setGammaLeakage");
    double gammaLeakage = 0.0;
    Manipulation instance = null;
    instance.setGammaLeakage(gammaLeakage);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setNeutronLeakage method, of class Manipulation.
   */
  @Test
  public void testSetNeutronLeakage()
  {
    System.out.println("setNeutronLeakage");
    double neutronLeakage = 0.0;
    Manipulation instance = null;
    instance.setNeutronLeakage(neutronLeakage);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setDescription method, of class Manipulation.
   */
  @Test
  public void testSetDescription()
  {
    System.out.println("setDescription");
    FeaturesDescription description = null;
    Manipulation instance = null;
    instance.setDescription(description);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setSpeedEnd method, of class Manipulation.
   */
  @Test
  public void testSetSpeedEnd()
  {
    System.out.println("setSpeedEnd");
    double speedEnd = 0.0;
    Manipulation instance = null;
    instance.setSpeedEnd(speedEnd);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getCompactSource method, of class Manipulation.
   */
  @Test
  public void testGetCompactSource()
  {
    System.out.println("getCompactSource");
    Manipulation instance = null;
    DoubleSpectrum expResult = null;
    DoubleSpectrum result = instance.getCompactSource();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setCompactSource method, of class Manipulation.
   */
  @Test
  public void testSetCompactSource()
  {
    System.out.println("setCompactSource");
    DoubleSpectrum compactSource = null;
    Manipulation instance = null;
    instance.setCompactSource(compactSource);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getDistributedSource method, of class Manipulation.
   */
  @Test
  public void testGetDistributedSource()
  {
    System.out.println("getDistributedSource");
    Manipulation instance = null;
    DoubleSpectrum expResult = null;
    DoubleSpectrum result = instance.getDistributedSource();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setDistributedSource method, of class Manipulation.
   */
  @Test
  public void testSetDistributedSource()
  {
    System.out.println("setDistributedSource");
    DoubleSpectrum distributedSource = null;
    Manipulation instance = null;
    instance.setDistributedSource(distributedSource);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getWhere method, of class Manipulation.
   */
  @Test
  public void testGetWhere()
  {
    System.out.println("getWhere");
    Manipulation instance = null;
    int expResult = 0;
    int result = instance.getWhere();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setWhere method, of class Manipulation.
   */
  @Test
  public void testSetWhere()
  {
    System.out.println("setWhere");
    int where = 0;
    Manipulation instance = null;
    instance.setWhere(where);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of isAlterVehicle method, of class Manipulation.
   */
  @Test
  public void testIsAlterVehicle()
  {
    System.out.println("isAlterVehicle");
    Manipulation instance = null;
    boolean expResult = false;
    boolean result = instance.isAlterVehicle();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setAlterVehicle method, of class Manipulation.
   */
  @Test
  public void testSetAlterVehicle()
  {
    System.out.println("setAlterVehicle");
    boolean alterVehicle = false;
    Manipulation instance = null;
    instance.setAlterVehicle(alterVehicle);
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