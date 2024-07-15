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
import org.testng.annotations.Test;

/**
 *
 * @author pham21
 */
public class TableExtractorNGTest
{
  
  public TableExtractorNGTest()
  {
  }

  /**
   * Test of create method, of class TableExtractor.
   */
  @Test
  public void testCreate()
  {
    System.out.println("create");
//    Object reader = null;
//    TableExtractor expResult = null;
//    TableExtractor result = TableExtractor.create(reader);
//    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of selectFields method, of class TableExtractor.
   */
  @Test
  public void testSelectFields()
  {
    System.out.println("selectFields");
    int[] col = null;
    TableExtractor instance = null;
    instance.selectFields(col);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of select method, of class TableExtractor.
   */
  @Test
  public void testSelect()
  {
    System.out.println("select");
    String[] names = null;
    TableExtractor instance = null;
    instance.select(names);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of fetchAll method, of class TableExtractor.
   */
  @Test
  public void testFetchAll()
  {
    System.out.println("fetchAll");
    TableExtractor instance = null;
    instance.fetchAll();
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of fetch method, of class TableExtractor.
   */
  @Test
  public void testFetch()
  {
    System.out.println("fetch");
    int max = 0;
    TableExtractor instance = null;
    instance.fetch(max);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of rows method, of class TableExtractor.
   */
  @Test
  public void testRows()
  {
    System.out.println("rows");
    TableExtractor instance = null;
    int expResult = 0;
    int result = instance.rows();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of columns method, of class TableExtractor.
   */
  @Test
  public void testColumns()
  {
    System.out.println("columns");
    TableExtractor instance = null;
    int expResult = 0;
    int result = instance.columns();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getRow method, of class TableExtractor.
   */
  @Test
  public void testGetRow()
  {
    System.out.println("getRow");
    int r = 0;
    TableExtractor instance = null;
    Object[] expResult = null;
    Object[] result = instance.getRow(r);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getColumn method, of class TableExtractor.
   */
  @Test
  public void testGetColumn()
  {
    System.out.println("getColumn");
    int c = 0;
    TableExtractor instance = null;
    Object expResult = null;
    Object result = instance.getColumn(c);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getData method, of class TableExtractor.
   */
  @Test
  public void testGetData()
  {
    System.out.println("getData");
    TableExtractor instance = null;
    Object[][] expResult = null;
    Object[][] result = instance.getData();
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