/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.utility.xml.bind;

import static org.testng.Assert.*;
import org.testng.annotations.Test;
import org.xml.sax.Attributes;

/**
 *
 * @author pham21
 */
public class AnyReaderNGTest
{

  public AnyReaderNGTest()
  {
  }

  /**
   * Test of AnyReader constructor, of class AnyReader.
   */
  @Test
  public void testConstructor()
  {
    System.out.println("AnyReader constructor");
    AnyReader instance = new AnyReader();
    assertNull(instance.cls);
    assertNull(instance.obj);
  }

  /**
   * Test of of method, of class AnyReader.
   */
  @Test
  public void testOf()
  {
    System.out.println("of");
    AnyReader result = AnyReader.of(String.class);
    assertNull(result.obj);
    assertEquals(result.cls, String.class);
  }

  /**
   * Test of start method, of class AnyReader.
   */
  @Test
  public void testStart() throws Exception
  {
    System.out.println("start");
    AnyReader instance = new AnyReader();
    instance.obj = "Hello";
    assertNotNull(instance.obj);
    assertNull(instance.start(null));
    assertNull(instance.obj);
  }

  /**
   * Test of end method, of class AnyReader.
   */
  @Test
  public void testEnd() throws Exception
  {
    System.out.println("end");
    AnyReader instance = new AnyReader();
    assertNull(instance.end());
    String str = "Me";
    instance.obj = str;
    assertSame(instance.end(), str);
  }

  /**
   * Test of getHandlers method, of class AnyReader.
   */
  @Test
  public void testGetHandlers() throws Exception
  {
    System.out.println("getHandlers");
    AnyReader instance = new AnyReader();
    instance.cls = Double.class;
    assertNotNull(instance.getHandlers());
  }

  /**
   * Test of getObjectClass method, of class AnyReader.
   */
  @Test
  public void testGetObjectClass()
  {
    System.out.println("getObjectClass");
    AnyReader instance = new AnyReader();
    assertNull(instance.getObjectClass());
    instance.cls = AnyReader.class;
    assertEquals(instance.getObjectClass(), AnyReader.class);
  }

  /**
   * Test of setObj method, of class AnyReader.
   */
  @Test
  public void testSetObj()
  {
    System.out.println("setObj");
    Object obj = new Object();
    AnyReader instance = new AnyReader();
    instance.setObj(obj);
    assertSame(instance.obj, obj);
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