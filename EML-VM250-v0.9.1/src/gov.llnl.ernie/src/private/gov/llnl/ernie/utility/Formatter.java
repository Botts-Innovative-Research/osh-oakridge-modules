/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.ernie.utility;

import java.util.HashMap;
import java.util.Properties;
import java.util.function.Function;

public class Formatter
{
  public static class FormatterException extends Exception
  {
    public FormatterException(String msg)
    {
      super(msg);
    }
  }

  /**
   * Creates a hashmap from an array of Strings. If the array is odd in length,
   * the last argument is ignored.
   *
   * @param str is the list of key,value pairs
   * @return
   */
  public static Function<String, String> mapFromStrings(String... str)
  {
    HashMap<String, String> hash = new HashMap<>();
    int n = (str.length & 0xfffffffe);
    for (int i = 0; i < n; i += 2)
    {
      hash.put(str[i], str[i + 1]);
    }
    return (String p) -> hash.get(p);
  }

  public static Function<String, String> mapFromProperties(Properties properties)
  {
    return (String p) -> properties.getProperty(p);
  }

  /**
   * Formats a string using substitution from a map.equivalent to python
   * string.format.
   *
   * @param maps
   * @throws gov.llnl.ernie.utility.Formatter.FormatterException
   */
  public static String format(String formatString,
          Function<String, String>... maps) throws FormatterException
  {
    StringBuilder buf = new StringBuilder();
    int i0 = 0;
    while (i0 < formatString.length())
    {
      int i1 = formatString.indexOf('{', i0);

      // No more replacements found, dump the remander
      if (i1 == -1)
      {
        break;
      }

      int i2 = formatString.indexOf('}', i0);
      if (i2 != -1 && i2 < i1)
      {
        i1 = i2;
      }

      // Dump up the the '{'
      buf.append(formatString.substring(i0, i1));

      char next = ' ';
      if (i1 + 1 < formatString.length())
      {
        next = formatString.charAt(i1 + 1);
      }

      // Check for '}}' or '{{'
      if (formatString.charAt(i1) == next)
      {
        buf.append(next);
        i0 = i1 + 2;
        continue;
      }

      // Error checks
      if (formatString.charAt(i1) == '}')
      {
        throw new FormatterException("Mismatched }");
      }
      if (next == ' ')
      {
        throw new FormatterException("Unterminated brace");
      }
      int i3 = formatString.indexOf('}', i1);
      if (i3 == -1)
      {
        throw new FormatterException("Unterminated brace");
      }

      // Lookup key
      String value = null;
      String key = formatString.substring(i1 + 1, i3);
      for (Function<String, String> map : maps)
      {
        value = map.apply(key);
        if (value != null)
          break;
      }

      if (value == null)
      {
        throw new FormatterException("Unable to find key " + key + " in " + formatString);
      }

      buf.append(value);
      i0 = i3 + 1;
    }

    // Finish off any remaining '}}'
    while (i0 < formatString.length())
    {
      int i1 = formatString.indexOf('}', i0);
      if (i1 == -1)
      {
        break;
      }

      // Dump up the the '{'
      buf.append(formatString.substring(i0, i1));

      char next = ' ';
      if (i1 + 1 < formatString.length())
      {
        next = formatString.charAt(i1 + 1);
      }

      if (formatString.charAt(i1) == next)
      {
        buf.append(next);
        i0 = i1 + 2;
        continue;
      }

      if (next == ' ')
      {
        throw new FormatterException("Unterminated brace");
      }

      throw new FormatterException("Mismatched }");
    }

    // Finish off the rest of the string
    if (i0 < formatString.length())
    {
      buf.append(formatString.substring(i0));
    }

    return buf.toString();
  }
};


/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */