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
import gov.llnl.utility.annotation.Debug;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.regex.Pattern;

/**
 * Simple regular expression tokenizer. Instances of TokenizerImpl are thread
 * safe, but the matcher is not.
 *
 * @author nelson85
 */
public class TokenizerImpl extends Tokenizer
{

  TokenDef[] tokenDef;

  /**
   * Create a new Tokenizer with specified patterns.
   *
   * @param patterns
   */
  public TokenizerImpl(String... patterns)
  {
    compile(patterns);
  }

  final void compile(String[] patterns)
  {
    this.tokenDef = new TokenDef[patterns.length];
    for (int i = 0; i < patterns.length; ++i)
    {
      TokenDef token = new TokenDef();
      token.regex = "^" + patterns[i];
      token.pattern = Pattern.compile(token.regex);
      tokenDef[i] = token;
    }
  }

  /**
   * Creates a matcher for a string of tokens.
   *
   * @param string
   * @return a new matcher.
   */
  @Override
  public TokenMatcher matcher(String string)
  {
    return new StringTokenMatcher(string, tokenDef);
  }

  /**
   * Creates a matcher for a stream of tokens.
   *
   * @param stream
   * @return a new matcher.
   */
  @Override
  public TokenMatcher matcher(InputStream stream)
  {
    return new StreamTokenMatcher(stream, tokenDef);
  }

  /**
   * Display the list of token patterns that have been defined.
   *
   * @param out
   */
  @Debug
  public void dump(PrintStream out)
  {
    out.println("Tokenizer:");
    int i = 0;
    for (TokenDef token : this.tokenDef)
    {
      out.println("  " + i + " " + token.pattern);
      i++;
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