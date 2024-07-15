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

import static org.testng.Assert.*;
import org.testng.annotations.Test;

/**
 *
 * @author pham21
 */
public class AttributeContentsNGTest
{

  public AttributeContentsNGTest()
  {
  }

  /**
   * Test of getAttributes method, of class AttributeContents.
   */
  @Test
  public void testGetAttributes()
  {
    System.out.println("getAttributes");
    org.xml.sax.helpers.AttributesImpl attributes = new org.xml.sax.helpers.AttributesImpl();
    AttributeContents instance = new AttributeContents(attributes);
    assertSame(instance.getAttributes(), attributes);
  }

  /**
   * Test of getContents method, of class AttributeContents.
   */
  @Test
  public void testGetContents()
  {
    System.out.println("getContents");
    AttributeContents instance = new AttributeContents(null);
    instance.contents = "contents";
    assertEquals(instance.getContents(), "contents");
  }

  /**
   * Test of AttributesImpl static class, of class AttributeContents.
   */
  @Test
  public void testAttributesImpl()
  {
    System.out.println("AttributesImpl");
    org.xml.sax.helpers.AttributesImpl attributes = new org.xml.sax.helpers.AttributesImpl();
    attributes.addAttribute("namespace0", "key0", "qname", "type", "value0");
    attributes.addAttribute("namespace1", "key1", "qname", "type", "value1");
    attributes.addAttribute("namespace2", "key2", "qname", "type", "value2");

    AttributeContents.AttributesImpl instance = new AttributeContents.AttributesImpl(attributes);

    assertNotNull(instance.namespace);
    assertNotNull(instance.key);
    assertNotNull(instance.value);
    assertTrue(instance.namespace.length == 3);
    assertTrue(instance.key.length == 3);
    assertTrue(instance.getLength() == 3);

    for (int i = 0; i < 3; ++i)
    {
      assertEquals(instance.getURI(i), "namespace" + i);
      assertEquals(instance.getLocalName(i), "key" + i);
      assertEquals(instance.getValue(i), "value" + i);

      assertEquals(instance.getIndex("key" + i), i);
      assertEquals(instance.getValue("key" + i), "value" + i);
    }

    assertTrue(instance.getIndex("a") == -1);
    assertNull(instance.getValue("a"));

    // UnsupportedOperationException
    try
    {
      instance.getQName(0);
    }
    catch (Exception ex)
    {
      assertEquals(ex.getClass(), UnsupportedOperationException.class);
    }

    try
    {
      instance.getType(0);
    }
    catch (Exception ex)
    {
      assertEquals(ex.getClass(), UnsupportedOperationException.class);
    }

    try
    {
      instance.getIndex("", "");
    }
    catch (Exception ex)
    {
      assertEquals(ex.getClass(), UnsupportedOperationException.class);
    }

    try
    {
      instance.getType("", "");
    }
    catch (Exception ex)
    {
      assertEquals(ex.getClass(), UnsupportedOperationException.class);
    }

    try
    {
      instance.getType("");
    }
    catch (Exception ex)
    {
      assertEquals(ex.getClass(), UnsupportedOperationException.class);
    }

    try
    {
      instance.getValue("", "");
    }
    catch (Exception ex)
    {
      assertEquals(ex.getClass(), UnsupportedOperationException.class);
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