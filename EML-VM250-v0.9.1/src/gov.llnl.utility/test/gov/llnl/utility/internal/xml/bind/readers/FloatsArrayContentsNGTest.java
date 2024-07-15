/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.utility.internal.xml.bind.readers;

import gov.llnl.utility.xml.DomBuilder;
import gov.llnl.utility.xml.bind.SchemaBuilder;
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
public class FloatsArrayContentsNGTest
{
  
  public FloatsArrayContentsNGTest()
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
   * Test of createSchemaType method, of class FloatsArrayContents.
   */
  @Test
  public void testCreateSchemaType() throws Exception
  {
    System.out.println("createSchemaType");
    SchemaBuilder builder = null;
    FloatsArrayContents instance = new FloatsArrayContents();
    instance.createSchemaType(builder);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of createSchemaElement method, of class FloatsArrayContents.
   */
  @Test
  public void testCreateSchemaElement()
  {
    System.out.println("createSchemaElement");
    SchemaBuilder builder = null;
    String name = "";
    DomBuilder group = null;
    boolean options = false;
    FloatsArrayContents instance = new FloatsArrayContents();
    DomBuilder expResult = null;
    DomBuilder result = instance.createSchemaElement(builder, name, group, options);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getObjectClass method, of class FloatsArrayContents.
   */
  @Test
  public void testGetObjectClass()
  {
    System.out.println("getObjectClass");
    FloatsArrayContents instance = new FloatsArrayContents();
    Class expResult = null;
    Class result = instance.getObjectClass();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of contents method, of class FloatsArrayContents.
   */
  @Test
  public void testContents() throws Exception
  {
    System.out.println("contents");
    String textContents = "";
    FloatsArrayContents instance = new FloatsArrayContents();
    float[][] expResult = null;
    float[][] result = instance.contents(textContents);
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