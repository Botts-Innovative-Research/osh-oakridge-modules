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
public class DelimitedReaderFactoryNGTest
{
  
  public DelimitedReaderFactoryNGTest()
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
   * Test of setAutomaticFields method, of class DelimitedReaderFactory.
   */
  @Test
  public void testSetAutomaticFields()
  {
    System.out.println("setAutomaticFields");
    boolean b = false;
    DelimitedReaderFactory instance = new DelimitedReaderFactory();
    instance.setAutomaticFields(b);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setDelimiter method, of class DelimitedReaderFactory.
   */
  @Test
  public void testSetDelimiter()
  {
    System.out.println("setDelimiter");
    String delimiter = "";
    DelimitedReaderFactory instance = new DelimitedReaderFactory();
    instance.setDelimiter(delimiter);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setExcelSplitter method, of class DelimitedReaderFactory.
   */
  @Test
  public void testSetExcelSplitter()
  {
    System.out.println("setExcelSplitter");
    DelimitedReaderFactory instance = new DelimitedReaderFactory();
    instance.setExcelSplitter();
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setHasHeader method, of class DelimitedReaderFactory.
   */
  @Test
  public void testSetHasHeader()
  {
    System.out.println("setHasHeader");
    boolean b = false;
    DelimitedReaderFactory instance = new DelimitedReaderFactory();
    instance.setHasHeader(b);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setSkipRowCount method, of class DelimitedReaderFactory.
   */
  @Test
  public void testSetSkipRowCount()
  {
    System.out.println("setSkipRowCount");
    int count = 0;
    DelimitedReaderFactory instance = new DelimitedReaderFactory();
    instance.setSkipRowCount(count);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setCommentPattern method, of class DelimitedReaderFactory.
   */
  @Test
  public void testSetCommentPattern()
  {
    System.out.println("setCommentPattern");
    String pattern = "";
    DelimitedReaderFactory instance = new DelimitedReaderFactory();
    instance.setCommentPattern(pattern);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of openFile method, of class DelimitedReaderFactory.
   */
  @Test
  public void testOpenFile() throws Exception
  {
    System.out.println("openFile");
    Path path = null;
    DelimitedReaderFactory instance = new DelimitedReaderFactory();
    DelimitedReader expResult = null;
    DelimitedReader result = instance.openFile(path);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of openStream method, of class DelimitedReaderFactory.
   */
  @Test
  public void testOpenStream() throws Exception
  {
    System.out.println("openStream");
    InputStream is = null;
    DelimitedReaderFactory instance = new DelimitedReaderFactory();
    DelimitedReader expResult = null;
    DelimitedReader result = instance.openStream(is);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of addField method, of class DelimitedReaderFactory.
   */
  @Test
  public void testAddField()
  {
    System.out.println("addField");
    int column = 0;
    DelimitedReaderFactory instance = new DelimitedReaderFactory();
    DelimitedReader.Field expResult = null;
    DelimitedReader.Field result = instance.addField(column);
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