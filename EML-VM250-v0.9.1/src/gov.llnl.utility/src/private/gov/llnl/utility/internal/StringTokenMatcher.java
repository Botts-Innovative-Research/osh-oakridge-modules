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
import gov.llnl.utility.Tokenizer.Token;
import java.util.Iterator;
import java.util.regex.Matcher;

/**
 * Breaks a string into a series of tokens. This uses the regular expression
 * library to create a token matcher. Created by calling
 * {@code TokenizerImpl.matcher}. StringTokenMatcher is not thread safe.
 *
 * @author nelson85
 */
public class StringTokenMatcher implements Tokenizer.TokenMatcher
{
  TokenImpl[] tokens;
  int start;
  int end;
  String target;

  StringTokenMatcher(String string, TokenDef[] tokenDef)
  {
    tokens = new TokenImpl[tokenDef.length];
    this.target = string;
    this.start = 0;
    this.end = string.length();
    for (int i = 0; i < tokenDef.length; ++i)
    {
      TokenImpl token = new TokenImpl();
      token.index = i;
      token.matcher = tokenDef[i].pattern.matcher(string);
      tokens[i] = token;
    }
  }

  /**
   * Get the next token. The contents of the previous extracted token is
   * destroyed, so capture all required information before the extracting the
   * next token.
   *
   * @return null if no tokens are available.
   * @throws gov.llnl.utility.Tokenizer.TokenException if the string contains
   * contents that can't be parsed into tokens.
   */
  @Override
  public TokenImpl next() throws Tokenizer.TokenException
  {
    if (start == end)
      return null;
    for (TokenImpl token : tokens)
    {
      Matcher matcher = token.matcher;
      matcher.region(start, end);
      if (matcher.lookingAt())
      {
        start = matcher.end();
        return token;
      }
    }
    throw new gov.llnl.utility.Tokenizer.TokenException("unable to find maching token at '"
            + target.substring(start, end) + "'");
  }

  /**
   * Get an iterator for extracting tokens.
   *
   * @return an iterator for all extracted tokens.
   */
  @Override
  public Iterator<Token> iterator()
  {
    return new TokenIterator();
  }

  class TokenIterator implements Iterator<Token>
  {
    TokenImpl token;
    boolean shouldAdvance = true;

    TokenIterator()
    {
    }

    @Override
    public boolean hasNext()
    {
      advance();
      return token != null;
    }

    @Override
    public TokenImpl next() throws Tokenizer.TokenException
    {
      advance();
      shouldAdvance = true;
      return token;
    }

    private void advance()
    {
      if (shouldAdvance)
      {
        shouldAdvance = false;
        token = StringTokenMatcher.this.next();
      }
    }

    @Override
    public void remove()
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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