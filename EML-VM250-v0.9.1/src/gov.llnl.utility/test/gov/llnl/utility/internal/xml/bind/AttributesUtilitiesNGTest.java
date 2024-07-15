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

import gov.llnl.utility.xml.DomBuilder;
import gov.llnl.utility.xml.bind.Reader;
import static org.testng.Assert.*;
import org.testng.annotations.Test;

/**
 *
 * @author pham21
 */
public class AttributesUtilitiesNGTest
{
  
  public AttributesUtilitiesNGTest()
  {
  }

  /**
   * Test of createSchemaType method, of class AttributesUtilities.
   */
  @Test
  public void testCreateSchemaType()
  {
    System.out.println("createSchemaType");
    DomBuilder type = null;
    Reader.Attribute[] attributes = null;
    Reader.AnyAttribute anyattribute = null;
    AttributesUtilities.createSchemaType(type, attributes, anyattribute);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of declareAttribute method, of class AttributesUtilities.
   */
  @Test
  public void testDeclareAttribute()
  {
    System.out.println("declareAttribute");
    DomBuilder type = null;
    Reader.Attribute ad = null;
    AttributesUtilities.declareAttribute(type, ad);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getAttributeType method, of class AttributesUtilities.
   */
  @Test
  public void testGetAttributeType()
  {
    System.out.println("getAttributeType");
    Class<?> parameterType = null;
    String expResult = "";
    String result = AttributesUtilities.getAttributeType(parameterType);
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