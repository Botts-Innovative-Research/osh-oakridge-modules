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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.regex.Matcher;

/**
 * Breaks a string into a series of tokens. This uses the regular expression
 * library to create a token matcher. Created by calling
 * {@code TokenizerImpl.matcher}. StringTokenMatcher is not thread safe.
 *
 * @author nelson85
 */
public class StreamTokenMatcher implements Tokenizer.TokenMatcher
{
  TokenImpl[] tokens;
  TokenDef[] tokenDef;
  LineReader reader;
  String target;
  int start, end, offset;

  StreamTokenMatcher(InputStream is, TokenDef[] tokenDef)
  {
    this.reader = new LineReader(new InputStreamReader(is));
    this.tokens = new TokenImpl[tokenDef.length];
    this.tokenDef = tokenDef;
  }

  /**
   * Get the next line of input
   *
   * @throws IOException
   */
  final void fetchLine() throws IOException
  {
    this.target = reader.readLine();
    if (this.target == null)
      return;

    for (int i = 0; i < tokens.length; ++i)
    {
      TokenImpl token = new TokenImpl();
      token.index = i;
      token.matcher = tokenDef[i].pattern.matcher(target);
      tokens[i] = token;
      tokens[i].offset = offset;
    }
    start = 0;
    end = this.target.length();
    offset += end;
  }

  /**
   * Get the readLine token. The contents of the previous extracted token is
   * destroyed, so capture all required information before the extracting the
   * readLine token.
   *
   * @return null if no tokens are available.
   * @throws Tokenizer.TokenException if the string contains contents that can't
   * be parsed into tokens.
   */
  @Override
  public TokenImpl next() throws Tokenizer.TokenException
  {
    while (start == end)
    {
      try
      {
        fetchLine();
        if (this.target == null)
          return null;
      }
      catch (IOException ex)
      {
        throw new Tokenizer.TokenException("Error in read", ex);
      }
    }

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
    throw new Tokenizer.TokenException("unable to find maching token at '"
            + target.substring(start, end) + "' at " + (offset + start) + " in line " + this.target);
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
        token = StreamTokenMatcher.this.next();
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