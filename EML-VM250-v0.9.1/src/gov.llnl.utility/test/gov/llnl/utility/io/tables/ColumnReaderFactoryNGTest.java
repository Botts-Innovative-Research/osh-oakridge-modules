/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.utility.io.tables;

import java.io.InputStream;
import java.nio.file.Path;
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
public class ColumnReaderFactoryNGTest
{
  
  public ColumnReaderFactoryNGTest()
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
   * Test of setHasHeader method, of class ColumnReaderFactory.
   */
  @Test
  public void testSetHasHeader()
  {
    System.out.println("setHasHeader");
    boolean b = false;
    ColumnReaderFactory instance = new ColumnReaderFactory();
    instance.setHasHeader(b);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setSkipRowCount method, of class ColumnReaderFactory.
   */
  @Test
  public void testSetSkipRowCount()
  {
    System.out.println("setSkipRowCount");
    int count = 0;
    ColumnReaderFactory instance = new ColumnReaderFactory();
    instance.setSkipRowCount(count);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setCommentPattern method, of class ColumnReaderFactory.
   */
  @Test
  public void testSetCommentPattern()
  {
    System.out.println("setCommentPattern");
    String pattern = "";
    ColumnReaderFactory instance = new ColumnReaderFactory();
    instance.setCommentPattern(pattern);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of openFile method, of class ColumnReaderFactory.
   */
  @Test
  public void testOpenFile() throws Exception
  {
    System.out.println("openFile");
    Path path = null;
    ColumnReaderFactory instance = new ColumnReaderFactory();
    ColumnReader expResult = null;
    ColumnReader result = instance.openFile(path);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of openStream method, of class ColumnReaderFactory.
   */
  @Test
  public void testOpenStream() throws Exception
  {
    System.out.println("openStream");
    InputStream is = null;
    ColumnReaderFactory instance = new ColumnReaderFactory();
    ColumnReader expResult = null;
    ColumnReader result = instance.openStream(is);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of addField method, of class ColumnReaderFactory.
   */
  @Test
  public void testAddField()
  {
    System.out.println("addField");
    int begin = 0;
    int end = 0;
    ColumnReaderFactory instance = new ColumnReaderFactory();
    ColumnReader.Field expResult = null;
    ColumnReader.Field result = instance.addField(begin, end);
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