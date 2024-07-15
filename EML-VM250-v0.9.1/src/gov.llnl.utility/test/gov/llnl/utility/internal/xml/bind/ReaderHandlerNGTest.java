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
import gov.llnl.utility.TestSupport.TestReader;
import gov.llnl.utility.TestSupport.TestReaderMixed;
import gov.llnl.utility.io.ReaderException;
import gov.llnl.utility.xml.DomBuilder;
import gov.llnl.utility.xml.bind.AnyReader;
import gov.llnl.utility.xml.bind.Reader;
import java.util.EnumSet;
import java.util.function.BiConsumer;
import static org.testng.Assert.*;
import org.testng.annotations.Test;

/**
 *
 * @author pham21
 */
public class ReaderHandlerNGTest
{

  public ReaderHandlerNGTest()
  {
  }

  /**
   * Test of ReaderHandler constructor, of class ReaderHandler.
   */
  @Test
  public void testConstructor()
  {
    System.out.println("ReaderHandler constructor");
    BiConsumer<Object, Object> doNothing = (a, b) -> System.out.print("");
    String key = "key";
    String target = "target";
    AnyReader anyReader = AnyReader.of(String.class);
    EnumSet<Reader.Options> flags = EnumSet.of(Reader.Options.ANY_ALL, Reader.Options.REQUIRED);

    ReaderHandler instance = new ReaderHandler(key, flags, target, doNothing, anyReader);

    assertEquals(instance.getKey(), key);
    assertEquals(instance.target, target);
    assertSame(instance.method, doNothing);
    assertSame(instance.options, flags);
    assertEquals(instance.getTargetClass(), anyReader.getObjectClass());

    assertSame(instance.reader, anyReader);
    assertSame(instance.decl, anyReader.getDeclaration());
  }

  /**
   * Test of onStart method, of class ReaderHandler.
   */
  @Test
  public void testOnStart() throws Exception
  {
    System.out.println("onStart");
    String str = "str";
    TestReader testReader = TestReader.of(String.class);
    testReader.obj = str;
    org.xml.sax.helpers.AttributesImpl attributes = new org.xml.sax.helpers.AttributesImpl();

    ReaderHandler instance = new ReaderHandler(null, null, null, null, testReader);

    Object obj = instance.onStart(null, attributes);

    assertSame(obj, str);

    // Test ReaderException
    try
    {
      testReader.obj = null;
      obj = instance.onStart(null, attributes);
    }
    catch (ReaderException re)
    {
      assertSame(re.getMessage(), "Auto attributes applied to null object");
    }
  }

  /**
   * Test of onEnd method, of class ReaderHandler.
   */
  @Test
  public void testOnEnd() throws ReaderException
  {
    System.out.println("onEnd");
    String str = "str";
    TestReader testReader = TestReader.of(String.class);
    testReader.setObj(str);

    ReaderHandler instance = new ReaderHandler(null, null, null, null, testReader);
    assertSame(instance.onEnd(null), str);
  }

  /**
   * Test of onTextContent method, of class ReaderHandler.
   */
  @Test
  public void testOnTextContent() throws Exception
  {
    System.out.println("onTextContent");
    AnyReader anyReader = AnyReader.of(String.class);
    ReaderHandler instance = new ReaderHandler(null, null, null, null, anyReader);

    // AnyReader.contents method return null
    assertNull(instance.onTextContent(null, ""));

    String str = "str";
    TestReader testReader = TestReader.of(String.class);
    testReader.setObj(str);
    instance = new ReaderHandler(null, null, null, null, testReader);
    assertSame(instance.onTextContent(null, ""), str);
  }

  /**
   * Test of getHandlers method, of class ReaderHandler.
   */
  @Test
  public void testGetHandlers() throws Exception
  {
    System.out.println("getHandlers");
    BiConsumer<Object, Object> doNothing = (a, b) -> System.out.print("");
    String key = "key";
    String target = "target";
    AnyReader anyReader = AnyReader.of(String.class);
    EnumSet<Reader.Options> flags = EnumSet.of(Reader.Options.ANY_ALL, Reader.Options.REQUIRED);

    ReaderHandler instance = new ReaderHandler(key, flags, target, doNothing, anyReader);

    Reader.ElementHandlerMap handlerMap = instance.getHandlers();
    assertEquals(handlerMap.toList().size(), 1);
    AnyHandlerImpl anyHandlerImpl = (AnyHandlerImpl) handlerMap.toList().get(0);
    assertEquals(anyHandlerImpl.getKey(), "##any");
    assertSame(anyHandlerImpl.target, anyReader);
    assertEquals(anyHandlerImpl.getTargetClass(), String.class);
  }

  /**
   * Test of getReader method, of class ReaderHandler.
   */
  @Test
  public void testGetReader()
  {
    System.out.println("getReader");
    AnyReader anyReader = AnyReader.of(String.class);
    ReaderHandler instance = new ReaderHandler(null, null, null, null, anyReader);
    assertSame(instance.getReader(), anyReader);
  }

  /**
   * Test of createSchemaElement method, of class ReaderHandler.
   */
  @Test
  public void testCreateSchemaElement() throws Exception
  {
    System.out.println("createSchemaElement");

    TestReader testReader = TestReader.of(String.class);
    EnumSet<Reader.Options> flags = EnumSet.of(Reader.Options.ANY_ALL, Reader.Options.REQUIRED);

    ReaderHandler instance = new ReaderHandler("", flags, null, null, testReader);

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
   * Test of hasTextContent method, of class ReaderHandler.
   */
  @Test
  public void testHasTextContent()
  {
    System.out.println("hasTextContent");
    AnyReader anyReader = AnyReader.of(String.class);

    ReaderHandler instance = new ReaderHandler(null, null, null, null, anyReader);
    assertFalse(instance.hasTextContent());

    TestReader testReader = TestReader.of(String.class);
    instance = new ReaderHandler(null, null, null, null, testReader);
    assertTrue(instance.hasTextContent());

    TestReaderMixed testReaderMixed = TestReaderMixed.of(String.class);
    instance = new ReaderHandler(null, null, null, null, testReaderMixed);
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