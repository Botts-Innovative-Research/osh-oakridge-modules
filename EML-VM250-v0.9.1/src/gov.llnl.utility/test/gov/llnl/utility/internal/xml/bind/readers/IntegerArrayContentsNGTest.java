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


import gov.llnl.utility.ArrayEncoding;
import gov.llnl.utility.TestSupport.TestElement;
import gov.llnl.utility.io.ReaderException;
import gov.llnl.utility.xml.DomBuilder;
import gov.llnl.utility.xml.bind.SchemaBuilder;
import static org.testng.Assert.*;
import org.testng.annotations.Test;

/**
 *
 * @author pham21
 */
public class IntegerArrayContentsNGTest
{

  public IntegerArrayContentsNGTest()
  {
  }

  /**
   * Test of createSchemaType method, of class IntegerArrayContents.
   */
  @Test
  public void testCreateSchemaType() throws Exception
  {
    System.out.println("createSchemaType");
    SchemaBuilder builder = null;
    IntegerArrayContents instance = new IntegerArrayContents();
    instance.createSchemaType(builder);
  }

  /**
   * Test of createSchemaElement method, of class IntegerArrayContents.
   */
  @Test
  public void testCreateSchemaElement()
  {
    System.out.println("createSchemaElement");
    IntegerArrayContents iac = new IntegerArrayContents();
    TestElement first = new TestElement("First");
    DomBuilder domBuilder = new DomBuilder(first);

    DomBuilder tooDomTooBuildrious = iac.createSchemaElement(null, "Toretto&O'Conner", domBuilder, false);
    assertEquals(tooDomTooBuildrious.toElement(), domBuilder.toElement().getFirstChild());

    TestElement second = (TestElement) tooDomTooBuildrious.toElement();
    assertEquals(second.getTagName(), "xs:element");
    assertEquals(second.attrMap.get("name"), "Toretto&O'Conner");
    assertEquals(second.attrMap.get("type"), "util:string-attr");
  }

  /**
   * Test of getObjectClass method, of class IntegerArrayContents.
   */
  @Test
  public void testGetObjectClass()
  {
    System.out.println("getObjectClass");
    IntegerArrayContents instance = new IntegerArrayContents();
    assertEquals(instance.getObjectClass(), int[].class);
  }

  /**
   * Test of contents method, of class IntegerArrayContents.
   */
  @Test(expectedExceptions =
  {
    ReaderException.class
  })
  public void testContents() throws Exception
  {
    System.out.println("contents");
    int[] intArray = new int[]
    {
      1, 1, 5, 5, 6, 6, 5, 4, 4, 3, 3, 2, 2, 1
    };
    String textContents = ArrayEncoding.encodeIntegers(intArray);
    IntegerArrayContents instance = new IntegerArrayContents();
    assertEquals(instance.contents(textContents), intArray);

    // Test ReaderException
    instance.contents("ccggaag");
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