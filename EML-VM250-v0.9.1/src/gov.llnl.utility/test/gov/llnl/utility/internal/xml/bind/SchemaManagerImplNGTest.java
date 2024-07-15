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
import gov.llnl.utility.xml.bind.SchemaManager;
import java.net.URI;
import java.net.URL;
import static org.testng.Assert.*;
import org.testng.annotations.Test;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

/**
 *
 * @author pham21
 */
public class SchemaManagerImplNGTest
{
  
  public SchemaManagerImplNGTest()
  {
  }

  /**
   * Test of mangleURI method, of class SchemaManagerImpl.
   */
  @Test
  public void testMangleURI()
  {
    System.out.println("mangleURI");
    URI uri = null;
    SchemaManagerImpl instance = new SchemaManagerImpl();
    URL expResult = null;
    URL result = instance.mangleURI(uri);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getObjectClass method, of class SchemaManagerImpl.
   */
  @Test
  public void testGetObjectClass() throws Exception
  {
    System.out.println("getObjectClass");
    String namespaceURI = "";
    String name = "";
    SchemaManagerImpl instance = new SchemaManagerImpl();
    Class expResult = null;
    Class result = instance.getObjectClass(namespaceURI, name);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of resolveEntity method, of class SchemaManagerImpl.
   */
  @Test
  public void testResolveEntity() throws Exception
  {
    System.out.println("resolveEntity");
    String publicId = "";
    String systemId = "";
    SchemaManagerImpl instance = new SchemaManagerImpl();
    InputSource expResult = null;
    InputSource result = instance.resolveEntity(publicId, systemId);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of alias method, of class SchemaManagerImpl.
   */
  @Test
  public void testAlias()
  {
    System.out.println("alias");
    URI systemId = null;
    URL location = null;
    SchemaManagerImpl instance = new SchemaManagerImpl();
    instance.alias(systemId, location);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of findObjectWriter method, of class SchemaManagerImpl.
   */
  @Test
  public void testFindObjectWriter() throws Exception
  {
    System.out.println("findObjectWriter");
//    WriterContext context = null;
//    Class<T> cls = null;
//    SchemaManagerImpl instance = new SchemaManagerImpl();
//    ObjectWriter expResult = null;
//    ObjectWriter result = instance.findObjectWriter(context, cls);
//    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of findObjectReader method, of class SchemaManagerImpl.
   */
  @Test
  public void testFindObjectReader() throws Exception
  {
    System.out.println("findObjectReader");
//    Class<T> cls = null;
//    SchemaManagerImpl instance = new SchemaManagerImpl();
//    ObjectReader expResult = null;
//    ObjectReader result = instance.findObjectReader(cls);
//    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getEntityResolver method, of class SchemaManagerImpl.
   */
  @Test
  public void testGetEntityResolver()
  {
    System.out.println("getEntityResolver");
    SchemaManagerImpl instance = new SchemaManagerImpl();
    EntityResolver expResult = null;
    EntityResolver result = instance.getEntityResolver();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of registerReaderFactory method, of class SchemaManagerImpl.
   */
  @Test
  public void testRegisterReaderFactory()
  {
    System.out.println("registerReaderFactory");
    SchemaManager.ReaderFactory factory = null;
    SchemaManagerImpl instance = new SchemaManagerImpl();
    instance.registerReaderFactory(factory);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of registerReaderFor method, of class SchemaManagerImpl.
   */
  @Test
  public void testRegisterReaderFor()
  {
    System.out.println("registerReaderFor");
    Class cls = null;
    Class readerCls = null;
    SchemaManagerImpl instance = new SchemaManagerImpl();
    instance.registerReaderFor(cls, readerCls);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of registerWriterFactory method, of class SchemaManagerImpl.
   */
  @Test
  public void testRegisterWriterFactory()
  {
    System.out.println("registerWriterFactory");
    SchemaManager.WriterFactory factory = null;
    SchemaManagerImpl instance = new SchemaManagerImpl();
    instance.registerWriterFactory(factory);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of registerWriterFor method, of class SchemaManagerImpl.
   */
  @Test
  public void testRegisterWriterFor()
  {
    System.out.println("registerWriterFor");
    Class cls = null;
    Class<? extends ObjectWriter> writerCls = null;
    SchemaManagerImpl instance = new SchemaManagerImpl();
    instance.registerWriterFor(cls, writerCls);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of scanSchema method, of class SchemaManagerImpl.
   */
  @Test
  public void testScanSchema()
  {
    System.out.println("scanSchema");
    URL url = null;
    SchemaManagerImpl instance = new SchemaManagerImpl();
    instance.scanSchema(url);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of processSchemaLocation method, of class SchemaManagerImpl.
   */
  @Test
  public void testProcessSchemaLocation()
  {
    System.out.println("processSchemaLocation");
    String includedSchema = "";
    SchemaManagerImpl instance = new SchemaManagerImpl();
    instance.processSchemaLocation(includedSchema);
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