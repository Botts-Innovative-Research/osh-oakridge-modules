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
package gov.llnl.ernie.api;

import gov.llnl.ernie.Analysis;
import java.io.File;
import java.io.InputStream;
import java.time.Instant;
import java.util.UUID;
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
public class ResultsNGTest
{
  
  public ResultsNGTest()
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
   * Test of setERNIEContextualInfo method, of class Results.
   */
  @Test
  public void testSetERNIEContextualInfo()
  {
    System.out.println("setERNIEContextualInfo");
    Results.ERNIEContextualInfo context = null;
    Results instance = new Results();
    instance.setERNIEContextualInfo(context);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getVersionID method, of class Results.
   */
  @Test
  public void testGetVersionID()
  {
    System.out.println("getVersionID");
    Results instance = new Results();
    String expResult = "";
    String result = instance.getVersionID();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getModelID method, of class Results.
   */
  @Test
  public void testGetModelID()
  {
    System.out.println("getModelID");
    Results instance = new Results();
    String expResult = "";
    String result = instance.getModelID();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getThresholds method, of class Results.
   */
  @Test
  public void testGetThresholds()
  {
    System.out.println("getThresholds");
    Results instance = new Results();
    Results.Thresholds expResult = null;
    Results.Thresholds result = instance.getThresholds();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getDateTime method, of class Results.
   */
  @Test
  public void testGetDateTime()
  {
    System.out.println("getDateTime");
    Results instance = new Results();
    Instant expResult = null;
    Instant result = instance.getDateTime();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setDateTime method, of class Results.
   */
  @Test
  public void testSetDateTime()
  {
    System.out.println("setDateTime");
    Instant dateTime = null;
    Results instance = new Results();
    instance.setDateTime(dateTime);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getPortID method, of class Results.
   */
  @Test
  public void testGetPortID()
  {
    System.out.println("getPortID");
    Results instance = new Results();
    String expResult = "";
    String result = instance.getPortID();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setPortID method, of class Results.
   */
  @Test
  public void testSetPortID()
  {
    System.out.println("setPortID");
    String PortID = "";
    Results instance = new Results();
    instance.setPortID(PortID);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getLaneID method, of class Results.
   */
  @Test
  public void testGetLaneID()
  {
    System.out.println("getLaneID");
    Results instance = new Results();
    long expResult = 0L;
    long result = instance.getLaneID();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setLaneID method, of class Results.
   */
  @Test
  public void testSetLaneID()
  {
    System.out.println("setLaneID");
    long LaneID = 0L;
    Results instance = new Results();
    instance.setLaneID(LaneID);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getERNIEGammaAlert method, of class Results.
   */
  @Test
  public void testGetERNIEGammaAlert()
  {
    System.out.println("getERNIEGammaAlert");
    Results instance = new Results();
    boolean expResult = false;
    boolean result = instance.getERNIEGammaAlert();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setERNIEGammaAlert method, of class Results.
   */
  @Test
  public void testSetERNIEGammaAlert()
  {
    System.out.println("setERNIEGammaAlert");
    boolean GammaAlert = false;
    Results instance = new Results();
    instance.setERNIEGammaAlert(GammaAlert);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getRPMGammaAlert method, of class Results.
   */
  @Test
  public void testGetRPMGammaAlert()
  {
    System.out.println("getRPMGammaAlert");
    Results instance = new Results();
    boolean expResult = false;
    boolean result = instance.getRPMGammaAlert();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setRPMGammaAlert method, of class Results.
   */
  @Test
  public void testSetRPMGammaAlert()
  {
    System.out.println("setRPMGammaAlert");
    boolean GammaAlert = false;
    Results instance = new Results();
    instance.setRPMGammaAlert(GammaAlert);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getERNIENeutronAlert method, of class Results.
   */
  @Test
  public void testGetERNIENeutronAlert()
  {
    System.out.println("getERNIENeutronAlert");
    Results instance = new Results();
    boolean expResult = false;
    boolean result = instance.getERNIENeutronAlert();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setERNIENeutronAlert method, of class Results.
   */
  @Test
  public void testSetERNIENeutronAlert()
  {
    System.out.println("setERNIENeutronAlert");
    boolean NeutronAlert = false;
    Results instance = new Results();
    instance.setERNIENeutronAlert(NeutronAlert);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getRPMNeutronAlert method, of class Results.
   */
  @Test
  public void testGetRPMNeutronAlert()
  {
    System.out.println("getRPMNeutronAlert");
    Results instance = new Results();
    boolean expResult = false;
    boolean result = instance.getRPMNeutronAlert();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setRPMNeutronAlert method, of class Results.
   */
  @Test
  public void testSetRPMNeutronAlert()
  {
    System.out.println("setRPMNeutronAlert");
    boolean NeutronAlert = false;
    Results instance = new Results();
    instance.setRPMNeutronAlert(NeutronAlert);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getRPMScanError method, of class Results.
   */
  @Test
  public void testGetRPMScanError()
  {
    System.out.println("getRPMScanError");
    Results instance = new Results();
    boolean expResult = false;
    boolean result = instance.getRPMScanError();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setRPMScanError method, of class Results.
   */
  @Test
  public void testSetRPMScanError()
  {
    System.out.println("setRPMScanError");
    boolean scanError = false;
    Results instance = new Results();
    instance.setRPMScanError(scanError);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getSegmentId method, of class Results.
   */
  @Test
  public void testGetSegmentId()
  {
    System.out.println("getSegmentId");
    Results instance = new Results();
    long expResult = 0L;
    long result = instance.getSegmentId();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setSegmentId method, of class Results.
   */
  @Test
  public void testSetSegmentId()
  {
    System.out.println("setSegmentId");
    long SegmentId = 0L;
    Results instance = new Results();
    instance.setSegmentId(SegmentId);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getResult method, of class Results.
   */
  @Test
  public void testGetResult()
  {
    System.out.println("getResult");
    Results instance = new Results();
    Analysis.RecommendedAction expResult = null;
    Analysis.RecommendedAction result = instance.getResult();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of isFallback method, of class Results.
   */
  @Test
  public void testIsFallback()
  {
    System.out.println("isFallback");
    Results instance = new Results();
    boolean expResult = false;
    boolean result = instance.isFallback();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setResult method, of class Results.
   */
  @Test
  public void testSetResult()
  {
    System.out.println("setResult");
    Analysis.RecommendedAction Result = null;
    Results instance = new Results();
    instance.setResult(Result);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getRPMResult method, of class Results.
   */
  @Test
  public void testGetRPMResult()
  {
    System.out.println("getRPMResult");
    Results instance = new Results();
    Analysis.RecommendedAction expResult = null;
    Analysis.RecommendedAction result = instance.getRPMResult();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setRPMResult method, of class Results.
   */
  @Test
  public void testSetRPMResult()
  {
    System.out.println("setRPMResult");
    Analysis.RecommendedAction RPMResult = null;
    Results instance = new Results();
    instance.setRPMResult(RPMResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getReleaseProbability method, of class Results.
   */
  @Test
  public void testGetReleaseProbability()
  {
    System.out.println("getReleaseProbability");
    Results instance = new Results();
    double expResult = 0.0;
    double result = instance.getReleaseProbability();
    assertEquals(result, expResult, 0.0);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setReleaseProbability method, of class Results.
   */
  @Test
  public void testSetReleaseProbability()
  {
    System.out.println("setReleaseProbability");
    double releaseProbability = 0.0;
    Results instance = new Results();
    instance.setReleaseProbability(releaseProbability);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getInvestigateProbability method, of class Results.
   */
  @Test
  public void testGetInvestigateProbability()
  {
    System.out.println("getInvestigateProbability");
    Results instance = new Results();
    double expResult = 0.0;
    double result = instance.getInvestigateProbability();
    assertEquals(result, expResult, 0.0);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setInvestigateProbability method, of class Results.
   */
  @Test
  public void testSetInvestigateProbability()
  {
    System.out.println("setInvestigateProbability");
    double investigateProbability = 0.0;
    Results instance = new Results();
    instance.setInvestigateProbability(investigateProbability);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getPosition method, of class Results.
   */
  @Test
  public void testGetPosition()
  {
    System.out.println("getPosition");
    Results instance = new Results();
    double[] expResult = null;
    double[] result = instance.getPosition();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setPosition method, of class Results.
   */
  @Test
  public void testSetPosition()
  {
    System.out.println("setPosition");
    double[] position = null;
    Results instance = new Results();
    instance.setPosition(position);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getVehicleLength method, of class Results.
   */
  @Test
  public void testGetVehicleLength()
  {
    System.out.println("getVehicleLength");
    Results instance = new Results();
    double expResult = 0.0;
    double result = instance.getVehicleLength();
    assertEquals(result, expResult, 0.0);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setVehicleLength method, of class Results.
   */
  @Test
  public void testSetVehicleLength()
  {
    System.out.println("setVehicleLength");
    double vehicleLength = 0.0;
    Results instance = new Results();
    instance.setVehicleLength(vehicleLength);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getVehicleClass method, of class Results.
   */
  @Test
  public void testGetVehicleClass()
  {
    System.out.println("getVehicleClass");
    Results instance = new Results();
    int expResult = 0;
    int result = instance.getVehicleClass();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setVehicleClass method, of class Results.
   */
  @Test
  public void testSetVehicleClass()
  {
    System.out.println("setVehicleClass");
    int vehicleClass = 0;
    Results instance = new Results();
    instance.setVehicleClass(vehicleClass);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getBackground method, of class Results.
   */
  @Test
  public void testGetBackground()
  {
    System.out.println("getBackground");
    Results instance = new Results();
    double[][] expResult = null;
    double[][] result = instance.getBackground();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setBackground method, of class Results.
   */
  @Test
  public void testSetBackground()
  {
    System.out.println("setBackground");
    double[][] background = null;
    Results instance = new Results();
    instance.setBackground(background);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of addSource method, of class Results.
   */
  @Test
  public void testAddSource()
  {
    System.out.println("addSource");
    Results.Source source = null;
    Results instance = new Results();
    instance.addSource(source);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setOverallSource method, of class Results.
   */
  @Test
  public void testSetOverallSource()
  {
    System.out.println("setOverallSource");
    Results.Source source = null;
    Results instance = new Results();
    instance.setOverallSource(source);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getSource method, of class Results.
   */
  @Test
  public void testGetSource()
  {
    System.out.println("getSource");
    int index = 0;
    Results instance = new Results();
    Results.Source expResult = null;
    Results.Source result = instance.getSource(index);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getOverallSource method, of class Results.
   */
  @Test
  public void testGetOverallSource()
  {
    System.out.println("getOverallSource");
    Results instance = new Results();
    Results.Source expResult = null;
    Results.Source result = instance.getOverallSource();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getNumberOfSources method, of class Results.
   */
  @Test
  public void testGetNumberOfSources()
  {
    System.out.println("getNumberOfSources");
    Results instance = new Results();
    int expResult = 0;
    int result = instance.getNumberOfSources();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getMessage method, of class Results.
   */
  @Test
  public void testGetMessage()
  {
    System.out.println("getMessage");
    Results instance = new Results();
    String expResult = "";
    String result = instance.getMessage();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setMessage method, of class Results.
   */
  @Test
  public void testSetMessage()
  {
    System.out.println("setMessage");
    String Message = "";
    Results instance = new Results();
    instance.setMessage(Message);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getException method, of class Results.
   */
  @Test
  public void testGetException()
  {
    System.out.println("getException");
    Results instance = new Results();
    Exception expResult = null;
    Exception result = instance.getException();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setException method, of class Results.
   */
  @Test
  public void testSetException()
  {
    System.out.println("setException");
    Exception exception = null;
    Results instance = new Results();
    instance.setException(exception);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setAction method, of class Results.
   */
  @Test
  public void testSetAction()
  {
    System.out.println("setAction");
    int action = 0;
    Results instance = new Results();
    instance.setAction(action);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setMessageInvalid method, of class Results.
   */
  @Test
  public void testSetMessageInvalid()
  {
    System.out.println("setMessageInvalid");
    UUID scanId = null;
    Results instance = new Results();
    instance.setMessageInvalid(scanId);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setMessageSuccessful method, of class Results.
   */
  @Test
  public void testSetMessageSuccessful()
  {
    System.out.println("setMessageSuccessful");
    UUID scanId = null;
    Results instance = new Results();
    instance.setMessageSuccessful(scanId);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setMessageError method, of class Results.
   */
  @Test
  public void testSetMessageError()
  {
    System.out.println("setMessageError");
    UUID scanId = null;
    Exception ex = null;
    Results instance = new Results();
    instance.setMessageError(scanId, ex);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getYellowLightMessage method, of class Results.
   */
  @Test
  public void testGetYellowLightMessage()
  {
    System.out.println("getYellowLightMessage");
    Results instance = new Results();
    String expResult = "";
    String result = instance.getYellowLightMessage();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setYellowLightMessage method, of class Results.
   */
  @Test
  public void testSetYellowLightMessage()
  {
    System.out.println("setYellowLightMessage");
    String YellowLightMessage = "";
    Results instance = new Results();
    instance.setYellowLightMessage(YellowLightMessage);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of saveToXML method, of class Results.
   */
  @Test
  public void testSaveToXML() throws Exception
  {
    System.out.println("saveToXML");
    File outputFile = null;
    Results instance = new Results();
    instance.saveToXML(outputFile);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of toXMLString method, of class Results.
   */
  @Test
  public void testToXMLString() throws Exception
  {
    System.out.println("toXMLString");
    Results instance = new Results();
    String expResult = "";
    String result = instance.toXMLString();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of loadFromXML method, of class Results.
   */
  @Test
  public void testLoadFromXML_File() throws Exception
  {
    System.out.println("loadFromXML");
    File xmlFile = null;
    Results expResult = null;
    Results result = Results.loadFromXML(xmlFile);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of loadFromXML method, of class Results.
   */
  @Test
  public void testLoadFromXML_InputStream() throws Exception
  {
    System.out.println("loadFromXML");
    InputStream in = null;
    Results expResult = null;
    Results result = Results.loadFromXML(in);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of loadFromXML method, of class Results.
   */
  @Test
  public void testLoadFromXML_String() throws Exception
  {
    System.out.println("loadFromXML");
    String xmlString = "";
    Results expResult = null;
    Results result = Results.loadFromXML(xmlString);
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