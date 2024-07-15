/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.utility;

import java.io.Serializable;
import java.nio.file.Path;
import static org.testng.Assert.*;
import org.testng.annotations.Test;

/**
 *
 * @author pham21
 */
public class SerializerNGTest
{
  
  public SerializerNGTest()
  {
  }

  /**
   * Test of renameClass method, of class Serializer.
   */
  @Test
  public void testRenameClass()
  {
    System.out.println("renameClass");
    String revised = "";
    String original = "";
    Serializer instance = new Serializer();
    instance.renameClass(revised, original);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setIgnoreVersionUID method, of class Serializer.
   */
  @Test
  public void testSetIgnoreVersionUID_String()
  {
    System.out.println("setIgnoreVersionUID");
    String cls = "";
    Serializer instance = new Serializer();
    instance.setIgnoreVersionUID(cls);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setIgnoreVersionUID method, of class Serializer.
   */
  @Test
  public void testSetIgnoreVersionUID_0args()
  {
    System.out.println("setIgnoreVersionUID");
    Serializer instance = new Serializer();
    instance.setIgnoreVersionUID();
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setCompress method, of class Serializer.
   */
  @Test
  public void testSetCompress()
  {
    System.out.println("setCompress");
    boolean compress = false;
    Serializer instance = new Serializer();
    instance.setCompress(compress);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of load method, of class Serializer.
   */
  @Test
  public void testLoad() throws Exception
  {
    System.out.println("load");
    Path file = null;
    Serializer instance = new Serializer();
    Serializable expResult = null;
    Serializable result = instance.load(file);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of save method, of class Serializer.
   */
  @Test
  public void testSave() throws Exception
  {
    System.out.println("save");
    Path file = null;
    Serializable object = null;
    Serializer instance = new Serializer();
    instance.save(file, object);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of convert method, of class Serializer.
   */
  @Test
  public void testConvert() throws Exception
  {
    System.out.println("convert");
    Path file = null;
    Serializer instance = new Serializer();
    instance.convert(file);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of pack method, of class Serializer.
   */
  @Test
  public void testPack() throws Exception
  {
    System.out.println("pack");
    Serializable object = null;
    Serializer instance = new Serializer();
    byte[] expResult = null;
    byte[] result = instance.pack(object);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of unpack method, of class Serializer.
   */
  @Test
  public void testUnpack() throws Exception
  {
    System.out.println("unpack");
    byte[] buffer = null;
    Serializer instance = new Serializer();
    Serializable expResult = null;
    Serializable result = instance.unpack(buffer);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of encode method, of class Serializer.
   */
  @Test
  public void testEncode() throws Exception
  {
    System.out.println("encode");
    Serializable object = null;
    String expResult = "";
    String result = Serializer.encode(object);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of decode method, of class Serializer.
   */
  @Test
  public void testDecode() throws Exception
  {
    System.out.println("decode");
    String str = "";
    Serializable expResult = null;
    Serializable result = Serializer.decode(str);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of copy method, of class Serializer.
   */
  @Test
  public void testCopy() throws Exception
  {
    System.out.println("copy");
//    Object object = null;
//    Object expResult = null;
//    Object result = Serializer.copy(object);
//    assertEquals(result, expResult);
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