/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.utility.options;

import java.io.PrintStream;
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
public class OptionParserNGTest
{
  
  public OptionParserNGTest()
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
   * Test of accepts method, of class OptionParser.
   */
  @Test
  public void testAccepts()
  {
    System.out.println("accepts");
    String key = "";
    String[] params = null;
    OptionParser instance = new OptionParser();
    OptionSpecBuilder expResult = null;
    OptionSpecBuilder result = instance.accepts(key, params);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of parse method, of class OptionParser.
   */
  @Test
  public void testParse() throws Exception
  {
    System.out.println("parse");
    String[] argv = null;
    OptionParser instance = new OptionParser();
    OptionSet expResult = null;
    OptionSet result = instance.parse(argv);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of printHelpOn method, of class OptionParser.
   */
  @Test
  public void testPrintHelpOn()
  {
    System.out.println("printHelpOn");
    PrintStream out = null;
    OptionParser instance = new OptionParser();
    instance.printHelpOn(out);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of formatHelpWith method, of class OptionParser.
   */
  @Test
  public void testFormatHelpWith()
  {
    System.out.println("formatHelpWith");
    HelpFormatter formatter = null;
    OptionParser instance = new OptionParser();
    instance.formatHelpWith(formatter);
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