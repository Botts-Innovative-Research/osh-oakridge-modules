/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.utility.internal.xml.bind;

import gov.llnl.utility.xml.bind.ObjectReader;
import gov.llnl.utility.xml.bind.PropertyMap;
import gov.llnl.utility.xml.bind.ReaderContext;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.Map;
import static org.testng.Assert.*;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.xml.sax.InputSource;

/**
 *
 * @author pham21
 */
public class DocumentReaderImplNGTest
{
  
  public DocumentReaderImplNGTest()
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
   * Test of getContext method, of class DocumentReaderImpl.
   */
  @Test
  public void testGetContext()
  {
    System.out.println("getContext");
    DocumentReaderImpl instance = null;
    ReaderContext expResult = null;
    ReaderContext result = instance.getContext();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of clearContext method, of class DocumentReaderImpl.
   */
  @Test
  public void testClearContext()
  {
    System.out.println("clearContext");
    DocumentReaderImpl instance = null;
    instance.clearContext();
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of createContext method, of class DocumentReaderImpl.
   */
  @Test
  public void testCreateContext() throws Exception
  {
    System.out.println("createContext");
    DocumentReaderImpl instance = null;
    ReaderContextImpl expResult = null;
    ReaderContextImpl result = instance.createContext();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of loadStream method, of class DocumentReaderImpl.
   */
  @Test
  public void testLoadStream() throws Exception
  {
    System.out.println("loadStream");
    InputStream stream = null;
    DocumentReaderImpl instance = null;
    Object expResult = null;
    Object result = instance.loadStream(stream);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of loadResource method, of class DocumentReaderImpl.
   */
  @Test
  public void testLoadResource() throws Exception
  {
    System.out.println("loadResource");
    String resourceName = "";
    DocumentReaderImpl instance = null;
    Object expResult = null;
    Object result = instance.loadResource(resourceName);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of loadFile method, of class DocumentReaderImpl.
   */
  @Test
  public void testLoadFile() throws Exception
  {
    System.out.println("loadFile");
    Path file = null;
    DocumentReaderImpl instance = null;
    Object expResult = null;
    Object result = instance.loadFile(file);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of loadURL method, of class DocumentReaderImpl.
   */
  @Test
  public void testLoadURL() throws Exception
  {
    System.out.println("loadURL");
    URL url = null;
    DocumentReaderImpl instance = null;
    Object expResult = null;
    Object result = instance.loadURL(url);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of loadSource method, of class DocumentReaderImpl.
   */
  @Test
  public void testLoadSource() throws Exception
  {
    System.out.println("loadSource");
    InputSource inputSource = null;
    DocumentReaderImpl instance = null;
    Object expResult = null;
    Object result = instance.loadSource(inputSource);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getObjectReader method, of class DocumentReaderImpl.
   */
  @Test
  public void testGetObjectReader()
  {
    System.out.println("getObjectReader");
    DocumentReaderImpl instance = null;
    ObjectReader expResult = null;
    ObjectReader result = instance.getObjectReader();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setErrorHandler method, of class DocumentReaderImpl.
   */
  @Test
  public void testSetErrorHandler()
  {
    System.out.println("setErrorHandler");
    ReaderContext.ExceptionHandler exceptionHandler = null;
    DocumentReaderImpl instance = null;
    instance.setErrorHandler(exceptionHandler);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setProperty method, of class DocumentReaderImpl.
   */
  @Test
  public void testSetProperty()
  {
    System.out.println("setProperty");
    String key = "";
    Object value = null;
    DocumentReaderImpl instance = null;
    instance.setProperty(key, value);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getProperty method, of class DocumentReaderImpl.
   */
  @Test
  public void testGetProperty()
  {
    System.out.println("getProperty");
    String key = "";
    DocumentReaderImpl instance = null;
    Object expResult = null;
    Object result = instance.getProperty(key);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getProperties method, of class DocumentReaderImpl.
   */
  @Test
  public void testGetProperties()
  {
    System.out.println("getProperties");
    DocumentReaderImpl instance = null;
    Map expResult = null;
    Map result = instance.getProperties();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setPropertyHandler method, of class DocumentReaderImpl.
   */
  @Test
  public void testSetPropertyHandler()
  {
    System.out.println("setPropertyHandler");
    PropertyMap handler = null;
    DocumentReaderImpl instance = null;
    instance.setPropertyHandler(handler);
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