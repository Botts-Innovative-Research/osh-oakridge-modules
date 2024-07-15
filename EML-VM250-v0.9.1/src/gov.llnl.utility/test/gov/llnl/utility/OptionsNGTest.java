/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.utility;

import java.util.List;
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
public class OptionsNGTest
{
  
  public OptionsNGTest()
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
   * Test of getError method, of class Options.
   */
  @Test
  public void testGetError()
  {
    System.out.println("getError");
    Options instance = new Options();
    String expResult = "";
    String result = instance.getError();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getNumArguments method, of class Options.
   */
  @Test
  public void testGetNumArguments()
  {
    System.out.println("getNumArguments");
    Options instance = new Options();
    int expResult = 0;
    int result = instance.getNumArguments();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getArgument method, of class Options.
   */
  @Test
  public void testGetArgument()
  {
    System.out.println("getArgument");
    int num = 0;
    Options instance = new Options();
    String expResult = "";
    String result = instance.getArgument(num);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getArguments method, of class Options.
   */
  @Test
  public void testGetArguments()
  {
    System.out.println("getArguments");
    Options instance = new Options();
    List expResult = null;
    List result = instance.getArguments();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setOptionString method, of class Options.
   */
  @Test
  public void testSetOptionString()
  {
    System.out.println("setOptionString");
    String getopt_string = "";
    Options instance = new Options();
    instance.setOptionString(getopt_string);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of addOption method, of class Options.
   */
  @Test
  public void testAddOption_String()
  {
    System.out.println("addOption");
    String key = "";
    Options instance = new Options();
    instance.addOption(key);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of addOption method, of class Options.
   */
  @Test
  public void testAddOption_String_boolean()
  {
    System.out.println("addOption");
    String key = "";
    boolean has_argument = false;
    Options instance = new Options();
    instance.addOption(key, has_argument);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of addOption method, of class Options.
   */
  @Test
  public void testAddOption_3args()
  {
    System.out.println("addOption");
    String key = "";
    boolean has_argument = false;
    String default_value = "";
    Options instance = new Options();
    instance.addOption(key, has_argument, default_value);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of parse method, of class Options.
   */
  @Test
  public void testParse()
  {
    System.out.println("parse");
    String[] argv = null;
    Options instance = new Options();
    boolean expResult = false;
    boolean result = instance.parse(argv);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of isOptionSpecified method, of class Options.
   */
  @Test
  public void testIsOptionSpecified()
  {
    System.out.println("isOptionSpecified");
    String key = "";
    Options instance = new Options();
    boolean expResult = false;
    boolean result = instance.isOptionSpecified(key);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getOptionValue method, of class Options.
   */
  @Test
  public void testGetOptionValue()
  {
    System.out.println("getOptionValue");
    String key = "";
    Options instance = new Options();
    String expResult = "";
    String result = instance.getOptionValue(key);
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