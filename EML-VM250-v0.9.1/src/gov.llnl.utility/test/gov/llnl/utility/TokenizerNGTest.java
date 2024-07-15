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

import gov.llnl.utility.Tokenizer.TokenException;
import gov.llnl.utility.internal.TokenizerImpl;
import static org.testng.Assert.*;
import org.testng.annotations.Test;

/**
 *
 * @author pham21
 */
public class TokenizerNGTest
{
  
  public TokenizerNGTest()
  {
  }

  /**
   * Test of create method, of class Tokenizer.
   */
  @Test
  public void testCreate()
  {
    System.out.println("create");     
    Tokenizer expResult = new TokenizerImpl("Hello");
    Tokenizer result = Tokenizer.create("Hello");
    assertEquals(result.matcher("Hello, World!").next().group(), 
            expResult.matcher("Hello, World!").next().group());
    
  }

  /**
   * Test of matcher method, of class Tokenizer.
   */
  @Test
  public void testTokenException()
  {
    try
    {
      throw new TokenException ("Test 1 of TokenException");
    }
    catch(TokenException ex)
    {
      assertEquals(ex.getClass(), TokenException.class);
      assertEquals(ex.getMessage(), "Test 1 of TokenException");
    }
    
    try
    {
      throw new TokenException ("Test 2 of TokenException", new Exception("Unit testing"));
    }
    catch(TokenException ex)
    {
        assertEquals(ex.getClass(), TokenException.class);
        assertEquals(ex.getMessage(), "Test 2 of TokenException");
        assertEquals(ex.getCause().getMessage(), "Unit testing");
    }    
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