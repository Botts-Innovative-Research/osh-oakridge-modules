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

import gov.llnl.utility.TestSupport;
import gov.llnl.utility.TestSupport.TestReaderContext;
import gov.llnl.utility.TestSupport.TestSection;
import gov.llnl.utility.xml.DomBuilder;
import gov.llnl.utility.xml.bind.Reader;
import gov.llnl.utility.xml.bind.SchemaBuilder;
import java.util.EnumSet;
import static org.testng.Assert.*;
import org.testng.annotations.Test;

/**
 *
 * @author pham21
 */
public class SectionHandlerNGTest
{

  public SectionHandlerNGTest()
  {
  }

  /**
   * Test of SectionHandler constructor, of class SectionHandler.
   */
  @Test
  public void testConstructor()
  {
    System.out.println("SectionHandler constructor");
    TestSection testSection = new TestSection();
    EnumSet<Reader.Options> flags = EnumSet.of(Reader.Options.OPTIONAL, Reader.Options.REQUIRED);
    SectionHandler instance = new SectionHandler(flags, testSection);

    assertSame(instance.section, testSection);
    assertEquals(instance.getKey(), testSection.getHandlerKey());
    assertTrue(instance.options.equals(flags));
    assertNull(instance.target);
    assertNull(instance.getTargetClass());
    assertNull(instance.method);
  }

  /**
   * Test of onStart method, of class SectionHandler.
   */
  @Test
  public void testOnStart() throws Exception
  {
    System.out.println("onStart");
    Integer intObj = Integer.valueOf(0);
    TestReaderContext testReaderContext = new TestReaderContext();
    HandlerContextImpl hci = new HandlerContextImpl();
    hci.parentObject = intObj;
    testReaderContext.handlerContext = hci;
    org.xml.sax.helpers.AttributesImpl attributes = new org.xml.sax.helpers.AttributesImpl();

    TestSection testSection = new TestSection();
    SectionHandler instance = new SectionHandler(null, testSection);

    Object obj = instance.onStart(testReaderContext, attributes);

    assertSame(hci.parentObject, intObj);
    assertSame(hci.targetObject, hci.parentObject);
    assertSame(obj, hci.targetObject);
    assertSame(testSection.readerContext, testReaderContext);
    assertSame(testSection.attributes, attributes);
  }

  /**
   * Test of onEnd method, of class SectionHandler.
   */
  @Test
  public void testOnEnd() throws Exception
  {
    System.out.println("onEnd");
    TestSection testSection = new TestSection();
    SectionHandler instance = new SectionHandler(null, testSection);
    assertNull(instance.onEnd(null));
  }

  /**
   * Test of onTextContent method, of class SectionHandler.
   */
  @Test
  public void testOnTextContent() throws Exception
  {
    System.out.println("onTextContent");
    TestSection testSection = new TestSection();
    SectionHandler instance = new SectionHandler(null, testSection);
    assertNull(instance.onTextContent(null, null));
  }

  /**
   * Test of getHandlers method, of class SectionHandler.
   */
  @Test
  public void testGetHandlers() throws Exception
  {
    System.out.println("getHandlers");
    TestSection testSection = new TestSection();
    StackTraceElement trace = new StackTraceElement("", "", "", 0);
    ReaderBuilderImpl.HandlerList handlerList = new ReaderBuilderImpl.HandlerList();
    handlerList.trace = trace;
    ElementHandlerMapImpl emap = ElementHandlerMapImpl.newInstance("#aot", handlerList);
    testSection.elementHandlerMap = emap;

    SectionHandler instance = new SectionHandler(null, testSection);
    assertSame(instance.getHandlers(), emap);
  }

  /**
   * Test of getReader method, of class SectionHandler.
   */
  @Test
  public void testGetReader()
  {
    System.out.println("getReader");
    TestSection testSection = new TestSection();
    SectionHandler instance = new SectionHandler(null, testSection);
    assertSame(instance.getReader(), testSection);
  }

  /**
   * Test of createSchemaElement method, of class SectionHandler.
   */
  @Test
  public void testCreateSchemaElement() throws Exception
  {
    System.out.println("createSchemaElement");

    EnumSet<Reader.Options> flags = EnumSet.of(Reader.Options.ANY_ALL, Reader.Options.REQUIRED);
    TestSection testSection = new TestSection();
    SectionHandler instance = new SectionHandler(flags, testSection);

    TestSupport.TestElement testElement = new TestSupport.TestElement("TestElement");
    DomBuilder group = new DomBuilder(testElement);

    instance.createSchemaElement(null, group);

    for (Reader.Options opt : flags)
    {
      assertTrue(testElement.attrMap.containsKey(opt.getKey()));
      assertSame(testElement.attrMap.get(opt.getKey()), opt.getValue());
    }
  }

  /**
   * Test of hasTextContent method, of class SectionHandler.
   */
  @Test
  public void testHasTextContent()
  {
    System.out.println("hasTextContent");
    TestSection testSection = new TestSection();
    SectionHandler instance = new SectionHandler(null, testSection);
    assertTrue(instance.hasTextContent());
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