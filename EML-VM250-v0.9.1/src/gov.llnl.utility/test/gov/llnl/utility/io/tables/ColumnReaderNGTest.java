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
public class ColumnReaderNGTest
{
  
  public ColumnReaderNGTest()
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
   * Test of close method, of class ColumnReader.
   */
  @Test
  public void testClose() throws Exception
  {
    System.out.println("close");
    ColumnReader instance = new ColumnReader();
    instance.close();
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getFields method, of class ColumnReader.
   */
  @Test
  public void testGetFields()
  {
    System.out.println("getFields");
    ColumnReader instance = new ColumnReader();
    ColumnReader.Field[] expResult = null;
    ColumnReader.Field[] result = instance.getFields();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getField method, of class ColumnReader.
   */
  @Test
  public void testGetField()
  {
    System.out.println("getField");
    int fieldNumber = 0;
    ColumnReader instance = new ColumnReader();
    ColumnReader.Field expResult = null;
    ColumnReader.Field result = instance.getField(fieldNumber);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of findField method, of class ColumnReader.
   */
  @Test
  public void testFindField()
  {
    System.out.println("findField");
    String headerKey = "";
    ColumnReader instance = new ColumnReader();
    ColumnReader.Field expResult = null;
    ColumnReader.Field result = instance.findField(headerKey);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of next method, of class ColumnReader.
   */
  @Test
  public void testNext()
  {
    System.out.println("next");
    ColumnReader instance = new ColumnReader();
    ColumnReader.ColumnRecord expResult = null;
    ColumnReader.ColumnRecord result = instance.next();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of hasNext method, of class ColumnReader.
   */
  @Test
  public void testHasNext()
  {
    System.out.println("hasNext");
    ColumnReader instance = new ColumnReader();
    boolean expResult = false;
    boolean result = instance.hasNext();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of remove method, of class ColumnReader.
   */
  @Test
  public void testRemove()
  {
    System.out.println("remove");
    ColumnReader instance = new ColumnReader();
    instance.remove();
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