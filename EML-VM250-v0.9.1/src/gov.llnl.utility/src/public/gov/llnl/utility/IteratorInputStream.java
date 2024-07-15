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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.function.Function;

/**
 *
 * @author nelson85
 */
public class IteratorInputStream<T> extends InputStream
{
  private final Iterator<T> src;
  private final Function<T, byte[]> mapper;
  private InputStream backer;

  /**
   * Create an input stream from a String iterator.
   *
   * @param src
   * @return
   */
  public static InputStream create(Iterator<String> src)
  {
    return new IteratorInputStream<>(src, (String s) -> s.getBytes());
  }

  /**
   * Create an input stream from a String iterable.
   *
   * @param src
   * @return
   */
  public static InputStream create(Iterable<String> src)
  {
    return new IteratorInputStream<>(src.iterator(), (String s) -> s.getBytes());
  }

  /**
   * Create an input Stream from an arbitrary source of bytes.
   *
   * @param src
   * @param mapper
   */
  public IteratorInputStream(Iterator<T> src, Function<T, byte[]> mapper)
  {
    this.src = src;
    this.mapper = mapper;
  }

  @Override
  public int read() throws IOException
  {
    // Fetch a backer from the source if we do not have one.
    if (backer == null)
      backer = next();

    // While we have yet to find bytes to use.
    while (backer != null)
    {
      // Get the next byte from the buffer
      int c = backer.read();

      // If it is eof, then switch to the next source.
      if (c == -1)
        backer = next();
      else
        return c;
    }

    // Otherwise we are at eof.
    return -1;
  }

  @Override
  public int read(byte b[], int off, int len) throws IOException 
  {
    // Fetch a backer from the source if we do not have one.
    if (backer == null)
      backer = next();
    
    while (backer != null)
    {
      // Get the next byte from the buffer
      int c = backer.read(b, off, len);
      
      // If it is eof, then switch to the next source.
      if (c == -1)
        backer = next();
      else
        return c;
    }
    return -1;
  }
  /**
   * Internal method to get the next source in the iterator.
   *
   * @return
   */
  private InputStream next()
  {
    while (this.src.hasNext())
    {
      byte[] contents = mapper.apply(this.src.next());
      if (contents == null)
        continue;
      return new ByteArrayInputStream(contents);
    }
    return null;
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