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

import gov.llnl.utility.options.OptionSpec;
import java.util.List;
import static org.testng.Assert.*;
import org.testng.annotations.Test;

/**
 *
 * @author pham21
 */
public class OptionSetImplNGTest
{
  
  public OptionSetImplNGTest()
  {
  }

  /**
   * Test of find method, of class OptionSetImpl.
   */
  @Test
  public void testFind()
  {
    System.out.println("find");
    String option = "";
    OptionSetImpl instance = new OptionSetImpl();
    OptionSpec expResult = null;
    OptionSpec result = instance.find(option);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of has method, of class OptionSetImpl.
   */
  @Test
  public void testHas_String()
  {
    System.out.println("has");
    String option = "";
    OptionSetImpl instance = new OptionSetImpl();
    boolean expResult = false;
    boolean result = instance.has(option);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of has method, of class OptionSetImpl.
   */
  @Test
  public void testHas_OptionSpec()
  {
    System.out.println("has");
    OptionSpec spec = null;
    OptionSetImpl instance = new OptionSetImpl();
    boolean expResult = false;
    boolean result = instance.has(spec);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of valueOf method, of class OptionSetImpl.
   */
  @Test
  public void testValueOf_OptionSpec()
  {
    System.out.println("valueOf");
//    OptionSpec<T> spec = null;
//    OptionSetImpl instance = new OptionSetImpl();
//    Object expResult = null;
//    Object result = instance.valueOf(spec);
//    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of nonOptionArguments method, of class OptionSetImpl.
   */
  @Test
  public void testNonOptionArguments()
  {
    System.out.println("nonOptionArguments");
    OptionSetImpl instance = new OptionSetImpl();
    List expResult = null;
    List result = instance.nonOptionArguments();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of valueOf method, of class OptionSetImpl.
   */
  @Test
  public void testValueOf_String()
  {
    System.out.println("valueOf");
    String flag = "";
    OptionSetImpl instance = new OptionSetImpl();
    Object expResult = null;
    Object result = instance.valueOf(flag);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of put method, of class OptionSetImpl.
   */
  @Test
  public void testPut()
  {
    System.out.println("put");
    OptionSpecImpl entry = null;
    Object value = null;
    OptionSetImpl instance = new OptionSetImpl();
    instance.put(entry, value);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setArguments method, of class OptionSetImpl.
   */
  @Test
  public void testSetArguments()
  {
    System.out.println("setArguments");
    List<String> asList = null;
    OptionSetImpl instance = new OptionSetImpl();
    instance.setArguments(asList);
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