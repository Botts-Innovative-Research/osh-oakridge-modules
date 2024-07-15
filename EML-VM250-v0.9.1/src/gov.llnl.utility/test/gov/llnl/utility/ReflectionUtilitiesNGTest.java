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

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import static org.testng.Assert.*;
import org.testng.annotations.Test;

/**
 *
 * @author pham21
 */
public class ReflectionUtilitiesNGTest
{
  
  public ReflectionUtilitiesNGTest()
  {
  }
  
  /**
   * Test of convertMethod method, of class ReflectionUtilities.
   */
  @Test
  public void testConvertMethod()
  {
    System.out.println("convertMethod");
    Method method = null;
    BiConsumer expResult = null;
    BiConsumer result = ReflectionUtilities.convertMethod(method);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getMethod method, of class ReflectionUtilities.
   */
  @Test
  public void testGetMethod()
  {
    System.out.println("getMethod");
    String methodName = "";
//    Class<T> cls = null;
//    Class<A> argument = null;
//    BiConsumer expResult = null;
//    BiConsumer result = ReflectionUtilities.getMethod(methodName, cls, argument);
//    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getConstructor method, of class ReflectionUtilities.
   */
  @Test
  public void testGetConstructor_String_Class()
  {
    System.out.println("getConstructor");
//    String clsName = "";
//    Class<R> resultType = null;
//    Supplier expResult = null;
//    Supplier result = ReflectionUtilities.getConstructor(clsName, resultType);
//    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getConstructor method, of class ReflectionUtilities.
   */
  @Test
  public void testGetConstructor_3args()
  {
    System.out.println("getConstructor");
//    String clsName = "";
//    Class<R> resultType = null;
//    Class<A> argument = null;
//    Function expResult = null;
//    Function result = ReflectionUtilities.getConstructor(clsName, resultType, argument);
//    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of newInstance method, of class ReflectionUtilities.
   */
  @Test
  public void testNewInstance()
  {
    System.out.println("newInstance");
    String clsName = "";
    Object[] arguments = null;
    Object expResult = null;
    Object result = ReflectionUtilities.newInstance(clsName, arguments);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of argumentsMatch method, of class ReflectionUtilities.
   */
  @Test
  public void testArgumentsMatch()
  {
    System.out.println("argumentsMatch");
    Parameter[] parameters = null;
    Class[] argumentCls = null;
    boolean expResult = false;
    boolean result = ReflectionUtilities.argumentsMatch(parameters, argumentCls);
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