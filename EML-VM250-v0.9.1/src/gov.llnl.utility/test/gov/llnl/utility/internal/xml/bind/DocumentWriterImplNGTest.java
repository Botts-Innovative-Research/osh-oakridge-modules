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
import gov.llnl.utility.xml.bind.WriterContext;
import java.io.OutputStream;
import java.nio.file.Path;
import static org.testng.Assert.*;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.w3c.dom.Document;

/**
 *
 * @author pham21
 */
public class DocumentWriterImplNGTest
{
  
  public DocumentWriterImplNGTest()
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
   * Test of getContext method, of class DocumentWriterImpl.
   */
  @Test
  public void testGetContext()
  {
    System.out.println("getContext");
    DocumentWriterImpl instance = null;
    WriterContext expResult = null;
    WriterContext result = instance.getContext();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of saveFileName method, of class DocumentWriterImpl.
   */
  @Test
  public void testSaveFileName() throws Exception
  {
    System.out.println("saveFileName");
    String filename = "";
    Object object = null;
    DocumentWriterImpl instance = null;
    instance.saveFileName(filename, object);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of saveFile method, of class DocumentWriterImpl.
   */
  @Test
  public void testSaveFile() throws Exception
  {
    System.out.println("saveFile");
    Path path = null;
    Object object = null;
    DocumentWriterImpl instance = null;
    instance.saveFile(path, object);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of saveStream method, of class DocumentWriterImpl.
   */
  @Test
  public void testSaveStream() throws Exception
  {
    System.out.println("saveStream");
    OutputStream stream = null;
    Object object = null;
    DocumentWriterImpl instance = null;
    instance.saveStream(stream, object);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of createDocument method, of class DocumentWriterImpl.
   */
  @Test
  public void testCreateDocument()
  {
    System.out.println("createDocument");
    ObjectWriter writer = null;
    Document expResult = null;
    Document result = DocumentWriterImpl.createDocument(writer);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of toDocument method, of class DocumentWriterImpl.
   */
  @Test
  public void testToDocument() throws Exception
  {
    System.out.println("toDocument");
    Object object = null;
    DocumentWriterImpl instance = null;
    Document expResult = null;
    Document result = instance.toDocument(object);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of toXML method, of class DocumentWriterImpl.
   */
  @Test
  public void testToXML() throws Exception
  {
    System.out.println("toXML");
    Object object = null;
    DocumentWriterImpl instance = null;
    String expResult = "";
    String result = instance.toXML(object);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getObjectWriter method, of class DocumentWriterImpl.
   */
  @Test
  public void testGetObjectWriter()
  {
    System.out.println("getObjectWriter");
    DocumentWriterImpl instance = null;
    ObjectWriter expResult = null;
    ObjectWriter result = instance.getObjectWriter();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setProperty method, of class DocumentWriterImpl.
   */
  @Test
  public void testSetProperty()
  {
    System.out.println("setProperty");
    String key = "";
    Object value = null;
    DocumentWriterImpl instance = null;
    instance.setProperty(key, value);
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