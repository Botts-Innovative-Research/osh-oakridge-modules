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

import gov.llnl.utility.PackageResource;
import gov.llnl.utility.xml.DomBuilder;
import gov.llnl.utility.xml.bind.ObjectWriter;
import gov.llnl.utility.xml.bind.WriterContext;
import static org.testng.Assert.*;
import org.testng.annotations.Test;
import org.w3c.dom.Document;

/**
 *
 * @author pham21
 */
public class WriterContextImplNGTest
{
  
  public WriterContextImplNGTest()
  {
  }

  /**
   * Test of setMarshaller method, of class WriterContextImpl.
   */
  @Test
  public void testSetMarshaller()
  {
    System.out.println("setMarshaller");
    WriterContext.Marshaller marshall = null;
    WriterContextImpl instance = null;
    instance.setMarshaller(marshall);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getMarshaller method, of class WriterContextImpl.
   */
  @Test
  public void testGetMarshaller() throws Exception
  {
    System.out.println("getMarshaller");
    Class cls = null;
    WriterContextImpl instance = null;
    WriterContext.Marshaller expResult = null;
    WriterContext.Marshaller result = instance.getMarshaller(cls);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getMarshallerOptions method, of class WriterContextImpl.
   */
  @Test
  public void testGetMarshallerOptions()
  {
    System.out.println("getMarshallerOptions");
    WriterContextImpl instance = null;
    WriterContext.MarshallerOptions expResult = null;
    WriterContext.MarshallerOptions result = instance.getMarshallerOptions();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setProperty method, of class WriterContextImpl.
   */
  @Test
  public void testSetProperty()
  {
    System.out.println("setProperty");
    String key = "";
    Object value = null;
    WriterContextImpl instance = null;
    instance.setProperty(key, value);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getProperty method, of class WriterContextImpl.
   */
  @Test
  public void testGetProperty_String()
  {
    System.out.println("getProperty");
    String key = "";
    WriterContextImpl instance = null;
    Object expResult = null;
    Object result = instance.getProperty(key);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getProperty method, of class WriterContextImpl.
   */
  @Test
  public void testGetProperty_3args()
  {
    System.out.println("getProperty");
//    String key = "";
//    Class<T> cls = null;
//    Object defaultValue = null;
//    WriterContextImpl instance = null;
//    Object expResult = null;
//    Object result = instance.getProperty(key, cls, defaultValue);
//    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of addContents method, of class WriterContextImpl.
   */
  @Test
  public void testAddContents() throws Exception
  {
    System.out.println("addContents");
    Object object = null;
    WriterContextImpl instance = null;
    instance.addContents(object);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of register method, of class WriterContextImpl.
   */
  @Test
  public void testRegister()
  {
    System.out.println("register");
    Object obj = null;
    String prefix = "";
    String key = "";
    WriterContextImpl instance = null;
    String expResult = "";
    String result = instance.register(obj, prefix, key);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of get method, of class WriterContextImpl.
   */
  @Test
  public void testGet()
  {
    System.out.println("get");
    Object obj = null;
    WriterContextImpl instance = null;
    String expResult = "";
    String result = instance.get(obj);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of write method, of class WriterContextImpl.
   */
  @Test
  public void testWrite() throws Exception
  {
    System.out.println("write");
//    Document document = null;
//    ObjectWriter<Type> writer = null;
//    String elementName = "";
//    Object object = null;
//    boolean objectRoot = false;
//    WriterContextImpl instance = null;
//    DomBuilder expResult = null;
//    DomBuilder result = instance.write(document, writer, elementName, object, objectRoot);
//    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of writeContent method, of class WriterContextImpl.
   */
  @Test
  public void testWriteContent() throws Exception
  {
    System.out.println("writeContent");
    String elementName = "";
    Object value = null;
    WriterContextImpl instance = null;
    instance.writeContent(elementName, value);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of newBuilder method, of class WriterContextImpl.
   */
  @Test
  public void testNewBuilder()
  {
    System.out.println("newBuilder");
    ObjectWriter writer = null;
    WriterContextImpl instance = null;
    WriterBuilderImpl expResult = null;
    WriterBuilderImpl result = instance.newBuilder(writer);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of pushContext method, of class WriterContextImpl.
   */
  @Test
  public void testPushContext()
  {
    System.out.println("pushContext");
    DomBuilder element = null;
    Object object = null;
    String elementName = "";
    PackageResource pkg = null;
    WriterContextImpl instance = null;
    WriterContextImpl.ContextEntry expResult = null;
    WriterContextImpl.ContextEntry result = instance.pushContext(element, object, elementName, pkg);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of popContext method, of class WriterContextImpl.
   */
  @Test
  public void testPopContext()
  {
    System.out.println("popContext");
    WriterContextImpl instance = null;
    instance.popContext();
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of current method, of class WriterContextImpl.
   */
  @Test
  public void testCurrent()
  {
    System.out.println("current");
    WriterContextImpl instance = null;
    WriterContextImpl.ContextEntry expResult = null;
    WriterContextImpl.ContextEntry result = instance.current();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of last method, of class WriterContextImpl.
   */
  @Test
  public void testLast()
  {
    System.out.println("last");
    WriterContextImpl instance = null;
    WriterContextImpl.ContextEntry expResult = null;
    WriterContextImpl.ContextEntry result = instance.last();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of newElement method, of class WriterContextImpl.
   */
  @Test
  public void testNewElement() throws Exception
  {
    System.out.println("newElement");
    String elementName = "";
    WriterContextImpl instance = null;
    DomBuilder expResult = null;
    DomBuilder result = instance.newElement(null, elementName);
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