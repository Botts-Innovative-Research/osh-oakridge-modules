/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.utility.xml;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import javax.xml.namespace.NamespaceContext;
import static org.testng.Assert.*;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 *
 * @author pham21
 */
public class DomUtilitiesNGTest
{
  
  public DomUtilitiesNGTest()
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
   * Test of findFirst method, of class DomUtilities.
   */
  @Test
  public void testFindFirst()
  {
    System.out.println("findFirst");
    Element origin = null;
    String path = "";
    Element expResult = null;
    Element result = DomUtilities.findFirst(origin, path);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of findAll method, of class DomUtilities.
   */
  @Test
  public void testFindAll()
  {
    System.out.println("findAll");
    Element origin = null;
    String path = "";
    DomUtilities.NodeListElementIterable expResult = null;
    DomUtilities.NodeListElementIterable result = DomUtilities.findAll(origin, path);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of removeWhitespace method, of class DomUtilities.
   */
  @Test
  public void testRemoveWhitespace()
  {
    System.out.println("removeWhitespace");
    Document document = null;
    DomUtilities.removeWhitespace(document);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of printXml method, of class DomUtilities.
   */
  @Test
  public void testPrintXml()
  {
    System.out.println("printXml");
    OutputStream out = null;
    Node document = null;
    DomUtilities.printXml(out, document);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of newPrinter method, of class DomUtilities.
   */
  @Test
  public void testNewPrinter()
  {
    System.out.println("newPrinter");
    OutputStream out = null;
    DomUtilities.XMLPrinter expResult = null;
    DomUtilities.XMLPrinter result = DomUtilities.newPrinter(out);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of appendElement method, of class DomUtilities.
   */
  @Test
  public void testAppendElement_Element_String()
  {
    System.out.println("appendElement");
    Element parent = null;
    String name = "";
    Element expResult = null;
    Element result = DomUtilities.appendElement(parent, name);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of appendElement method, of class DomUtilities.
   */
  @Test
  public void testAppendElement_3args_1()
  {
    System.out.println("appendElement");
    Element parent = null;
    String name = "";
    String value = "";
    Element expResult = null;
    Element result = DomUtilities.appendElement(parent, name, value);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of appendElement method, of class DomUtilities.
   */
  @Test
  public void testAppendElement_3args_2()
  {
    System.out.println("appendElement");
    Element parent = null;
    String name = "";
    int value = 0;
    Element expResult = null;
    Element result = DomUtilities.appendElement(parent, name, value);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of appendElement method, of class DomUtilities.
   */
  @Test
  public void testAppendElement_3args_3()
  {
    System.out.println("appendElement");
    Element parent = null;
    String name = "";
    double value = 0.0;
    Element expResult = null;
    Element result = DomUtilities.appendElement(parent, name, value);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getXmlPath method, of class DomUtilities.
   */
  @Test
  public void testGetXmlPath()
  {
    System.out.println("getXmlPath");
    Node e = null;
    String expResult = "";
    String result = DomUtilities.getXmlPath(e);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of parseStreamToDOM method, of class DomUtilities.
   */
  @Test
  public void testParseStreamToDOM() throws Exception
  {
    System.out.println("parseStreamToDOM");
    InputStream inputStream = null;
    Document expResult = null;
    Document result = DomUtilities.parseStreamToDOM(inputStream);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of parseFileToDOM method, of class DomUtilities.
   */
  @Test
  public void testParseFileToDOM() throws Exception
  {
    System.out.println("parseFileToDOM");
    Path file = null;
    Document expResult = null;
    Document result = DomUtilities.parseFileToDOM(file);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of toFloat method, of class DomUtilities.
   */
  @Test
  public void testToFloat()
  {
    System.out.println("toFloat");
    Element e = null;
    float expResult = 0.0F;
    float result = DomUtilities.toFloat(e);
    assertEquals(result, expResult, 0.0);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of toDouble method, of class DomUtilities.
   */
  @Test
  public void testToDouble()
  {
    System.out.println("toDouble");
    Element e = null;
    double expResult = 0.0;
    double result = DomUtilities.toDouble(e);
    assertEquals(result, expResult, 0.0);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of toInteger method, of class DomUtilities.
   */
  @Test
  public void testToInteger()
  {
    System.out.println("toInteger");
    Element e = null;
    int expResult = 0;
    int result = DomUtilities.toInteger(e);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of toIntegerAttribute method, of class DomUtilities.
   */
  @Test
  public void testToIntegerAttribute()
  {
    System.out.println("toIntegerAttribute");
    Element element = null;
    String attribute = "";
    int defaultValue = 0;
    int expResult = 0;
    int result = DomUtilities.toIntegerAttribute(element, attribute, defaultValue);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of toBoolean method, of class DomUtilities.
   */
  @Test
  public void testToBoolean()
  {
    System.out.println("toBoolean");
    Element e = null;
    boolean expResult = false;
    boolean result = DomUtilities.toBoolean(e);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of toDoubleAttribute method, of class DomUtilities.
   */
  @Test
  public void testToDoubleAttribute()
  {
    System.out.println("toDoubleAttribute");
    Element element = null;
    String attribute = "";
    double defaultValue = 0.0;
    double expResult = 0.0;
    double result = DomUtilities.toDoubleAttribute(element, attribute, defaultValue);
    assertEquals(result, expResult, 0.0);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of createNamespaceContext method, of class DomUtilities.
   */
  @Test
  public void testCreateNamespaceContext()
  {
    System.out.println("createNamespaceContext");
    Document doc = null;
    NamespaceContext expResult = null;
    NamespaceContext result = DomUtilities.createNamespaceContext(doc);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of trimWhitespace method, of class DomUtilities.
   */
  @Test
  public void testTrimWhitespace()
  {
    System.out.println("trimWhitespace");
    Node node = null;
    DomUtilities.trimWhitespace(node);
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