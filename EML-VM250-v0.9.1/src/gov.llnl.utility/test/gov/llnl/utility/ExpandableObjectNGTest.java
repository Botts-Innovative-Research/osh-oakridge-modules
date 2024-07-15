/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.utility;

import java.io.Serializable;
import java.util.TreeMap;
import static org.testng.Assert.*;
import org.testng.annotations.Test;

/**
 *
 * @author pham21
 */
public class ExpandableObjectNGTest
{

  public ExpandableObjectNGTest()
  {
  }

  /**
   * Test copy constructor
   */
  @Test(expectedExceptions =
  {
    NullPointerException.class
  })
  public void testCopyConstructor()
  {
    System.out.println("Copy Constructor");
    ExpandableObject instance = new ExpandableObject();
    instance.setAttribute("int", 1);
    instance.setAttribute("double", 3.14159265359);

    ExpandableObject other = new ExpandableObject(instance);
    assertEquals(other.getAttributes(), instance.getAttributes());

    // Test NullPointerException
    instance = new ExpandableObject(null);
  }

  /**
   * Test of setAttribute method, of class ExpandableObject.
   */
  @Test
  public void testSetAttribute()
  {
    System.out.println("setAttribute");
    ExpandableObject instance = new ExpandableObject();
    instance.setAttribute("int", 1);
    instance.setAttribute("double", 3.14159265359);

    assertEquals(instance.getAttribute("int"), 1);
    assertEquals(instance.getAttribute("double"), 3.14159265359);
    assertNull(instance.getAttribute("live long and prosper"));
  }

  /**
   * Test of getAttribute method, of class ExpandableObject.
   */
  @Test
  public void testGetAttribute()
  {
    System.out.println("getAttribute");
    ExpandableObject instance = new ExpandableObject();
    instance.setAttribute("int", 1);
    instance.setAttribute("double", 3.14159265359);

    assertEquals(instance.getAttribute("int"), 1);
    assertEquals(instance.getAttribute("double"), 3.14159265359);
    assertNull(instance.getAttribute("live long and prosper"));
  }

  /**
   * Test of getAttribute method, of class ExpandableObject.
   */
  @Test(expectedExceptions =
  {
    ClassCastException.class
  })
  public void testGetAttribute_2args_1()
  {
    System.out.println("getAttribute 2args1");
    ExpandableObject instance = new ExpandableObject();
    instance.setAttribute("int", 1);
    instance.setAttribute("double", 3.14159265359);

    // Test ClassCastException
    instance.getAttribute("int", Double.TYPE);

    assertNull(instance.getAttribute("string", String.class));

    assertEquals(instance.getAttribute("int", Integer.TYPE), Integer.valueOf(1));
    assertEquals((Double)instance.getAttribute("double", Double.TYPE), (Double)3.14159265359D);
  }

  @Test(expectedExceptions =
  {
    ClassCastException.class
  })
  public void testGetAttribute_1args_1()
  {
    System.out.println("getAttribute 1args1");
    ExpandableObject instance = new ExpandableObject();
    instance.setAttribute("int", 1);
    instance.setAttribute("double", 3.14159265359);

    // Test ClassCastException
    instance.getAttribute(Attribute.of("int", Double.TYPE));

    assertNull(instance.getAttribute(Attribute.of("AutobotsRollOut!", Integer.TYPE)));

    assertEquals(instance.getAttribute(Attribute.of("int", Integer.TYPE)), Integer.valueOf(1));
    assertEquals((Double)instance.getAttribute(Attribute.of("double", Double.TYPE)), (Double)3.14159265359D);
  }

  @Test(expectedExceptions =
  {
    ClassCastException.class
  })
  public void testGetAttribute_3args_1()
  {
    System.out.println("getAttribute 3args1");
    ExpandableObject instance = new ExpandableObject();
    instance.setAttribute("int", 1);
    instance.setAttribute("double", 3.14159265359);

    // Test ClassCastException
    instance.getAttribute("int", Double.TYPE, null);

    assertNull(instance.getAttribute("Ohana means family. Family means no one gets left behind.", String.class, null));

    assertEquals(instance.getAttribute("int", Integer.TYPE, null), Integer.valueOf(1));
    assertEquals((Double)instance.getAttribute("double", Double.TYPE, null), (Double)3.14159265359D);
    assertEquals(instance.getAttribute("You are who you choose to be", String.class, "Superman"), "Superman");
  }

  /**
   * Test of getAttributes method, of class ExpandableObject.
   */
  @Test
  public void testGetAttributes()
  {
    System.out.println("getAttributes");
    ExpandableObject instance = new ExpandableObject();
    assertEquals(instance.getAttributes().size(), 0);

    instance.setAttribute("int", 1);
    instance.setAttribute("double", 3.14159265359);

    TreeMap<String, Serializable> controlTreeMap = new TreeMap<>();
    controlTreeMap.put("int", 1);
    controlTreeMap.put("double", 3.14159265359);

    assertEquals(instance.getAttributes(), controlTreeMap);
  }

  @Test
  public void testhasAttribute()
  {
    System.out.println("hasAttribute");
    ExpandableObject instance = new ExpandableObject();
    instance.setAttribute("int", 1);

    assertTrue(instance.hasAttribute("int"));
    assertTrue(instance.hasAttribute("True") == false);
  }

  @Test
  public void testRemoveAttribute()
  {
    System.out.println("removeAttribute");
    ExpandableObject instance = new ExpandableObject();
    instance.setAttribute("int", 1);

    assertEquals(instance.getAttribute("int"), 1);
    instance.removeAttribute("int");
    assertNull(instance.getAttribute("int"));
  }

  @Test
  public void testCopyAttributes()
  {
    System.out.println("copyAttributes");
    ExpandableObject instance = new ExpandableObject();
    instance.setAttribute("int", 1);

    ExpandableObject other = new ExpandableObject();
    other.copyAttributes(instance);

    assertEquals(other.getAttributes(), instance.getAttributes());
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