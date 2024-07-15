/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.utility.internal;

import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
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
public class ByteArrayChannelNGTest
{
  
  public ByteArrayChannelNGTest()
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
   * Test of read method, of class ByteArrayChannel.
   */
  @Test
  public void testRead() throws Exception
  {
    System.out.println("read");
    ByteBuffer bb = null;
    ByteArrayChannel instance = null;
    int expResult = 0;
    int result = instance.read(bb);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of write method, of class ByteArrayChannel.
   */
  @Test
  public void testWrite() throws Exception
  {
    System.out.println("write");
    ByteBuffer bb = null;
    ByteArrayChannel instance = null;
    int expResult = 0;
    int result = instance.write(bb);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of position method, of class ByteArrayChannel.
   */
  @Test
  public void testPosition_0args() throws Exception
  {
    System.out.println("position");
    ByteArrayChannel instance = null;
    long expResult = 0L;
    long result = instance.position();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of position method, of class ByteArrayChannel.
   */
  @Test
  public void testPosition_long() throws Exception
  {
    System.out.println("position");
    long l = 0L;
    ByteArrayChannel instance = null;
    SeekableByteChannel expResult = null;
    SeekableByteChannel result = instance.position(l);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of size method, of class ByteArrayChannel.
   */
  @Test
  public void testSize() throws Exception
  {
    System.out.println("size");
    ByteArrayChannel instance = null;
    long expResult = 0L;
    long result = instance.size();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of truncate method, of class ByteArrayChannel.
   */
  @Test
  public void testTruncate() throws Exception
  {
    System.out.println("truncate");
    long l = 0L;
    ByteArrayChannel instance = null;
    SeekableByteChannel expResult = null;
    SeekableByteChannel result = instance.truncate(l);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of isOpen method, of class ByteArrayChannel.
   */
  @Test
  public void testIsOpen()
  {
    System.out.println("isOpen");
    ByteArrayChannel instance = null;
    boolean expResult = false;
    boolean result = instance.isOpen();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of close method, of class ByteArrayChannel.
   */
  @Test
  public void testClose() throws Exception
  {
    System.out.println("close");
    ByteArrayChannel instance = null;
    instance.close();
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