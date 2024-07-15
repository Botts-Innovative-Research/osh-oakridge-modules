/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.utility.io;

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
public class MD5FilterInputStreamNGTest
{
  
  public MD5FilterInputStreamNGTest()
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
   * Test of read method, of class MD5FilterInputStream.
   */
  @Test
  public void testRead_0args() throws Exception
  {
    System.out.println("read");
    MD5FilterInputStream instance = null;
    int expResult = 0;
    int result = instance.read();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of read method, of class MD5FilterInputStream.
   */
  @Test
  public void testRead_byteArr() throws Exception
  {
    System.out.println("read");
    byte[] b = null;
    MD5FilterInputStream instance = null;
    int expResult = 0;
    int result = instance.read(b);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of read method, of class MD5FilterInputStream.
   */
  @Test
  public void testRead_3args() throws Exception
  {
    System.out.println("read");
    byte[] b = null;
    int off = 0;
    int len = 0;
    MD5FilterInputStream instance = null;
    int expResult = 0;
    int result = instance.read(b, off, len);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getChecksum method, of class MD5FilterInputStream.
   */
  @Test
  public void testGetChecksum()
  {
    System.out.println("getChecksum");
    MD5FilterInputStream instance = null;
    String expResult = "";
    String result = instance.getChecksum();
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