/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.utility.xml.bind;

import gov.llnl.utility.PackageResource;
import gov.llnl.utility.io.WriterException;
import static org.testng.Assert.*;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 *
 * @author pham21
 */
public class ObjectWriterNGTest
{
  
  public ObjectWriterNGTest()
  {
  }

  /**
   * Test of create method, of class ObjectWriter.
   */
  @Test
  public void testCreate() throws Exception
  {
    System.out.println("create");
//    Class<T> cls = null;
//    ObjectWriter expResult = null;
//    ObjectWriter result = ObjectWriter.create(cls);
//    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of attributes method, of class ObjectWriter.
   */
  @Test
  public void testAttributes() throws Exception
  {
    System.out.println("attributes");
    ObjectWriter.WriterAttributes attributes = null;
    Object object = null;
    ObjectWriter instance = null;
    instance.attributes(attributes, object);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of contents method, of class ObjectWriter.
   */
  @Test
  public void testContents() throws Exception
  {
    System.out.println("contents");
    Object object = null;
    ObjectWriter instance = null;
    instance.contents(object);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of addContents method, of class ObjectWriter.
   */
  @Test
  public void testAddContents() throws Exception
  {
    System.out.println("addContents");
    Object object = null;
    ObjectWriter instance = null;
    instance.addContents(object);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of newBuilder method, of class ObjectWriter.
   */
  @Test
  public void testNewBuilder()
  {
    System.out.println("newBuilder");
    ObjectWriter instance = null;
    ObjectWriter.WriterBuilder expResult = null;
    ObjectWriter.WriterBuilder result = instance.newBuilder();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of toXML method, of class ObjectWriter.
   */
  @Test
  public void testToXML() throws Exception
  {
    System.out.println("toXML");
    Object t = null;
    ObjectWriter instance = null;
    String expResult = "";
    String result = instance.toXML(t);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of convertToElement method, of class ObjectWriter.
   */
  @Test
  public void testConvertToElement() throws Exception
  {
    System.out.println("convertToElement");
    Document document = null;
    Object object = null;
    ObjectWriter instance = null;
    Element expResult = null;
    Element result = instance.convertToElement(document, object);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getElementName method, of class ObjectWriter.
   */
  @Test
  public void testGetElementName()
  {
    System.out.println("getElementName");
    ObjectWriter instance = null;
    String expResult = "";
    String result = instance.getElementName();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getOptions method, of class ObjectWriter.
   */
  @Test
  public void testGetOptions()
  {
    System.out.println("getOptions");
    ObjectWriter instance = null;
    int expResult = 0;
    int result = instance.getOptions();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getPackage method, of class ObjectWriter.
   */
  @Test
  public void testGetPackage()
  {
    System.out.println("getPackage");
    ObjectWriter instance = null;
    PackageResource expResult = null;
    PackageResource result = instance.getPackage();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getContext method, of class ObjectWriter.
   */
  @Test
  public void testGetContext() throws Exception
  {
    System.out.println("getContext");
    ObjectWriter instance = null;
    WriterContext expResult = null;
    WriterContext result = instance.getContext();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setContext method, of class ObjectWriter.
   */
  @Test
  public void testSetContext()
  {
    System.out.println("setContext");
    WriterContext context = null;
    ObjectWriter instance = null;
    instance.setContext(context);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

//  public class ObjectWriterImpl extends ObjectWriter
//  {
//    public ObjectWriterImpl()
//    {
//      super(0, "", null);
//    }
//
//    public void attributes(WriterAttributes attributes, Type object) throws WriterException
//    {
//    }
//
//    public void contents(Type object) throws WriterException
//    {
//    }
//  }
  
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