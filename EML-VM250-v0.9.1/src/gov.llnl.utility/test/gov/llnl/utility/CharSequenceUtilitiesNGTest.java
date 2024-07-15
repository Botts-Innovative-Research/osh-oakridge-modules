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

import java.util.function.Function;
import static org.testng.Assert.*;
import org.testng.annotations.Test;

/**
 *
 * @author pham21
 */
public class CharSequenceUtilitiesNGTest
{
  
  public CharSequenceUtilitiesNGTest()
  {
  }
  
  /**
   * Test of substitute method, of class CharSequenceUtilities.
   */
  @Test
  public void testSubstitute()
  {
    System.out.println("substitute");
    CharSequence str = "I felt happy because I saw the others were happy and because I knew I should feel happy, but I wasn’t really happy. - Roberto Bolano, 2666";
    String from = "happy";
    String to = "joy";
    int occurrenceStart = 0;
    int occurrenceEnd = -1;
    StringBuilder expResult = new StringBuilder("I felt joy because I saw the others were joy and because I knew I should feel joy, but I wasn’t really joy. - Roberto Bolano, 2666");
    StringBuilder result = CharSequenceUtilities.substitute(str, from, to, occurrenceStart, occurrenceEnd);
    assertEquals(result.toString(), expResult.toString());    
    assertNotEquals(result, expResult);
  }

  /**
   * Test of substituteFunction method, of class CharSequenceUtilities.
   */
  @Test
  public void testSubstituteFunction()
  {
    System.out.println("substituteFunction");
    Function<String, String> to = s -> "joy";
    CharSequence str = "I felt happy because I saw the others were happy and because I knew I should feel happy, but I wasn’t really happy. - Roberto Bolano, 2666";
    String from = "happy";
    int occurrenceStart = 0;
    int occurrenceEnd = -1;
    StringBuilder expResult = new StringBuilder("I felt joy because I saw the others were joy and because I knew I should feel joy, but I wasn’t really joy. - Roberto Bolano, 2666");
    StringBuilder result = CharSequenceUtilities.substituteFunction(str, from, to, occurrenceStart, occurrenceEnd);
    assertEquals(result.toString(), expResult.toString());
  }

  /**
   * Test of substituteAssign method, of class CharSequenceUtilities.
   */
  @Test
  public void testSubstituteAssign()
  {
    System.out.println("substituteAssign");
    StringBuilder str = new StringBuilder("I felt happy because I saw the others were happy and because I knew I should feel happy, but I wasn’t really happy. - Roberto Bolano, 2666");
    String from = "happy";
    String to = "joy";
    int occurrenceStart = 0;
    int occurrenceEnd = -1;
    StringBuilder expResult = new StringBuilder("I felt joy because I saw the others were joy and because I knew I should feel joy, but I wasn’t really joy. - Roberto Bolano, 2666");
    StringBuilder result = CharSequenceUtilities.substituteAssign(str, from, to, occurrenceStart, occurrenceEnd);
    assertEquals(result.toString(), expResult.toString());
    assertEquals(result, str);
    assertEquals(result.toString(), str.toString());
    
    result = CharSequenceUtilities.substituteAssign(str, "joy", null, occurrenceStart, occurrenceEnd);
    assertEquals(result, str);
    assertEquals(result.toString(), str.toString());
    assertEquals(result.toString(), "I felt  because I saw the others were  and because I knew I should feel , but I wasn’t really . - Roberto Bolano, 2666");
  }

  /**
   * Test of substituteFunctionAssign method, of class CharSequenceUtilities.
   */
  @Test
  public void testSubstituteFunctionAssign()
  {
    System.out.println("substituteFunctionAssign");
    StringBuilder str = new StringBuilder("I felt happy because I saw the others were happy and because I knew I should feel happy, but I wasn’t really happy. - Roberto Bolano, 2666");
    String from = "happy";
    Function<String, String> to = s -> "joy";
    int occurrenceStart = 0;
    int occurrenceEnd = -1;
    StringBuilder expResult = new StringBuilder("I felt joy because I saw the others were joy and because I knew I should feel joy, but I wasn’t really joy. - Roberto Bolano, 2666");
    StringBuilder result = CharSequenceUtilities.substituteFunctionAssign(str, from, to, occurrenceStart, occurrenceEnd);
    assertEquals(result.toString(), expResult.toString());
    assertEquals(result, str);
    assertEquals(result.toString(), str.toString());
  }

  /**
   * Test of translateAll method, of class CharSequenceUtilities.
   */
  @Test(expectedExceptions = { RuntimeException.class })
  public void testTranslateAll()
  {
    System.out.println("translateAll");
    CharSequence str = "I felt happy because I saw the others were happy and because I knew I should feel happy, but I wasn’t really happy. - Roberto Bolano, 2666";
    CharSequence from = "happy";
    CharSequence to = "HAPPY";
    StringBuilder expResult = new StringBuilder("I felt HAPPY becAuse I sAw tHe otHers were HAPPY And becAuse I knew I sHould feel HAPPY, but I wAsn’t reAllY HAPPY. - Roberto BolAno, 2666");
    StringBuilder result = CharSequenceUtilities.translateAll(str, from, to);
    assertEquals(result.toString(), expResult.toString());
    assertNotEquals(result, str);
        
    result = CharSequenceUtilities.translateAll(str, from, null);
    assertEquals(result.toString(), "I felt  becuse I sw te oters were  nd becuse I knew I sould feel , but I wsn’t rell . - Roberto Bolno, 2666");
    
    // Test RuntimeException
    CharSequenceUtilities.translateAll(str, "happy", "joy");
    
  }

  /**
   * Test of translateAllAssign method, of class CharSequenceUtilities.
   */
  @Test
  public void testTranslateAllAssign()
  {
    System.out.println("translateAllAssign");
    StringBuilder str = new StringBuilder("I felt happy because I saw the others were happy and because I knew I should feel happy, but I wasn’t really happy. - Roberto Bolano, 2666");
    CharSequence from = "happy";
    CharSequence to = "HAPPY";
    StringBuilder expResult = new StringBuilder("I felt HAPPY becAuse I sAw tHe otHers were HAPPY And becAuse I knew I sHould feel HAPPY, but I wAsn’t reAllY HAPPY. - Roberto BolAno, 2666");
    StringBuilder result = CharSequenceUtilities.translateAllAssign(str, from, to);
    assertEquals(result.toString(), expResult.toString());
    assertEquals(result, str);
    assertEquals(result.toString(), str.toString());    
   
    str = new StringBuilder("I felt happy because I saw the others were happy and because I knew I should feel happy, but I wasn’t really happy. - Roberto Bolano, 2666");
    result = CharSequenceUtilities.translateAllAssign(str, from, null);
    assertEquals(result.toString(), "I felt  becuse I sw te oters were  nd becuse I knew I sould feel , but I wsn’t rell . - Roberto Bolno, 2666");
    assertEquals(result, str);
    assertEquals(result.toString(), str.toString()); 
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