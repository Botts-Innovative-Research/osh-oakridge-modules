/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.utility.internal.options;

import java.util.List;
import static org.testng.Assert.*;
import org.testng.annotations.Test;

/**
 *
 * @author pham21
 */
public class OptionSpecImplNGTest
{
  
  public OptionSpecImplNGTest()
  {
  }

  /**
   * Test of required method, of class OptionSpecImpl.
   */
  @Test
  public void testRequired()
  {
    System.out.println("required");
    OptionSpecImpl instance = null;
    OptionSpecImpl expResult = null;
    OptionSpecImpl result = instance.required();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of withRequiredArg method, of class OptionSpecImpl.
   */
  @Test
  public void testWithRequiredArg()
  {
    System.out.println("withRequiredArg");
    OptionSpecImpl instance = null;
    OptionSpecImpl expResult = null;
    OptionSpecImpl result = instance.withRequiredArg();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of withOptionalArg method, of class OptionSpecImpl.
   */
  @Test
  public void testWithOptionalArg()
  {
    System.out.println("withOptionalArg");
    OptionSpecImpl instance = null;
    OptionSpecImpl expResult = null;
    OptionSpecImpl result = instance.withOptionalArg();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of defaultsTo method, of class OptionSpecImpl.
   */
  @Test
  public void testDefaultsTo()
  {
    System.out.println("defaultsTo");
    Object value = null;
    OptionSpecImpl instance = null;
    OptionSpecImpl expResult = null;
    OptionSpecImpl result = instance.defaultsTo(value);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of ofType method, of class OptionSpecImpl.
   */
  @Test
  public void testOfType()
  {
    System.out.println("ofType");
//    Class<T2> cls = null;
//    OptionSpecImpl instance = null;
//    OptionSpecImpl expResult = null;
//    OptionSpecImpl result = instance.ofType(cls);
//    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of accepts method, of class OptionSpecImpl.
   */
  @Test
  public void testAccepts()
  {
    System.out.println("accepts");
    String optKey = "";
    OptionSpecImpl instance = null;
    boolean expResult = false;
    boolean result = instance.accepts(optKey);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of description method, of class OptionSpecImpl.
   */
  @Test
  public void testDescription()
  {
    System.out.println("description");
    OptionSpecImpl instance = null;
    String expResult = "";
    String result = instance.description();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of requiresArgument method, of class OptionSpecImpl.
   */
  @Test
  public void testRequiresArgument()
  {
    System.out.println("requiresArgument");
    OptionSpecImpl instance = null;
    boolean expResult = false;
    boolean result = instance.requiresArgument();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of isRequired method, of class OptionSpecImpl.
   */
  @Test
  public void testIsRequired()
  {
    System.out.println("isRequired");
    OptionSpecImpl instance = null;
    boolean expResult = false;
    boolean result = instance.isRequired();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of acceptsArguments method, of class OptionSpecImpl.
   */
  @Test
  public void testAcceptsArguments()
  {
    System.out.println("acceptsArguments");
    OptionSpecImpl instance = null;
    boolean expResult = false;
    boolean result = instance.acceptsArguments();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of options method, of class OptionSpecImpl.
   */
  @Test
  public void testOptions()
  {
    System.out.println("options");
    OptionSpecImpl instance = null;
    List expResult = null;
    List result = instance.options();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getDefaultValue method, of class OptionSpecImpl.
   */
  @Test
  public void testGetDefaultValue()
  {
    System.out.println("getDefaultValue");
    OptionSpecImpl instance = null;
    Object expResult = null;
    Object result = instance.getDefaultValue();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getArgumentClass method, of class OptionSpecImpl.
   */
  @Test
  public void testGetArgumentClass()
  {
    System.out.println("getArgumentClass");
    OptionSpecImpl instance = null;
    Class expResult = null;
    Class result = instance.getArgumentClass();
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