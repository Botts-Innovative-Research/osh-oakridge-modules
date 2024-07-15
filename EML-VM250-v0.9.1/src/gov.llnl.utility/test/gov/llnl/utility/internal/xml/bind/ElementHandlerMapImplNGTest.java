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

import gov.llnl.utility.TestSupport.TestAnyHandler;
import gov.llnl.utility.TestSupport.TestElement;
import gov.llnl.utility.TestSupport.TestElementGroup;
import gov.llnl.utility.TestSupport.TestElementHandler;
import gov.llnl.utility.xml.DomBuilder;
import gov.llnl.utility.xml.bind.Reader;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.function.BiConsumer;
import static org.testng.Assert.*;
import org.testng.annotations.Test;

/**
 *
 * @author pham21
 */
public class ElementHandlerMapImplNGTest
{

  public ElementHandlerMapImplNGTest()
  {
  }

  /**
   * Test of newInstance method, of class ElementHandlerMapImpl.
   */
  @Test
  public void testNewInstance()
  {
    System.out.println("newInstance");

    // Setup Handlers
    BiConsumer<Object, Object> onlyYesterday = (Taeko, Okajima) -> System.out.println("Ai ha Hana, Kimi ha Sono Tane");

    TestElementHandler firstHandler = new TestElementHandler();
    firstHandler.key = "##safflower";
    firstHandler.method = onlyYesterday;
    TestAnyHandler anyHandler = new TestAnyHandler();
    anyHandler.key = "##Toshio";
    anyHandler.method = onlyYesterday;
    firstHandler.nextHandler = anyHandler;

    // Setup HandlerList
    StackTraceElement trace = new StackTraceElement("", "", "", 0);
    ReaderBuilderImpl.HandlerList handlerList = new ReaderBuilderImpl.HandlerList();
    handlerList.firstHandler = firstHandler;
    handlerList.trace = trace;

    // Test
    ElementHandlerMapImpl instance = ElementHandlerMapImpl.newInstance("#StudioGhibli", handlerList);

    assertEquals(instance.namespaceURI, "#StudioGhibli");
    assertSame(instance.first, firstHandler);
    assertEquals(instance.handlers.length, 2);
    assertSame(instance.handlers[0], anyHandler);
    assertSame(instance.handlers[1], firstHandler);
    assertTrue(instance.hasAny);
    assertSame(instance.getTrace(), trace);

    // Test exceptions 
    // namespace issue
    try
    {
      ElementHandlerMapImpl.newInstance("StudioGhibli", handlerList);
    }
    catch (RuntimeException ex)
    {
      assertEquals(ex.getMessage(), "namespace issue");
    }

    // bad key
    try
    {
      firstHandler.key = "#safflower";
      ElementHandlerMapImpl.newInstance("#StudioGhibli", handlerList);
    }
    catch (RuntimeException ex)
    {
      assertEquals(ex.getMessage(), "bad key #safflower");
    }

    // null key 
    try
    {
      firstHandler.key = null;
      ElementHandlerMapImpl.newInstance("#StudioGhibli", handlerList);
    }
    catch (RuntimeException ex)
    {
      assertEquals(ex.getMessage(), "Null pointer getting key " + firstHandler + " " + firstHandler.getMethod());
    }
  }

  /**
   * Test of getTrace method, of class ElementHandlerMapImpl.
   */
  @Test
  public void testGetTrace()
  {
    System.out.println("getTrace");
    StackTraceElement trace = new StackTraceElement("", "", "", 0);
    ReaderBuilderImpl.HandlerList handlerList = new ReaderBuilderImpl.HandlerList();
    handlerList.trace = trace;
    ElementHandlerMapImpl instance = ElementHandlerMapImpl.newInstance("#StudioGhibli", handlerList);
    assertSame(instance.getTrace(), trace);
  }

  /**
   * Test of toList method, of class ElementHandlerMapImpl.
   */
  @Test
  public void testToList()
  {
    System.out.println("toList");
    // Setup handlers
    TestElementHandler firstHandler = new TestElementHandler();
    firstHandler.key = "##Ashitaka";
    TestAnyHandler anyHandler = new TestAnyHandler();
    anyHandler.key = "##San";
    firstHandler.nextHandler = anyHandler;
    // Setup HandlerList
    StackTraceElement trace = new StackTraceElement("", "", "", 0);
    ReaderBuilderImpl.HandlerList handlerList = new ReaderBuilderImpl.HandlerList();
    handlerList.firstHandler = firstHandler;
    handlerList.trace = trace;

    // Test
    ElementHandlerMapImpl instance = ElementHandlerMapImpl.newInstance("#PrincessMononoke", handlerList);
    List<Reader.ElementHandler> list = instance.toList();
    assertTrue(list.contains(firstHandler));
    assertTrue(list.contains(anyHandler));
  }

  /**
   * Test of getAnyHandler method, of class ElementHandlerMapImpl.
   */
  @Test
  public void testGetAnyHandler()
  {
    System.out.println("getAnyHandler");
    // Setup Handlers
    TestElementHandler firstHandler = new TestElementHandler();
    firstHandler.key = "##Chihiro";
    TestAnyHandler anyHandler = new TestAnyHandler();
    anyHandler.key = "##Haku";

    // Setup HandlerList
    StackTraceElement trace = new StackTraceElement("", "", "", 0);
    ReaderBuilderImpl.HandlerList handlerList = new ReaderBuilderImpl.HandlerList();
    handlerList.firstHandler = firstHandler;
    handlerList.trace = trace;

    // Test null
    ElementHandlerMapImpl instance = ElementHandlerMapImpl.newInstance("#SpiritedAway", handlerList);
    assertNull(instance.getAnyHandler(null));

    // Test first while logic
    firstHandler.nextHandler = anyHandler;
    instance = ElementHandlerMapImpl.newInstance("#SpiritedAway", handlerList);
    assertSame(instance.getAnyHandler(firstHandler), anyHandler);

    // Test second while logic
    assertSame(instance.getAnyHandler(null), anyHandler);
  }

  /**
   * Test of get method, of class ElementHandlerMapImpl.
   */
  @Test
  public void testGet()
  {
    System.out.println("get");
    // Setup handlers
    TestElementHandler firstHandler = new TestElementHandler();
    firstHandler.key = "##OneSummersDay";
    TestAnyHandler anyHandler = new TestAnyHandler();
    anyHandler.key = "##ARoadtoSomewhere";
    firstHandler.nextHandler = anyHandler;
    // Setup HandlerList
    StackTraceElement trace = new StackTraceElement("", "", "", 0);
    ReaderBuilderImpl.HandlerList handlerList = new ReaderBuilderImpl.HandlerList();
    handlerList.firstHandler = firstHandler;
    handlerList.trace = trace;

    // Test
    ElementHandlerMapImpl instance = ElementHandlerMapImpl.newInstance("#SpiritedAwayOST", handlerList);

    assertNull(instance.get(null, "#"));
    assertSame(instance.get("OneSummersDay", "#"), firstHandler);
    assertSame(instance.get("ARoadtoSomewhere", "#"), anyHandler);
  }

  /**
   * Test of dump method, of class ElementHandlerMapImpl.
   */
  @Test
  public void testDump()
  {
    System.out.println("dump");
    // Setup handlers
    TestElementHandler firstHandler = new TestElementHandler();
    firstHandler.key = "##AwaysWithMe";
    TestAnyHandler anyHandler = new TestAnyHandler();
    anyHandler.key = "##TheEmptyRestaurant";
    firstHandler.nextHandler = anyHandler;
    // Setup HandlerList
    StackTraceElement trace = new StackTraceElement("", "", "", 0);
    ReaderBuilderImpl.HandlerList handlerList = new ReaderBuilderImpl.HandlerList();
    handlerList.trace = trace;

    // Test
    ElementHandlerMapImpl instance = ElementHandlerMapImpl.newInstance("#SpiritedAwayOST", handlerList);
    // no output 
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintStream ps = new PrintStream(baos);
    instance.dump(ps);
    assertEquals(baos.toString(), "");
    ps.close();
    // Print
    baos = new ByteArrayOutputStream();
    ps = new PrintStream(baos);
    handlerList.firstHandler = firstHandler;
    instance = ElementHandlerMapImpl.newInstance("#SpiritedAwayOST", handlerList);
    instance.dump(ps);
    String str = "handler=##AwaysWithMe" + System.lineSeparator()
            + "handler=##TheEmptyRestaurant" + System.lineSeparator();
    assertEquals(baos.toString(), str);
  }

  /**
   * Test of isEmpty method, of class ElementHandlerMapImpl.
   */
  @Test
  public void testIsEmpty()
  {
    System.out.println("isEmpty");
    // Setup HandlerList
    StackTraceElement trace = new StackTraceElement("", "", "", 0);
    ReaderBuilderImpl.HandlerList handlerList = new ReaderBuilderImpl.HandlerList();
    handlerList.trace = trace;

    // Test
    ElementHandlerMapImpl instance = ElementHandlerMapImpl.newInstance("#SpiritedAwayOST", handlerList);
    assertTrue(instance.isEmpty());
  }

  /**
   * Test of createSchemaType method, of class ElementHandlerMapImpl.
   */
  @Test
  public void testCreateSchemaType() throws Exception
  {
    System.out.println("createSchemaType");
    // Setup handlers
    TestElementGroup grandElementGroup = new TestElementGroup();

    TestElementHandler firstHandler = new TestElementHandler();
    firstHandler.key = "##TheReturn";
    TestElementGroup firstParentElementGroup = new TestElementGroup();
    firstParentElementGroup.parent = grandElementGroup;
    firstHandler.parentElementGroup = firstParentElementGroup;

    TestAnyHandler anyHandler = new TestAnyHandler();
    anyHandler.key = "##Reprise";
    anyHandler.parentElementGroup = new TestElementGroup();
    firstHandler.nextHandler = anyHandler;
    // Setup HandlerList
    StackTraceElement trace = new StackTraceElement("", "", "", 0);
    ReaderBuilderImpl.HandlerList handlerList = new ReaderBuilderImpl.HandlerList();
    handlerList.trace = trace;

    // Test
    ElementHandlerMapImpl instance = ElementHandlerMapImpl.newInstance("#SpiritedAwayOST", handlerList);

    // return immediately
    instance.createSchemaType(null, null);

    // Test BuildSchema - cover if block in process()
    DomBuilder db = new DomBuilder(new TestElement("TestElement"));
    handlerList.firstHandler = firstHandler;
    instance = ElementHandlerMapImpl.newInstance("#SpiritedAwayOST", handlerList);
    instance.createSchemaType(null, db);

    // Test BuildSchema - cover else block in process()
    firstParentElementGroup.parent = null;
    instance = ElementHandlerMapImpl.newInstance("#SpiritedAwayOST", handlerList);
    instance.createSchemaType(null, db);
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