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
import gov.llnl.utility.xml.DomBuilder;
import gov.llnl.utility.xml.bind.Reader;
import java.util.EnumSet;
import java.util.function.BiConsumer;
import static org.testng.Assert.*;
import org.testng.annotations.Test;

/**
 *
 * @author pham21
 */
public class ReferenceHandlerNGTest
{

  public ReferenceHandlerNGTest()
  {
  }

  /**
   * Test of ReferenceHandler constructor, of class ReferenceHandler.
   */
  @Test
  public void testConstructor()
  {
    System.out.println("ReferenceHandler constructor");
    String key = "key";
    EnumSet<Reader.Options> flags = EnumSet.of(Reader.Options.ANY_ALL, Reader.Options.DEFERRABLE);
    Long target = Long.valueOf(0);
    Class<Integer> resultCls = Integer.class;
    BiConsumer<String, String> method = (a, b) -> System.out.print("");

    ReferenceHandler instance = new ReferenceHandler(key, flags, target, resultCls, method);
    assertSame(instance.getKey(), key);
    assertSame(instance.target, target);
    assertSame(instance.method, method);
    assertTrue(flags.equals(instance.options));
    assertSame(instance.getTargetClass(), resultCls);
  }

  /**
   * Test of onStart method, of class ReferenceHandler.
   */
  @Test
  public void testOnStart() throws Exception
  {
    System.out.println("onStart");
    ReferenceHandler instance = new ReferenceHandler(null, null, null, null, null);
    assertNull(instance.onStart(null, null));
  }

  /**
   * Test of getHandlers method, of class ReferenceHandler.
   */
  @Test
  public void testGetHandlers()
  {
    System.out.println("getHandlers");
    ReferenceHandler instance = new ReferenceHandler(null, null, null, null, null);
    assertNull(instance.getHandlers());
  }

  /**
   * Test of createSchemaElement method, of class ReferenceHandler.
   */
  @Test
  public void testCreateSchemaElement() throws Exception
  {
    System.out.println("createSchemaElement");
    String key = "key";
    EnumSet<Reader.Options> flags = EnumSet.of(Reader.Options.OPTIONAL, Reader.Options.ANY_STRICT);
    TestElement testElement = new TestElement("TestElement");
    DomBuilder group = new DomBuilder(testElement);

    ReferenceHandler instance = new ReferenceHandler(key, flags, null, null, null);
    instance.createSchemaElement(null, group);

    TestElement childElement = (TestElement) testElement.childrenList.get(0);

    assertEquals(childElement.tagName, "xs:element");
    assertTrue(childElement.attrMap.containsKey("name"));
    assertEquals(childElement.attrMap.get("name"), key);
    assertTrue(childElement.attrMap.containsKey("type"));
    assertEquals(childElement.attrMap.get("type"), "util:reference-type");

    for (Reader.Options opt : flags)
    {
      assertTrue(childElement.attrMap.containsKey(opt.getKey()));
      assertSame(childElement.attrMap.get(opt.getKey()), opt.getValue());
    }
  }

  /**
   * Test of mustReference method, of class ReferenceHandler.
   */
  @Test
  public void testMustReference()
  {
    System.out.println("mustReference");
    ReferenceHandler instance = new ReferenceHandler(null, null, null, null, null);
    assertTrue(instance.mustReference());
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