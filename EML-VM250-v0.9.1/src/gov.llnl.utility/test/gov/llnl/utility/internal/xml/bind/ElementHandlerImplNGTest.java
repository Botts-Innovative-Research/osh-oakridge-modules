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
import gov.llnl.utility.TestSupport.TestElementGroup;
import gov.llnl.utility.TestSupport.TestElementHandler;
import gov.llnl.utility.io.ReaderException;
import gov.llnl.utility.xml.bind.ElementGroup;
import gov.llnl.utility.xml.bind.Reader;
import gov.llnl.utility.xml.bind.Reader.Options;
import java.util.EnumSet;
import java.util.function.BiConsumer;
import static org.testng.Assert.*;
import org.testng.annotations.Test;

/**
 *
 * @author pham21
 */
public class ElementHandlerImplNGTest
{

  public ElementHandlerImplNGTest()
  {
  }

  /**
   * Test of ElementHandlerImpl constructor, of class ElementHandlerImpl.
   */
  @Test
  public void testConstructor()
  {
    System.out.println("ElementHandlerImpl constructor");
    String key = "key";
    EnumSet<Options> flags = EnumSet.of(Reader.Options.ANY_ALL, Reader.Options.DEFERRABLE);
    Long target = Long.valueOf(0);
    Class<Integer> resultCls = Integer.class;
    BiConsumer<String, String> method = (a, b) -> System.out.print("");

    ElementHandlerImpl instance = new ElementHandlerImpl(key, flags, target, resultCls, method);
    assertSame(instance.getKey(), key);
    assertSame(instance.target, target);
    assertSame(instance.method, method);
    assertTrue(flags.equals(instance.options));
    assertSame(instance.getTargetClass(), resultCls);
  }

  /**
   * Test of setOptions method, of class ElementHandlerImpl.
   */
  @Test
  public void testSetOptions()
  {
    System.out.println("setOptions");
    ElementHandlerImpl instance = new ElementHandlerImpl(null, null, null, null, null);
    // Immediate return
    instance.setOptions(null);

    // Set options
    assertNull(instance.options);
    EnumSet<Options> flags = EnumSet.of(Reader.Options.ANY_ALL);
    instance.setOptions(flags);
    assertEquals(instance.options, flags);

    // Add options
    EnumSet<Options> flags2 = EnumSet.of(Reader.Options.DEFERRABLE);
    instance.setOptions(flags2);
    flags2.addAll(flags);
    assertEquals(instance.options, flags2);

    // Remove Options.OPTIONAL
    instance.options.add(Reader.Options.OPTIONAL);
    assertTrue(instance.options.contains(Reader.Options.OPTIONAL));
    instance.setOptions(EnumSet.of(Reader.Options.REQUIRED));
    assertTrue(instance.options.contains(Reader.Options.REQUIRED));
    assertFalse(instance.options.contains(Reader.Options.OPTIONAL));

    // Revmoe Options.REQUIRED
    assertTrue(instance.options.contains(Reader.Options.REQUIRED));
    instance.setOptions(EnumSet.of(Reader.Options.OPTIONAL));
    assertTrue(instance.options.contains(Reader.Options.OPTIONAL));
    assertFalse(instance.options.contains(Reader.Options.REQUIRED));
  }

  /**
   * Test of onStart method, of class ElementHandlerImpl.
   */
  @Test
  public void testOnStart() throws Exception
  {
    System.out.println("onStart");
    assertNull(new ElementHandlerImpl(null, null, null, null, null).onStart(null, null));
  }

  /**
   * Test of onEnd method, of class ElementHandlerImpl.
   */
  @Test
  public void testOnEnd() throws Exception
  {
    System.out.println("onEnd");
    assertNull(new ElementHandlerImpl(null, null, null, null, null).onEnd(null));
  }

  /**
   * Test of onCall method, of class ElementHandlerImpl.
   */
  @Test
  public void testOnCall() throws Exception
  {
    System.out.println("onCall");
    ElementHandlerImpl instance = new ElementHandlerImpl(null, null, null, null, null);
    // Immediate return
    instance.onCall(null, null, null);

    BiConsumer<String, String> doNothing = (a, b) -> System.out.print("");
    instance = new ElementHandlerImpl(null, null, null, null, doNothing);
    instance.onCall(null, "Hello", "World");

    // Test exceptions
    // RuntimeException with ReaderException cause
    instance = new ElementHandlerImpl(null, null, null, null, ElementHandlerImplNGTest::throwEx1);
    try
    {
      instance.onCall(null, new Object(), new Object());
    }
    catch (ReaderException re)
    {
      assertEquals(re.getMessage(), "ReaderException");
    }

    // Wrapped ReaderException 
    instance = new ElementHandlerImpl(null, null, null, null, ElementHandlerImplNGTest::throwEx2);
    try
    {
      instance.onCall(null, new Object(), new Object());
    }
    catch (ReaderException re)
    {
      assertEquals(re.getCause().getMessage(), "Wrapped ReaderException");
    }

    // parent is null
    instance = new ElementHandlerImpl(null, null, null, null, ElementHandlerImplNGTest::throwEx3);
    try
    {
      instance.onCall(null, null, null);
    }
    catch (ReaderException re)
    {
      assertEquals(re.getMessage(), "Parent object was not constructed by start.");
      assertEquals(re.getCause().getMessage(), "Test3");
    }

    // Runtime with no caused
    instance = new ElementHandlerImpl(null, null, null, null, ElementHandlerImplNGTest::throwEx4);
    try
    {
      instance.onCall(null, new Object(), new Object());
    }
    catch (ReaderException re)
    {
      assertEquals(re.getCause().getMessage(), "Test4");
    }
  }

  static void throwEx1(Object obj1, Object obj2)
  {
    throw new RuntimeException("Test1", new ReaderException("ReaderException"));
  }

  static void throwEx2(Object obj1, Object obj2)
  {
    throw new RuntimeException("Test2", new RuntimeException("Wrapped ReaderException"));
  }

  static void throwEx3(Object obj1, Object obj2)
  {
    throw new RuntimeException("Test3");
  }

  static void throwEx4(Object obj1, Object obj2)
  {
    throw new RuntimeException("Test4");
  }

  /**
   * Test of onTextContent method, of class ElementHandlerImpl.
   */
  @Test
  public void testOnTextContent() throws Exception
  {
    System.out.println("onTextContent");
    assertNull(new ElementHandlerImpl(null, null, null, null, null).onTextContent(null, null));
  }

  /**
   * Test of getHandlers method, of class ElementHandlerImpl.
   */
  @Test
  public void testGetHandlers() throws Exception
  {
    System.out.println("getHandlers");
    assertNull(new ElementHandlerImpl(null, null, null, null, null).getHandlers());
  }

  /**
   * Test of getKey method, of class ElementHandlerImpl.
   */
  @Test
  public void testGetKey()
  {
    System.out.println("getKey");
    assertEquals(new ElementHandlerImpl("key", null, null, null, null).getKey(), "key");
  }

  /**
   * Test of getName method, of class ElementHandlerImpl.
   */
  @Test
  public void testGetName()
  {
    System.out.println("getName");
    assertEquals(new ElementHandlerImpl("key", null, null, null, null).getName(), "key");
    assertEquals(new ElementHandlerImpl("#key", null, null, null, null).getName(), "");
    assertEquals(new ElementHandlerImpl("key#lock", null, null, null, null).getName(), "key");
  }

  /**
   * Test of createSchemaElement method, of class ElementHandlerImpl.
   */
  @Test
  public void testCreateSchemaElement() throws Exception
  {
    System.out.println("createSchemaElement");
    // Empty body
    new ElementHandlerImpl(null, null, null, null, null).createSchemaElement(null, null);
  }

  /**
   * Test of hasTextContent method, of class ElementHandlerImpl.
   */
  @Test
  public void testHasTextContent()
  {
    System.out.println("hasTextContent");
    assertFalse(new ElementHandlerImpl(null, null, null, null, null).hasTextContent());
  }

  /**
   * Test of getParent method, of class ElementHandlerImpl.
   */
  @Test
  public void testGetParent()
  {
    System.out.println("getParent");
    TestElement te = new TestElement("TestElement");
    ElementHandlerImpl instance = new ElementHandlerImpl(null, null, te, null, null);
    assertSame(instance.getParent(null), te);

    HandlerContextImpl hci = new HandlerContextImpl();
    hci.targetObject = te;
    instance = new ElementHandlerImpl(null, null, null, null, null);
    assertSame(instance.getParent(hci), te);
  }

  /**
   * Test of mustReference method, of class ElementHandlerImpl.
   */
  @Test
  public void testMustReference()
  {
    System.out.println("mustReference");
    assertFalse(new ElementHandlerImpl(null, null, null, null, null).mustReference());
  }

  /**
   * Test of getReader method, of class ElementHandlerImpl.
   */
  @Test
  public void testGetReader()
  {
    System.out.println("getReader");
    assertNull(new ElementHandlerImpl(null, null, null, null, null).getReader());
  }

  /**
   * Test of getOptions method, of class ElementHandlerImpl.
   */
  @Test
  public void testGetOptions()
  {
    System.out.println("getOptions");
    ElementHandlerImpl instance = new ElementHandlerImpl(null, null, null, null, null);
    EnumSet<Options> flags = EnumSet.of(
            Reader.Options.ANY_ALL, Reader.Options.ANY_SKIP, Reader.Options.DEFERRABLE,
            Reader.Options.REQUIRED);
    instance.setOptions(flags);
    assertEquals(instance.options, flags);
  }

  /**
   * Test of getTargetClass method, of class ElementHandlerImpl.
   */
  @Test
  public void testGetTargetClass()
  {
    System.out.println("getTargetClass");
    ElementHandlerImpl instance = new ElementHandlerImpl(null, null, null, String.class, null);
    assertEquals(instance.getTargetClass(), String.class);
  }

  /**
   * Test of getParentGroup method, of class ElementHandlerImpl.
   */
  @Test
  public void testGetParentGroup()
  {
    System.out.println("getParentGroup");
    TestElementGroup teg = new TestElementGroup();
    ElementHandlerImpl instance = new ElementHandlerImpl(null, null, null, null, null);
    instance.setParentGroup(teg);
    assertTrue(instance.getParentGroup() instanceof ElementGroup);
    assertSame(instance.getParentGroup(), teg);
  }

  /**
   * Test of getNextHandler method, of class ElementHandlerImpl.
   */
  @Test
  public void testGetNextHandler()
  {
    System.out.println("getNextHandler");
    TestElementHandler teh = new TestElementHandler();
    ElementHandlerImpl instance = new ElementHandlerImpl(null, null, null, null, null);
    instance.setNextHandler(teh);
    assertTrue(instance.getNextHandler() instanceof Reader.ElementHandler);
    assertSame(instance.getNextHandler(), teh);
  }

  /**
   * Test of setParentGroup method, of class ElementHandlerImpl.
   */
  @Test
  public void testSetParentGroup()
  {
    System.out.println("setParentGroup");
    TestElementGroup teg = new TestElementGroup();
    ElementHandlerImpl instance = new ElementHandlerImpl(null, null, null, null, null);
    instance.setParentGroup(teg);
    assertTrue(instance.getParentGroup() instanceof ElementGroup);
    assertSame(instance.getParentGroup(), teg);
  }

  /**
   * Test of setNextHandler method, of class ElementHandlerImpl.
   */
  @Test
  public void testSetNextHandler()
  {
    System.out.println("setNextHandler");
    TestElementHandler teh = new TestElementHandler();
    ElementHandlerImpl instance = new ElementHandlerImpl(null, null, null, null, null);
    instance.setNextHandler(teh);
    assertTrue(instance.getNextHandler() instanceof Reader.ElementHandler);
    assertSame(instance.getNextHandler(), teh);
  }

  /**
   * Test of getMethod method, of class ElementHandlerImpl.
   */
  @Test
  public void testGetMethod()
  {
    System.out.println("getMethod");
    BiConsumer<String, String> twoString = (a, b) -> System.out.println(a + b);
    ElementHandlerImpl instance = new ElementHandlerImpl(null, null, null, String.class, twoString);
    assertSame(instance.getMethod(), twoString);
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