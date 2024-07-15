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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import static org.testng.Assert.*;
import org.testng.annotations.Test;

/**
 *
 * @author pham21
 */
public class TokenizerImplNGTest
{

  public TokenizerImplNGTest()
  {
  }

  /**
   * Test of compile method, of class TokenizerImpl.
   */
  @Test
  public void testCompile()
  {
    System.out.println("compile");
    String[] patterns = new String[]
    {
      "\\\\ ", "\\\\.", " +", "[^\\\\ ]+"
    };
    TokenizerImpl instance = new TokenizerImpl();
    instance.compile(patterns);

    for (int i = 0; i < patterns.length; ++i)
    {
      TokenDef tokenDef = instance.tokenDef[i];
      assertEquals(tokenDef.regex, "^" + patterns[i]);
      assertEquals(tokenDef.pattern.pattern(), "^" + patterns[i]);
    }
  }

  /**
   * Test of matcher method, of class TokenizerImpl.
   */
  @Test
  public void testMatcher_String()
  {
    System.out.println("matcher");
    String parseThis = "this ${subst i,j,${} ${contents}}";
    String[] expStrArray = new String[]
    {
      "this ", "$", "{subst i,j,", "${}", " ", "${contents}", "}"
    };
    TokenizerImpl instance = new TokenizerImpl(
            "\\$\\{(?:(?:[^\\$}]*)(?:\\\\[$}])*)*?\\}",
            "\\\\\\$", "[^$\\\\]+", "[$]");

    Tokenizer.TokenMatcher matcher = instance.matcher(parseThis);
    int index = 0;
    for (Tokenizer.Token token : matcher)
    {
      assertEquals(token.group(), expStrArray[index]);
      ++index;
    }
  }

  /**
   * Test of matcher method, of class TokenizerImpl.
   */
  @Test
  public void testMatcher_InputStream()
  {
    System.out.println("matcher");
    String parseThis = "this ${subst i,j,${} ${contents}}";
    String[] expStrArray = new String[]
    {
      "this ", "$", "{subst i,j,", "${}", " ", "${contents}", "}"
    };
    TokenizerImpl instance = new TokenizerImpl(
            "\\$\\{(?:(?:[^\\$}]*)(?:\\\\[$}])*)*?\\}",
            "\\\\\\$", "[^$\\\\]+", "[$]");

    Tokenizer.TokenMatcher matcher = instance.matcher(new ByteArrayInputStream(parseThis.getBytes()));
    int index = 0;
    for (Tokenizer.Token token : matcher)
    {
      assertEquals(token.group(), expStrArray[index]);
      ++index;
    }
  }

  /**
   * Test of dump method, of class TokenizerImpl.
   */
  @Test
  public void testDump()
  {
    System.out.println("dump");
    
    String onemorelight = "Tokenizer:" + System.lineSeparator()
            + "  0 ^who" + System.lineSeparator()
            + "  1 ^cares" + System.lineSeparator()
            + "  2 ^if" + System.lineSeparator()
            + "  3 ^one" + System.lineSeparator()
            + "  4 ^more" + System.lineSeparator()
            + "  5 ^light" + System.lineSeparator()
            + "  6 ^goes" + System.lineSeparator()
            + "  7 ^out" + System.lineSeparator()
            + "  8 ^?" + System.lineSeparator()
            + "  9 ^well" + System.lineSeparator()
            + "  10 ^i" + System.lineSeparator()
            + "  11 ^do" + System.lineSeparator();
    TokenizerImpl instance = new TokenizerImpl(
            "who", "cares", "if", "one", "more", "light", "goes", "out", "?",
            "well", "i", "do");
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintStream ps = new PrintStream(baos);
    instance.dump(ps);
    assertEquals(baos.toString(), onemorelight);
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