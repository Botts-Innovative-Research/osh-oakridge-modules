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
import static org.testng.Assert.*;
import org.testng.annotations.Test;

/**
 *
 * @author pham21
 */
public class DoubleArrayContentsNGTest
{

  public DoubleArrayContentsNGTest()
  {
  }

  /**
   * Test of contents method, of class DoubleArrayContents.
   */
  @Test(expectedExceptions =
  {
    ReaderException.class
  })
  public void testContents() throws Exception
  {
    System.out.println("contents");
    double[] doubleArray = new double[]
    {
      1D, 1D, 5D, 5D, 6D, 6D, 5D, 4D, 4D, 3D, 3D, 2D, 2D, 1D
    };
    String textContents = ArrayEncoding.encodeDoubles(doubleArray);
    DoubleArrayContents instance = new DoubleArrayContents();
    assertEquals(instance.contents(textContents), doubleArray);

    // Test ReaderException
    instance.contents("ccggaag");
  }

  /**
   * Test of getObjectClass method, of class DoubleArrayContents.
   */
  @Test
  public void testGetObjectClass()
  {
    System.out.println("getObjectClass");
    assertEquals(new DoubleArrayContents().getObjectClass(), double[].class);
  }

  /**
   * Test of createSchemaType method, of class DoubleArrayContents.
   */
  @Test
  public void testCreateSchemaType() throws Exception
  {
    System.out.println("createSchemaType");
    new DoubleArrayContents().createSchemaType(null);
  }

  /**
   * Test of createSchemaElement method, of class DoubleArrayContents.
   */
  @Test
  public void testCreateSchemaElement()
  {
    System.out.println("createSchemaElement");
    DoubleArrayContents dac = new DoubleArrayContents();
    TestElement first = new TestElement("First");
    DomBuilder domBuilder = new DomBuilder(first);

    DomBuilder tooDomTooBuildrious = dac.createSchemaElement(null, "Toretto&O'Conner", domBuilder, false);
    assertEquals(tooDomTooBuildrious.toElement(), domBuilder.toElement().getFirstChild());

    TestElement second = (TestElement) tooDomTooBuildrious.toElement();
    assertEquals(second.getTagName(), "xs:element");
    assertEquals(second.attrMap.get("name"), "Toretto&O'Conner");
    assertEquals(second.attrMap.get("type"), "util:string-attr");
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