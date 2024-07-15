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

import gov.llnl.ernie.analysis.AnalysisPreprocessor;
import gov.llnl.ernie.analysis.FeatureExtractor;
import gov.llnl.ernie.classifier.Classifier;
import gov.llnl.ernie.data.AnalysisResult;
import gov.llnl.ernie.data.Record;
import gov.llnl.ernie.impl.AnalysisResultImpl;
import java.util.Collection;
import java.util.List;
import java.util.Map;
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
public class VM250AnalysisNGTest
{
  
  public VM250AnalysisNGTest()
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
   * Test of initialize method, of class VM250Analysis.
   */
  @Test
  public void testInitialize() throws Exception
  {
    System.out.println("initialize");
    VM250Analysis instance = new VM250Analysis();
    instance.initialize();
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of processRecord method, of class VM250Analysis.
   */
  @Test
  public void testProcessRecord() throws Exception
  {
    System.out.println("processRecord");
    Record record = null;
    VM250Analysis instance = new VM250Analysis();
    AnalysisResult expResult = null;
    AnalysisResult result = instance.processRecord(record);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of irregularVehicle method, of class VM250Analysis.
   */
  @Test
  public void testIrregularVehicle()
  {
    System.out.println("irregularVehicle");
    Record record = null;
    VM250Analysis instance = new VM250Analysis();
    boolean expResult = false;
    boolean result = instance.irregularVehicle(record);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of allocateResult method, of class VM250Analysis.
   */
  @Test
  public void testAllocateResult()
  {
    System.out.println("allocateResult");
    VM250Analysis instance = new VM250Analysis();
    AnalysisResult expResult = null;
    AnalysisResult result = instance.allocateResult();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of prepare method, of class VM250Analysis.
   */
  @Test
  public void testPrepare() throws Exception
  {
    System.out.println("prepare");
    Record record = null;
    VM250Analysis instance = new VM250Analysis();
    instance.prepare(record);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of extractFeatures method, of class VM250Analysis.
   */
  @Test
  public void testExtractFeatures() throws Exception
  {
    System.out.println("extractFeatures");
    AnalysisResult results = null;
    Record record = null;
    Collection<FeatureExtractor> extractors = null;
    VM250Analysis instance = new VM250Analysis();
    instance.extractFeatures(results, record, extractors);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of executeFallback method, of class VM250Analysis.
   */
  @Test
  public void testExecuteFallback()
  {
    System.out.println("executeFallback");
    AnalysisResult result2 = null;
    Record record = null;
    VM250Analysis instance = new VM250Analysis();
    instance.executeFallback(result2, record);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of executeClassifier method, of class VM250Analysis.
   */
  @Test
  public void testExecuteClassifier() throws Exception
  {
    System.out.println("executeClassifier");
    AnalysisResultImpl result_2 = null;
    VM250Analysis instance = new VM250Analysis();
    instance.executeClassifier(result_2);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getThreshold method, of class VM250Analysis.
   */
  @Test
  public void testGetThreshold()
  {
    System.out.println("getThreshold");
    VM250Analysis instance = new VM250Analysis();
    double expResult = 0.0;
    double result = instance.getThreshold();
    assertEquals(result, expResult, 0.0);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setThreshold method, of class VM250Analysis.
   */
  @Test
  public void testSetThreshold()
  {
    System.out.println("setThreshold");
    double threshold = 0.0;
    VM250Analysis instance = new VM250Analysis();
    instance.setThreshold(threshold);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setClassifier method, of class VM250Analysis.
   */
  @Test
  public void testSetClassifier()
  {
    System.out.println("setClassifier");
    Classifier cls = null;
    VM250Analysis instance = new VM250Analysis();
    instance.setClassifier(cls);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getModelName method, of class VM250Analysis.
   */
  @Test
  public void testGetModelName()
  {
    System.out.println("getModelName");
    VM250Analysis instance = new VM250Analysis();
    String expResult = "";
    String result = instance.getModelName();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getFeatureExtractors method, of class VM250Analysis.
   */
  @Test
  public void testGetFeatureExtractors()
  {
    System.out.println("getFeatureExtractors");
    VM250Analysis instance = new VM250Analysis();
    List expResult = null;
    List result = instance.getFeatureExtractors();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setPreprocessors method, of class VM250Analysis.
   */
  @Test
  public void testSetPreprocessors()
  {
    System.out.println("setPreprocessors");
    List<AnalysisPreprocessor> preprocessors = null;
    VM250Analysis instance = new VM250Analysis();
    instance.setPreprocessors(preprocessors);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setFeatureExtractors method, of class VM250Analysis.
   */
  @Test
  public void testSetFeatureExtractors()
  {
    System.out.println("setFeatureExtractors");
    List<FeatureExtractor> featureExtractors = null;
    VM250Analysis instance = new VM250Analysis();
    instance.setFeatureExtractors(featureExtractors);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of isVerbose method, of class VM250Analysis.
   */
  @Test
  public void testIsVerbose()
  {
    System.out.println("isVerbose");
    VM250Analysis instance = new VM250Analysis();
    boolean expResult = false;
    boolean result = instance.isVerbose();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getBackgroundPreprocessor method, of class VM250Analysis.
   */
  @Test
  public void testGetBackgroundPreprocessor()
  {
    System.out.println("getBackgroundPreprocessor");
    VM250Analysis instance = new VM250Analysis();
    VM250BackgroundEstimator expResult = null;
    VM250BackgroundEstimator result = instance.getBackgroundPreprocessor();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getPreprocessors method, of class VM250Analysis.
   */
  @Test
  public void testGetPreprocessors()
  {
    System.out.println("getPreprocessors");
    VM250Analysis instance = new VM250Analysis();
    List expResult = null;
    List result = instance.getPreprocessors();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getExtractors method, of class VM250Analysis.
   */
  @Test
  public void testGetExtractors()
  {
    System.out.println("getExtractors");
    VM250Analysis instance = new VM250Analysis();
    List expResult = null;
    List result = instance.getExtractors();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getClassifier method, of class VM250Analysis.
   */
  @Test
  public void testGetClassifier()
  {
    System.out.println("getClassifier");
    VM250Analysis instance = new VM250Analysis();
    Classifier expResult = null;
    Classifier result = instance.getClassifier();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of addSetting method, of class VM250Analysis.
   */
  @Test
  public void testAddSetting()
  {
    System.out.println("addSetting");
    String key = "";
    String value = "";
    VM250Analysis instance = new VM250Analysis();
    instance.addSetting(key, value);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getSettings method, of class VM250Analysis.
   */
  @Test
  public void testGetSettings()
  {
    System.out.println("getSettings");
    VM250Analysis instance = new VM250Analysis();
    Map expResult = null;
    Map result = instance.getSettings();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getFallbackExtractors method, of class VM250Analysis.
   */
  @Test
  public void testGetFallbackExtractors()
  {
    System.out.println("getFallbackExtractors");
    VM250Analysis instance = new VM250Analysis();
    List expResult = null;
    List result = instance.getFallbackExtractors();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getFaultClass method, of class VM250Analysis.
   */
  @Test
  public void testGetFaultClass()
  {
    System.out.println("getFaultClass");
    VM250Analysis instance = new VM250Analysis();
    Class expResult = null;
    Class result = instance.getFaultClass();
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