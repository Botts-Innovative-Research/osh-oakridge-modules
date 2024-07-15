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

import gov.llnl.utility.PackageResource;
import static org.testng.Assert.*;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.w3c.dom.Element;

/**
 *
 * @author pham21
 */
public class DomBuilderNGTest
{
  
  public DomBuilderNGTest()
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
   * Test of attr method, of class DomBuilder.
   */
  @Test
  public void testAttr()
  {
    System.out.println("attr");
    String name = "";
    String value = "";
    DomBuilder instance = null;
    DomBuilder expResult = null;
    DomBuilder result = instance.attr(name, value);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of attrNS method, of class DomBuilder.
   */
  @Test
  public void testAttrNS()
  {
    System.out.println("attrNS");
    String namespaceURI = "";
    String qualifiedName = "";
    String value = "";
    DomBuilder instance = null;
    instance.attrNS(namespaceURI, qualifiedName, value);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of elementNS method, of class DomBuilder.
   */
  @Test
  public void testElementNS()
  {
    System.out.println("elementNS");
    PackageResource pkg = null;
    String name = "";
    DomBuilder instance = null;
    DomBuilder expResult = null;
    DomBuilder result = instance.elementNS(pkg, name);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of element method, of class DomBuilder.
   */
  @Test
  public void testElement_String()
  {
    System.out.println("element");
    String name = "";
    DomBuilder instance = null;
    DomBuilder expResult = null;
    DomBuilder result = instance.element(name);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of element method, of class DomBuilder.
   */
  @Test
  public void testElement_String_boolean()
  {
    System.out.println("element");
    String name = "";
    boolean first = false;
    DomBuilder instance = null;
    DomBuilder expResult = null;
    DomBuilder result = instance.element(name, first);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of comment method, of class DomBuilder.
   */
  @Test
  public void testComment()
  {
    System.out.println("comment");
    String string = "";
    DomBuilder instance = null;
    DomBuilder expResult = null;
    DomBuilder result = instance.comment(string);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of text method, of class DomBuilder.
   */
  @Test
  public void testText()
  {
    System.out.println("text");
    String value = "";
    DomBuilder instance = null;
    DomBuilder expResult = null;
    DomBuilder result = instance.text(value);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of toElement method, of class DomBuilder.
   */
  @Test
  public void testToElement()
  {
    System.out.println("toElement");
    DomBuilder instance = null;
    Element expResult = null;
    Element result = instance.toElement();
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