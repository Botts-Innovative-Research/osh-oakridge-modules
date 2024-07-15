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

import gov.llnl.utility.TestSupport.TestElementHandler;
import gov.llnl.utility.internal.xml.bind.ReaderBuilderImpl.HandlerList;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.TreeMap;
import static org.testng.Assert.*;
import org.testng.annotations.Test;

/**
 *
 * @author pham21
 */
public class HandlerContextImplNGTest
{

  public HandlerContextImplNGTest()
  {
  }

  /**
   * Test of getNamespaceURI method, of class HandlerContextImpl.
   */
  @Test
  public void testGetNamespaceURI()
  {
    System.out.println("getNamespaceURI");
    HandlerContextImpl instance = new HandlerContextImpl();
    assertNull(instance.getNamespaceURI());
    instance.namespaceURI = "subjectofymir";
    assertEquals(instance.getNamespaceURI(), "subjectofymir");
  }

  /**
   * Test of getLocalName method, of class HandlerContextImpl.
   */
  @Test
  public void testGetLocalName()
  {
    System.out.println("getLocalName");
    HandlerContextImpl instance = new HandlerContextImpl();
    assertNull(instance.getLocalName());
    instance.elementName = "subjectofymir";
    assertEquals(instance.getLocalName(), "subjectofymir");
  }

  /**
   * Test of characters method, of class HandlerContextImpl.
   */
  @Test
  public void testCharacters()
  {
    System.out.println("characters");
    HandlerContextImpl instance = new HandlerContextImpl();
    // textContent is null
    instance.characters(null, 0, 0);

    char[] msg = new char[]
    {
      'l', 'e', 'v', 'i', 'a', 'c', 'k', 'e', 'r', 'm', 'a', 'n'
    };
    instance.textContent = new StringBuilder();
    instance.characters(msg, 0, msg.length);
    assertEquals(instance.textContent.toString(), String.valueOf(msg));
  }

  /**
   * Test of dump method, of class HandlerContextImpl.
   */
  @Test
  public void testDump()
  {
    System.out.println("dump");
    String str = "previousContext=null" + System.lineSeparator()
            + "handler=null" + System.lineSeparator()
            + "handlerMap=null" + System.lineSeparator()
            + "parentObject=null" + System.lineSeparator()
            + "targetObject=null" + System.lineSeparator()
            + "reference=null" + System.lineSeparator()
            + "textContent=null" + System.lineSeparator()
            + "elementName=null" + System.lineSeparator();
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintStream ps = new PrintStream(baos);
    HandlerContextImpl instance = new HandlerContextImpl();
    instance.dump(ps);
    assertEquals(baos.toString(), str);
  }

  /**
   * Test of getReference method, of class HandlerContextImpl.
   */
  @Test
  public void testGetReference()
  {
    System.out.println("getReference");
    assertNull(new HandlerContextImpl().getReference(""));

    // Setup
    HandlerContextImpl grandparent = new HandlerContextImpl();
    HandlerContextImpl parent = new HandlerContextImpl();
    HandlerContextImpl child = new HandlerContextImpl();

    grandparent.references = new TreeMap<>()
    {
      {
        put("Integer", Integer.valueOf(0));
        put("Double", Double.valueOf(1.0D));
      }
    };
    parent.references = new TreeMap<>()
    {
      {
        put("Float", Float.valueOf(2.0f));
        put("Boolean", Boolean.valueOf(true));
      }
    };
    child.references = new TreeMap<>()
    {
      {
        put("Byte", Byte.valueOf((byte) 64));
        put("attacktitan", "ErenYeager");
      }
    };

    child.previousContext = parent;
    parent.previousContext = grandparent;

    // Test
    assertNull(child.getReference("scoutregiment"));
    assertEquals(child.getReference("attacktitan"), "ErenYeager");
    assertEquals(child.getReference("Byte"), Byte.valueOf("64"));
    assertEquals(child.getReference("Integer"), 0);
    assertEquals(child.getReference("Double"), 1D);
    assertEquals(child.getReference("Float"), 2.0f);
    assertEquals(child.getReference("Boolean"), true);
  }

  /**
   * Test of putReference method, of class HandlerContextImpl.
   */
  @Test
  public void testPutReference()
  {
    System.out.println("putReference");
    HandlerContextImpl instance = new HandlerContextImpl();
    instance.references = new TreeMap<>();
    assertNotNull(instance.putReference("Historia", "Reiss"));
    assertEquals(instance.references.get("Historia"), "Reiss");
  }

  /**
   * Test of getTargetObject method, of class HandlerContextImpl.
   */
  @Test
  public void testGetTargetObject()
  {
    System.out.println("getTargetObject");
    HandlerContextImpl instance = new HandlerContextImpl();
    instance.targetObject = new Object();
    assertSame(instance.getTargetObject(), instance.targetObject);
  }

  /**
   * Test of getParentObject method, of class HandlerContextImpl.
   */
  @Test
  public void testGetParentObject()
  {
    System.out.println("getParentObject");
    HandlerContextImpl instance = new HandlerContextImpl();
    instance.parentObject = new Object();
    assertSame(instance.getParentObject(), instance.parentObject);
  }

  /**
   * Test of getPreviousContext method, of class HandlerContextImpl.
   */
  @Test
  public void testGetPreviousContext()
  {
    System.out.println("getPreviousContext");
    HandlerContextImpl parent = new HandlerContextImpl();
    HandlerContextImpl child = new HandlerContextImpl();

    child.previousContext = parent;

    assertSame(child.getPreviousContext(), parent);
    assertNull(parent.getPreviousContext(), null);
  }

  /**
   * Test of getCurrentHandler method, of class HandlerContextImpl.
   */
  @Test
  public void testGetCurrentHandler()
  {
    System.out.println("getCurrentHandler");
    HandlerContextImpl instance = new HandlerContextImpl();
    instance.currentHandler = new TestElementHandler();
    assertSame(instance.getCurrentHandler(), instance.currentHandler);
  }

  /**
   * Test of getLastHandler method, of class HandlerContextImpl.
   */
  @Test
  public void testGetLastHandler()
  {
    System.out.println("getLastHandler");
    HandlerContextImpl instance = new HandlerContextImpl();
    instance.lastHandler = new TestElementHandler();
    assertSame(instance.getLastHandler(), instance.lastHandler);
  }

  /**
   * Test of getHandlerMap method, of class HandlerContextImpl.
   */
  @Test
  public void testGetHandlerMap()
  {
    System.out.println("getHandlerMap");
    HandlerContextImpl instance = new HandlerContextImpl();
    instance.handlerMap = ElementHandlerMapImpl.newInstance("#aot", new HandlerList());
    assertSame(instance.getHandlerMap(), instance.handlerMap);
  }

  /**
   * Test of dumpTrace method, of class HandlerContextImpl.
   */
  @Test
  public void testDumpTrace()
  {
    System.out.println("dumpTrace");
    // Set up
    HandlerContextImpl parent = new HandlerContextImpl();
    HandlerContextImpl child = new HandlerContextImpl();
    parent.references = new TreeMap<>()
    {
      {
        put("Float", Float.valueOf(2.0f));
        put("Boolean", Boolean.valueOf(true));
      }
    };
    child.references = new TreeMap<>()
    {
      {
        put("Byte", Byte.valueOf((byte) 64));
        put("attacktitan", "ErenYeager");
      }
    };    
    
    TestElementHandler parentHandler = new TestElementHandler();
    TestElementHandler childHandler = new TestElementHandler();
    
    parent.parentObject = Double.valueOf(64.0D);
    parent.targetObject = Float.valueOf(32.0f);
    parent.currentHandler = parentHandler;

    child.parentObject = "Hero";
    child.targetObject = "Call of Silence";
    child.currentHandler = childHandler;
    child.previousContext = parent;

    String str = "HandlerContext:" + System.lineSeparator()
            + "  parent=Hero" + System.lineSeparator()
            + "  target=Call of Silence" + System.lineSeparator()
            + "  handler=" + childHandler + System.lineSeparator()
            + "HandlerContext:" + System.lineSeparator()
            + "  parent=64.0" + System.lineSeparator()
            + "  target=32.0" + System.lineSeparator()
            + "  handler=" + parentHandler + System.lineSeparator(); 
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintStream ps = new PrintStream(baos);
    child.dumpTrace(ps);
    assertEquals(baos.toString(), str);
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