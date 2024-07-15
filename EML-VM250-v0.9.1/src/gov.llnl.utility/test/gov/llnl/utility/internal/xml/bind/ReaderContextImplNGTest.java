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

import gov.llnl.utility.io.PathLocation;
import gov.llnl.utility.xml.bind.DocumentReader;
import gov.llnl.utility.xml.bind.PropertyMap;
import gov.llnl.utility.xml.bind.ReaderContext;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.function.BiConsumer;
import static org.testng.Assert.*;
import org.testng.annotations.Test;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;

/**
 *
 * @author pham21
 */
public class ReaderContextImplNGTest
{
  
  public ReaderContextImplNGTest()
  {
  }

  /**
   * Test of getLocation method, of class ReaderContextImpl.
   */
  @Test
  public void testGetLocation()
  {
    System.out.println("getLocation");
    ReaderContextImpl instance = new ReaderContextImpl();
    PathLocation expResult = null;
    PathLocation result = instance.getLocation();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getElementPath method, of class ReaderContextImpl.
   */
  @Test
  public void testGetElementPath()
  {
    System.out.println("getElementPath");
    ReaderContextImpl instance = new ReaderContextImpl();
    String expResult = "";
    String result = instance.getElementPath();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setFile method, of class ReaderContextImpl.
   */
  @Test
  public void testSetFile()
  {
    System.out.println("setFile");
    URI file = null;
    ReaderContextImpl instance = new ReaderContextImpl();
    instance.setFile(file);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getFile method, of class ReaderContextImpl.
   */
  @Test
  public void testGetFile()
  {
    System.out.println("getFile");
    ReaderContextImpl instance = new ReaderContextImpl();
    URI expResult = null;
    URI result = instance.getFile();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setLocator method, of class ReaderContextImpl.
   */
  @Test
  public void testSetLocator()
  {
    System.out.println("setLocator");
    Locator locator = null;
    ReaderContextImpl instance = new ReaderContextImpl();
    instance.setLocator(locator);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of pushTemporaryContext method, of class ReaderContextImpl.
   */
  @Test
  public void testPushTemporaryContext()
  {
    System.out.println("pushTemporaryContext");
    Object parent = null;
    Object child = null;
    ReaderContextImpl instance = new ReaderContextImpl();
    instance.pushTemporaryContext(parent, child);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of popTemporaryContext method, of class ReaderContextImpl.
   */
  @Test
  public void testPopTemporaryContext()
  {
    System.out.println("popTemporaryContext");
    ReaderContextImpl instance = new ReaderContextImpl();
    instance.popTemporaryContext();
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of addDeferred method, of class ReaderContextImpl.
   */
  @Test
  public void testAddDeferred_4args() throws Exception
  {
    System.out.println("addDeferred");
//    Object target = null;
//    BiConsumer<T, T2> method = null;
//    String refId = "";
//    Class<T2> cls = null;
//    ReaderContextImpl instance = new ReaderContextImpl();
//    instance.addDeferred(target, method, refId, cls);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of addDeferred method, of class ReaderContextImpl.
   */
  @Test
  public void testAddDeferred_String_DeferredHandler() throws Exception
  {
    System.out.println("addDeferred");
    String refId = "";
    DeferredHandler handler = null;
    ReaderContextImpl instance = new ReaderContextImpl();
    instance.addDeferred(refId, handler);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of hasDeferred method, of class ReaderContextImpl.
   */
  @Test
  public void testHasDeferred()
  {
    System.out.println("hasDeferred");
    ReaderContextImpl instance = new ReaderContextImpl();
    boolean expResult = false;
    boolean result = instance.hasDeferred();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getDeferredElements method, of class ReaderContextImpl.
   */
  @Test
  public void testGetDeferredElements()
  {
    System.out.println("getDeferredElements");
    ReaderContextImpl instance = new ReaderContextImpl();
    String expResult = "";
    String result = instance.getDeferredElements();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getExternal method, of class ReaderContextImpl.
   */
  @Test
  public void testGetExternal() throws Exception
  {
    System.out.println("getExternal");
    String file = "";
    ReaderContextImpl instance = new ReaderContextImpl();
    URL expResult = null;
    URL result = instance.getExternal(file);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of put method, of class ReaderContextImpl.
   */
  @Test
  public void testPut() throws Exception
  {
    System.out.println("put");
    String name = "";
    Object object = null;
    ReaderContextImpl instance = new ReaderContextImpl();
    Object expResult = null;
    Object result = instance.put(name, object);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of putScoped method, of class ReaderContextImpl.
   */
  @Test
  public void testPutScoped()
  {
    System.out.println("putScoped");
    String name = "";
    Object object = null;
    ReaderContextImpl instance = new ReaderContextImpl();
    Object expResult = null;
    Object result = instance.putScoped(name, object);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of get method, of class ReaderContextImpl.
   */
  @Test
  public void testGet_String_Class() throws Exception
  {
    System.out.println("get");
//    String name = "";
//    Class<T> kls = null;
//    ReaderContextImpl instance = new ReaderContextImpl();
//    Object expResult = null;
//    Object result = instance.get(name, kls);
//    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getReferences method, of class ReaderContextImpl.
   */
  @Test
  public void testGetReferences()
  {
    System.out.println("getReferences");
    ReaderContextImpl instance = new ReaderContextImpl();
    List expResult = null;
    List result = instance.getReferences();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of get method, of class ReaderContextImpl.
   */
  @Test
  public void testGet_String()
  {
    System.out.println("get");
    String name = "";
    ReaderContextImpl instance = new ReaderContextImpl();
    Object expResult = null;
    Object result = instance.get(name);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setPropertyHandler method, of class ReaderContextImpl.
   */
  @Test
  public void testSetPropertyHandler()
  {
    System.out.println("setPropertyHandler");
    PropertyMap handler = null;
    ReaderContextImpl instance = new ReaderContextImpl();
    instance.setPropertyHandler(handler);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getCurrentHandlerContext method, of class ReaderContextImpl.
   */
  @Test
  public void testGetCurrentHandlerContext()
  {
    System.out.println("getCurrentHandlerContext");
    ReaderContextImpl instance = new ReaderContextImpl();
    HandlerContextImpl expResult = null;
    HandlerContextImpl result = instance.getCurrentHandlerContext();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getLastHandlerContext method, of class ReaderContextImpl.
   */
  @Test
  public void testGetLastHandlerContext()
  {
    System.out.println("getLastHandlerContext");
    ReaderContextImpl instance = new ReaderContextImpl();
    HandlerContextImpl expResult = null;
    HandlerContextImpl result = instance.getLastHandlerContext();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of startElement method, of class ReaderContextImpl.
   */
  @Test
  public void testStartElement() throws Exception
  {
    System.out.println("startElement");
    String namespaceURI = "";
    String localName = "";
    String qualifiedName = "";
    Attributes attr = null;
    ReaderContextImpl instance = new ReaderContextImpl();
    HandlerContextImpl expResult = null;
    HandlerContextImpl result = instance.startElement(namespaceURI, localName, qualifiedName, attr);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of endElement method, of class ReaderContextImpl.
   */
  @Test
  public void testEndElement() throws Exception
  {
    System.out.println("endElement");
    ReaderContextImpl instance = new ReaderContextImpl();
    HandlerContextImpl expResult = null;
    HandlerContextImpl result = instance.endElement();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setLastObject method, of class ReaderContextImpl.
   */
  @Test
  public void testSetLastObject()
  {
    System.out.println("setLastObject");
    Object lastObject = null;
    ReaderContextImpl instance = new ReaderContextImpl();
    instance.setLastObject(lastObject);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getLastObject method, of class ReaderContextImpl.
   */
  @Test
  public void testGetLastObject()
  {
    System.out.println("getLastObject");
    ReaderContextImpl instance = new ReaderContextImpl();
    Object expResult = null;
    Object result = instance.getLastObject();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setErrorHandler method, of class ReaderContextImpl.
   */
  @Test
  public void testSetErrorHandler()
  {
    System.out.println("setErrorHandler");
    ReaderContext.ExceptionHandler handler = null;
    ReaderContextImpl instance = new ReaderContextImpl();
    instance.setErrorHandler(handler);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of handleException method, of class ReaderContextImpl.
   */
  @Test
  public void testHandleException() throws Exception
  {
    System.out.println("handleException");
    Throwable ex = null;
    ReaderContextImpl instance = new ReaderContextImpl();
    instance.handleException(ex);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getDocumentReader method, of class ReaderContextImpl.
   */
  @Test
  public void testGetDocumentReader()
  {
    System.out.println("getDocumentReader");
    ReaderContextImpl instance = new ReaderContextImpl();
    DocumentReader expResult = null;
    DocumentReader result = instance.getDocumentReader();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setDocumentReader method, of class ReaderContextImpl.
   */
  @Test
  public void testSetDocumentReader()
  {
    System.out.println("setDocumentReader");
    DocumentReader documentReader = null;
    ReaderContextImpl instance = new ReaderContextImpl();
    instance.setDocumentReader(documentReader);
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