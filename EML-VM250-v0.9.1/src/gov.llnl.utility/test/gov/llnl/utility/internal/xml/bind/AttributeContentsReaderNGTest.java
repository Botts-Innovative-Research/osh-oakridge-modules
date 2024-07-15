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
import org.xml.sax.Attributes;

/**
 *
 * @author pham21
 */
public class AttributeContentsReaderNGTest
{

  public AttributeContentsReaderNGTest()
  {
  }

  /**
   * Test of start method, of class AttributeContentsReader.
   */
  @Test
  public void testStart() throws Exception
  {
    System.out.println("start");
    org.xml.sax.helpers.AttributesImpl helpersAi = new org.xml.sax.helpers.AttributesImpl();
    helpersAi.addAttribute("uri0", "local0", "", "", "0");
    helpersAi.addAttribute("uri1", "local1", "", "", "1");
    helpersAi.addAttribute("uri2", "local2", "", "", "2");

    AttributeContentsReader instance = new AttributeContentsReader();
    AttributeContents ac = instance.start(helpersAi);

    Attributes attributes = ac.getAttributes();

    for (int i = 0; i < attributes.getLength(); ++i)
    {
      assertEquals(attributes.getURI(i), helpersAi.getURI(i));
      assertEquals(attributes.getLocalName(i), helpersAi.getLocalName(i));
      assertEquals(attributes.getValue(i), helpersAi.getValue(i));
    }
  }

  /**
   * Test of contents method, of class AttributeContentsReader.
   */
  @Test
  public void testContents() throws Exception
  {
    System.out.println("contents");
    ReaderContextImpl rci = new ReaderContextImpl();
    HandlerContextImpl hci = new HandlerContextImpl();
    AttributeContents ac = new AttributeContents(null);
    hci.targetObject = ac;
    rci.currentHandlerContext = hci;

    AttributeContentsReader instance = new AttributeContentsReader();
    instance.setContext(rci);

    assertNull(instance.contents("textContents"));
    assertEquals(ac.contents, "textContents");
  }

  /**
   * Test of getObjectClass method, of class AttributeContentsReader.
   */
  @Test
  public void testGetObjectClass()
  {
    System.out.println("getObjectClass");
    AttributeContentsReader instance = new AttributeContentsReader();
    assertEquals(instance.getObjectClass(), AttributeContents.class);
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