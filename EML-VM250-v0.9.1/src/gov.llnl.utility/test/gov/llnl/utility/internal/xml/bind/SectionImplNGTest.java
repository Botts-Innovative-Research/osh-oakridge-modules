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

import gov.llnl.utility.TestSupport.TestSectionImpl;
import static org.testng.Assert.*;
import org.testng.annotations.Test;

/**
 *
 * @author pham21
 */
public class SectionImplNGTest
{

  public SectionImplNGTest()
  {
  }

  /**
   * Test of start method, of class SectionImpl.
   */
  @Test
  public void testStart() throws Exception
  {
    System.out.println("start");
    assertNull(new TestSectionImpl().start(null));
    assertNull(new TestSectionImpl().start(new org.xml.sax.helpers.AttributesImpl()));
  }

  /**
   * Test of contents method, of class SectionImpl.
   */
  @Test
  public void testContents() throws Exception
  {
    System.out.println("contents");
    assertNull(new TestSectionImpl().contents("textContents"));
  }

  /**
   * Test of end method, of class SectionImpl.
   */
  @Test
  public void testEnd() throws Exception
  {
    System.out.println("end");
    assertNull(new TestSectionImpl().end());
  }

  /**
   * Test of getHandlerKey method, of class SectionImpl.
   */
  @Test
  public void testGetHandlerKey()
  {
    System.out.println("getHandlerKey");
    SectionImpl instance = new TestSectionImpl();
    assertEquals(instance.getHandlerKey(), "TestSectionImpl#http://utility.llnl.gov");
  }

  /**
   * Test of getXmlPrefix method, of class SectionImpl.
   */
  @Test
  public void testGetXmlPrefix()
  {
    System.out.println("getXmlPrefix");
    SectionImpl instance = new TestSectionImpl();
    assertEquals(instance.getXmlPrefix(), "TestSectionPrefix:");
  }

  /**
   * Test of getSchemaType method, of class SectionImpl.
   */
  @Test
  public void testGetSchemaType()
  {
    System.out.println("getSchemaType");
    SectionImpl instance = new TestSectionImpl();
    assertEquals(instance.getSchemaType(), "SectionImplNGTest-TestSectionImpl-type");
  }

  /**
   * Test of setContext method, of class SectionImpl.
   */
  @Test
  public void testSetContext()
  {
    System.out.println("setContext");
    SectionImpl instance = new TestSectionImpl();
    // Set null context
    instance.setContext(null);
    
    // Assigned context
    ReaderContextImpl rci = new ReaderContextImpl();
    instance.setContext(rci);
    assertNotNull(instance.getContext());
    
    // Assigned null
    instance.setContext(null);
    
    // Test RuntimeException
    instance.setContext(rci);
    ReaderContextImpl rci2 = new ReaderContextImpl();
    try
    {
      instance.setContext(rci2);
    }
    catch(RuntimeException re)
    {
      assertEquals(re.getMessage(), "reentrant issue ");
    }
  }

  /**
   * Test of getContext method, of class SectionImpl.
   */
  @Test
  public void testGetContext()
  {
    System.out.println("getContext");
    SectionImpl instance = new TestSectionImpl();
    ReaderContextImpl rci = new ReaderContextImpl();
    instance.setContext(rci);
    assertSame(instance.getContext(), rci);
  }

  /**
   * Test of createSchemaType method, of class SectionImpl.
   */
  @Test
  public void testCreateSchemaType() throws Exception
  {
    System.out.println("createSchemaType");
    // FIXME Need to review the method createSchemaType and how to test it
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
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