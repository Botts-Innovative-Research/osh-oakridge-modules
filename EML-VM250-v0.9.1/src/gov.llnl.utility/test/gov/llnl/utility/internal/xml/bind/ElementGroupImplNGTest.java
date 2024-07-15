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
import static org.testng.Assert.*;
import org.testng.annotations.Test;

/**
 *
 * @author pham21
 */
public class ElementGroupImplNGTest
{

  public ElementGroupImplNGTest()
  {
  }

  /**
   * Test of ElementGroupImpl constructor, of class ElementGroupImpl.
   */
  @Test
  public void testConstructor()
  {
    System.out.println("ElementGroupImpl constructor");
    TestElementGroupImpl parent = new TestElementGroupImpl(null, null);
    EnumSet<Reader.Options> flags = EnumSet.of(Reader.Options.ANY_ALL);

    ElementGroupImpl instance = new TestElementGroupImpl(parent, flags);
    assertSame(instance.getParent(), parent);
    assertSame(instance.flags, flags);
    assertTrue(flags.equals(instance.flags));
  }

  /**
   * Test of getElementOptions method, of class ElementGroupImpl.
   */
  @Test
  public void testGetElementOptions()
  {
    System.out.println("getElementOptions");
    ElementGroupImpl instance = new TestElementGroupImpl(null, null);
    assertNull(instance.getElementOptions());
  }

  /**
   * Test of getParent method, of class ElementGroupImpl.
   */
  @Test
  public void testGetParent()
  {
    System.out.println("getParent");
    ElementGroupImpl instance = new TestElementGroupImpl(null, null);
    assertNull(instance.getParent());
    TestElementGroupImpl parent = new TestElementGroupImpl(null, null);
    instance = new TestElementGroupImpl(parent, null);
    assertSame(instance.getParent(), parent);
  }

  /**
   * Test of newInstance method, of class ElementGroupImpl.
   */
  @Test
  public void testNewInstance()
  {
    System.out.println("newInstance");

    assertSame(
            ElementGroupImpl.newInstance(null, Reader.Order.ALL, null).getClass(),
            ElementGroupImpl.AllGroup.class
    );
    assertSame(
            ElementGroupImpl.newInstance(null, Reader.Order.OPTIONS, null).getClass(),
            ElementGroupImpl.OptionsGroup.class
    );

    assertSame(
            ElementGroupImpl.newInstance(null, Reader.Order.SEQUENCE, null).getClass(),
            ElementGroupImpl.SequenceGroup.class
    );

    assertSame(
            ElementGroupImpl.newInstance(null, Reader.Order.CHOICE, null).getClass(),
            ElementGroupImpl.ChoiceGroup.class
    );

    assertSame(
            ElementGroupImpl.newInstance(null, Reader.Order.FREE, null).getClass(),
            ElementGroupImpl.FreeGroup.class
    );

    // Cannot test RuntimeException    
  }

  /**
   * Test of createSchemaGroup method, of class ElementGroupImpl.
   */
  @Test
  public void testCreateSchemaGroup()
  {
    System.out.println("createSchemaGroup");
    TestElement testElement = new TestElement("TestElement");
    DomBuilder type = new DomBuilder(testElement);
    EnumSet<Reader.Options> flags = EnumSet.of(Reader.Options.ANY_ALL, Reader.Options.ANY_SKIP);
    ElementGroupImpl instance = new TestElementGroupImpl(null, flags);

    DomBuilder result = instance.createSchemaGroup(type);
    assertSame(result, type);
    assertSame(result.toElement(), testElement);

    TestElement element = (TestElement) result.toElement();
    for (Reader.Options opt : flags)
    {
      assertTrue(element.attrMap.containsKey(opt.getKey()));
      assertEquals(element.attrMap.get(opt.getKey()), opt.getValue());
    }
  }

  /**
   * Test of createSchemaGroupImpl method, of class ElementGroupImpl.
   */
  @Test
  public void testCreateSchemaGroupImpl()
  {
    System.out.println("createSchemaGroupImpl");
    TestElement testElement = new TestElement("TestElement");
    DomBuilder type = new DomBuilder(testElement);

    ElementGroupImpl instance = new TestElementGroupImpl(null, null);
    assertSame(instance.createSchemaGroupImpl(type), type);
  }

  /**
   * Test of AllGroup static class, of class ElementGroupImpl.
   */
  @Test
  public void testAllGroup()
  {
    System.out.println("AllGroup");
    TestElementGroupImpl parent = new TestElementGroupImpl(null, null);
    EnumSet<Reader.Options> flags = EnumSet.of(Reader.Options.ANY_ALL);

    ElementGroupImpl.AllGroup instance = new ElementGroupImpl.AllGroup(parent, flags);

    assertSame(instance.getParent(), parent);
    assertSame(instance.flags, flags);

    TestElement testElement = new TestElement("TestElement");
    DomBuilder type = new DomBuilder(testElement);

    DomBuilder type2 = instance.createSchemaGroupImpl(type);
    assertEquals(type2.toElement().getTagName(), "xs:all");
  }

  /**
   * Test of OptionsGroup static class, of class ElementGroupImpl.
   */
  @Test
  public void testOptionsGroup()
  {
    System.out.println("OptionsGroup");
    TestElementGroupImpl parent = new TestElementGroupImpl(null, null);
    EnumSet<Reader.Options> flags = EnumSet.of(Reader.Options.ANY_ALL);

    ElementGroupImpl.OptionsGroup instance = new ElementGroupImpl.OptionsGroup(parent, flags);

    assertSame(instance.getParent(), parent);
    assertSame(instance.flags, flags);

    assertTrue(EnumSet.of(Reader.Options.OPTIONAL).equals(instance.getElementOptions()));

    TestElement testElement = new TestElement("TestElement");
    DomBuilder type = new DomBuilder(testElement);

    DomBuilder type2 = instance.createSchemaGroupImpl(type);
    assertEquals(type2.toElement().getTagName(), "xs:all");
  }

  /**
   * Test of SequenceGroup static class, of class ElementGroupImpl.
   */
  @Test
  public void testSequenceGroup()
  {
    System.out.println("SequenceGroup");
    TestElementGroupImpl parent = new TestElementGroupImpl(null, null);
    EnumSet<Reader.Options> flags = EnumSet.of(Reader.Options.ANY_ALL);

    ElementGroupImpl.SequenceGroup instance = new ElementGroupImpl.SequenceGroup(parent, flags);

    assertSame(instance.getParent(), parent);
    assertSame(instance.flags, flags);

    TestElement testElement = new TestElement("TestElement");
    DomBuilder type = new DomBuilder(testElement);

    DomBuilder type2 = instance.createSchemaGroupImpl(type);
    assertEquals(type2.toElement().getTagName(), "xs:sequence");
  }

  /**
   * Test of ChoiceGroup static class, of class ElementGroupImpl.
   */
  @Test
  public void testChoiceGroup()
  {
    System.out.println("ChoiceGroup");
    TestElementGroupImpl parent = new TestElementGroupImpl(null, null);
    EnumSet<Reader.Options> flags = EnumSet.of(Reader.Options.ANY_ALL);

    ElementGroupImpl.ChoiceGroup instance = new ElementGroupImpl.ChoiceGroup(parent, flags);

    assertSame(instance.getParent(), parent);
    assertSame(instance.flags, flags);

    TestElement testElement = new TestElement("TestElement");
    DomBuilder type = new DomBuilder(testElement);

    DomBuilder type2 = instance.createSchemaGroupImpl(type);
    assertEquals(type2.toElement().getTagName(), "xs:choice");
  }

  /**
   * Test of FreeGroup static class, of class ElementGroupImpl.
   */
  @Test
  public void testFreeGroup()
  {
    System.out.println("FreeGroup");
    TestElementGroupImpl parent = new TestElementGroupImpl(null, null);
    EnumSet<Reader.Options> flags = EnumSet.of(Reader.Options.ANY_ALL);

    ElementGroupImpl.FreeGroup instance = new ElementGroupImpl.FreeGroup(parent, flags);

    assertSame(instance.getParent(), parent);
    assertSame(instance.flags, flags);

    TestElement testElement = new TestElement("TestElement");
    DomBuilder type = new DomBuilder(testElement);

    DomBuilder type2 = instance.createSchemaGroupImpl(type);
    TestElement childElement = (TestElement) type2.toElement();
    assertEquals(childElement.getTagName(), "xs:choice");

    assertTrue(childElement.attrMap.containsKey("maxOccurs"));
    assertEquals(childElement.attrMap.get("maxOccurs"), "unbounded");
  }

  static public class TestElementGroupImpl extends ElementGroupImpl
  {
    public TestElementGroupImpl(ElementGroupImpl parent, EnumSet<Reader.Options> flags)
    {
      super(parent, flags);
    }

    public DomBuilder createSchemaGroupImpl(DomBuilder type)
    {
      return type;
    }
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