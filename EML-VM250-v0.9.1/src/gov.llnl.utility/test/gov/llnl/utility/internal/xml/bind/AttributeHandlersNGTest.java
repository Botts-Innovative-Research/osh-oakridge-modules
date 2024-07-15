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
import gov.llnl.utility.xml.bind.ReaderContext;
import java.lang.reflect.Method;
import static org.testng.Assert.*;
import org.testng.annotations.Test;
import org.xml.sax.Attributes;

/**
 *
 * @author pham21
 */
public class AttributeHandlersNGTest
{
  
  public AttributeHandlersNGTest()
  {
  }

  /**
   * Test of create method, of class AttributeHandlers.
   */
  @Test
  public void testCreate()
  {
    System.out.println("create");
//    Class<Type> cls = null;
//    AttributeHandlers expResult = null;
//    AttributeHandlers result = AttributeHandlers.create(cls);
//    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of applyAttributes method, of class AttributeHandlers.
   */
  @Test
  public void testApplyAttributes() throws Exception
  {
    System.out.println("applyAttributes");
    ReaderContext rc = null;
    Object obj = null;
    Attributes attributes = null;
    AttributeHandlers instance = null;
    instance.applyAttributes(rc, obj, attributes);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of createSchemaType method, of class AttributeHandlers.
   */
  @Test
  public void testCreateSchemaType()
  {
    System.out.println("createSchemaType");
    DomBuilder type = null;
    AttributeHandlers instance = null;
    instance.createSchemaType(type);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of declareAttribute method, of class AttributeHandlers.
   */
  @Test
  public void testDeclareAttribute()
  {
    System.out.println("declareAttribute");
    DomBuilder type = null;
    Reader.Attribute attr = null;
    Method method = null;
    AttributeHandlers.declareAttribute(type, attr, method);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getAttributeName method, of class AttributeHandlers.
   */
  @Test
  public void testGetAttributeName()
  {
    System.out.println("getAttributeName");
    Reader.Attribute attr = null;
    Method method = null;
    String expResult = "";
    String result = AttributeHandlers.getAttributeName(attr, method);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getAttributeType method, of class AttributeHandlers.
   */
  @Test
  public void testGetAttributeType()
  {
    System.out.println("getAttributeType");
    Reader.Attribute attr = null;
    Method method = null;
    String expResult = "";
    String result = AttributeHandlers.getAttributeType(attr, method);
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