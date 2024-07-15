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

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Utilities for operating on strings. Currently only has methods for joining
 * strings.
 */
public class StringUtilities
{
  /**
   * Join a collection of strings with a delimiter.
   *
   * @param strings is the collection to join
   * @param delimiter is the delimiter to place between strings
   * @return the merged strings
   */
  static public String join(Iterable<String> strings, String delimiter)
  {
    StringBuilder sb = new StringBuilder();
    String next = "";
    for (String s : strings)
    {
      sb.append(next).append(s);
      next = delimiter;
    }
    return sb.toString();
  }

  /**
   * Join a array of strings with a delimiter. Convenience function to
   * {@link #join(java.lang.Iterable, java.lang.String)}
   *
   * @param strings is the array to join
   * @param delimiter is the delimiter to place between strings
   * @return the merged strings
   */
  static public String join(String[] strings, String delimiter)
  {
    return join(Arrays.asList(strings), delimiter);
  }

  static public String getFixedString(ByteBuffer bb, int length)
  {
    try
    {
      byte content[] = new byte[length];
      bb.get(content);
      return (new String(content, "UTF8")).trim();
    }
    catch (UnsupportedEncodingException ex)
    {
      throw new RuntimeException(ex);
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