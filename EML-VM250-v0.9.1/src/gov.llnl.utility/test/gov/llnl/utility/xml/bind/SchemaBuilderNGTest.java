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
import gov.llnl.utility.xml.DomBuilder;
import java.nio.file.Path;
import static org.testng.Assert.*;
import org.testng.annotations.Test;
import org.w3c.dom.Document;

/**
 *
 * @author pham21
 */
public class SchemaBuilderNGTest
{
  
  public SchemaBuilderNGTest()
  {
  }

  /**
   * Test of getXmlPrefix method, of class SchemaBuilder.
   */
  @Test
  public void testGetXmlPrefix()
  {
    System.out.println("getXmlPrefix");
    Reader reader = null;
    String expResult = "";
    String result = SchemaBuilder.getXmlPrefix(reader);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getRoot method, of class SchemaBuilder.
   */
  @Test
  public void testGetRoot()
  {
    System.out.println("getRoot");
    SchemaBuilder instance = new SchemaBuilder();
    DomBuilder expResult = null;
    DomBuilder result = instance.getRoot();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of addObjectReader method, of class SchemaBuilder.
   */
  @Test
  public void testAddObjectReader() throws Exception
  {
    System.out.println("addObjectReader");
    Reader reader = null;
    SchemaBuilder instance = new SchemaBuilder();
    instance.addObjectReader(reader);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of addObjectReaderForClass method, of class SchemaBuilder.
   */
  @Test
  public void testAddObjectReaderForClass() throws Exception
  {
    System.out.println("addObjectReaderForClass");
    Class cls = null;
    SchemaBuilder instance = new SchemaBuilder();
    instance.addObjectReaderForClass(cls);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of alias method, of class SchemaBuilder.
   */
  @Test
  public void testAlias() throws Exception
  {
    System.out.println("alias");
//    Class<T> cls = null;
//    String name = "";
//    SchemaBuilder instance = new SchemaBuilder();
//    instance.alias(cls, name);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of createReaderSchemaElement method, of class SchemaBuilder.
   */
  @Test
  public void testCreateReaderSchemaElement() throws Exception
  {
    System.out.println("createReaderSchemaElement");
    Reader reader = null;
    String name = "";
    SchemaBuilder instance = new SchemaBuilder();
    instance.createReaderSchemaElement(reader, name);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of createReaderSchemaType method, of class SchemaBuilder.
   */
  @Test
  public void testCreateReaderSchemaType() throws Exception
  {
    System.out.println("createReaderSchemaType");
    Reader reader = null;
    boolean recursive = false;
    SchemaBuilder instance = new SchemaBuilder();
    instance.createReaderSchemaType(reader, recursive);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getDocument method, of class SchemaBuilder.
   */
  @Test
  public void testGetDocument()
  {
    System.out.println("getDocument");
    SchemaBuilder instance = new SchemaBuilder();
    Document expResult = null;
    Document result = instance.getDocument();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of include method, of class SchemaBuilder.
   */
  @Test
  public void testInclude()
  {
    System.out.println("include");
    String file = "";
    SchemaBuilder instance = new SchemaBuilder();
    instance.include(file);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of imports method, of class SchemaBuilder.
   */
  @Test
  public void testImports()
  {
    System.out.println("imports");
    PackageResource resource = null;
    SchemaBuilder instance = new SchemaBuilder();
    instance.imports(resource);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of addNamespace method, of class SchemaBuilder.
   */
  @Test
  public void testAddNamespace()
  {
    System.out.println("addNamespace");
    PackageResource resource = null;
    SchemaBuilder instance = new SchemaBuilder();
    instance.addNamespace(resource);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setTargetNamespace method, of class SchemaBuilder.
   */
  @Test
  public void testSetTargetNamespace()
  {
    System.out.println("setTargetNamespace");
    PackageResource resource = null;
    SchemaBuilder instance = new SchemaBuilder();
    instance.setTargetNamespace(resource);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of scanForReaders method, of class SchemaBuilder.
   */
  @Test
  public void testScanForReaders_Path() throws Exception
  {
    System.out.println("scanForReaders");
    Path dir = null;
    SchemaBuilder instance = new SchemaBuilder();
    instance.scanForReaders(dir);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of scanForReaders method, of class SchemaBuilder.
   */
  @Test
  public void testScanForReaders_Path_String() throws Exception
  {
    System.out.println("scanForReaders");
    Path dir = null;
    String extension = "";
    SchemaBuilder instance = new SchemaBuilder();
    instance.scanForReaders(dir, extension);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getReaderContext method, of class SchemaBuilder.
   */
  @Test
  public void testGetReaderContext()
  {
    System.out.println("getReaderContext");
    Class cls = null;
    SchemaBuilder instance = new SchemaBuilder();
    ReaderContext expResult = null;
    ReaderContext result = instance.getReaderContext(cls);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getError method, of class SchemaBuilder.
   */
  @Test
  public void testGetError()
  {
    System.out.println("getError");
    SchemaBuilder instance = new SchemaBuilder();
    int expResult = 0;
    int result = instance.getError();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of notUsed method, of class SchemaBuilder.
   */
  @Test
  public void testNotUsed()
  {
    System.out.println("notUsed");
    Object expResult = null;
    Object result = SchemaBuilder.notUsed();
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