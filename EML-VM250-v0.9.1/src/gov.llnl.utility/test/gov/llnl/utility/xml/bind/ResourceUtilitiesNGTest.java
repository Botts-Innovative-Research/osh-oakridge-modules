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

import java.io.InputStream;
import static org.testng.Assert.*;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 *
 * @author pham21
 */
public class ResourceUtilitiesNGTest
{
  
  public ResourceUtilitiesNGTest()
  {
  }

  @BeforeClass
  public static void setUpClass() throws Exception
  {
  }

  @AfterClass
  public static void tearDownClass() throws Exception
  {
  }

  @BeforeMethod
  public void setUpMethod() throws Exception
  {
  }

  @AfterMethod
  public void tearDownMethod() throws Exception
  {
  }

  /**
   * Test of openResourceStream method, of class ResourceUtilities.
   */
  @Test
  public void testOpenResourceStream_3args_1() throws Exception
  {
    System.out.println("openResourceStream");
    Class cls = null;
    String resource = "";
    boolean cached = false;
    InputStream expResult = null;
    InputStream result = ResourceUtilities.openResourceStream(cls, resource, cached);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of openResourceStream method, of class ResourceUtilities.
   */
  @Test
  public void testOpenResourceStream_3args_2() throws Exception
  {
    System.out.println("openResourceStream");
    ClassLoader classLoader = null;
    String resource = "";
    boolean cached = false;
    InputStream expResult = null;
    InputStream result = ResourceUtilities.openResourceStream(classLoader, resource, cached);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setDefaultUseCache method, of class ResourceUtilities.
   */
  @Test
  public void testSetDefaultUseCache()
  {
    System.out.println("setDefaultUseCache");
    boolean use = false;
    ResourceUtilities.setDefaultUseCache(use);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getDefaultUseCache method, of class ResourceUtilities.
   */
  @Test
  public void testGetDefaultUseCache()
  {
    System.out.println("getDefaultUseCache");
    boolean expResult = false;
    boolean result = ResourceUtilities.getDefaultUseCache();
    assertEquals(result, expResult);
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