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

import gov.llnl.utility.TestSupport.TestElement;
import gov.llnl.utility.TestSupport.TestReader;
import gov.llnl.utility.xml.DomBuilder;
import gov.llnl.utility.xml.bind.AnyReader;
import gov.llnl.utility.xml.bind.DocumentReader;
import gov.llnl.utility.xml.bind.Reader;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import static org.testng.Assert.*;
import org.testng.annotations.Test;

/**
 *
 * @author pham21
 */
public class ImportsReaderNGTest
{

  public ImportsReaderNGTest()
  {
  }

  /**
   * Test of ImportsReader constructor, of class ImportsReader.
   */
  @Test
  public void testConstructor()
  {
    System.out.println("ImportsReader constructor");
    TestReader testReader = TestReader.of(String.class);
    ImportsReader instance = new ImportsReader(testReader);
    assertSame(instance.objectReader, testReader);
    assertNotNull(instance.documentReader);
    assertSame(instance.documentReader.objectReader, testReader);
    assertNull(instance.localContext);
  }

  /**
   * Test of getDeclaration method, of class ImportsReader.
   */
  @Test
  public void testGetDeclaration()
  {
    System.out.println("getDeclaration");
    TestReader testReader = TestReader.of(String.class);
    ImportsReader instance = new ImportsReader(testReader);

    Reader.Declaration dec = instance.getDeclaration();
    assertNotNull(dec);
    assertSame(dec.pkg(), testReader.getDeclaration().pkg());
    assertEquals(dec.name(), testReader.getDeclaration().name());
    assertEquals(dec.order(), Reader.Order.FREE);
  }

  /**
   * Test of start method, of class ImportsReader.
   */
  @Test
  public void testStart() throws Exception
  {
    System.out.println("start");
    File currentDir = new File("../../");
    org.xml.sax.helpers.AttributesImpl attributes = new org.xml.sax.helpers.AttributesImpl();
    attributes.addAttribute("extern", "extern", "extern", "extern", currentDir.getCanonicalPath().replace("\\", "/") + "/config/rpm8RecordDatabase.xml");
    AnyReader anyReader = AnyReader.of(Integer.class);
    ReaderContextImpl rci = new ReaderContextImpl();
    DocumentReaderImpl documentReader = new DocumentReaderImpl(anyReader);
    documentReader.setProperty(DocumentReader.SEARCH_PATHS, new Path[]
    {
      Paths.get("."),
      Paths.get("../../config/"),
      Paths.get("../../src/")

    });
    rci.setDocumentReader(documentReader);

    File file = new File(currentDir.getCanonicalPath() + "/config/rpm8RecordDatabase.xml");
    rci.setFile(file.toURI());
    ImportsReader instance = new ImportsReader(anyReader);
    instance.setContext(rci);

    // FIXME Need to figure out how start works so we can test correctly.
    instance.start(attributes);

  }

  /**
   * Test of getObjectClass method, of class ImportsReader.
   */
  @Test
  public void testGetObjectClass()
  {
    System.out.println("getObjectClass");
    TestReader testReader = TestReader.of(String.class);
    ImportsReader instance = new ImportsReader(testReader);
    assertSame(instance.getObjectClass(), String.class);
    assertSame(instance.getObjectClass(), testReader.getObjectClass());
  }

  /**
   * Test of getHandlers method, of class ImportsReader.
   */
  @Test
  public void testGetHandlers() throws Exception
  {
    System.out.println("getHandlers");
    TestReader testReader = TestReader.of(String.class);
    ImportsReader instance = new ImportsReader(testReader);
    assertNotNull(instance.getHandlers());
  }

  /**
   * Test of createSchemaType method, of class ImportsReader.
   */
  @Test
  public void testCreateSchemaType() throws Exception
  {
    System.out.println("createSchemaType");
    ImportsReader instance = new ImportsReader(TestReader.of(String.class));
    // Empty method body
    instance.createSchemaType(null);
  }

  /**
   * Test of createSchemaElement method, of class ImportsReader.
   */
  @Test
  public void testCreateSchemaElement() throws Exception
  {
    System.out.println("createSchemaElement");
    DomBuilder type = new DomBuilder(new TestElement(""));
    ImportsReader instance = new ImportsReader(TestReader.of(String.class));
    assertSame(instance.createSchemaElement(null, null, type, true), type);
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