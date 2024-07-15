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

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Map;
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
public class VariableSubstitutionNGTest
{
  
  public VariableSubstitutionNGTest()
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
   * Test of setOutput method, of class VariableSubstitution.
   */
  @Test
  public void testSetOutput_Writer()
  {
    System.out.println("setOutput");
    Writer writer = null;
    VariableSubstitution instance = new VariableSubstitution();
    instance.setOutput(writer);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setOutput method, of class VariableSubstitution.
   */
  @Test
  public void testSetOutput_OutputStream()
  {
    System.out.println("setOutput");
    OutputStream writer = null;
    VariableSubstitution instance = new VariableSubstitution();
    instance.setOutput(writer);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of execute method, of class VariableSubstitution.
   */
  @Test
  public void testExecute_String() throws Exception
  {
    System.out.println("execute");
    String input = "";
    VariableSubstitution instance = new VariableSubstitution();
    instance.execute(input);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of execute method, of class VariableSubstitution.
   */
  @Test
  public void testExecute_InputStream() throws Exception
  {
    System.out.println("execute");
    InputStream input = null;
    VariableSubstitution instance = new VariableSubstitution();
    instance.execute(input);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of put method, of class VariableSubstitution.
   */
  @Test
  public void testPut_String_String()
  {
    System.out.println("put");
    String key = "";
    String value = "";
    VariableSubstitution instance = new VariableSubstitution();
    VariableSubstitution expResult = null;
    VariableSubstitution result = instance.put(key, value);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of put method, of class VariableSubstitution.
   */
  @Test
  public void testPut_String_Object()
  {
    System.out.println("put");
    String key = "";
    Object value = null;
    VariableSubstitution instance = new VariableSubstitution();
    VariableSubstitution expResult = null;
    VariableSubstitution result = instance.put(key, value);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of put method, of class VariableSubstitution.
   */
  @Test
  public void testPut_String_int()
  {
    System.out.println("put");
    String key = "";
    int value = 0;
    VariableSubstitution instance = new VariableSubstitution();
    VariableSubstitution expResult = null;
    VariableSubstitution result = instance.put(key, value);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of substitute method, of class VariableSubstitution.
   */
  @Test
  public void testSubstitute()
  {
    System.out.println("substitute");
    String input = "";
    VariableSubstitution instance = new VariableSubstitution();
    String expResult = "";
    String result = instance.substitute(input);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of substituteString method, of class VariableSubstitution.
   */
  @Test
  public void testSubstituteString()
  {
    System.out.println("substituteString");
    String input = "";
    Map<String, String> substitutions = null;
    String expResult = "";
    String result = VariableSubstitution.substituteString(input, substitutions);
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