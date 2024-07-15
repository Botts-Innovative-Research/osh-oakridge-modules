/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gov.llnl.ernie.vm250;

import gov.llnl.ernie.data.Record;
import gov.llnl.ernie.io.LaneDatabase;
import gov.llnl.ernie.vm250.data.VM250RecordInternal;
import java.io.PrintStream;
import java.sql.ResultSet;
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
public class VM250RecordDatabaseNGTest
{
  
  public VM250RecordDatabaseNGTest()
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
   * Test of getRecord method, of class VM250RecordDatabase.
   */
  @Test
  public void testGetRecord() throws Exception
  {
    System.out.println("getRecord");
    long segmentDescriptorId = 0L;
    VM250RecordDatabase instance = new VM250RecordDatabase();
    Record expResult = null;
    Record result = instance.getRecord(segmentDescriptorId);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of prepare method, of class VM250RecordDatabase.
   */
  @Test
  public void testPrepare() throws Exception
  {
    System.out.println("prepare");
    VM250RecordDatabase instance = new VM250RecordDatabase();
    instance.prepare();
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of fetchSegmentDescriptor method, of class VM250RecordDatabase.
   */
  @Test
  public void testFetchSegmentDescriptor() throws Exception
  {
    System.out.println("fetchSegmentDescriptor");
    VM250RecordInternal record = null;
    VM250RecordDatabase.RecordKey key = null;
    VM250RecordDatabase instance = new VM250RecordDatabase();
    instance.fetchSegmentDescriptor(record, key);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getLaneDatabase method, of class VM250RecordDatabase.
   */
  @Test
  public void testGetLaneDatabase()
  {
    System.out.println("getLaneDatabase");
    VM250RecordDatabase instance = new VM250RecordDatabase();
    LaneDatabase expResult = null;
    LaneDatabase result = instance.getLaneDatabase();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setLaneDatabase method, of class VM250RecordDatabase.
   */
  @Test
  public void testSetLaneDatabase()
  {
    System.out.println("setLaneDatabase");
    LaneDatabase laneDatabase = null;
    VM250RecordDatabase instance = new VM250RecordDatabase();
    instance.setLaneDatabase(laneDatabase);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of dumpResultSet method, of class VM250RecordDatabase.
   */
  @Test
  public void testDumpResultSet_ResultSet() throws Exception
  {
    System.out.println("dumpResultSet");
    ResultSet rs = null;
    VM250RecordDatabase instance = new VM250RecordDatabase();
    instance.dumpResultSet(rs);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of dumpResultSet method, of class VM250RecordDatabase.
   */
  @Test
  public void testDumpResultSet_ResultSet_PrintStream() throws Exception
  {
    System.out.println("dumpResultSet");
    ResultSet rs = null;
    PrintStream out = null;
    VM250RecordDatabase instance = new VM250RecordDatabase();
    instance.dumpResultSet(rs, out);
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