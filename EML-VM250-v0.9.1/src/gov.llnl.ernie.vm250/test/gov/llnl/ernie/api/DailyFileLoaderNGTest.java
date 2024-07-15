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
package gov.llnl.ernie.api;

import gov.llnl.ernie.vm250.data.VM250Record;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;
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
public class DailyFileLoaderNGTest
{
  
  public DailyFileLoaderNGTest()
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
   * Test of load method, of class DailyFileLoader.
   */
  @Test
  public void testLoad_String() throws Exception
  {
    System.out.println("load");
    String filename = "";
    DailyFileLoader instance = new DailyFileLoader();
    VM250Record expResult = null;
    //VM250Record result = instance.load(filename);
    //assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of load method, of class DailyFileLoader.
   */
  @Test
  public void testLoad_Stream()
  {
    System.out.println("load");
    Stream input = null;
    DailyFileLoader instance = new DailyFileLoader();
    VM250Record expResult = null;
    //VM250Record result = instance.load(input);
    //assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of load method, of class DailyFileLoader.
   */
  @Test
  public void testLoad_String_String() throws Exception
  {
    System.out.println("load");
    String filename = "";
    String vm250LaneDatabaseXml = "";
    DailyFileLoader instance = new DailyFileLoader();
    List expResult = null;
    List result = instance.load(filename, vm250LaneDatabaseXml);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of load method, of class DailyFileLoader.
   */
  @Test
  public void testLoad_3args_1() throws Exception
  {
    System.out.println("load");
    String filename = "";
    Instant baseTimestamp = null;
    String vm250LaneDatabaseXml = "";
    DailyFileLoader instance = new DailyFileLoader();
    List expResult = null;
    List result = instance.load(filename, baseTimestamp, vm250LaneDatabaseXml);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of load method, of class DailyFileLoader.
   */
  @Test
  public void testLoad_Stream_Path() throws Exception
  {
    System.out.println("load");
    Stream<String> input = null;
    Path vm250LaneDatabaseXmlPath = null;
    DailyFileLoader instance = new DailyFileLoader();
    List expResult = null;
    List result = instance.load(input, vm250LaneDatabaseXmlPath);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of load method, of class DailyFileLoader.
   */
  @Test
  public void testLoad_3args_2() throws Exception
  {
    System.out.println("load");
    Stream<String> input = null;
    Instant baseTimestamp = null;
    Path vm250LaneDatabaseXmlPath = null;
    DailyFileLoader instance = new DailyFileLoader();
    List expResult = null;
    List result = instance.load(input, baseTimestamp, vm250LaneDatabaseXmlPath);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of loadFirstOccupancy method, of class DailyFileLoader.
   */
  @Test
  public void testLoadFirstOccupancy_String() throws Exception
  {
    System.out.println("loadFirstOccupancy");
    String filename = "";
    DailyFileLoader instance = new DailyFileLoader();
    VM250Record expResult = null;
//    VM250Record result = instance.loadFirstOccupancy(filename);
//    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of loadFirstOccupancy method, of class DailyFileLoader.
   */
  @Test
  public void testLoadFirstOccupancy_Stream()
  {
    System.out.println("loadFirstOccupancy");
    Stream input = null;
    DailyFileLoader instance = new DailyFileLoader();
    VM250Record expResult = null;
//    VM250Record result = instance.loadFirstOccupancy(input);
//    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of main method, of class DailyFileLoader.
   */
  @Test
  public void testMain() throws Exception
  {
    System.out.println("main");
    String[] args = null;
    DailyFileLoader.main(args);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of loadFirstOccupancy method, of class DailyFileLoader.
   */
  @Test
  public void testLoadFirstOccupancy_String_String() throws Exception
  {
    System.out.println("loadFirstOccupancy");
    String filename = "";
    String vm250LaneDatabaseXml = "";
    DailyFileLoader instance = new DailyFileLoader();
    VM250Record expResult = null;
    VM250Record result = instance.loadFirstOccupancy(filename, vm250LaneDatabaseXml);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of loadFirstOccupancy method, of class DailyFileLoader.
   */
  @Test
  public void testLoadFirstOccupancy_Stream_Path() throws Exception
  {
    System.out.println("loadFirstOccupancy");
    Stream input = null;
    Path vm250LaneDatabaseXmlPath = null;
    DailyFileLoader instance = new DailyFileLoader();
    VM250Record expResult = null;
    VM250Record result = instance.loadFirstOccupancy(input, vm250LaneDatabaseXmlPath);
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