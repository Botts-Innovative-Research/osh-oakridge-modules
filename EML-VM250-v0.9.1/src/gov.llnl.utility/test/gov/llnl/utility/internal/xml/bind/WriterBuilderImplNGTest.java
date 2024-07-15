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

import gov.llnl.utility.xml.bind.ObjectWriter;
import static org.testng.Assert.*;
import org.testng.annotations.Test;

/**
 *
 * @author pham21
 */
public class WriterBuilderImplNGTest
{
  
  public WriterBuilderImplNGTest()
  {
  }

  /**
   * Test of element method, of class WriterBuilderImpl.
   */
  @Test
  public void testElement()
  {
    System.out.println("element");
    String name = "";
    WriterBuilderImpl instance = null;
    WriterBuilderImpl expResult = null;
    WriterBuilderImpl result = instance.element(name);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of writer method, of class WriterBuilderImpl.
   */
  @Test
  public void testWriter()
  {
    System.out.println("writer");
//    ObjectWriter<Type> writer = null;
//    WriterBuilderImpl instance = null;
//    WriterBuilderImpl expResult = null;
//    WriterBuilderImpl result = instance.writer(writer);
//    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of contents method, of class WriterBuilderImpl.
   */
  @Test
  public void testContents() throws Exception
  {
    System.out.println("contents");
//    Class<? extends Type> cls = null;
//    WriterBuilderImpl instance = null;
//    WriterBuilderImpl expResult = null;
//    WriterBuilderImpl result = instance.contents(cls);
//    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of comment method, of class WriterBuilderImpl.
   */
  @Test
  public void testComment() throws Exception
  {
    System.out.println("comment");
    String str = "";
    WriterBuilderImpl instance = null;
    instance.comment(str);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of put method, of class WriterBuilderImpl.
   */
  @Test
  public void testPut_Object() throws Exception
  {
    System.out.println("put");
    Object object = null;
    WriterBuilderImpl instance = null;
    WriterBuilderImpl expResult = null;
    WriterBuilderImpl result = instance.put(object);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of section method, of class WriterBuilderImpl.
   */
  @Test
  public void testSection() throws Exception
  {
    System.out.println("section");
    ObjectWriter.Section writer = null;
    WriterBuilderImpl instance = null;
    instance.section(writer);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of id method, of class WriterBuilderImpl.
   */
  @Test
  public void testId() throws Exception
  {
    System.out.println("id");
    String id = "";
    WriterBuilderImpl instance = null;
    WriterBuilderImpl expResult = null;
    WriterBuilderImpl result = instance.id(id);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of attr method, of class WriterBuilderImpl.
   */
  @Test
  public void testAttr() throws Exception
  {
    System.out.println("attr");
    String key = "";
    Object value = null;
    WriterBuilderImpl instance = null;
    WriterBuilderImpl expResult = null;
    WriterBuilderImpl result = instance.attr(key, value);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of putContents method, of class WriterBuilderImpl.
   */
  @Test
  public void testPutContents() throws Exception
  {
    System.out.println("putContents");
    Object value = null;
    WriterBuilderImpl instance = null;
    WriterBuilderImpl expResult = null;
    WriterBuilderImpl result = instance.putContents(value);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of putList method, of class WriterBuilderImpl.
   */
  @Test
  public void testPutList() throws Exception
  {
    System.out.println("putList");
//    Iterable<Type> value = null;
//    ObjectWriter<Type> writer = null;
//    WriterBuilderImpl instance = null;
//    WriterBuilderImpl expResult = null;
//    WriterBuilderImpl result = instance.putList(value, writer);
//    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of reference method, of class WriterBuilderImpl.
   */
  @Test
  public void testReference() throws Exception
  {
    System.out.println("reference");
    String id = "";
    WriterBuilderImpl instance = null;
    instance.reference(id);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of put method, of class WriterBuilderImpl.
   */
  @Test
  public void testPut_0args() throws Exception
  {
    System.out.println("put");
    WriterBuilderImpl instance = null;
    WriterBuilderImpl expResult = null;
    WriterBuilderImpl result = instance.put();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of putString method, of class WriterBuilderImpl.
   */
  @Test
  public void testPutString() throws Exception
  {
    System.out.println("putString");
    String value = "";
    WriterBuilderImpl instance = null;
    WriterBuilderImpl expResult = null;
    WriterBuilderImpl result = instance.putString(value);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of putInteger method, of class WriterBuilderImpl.
   */
  @Test
  public void testPutInteger() throws Exception
  {
    System.out.println("putInteger");
    int value = 0;
    WriterBuilderImpl instance = null;
    WriterBuilderImpl expResult = null;
    WriterBuilderImpl result = instance.putInteger(value);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of putLong method, of class WriterBuilderImpl.
   */
  @Test
  public void testPutLong() throws Exception
  {
    System.out.println("putLong");
    long value = 0L;
    WriterBuilderImpl instance = null;
    WriterBuilderImpl expResult = null;
    WriterBuilderImpl result = instance.putLong(value);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of putDouble method, of class WriterBuilderImpl.
   */
  @Test
  public void testPutDouble_double() throws Exception
  {
    System.out.println("putDouble");
    double value = 0.0;
    WriterBuilderImpl instance = null;
    WriterBuilderImpl expResult = null;
    WriterBuilderImpl result = instance.putDouble(value);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of putDouble method, of class WriterBuilderImpl.
   */
  @Test
  public void testPutDouble_double_String() throws Exception
  {
    System.out.println("putDouble");
    double value = 0.0;
    String format = "";
    WriterBuilderImpl instance = null;
    WriterBuilderImpl expResult = null;
    WriterBuilderImpl result = instance.putDouble(value, format);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of putBoolean method, of class WriterBuilderImpl.
   */
  @Test
  public void testPutBoolean() throws Exception
  {
    System.out.println("putBoolean");
    boolean value = false;
    WriterBuilderImpl instance = null;
    WriterBuilderImpl expResult = null;
    WriterBuilderImpl result = instance.putBoolean(value);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of putFlag method, of class WriterBuilderImpl.
   */
  @Test
  public void testPutFlag() throws Exception
  {
    System.out.println("putFlag");
    boolean value = false;
    WriterBuilderImpl instance = null;
    WriterBuilderImpl expResult = null;
    WriterBuilderImpl result = instance.putFlag(value);
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