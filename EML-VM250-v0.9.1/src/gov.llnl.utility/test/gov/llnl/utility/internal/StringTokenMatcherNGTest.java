/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.utility.internal;

import gov.llnl.utility.Tokenizer;
import java.util.Iterator;
import java.util.regex.Pattern;
import static org.testng.Assert.*;
import org.testng.annotations.Test;

/**
 *
 * @author pham21
 */
public class StringTokenMatcherNGTest
{

  public StringTokenMatcherNGTest()
  {
  }

  /**
   * Test of next method, of class StringTokenMatcher.
   */
  @Test(expectedExceptions =
  {
    Tokenizer.TokenException.class
  })
  public void testNext()
  {
    System.out.println("next");
    String str = "Hello, World!";
    TokenDef[] tokenDefArray = new TokenDef[]
    {
      new TokenDef()
      {
        {
          regex = ".*?\\bHello\\b.*?";
          pattern = Pattern.compile(regex);
        }
      },
      new TokenDef()
      {
        {
          regex = ".*?\\bWorld\\b.*?";
          pattern = Pattern.compile(regex);
        }
      },
      new TokenDef()
      {
        {
          regex = ".*?!$";
          pattern = Pattern.compile(regex);
        }
      }
    };
    StringTokenMatcher stringTokenMatcher = new StringTokenMatcher(str, tokenDefArray);

    TokenImpl tokenImpl = stringTokenMatcher.next();
    assertNotNull(tokenImpl);
    assertEquals(tokenImpl.group(), "Hello");
    tokenImpl = stringTokenMatcher.next();
    assertNotNull(tokenImpl);
    assertEquals(tokenImpl.group(), ", World");
    assertEquals(stringTokenMatcher.next().group(), "!");

    assertNull(stringTokenMatcher.next());

    // Test Tokenizer.TokenException
    stringTokenMatcher = new StringTokenMatcher("no match", tokenDefArray);
    stringTokenMatcher.next();
  }

  /**
   * Test of iterator method, of class StringTokenMatcher.
   */
  @Test(expectedExceptions =
  {
    UnsupportedOperationException.class
  })
  public void testIterator()
  {
    System.out.println("iterator");
    String str = "Hello, World!";
    TokenDef[] tokenDefArray = new TokenDef[]
    {
      new TokenDef()
      {
        {
          regex = ".*?\\bHello\\b.*?";
          pattern = Pattern.compile(regex);
        }
      },
      new TokenDef()
      {
        {
          regex = ".*?\\bWorld\\b.*?";
          pattern = Pattern.compile(regex);
        }
      },
      new TokenDef()
      {
        {
          regex = ".*?!$";
          pattern = Pattern.compile(regex);
        }
      }
    };
    StringTokenMatcher stringTokenMatcher = new StringTokenMatcher(str, tokenDefArray);

    Iterator<Tokenizer.Token> iterator = stringTokenMatcher.iterator();
    // Hello
    assertTrue(iterator.hasNext());
    Tokenizer.Token tokenImpl = iterator.next();
    assertNotNull(tokenImpl);
    assertEquals(tokenImpl.group(), "Hello");
    // World
    assertTrue(iterator.hasNext());
    tokenImpl = iterator.next();
    assertNotNull(tokenImpl);
    assertEquals(tokenImpl.group(), ", World");
    // !
    assertTrue(iterator.hasNext());
    tokenImpl = iterator.next();
    assertNotNull(tokenImpl);
    assertEquals(tokenImpl.group(), "!");

    assertEquals(iterator.hasNext(), false);
    assertNull(iterator.next());

    // Test UnsupportedOperationException
    iterator.remove();
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