/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.utility.options;

import gov.llnl.utility.internal.options.OptionSetImpl;
import gov.llnl.utility.internal.options.OptionSpecImpl;
import gov.llnl.utility.ClassUtilities;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Implementation of options roughly modeled on joptsimple.
 *
 * @author nelson85
 */
public class OptionParser
{
  List<OptionSpecImpl> options_ = new ArrayList<>();
  private HelpFormatter formatter;

  public OptionSpecBuilder accepts(String key, String... params)
  {
    String help = null;
    List<String> keys = new ArrayList<>();

    // decide if we have accepts(key), accepts(key, help) or accepts(key1, key2, ..., help)
    keys.add(key);
    if (params.length == 0)
    {
    }
    else if (params.length == 1)
    {
      help = params[0];
    }
    else
    {
      help = params[params.length - 1];
      keys.addAll(Arrays.asList(params).subList(0, params.length - 1));
    }

    for (int i = 0; i < keys.size(); ++i)
    {
      key = keys.get(i);

      if (key.startsWith("--"))
        continue;
      if (key.startsWith("-") && key.length() == 2)
        continue;
      if (key.length() == 1)
      {
        // Imply short option
        key = '-' + key;
      }
      else
      {
        // Imply long option
        key = "--" + key;
      }

      keys.set(i, key);
    }

    OptionSpecImpl out = new OptionSpecImpl(keys, help);
    this.options_.add(out);
    return out;
  }

  /**
   * Parse a command line into options.
   *
   * @param argv is a list of options to parse.
   * @return an OptionSet holding all of the specified options.
   * @throws OptionException is there is a problem with the options parsed.
   */
  public OptionSet parse(String... argv) throws OptionException
  {
    int argc = argv.length;
    int index;
    String optKey;
    String optValue = null;
    OptionSetImpl out = new OptionSetImpl();

    OUTER:
    for (index = 0; index < argc; ++index)
    {
      boolean hasValue = false;
      boolean isShort = false;
      String arg = argv[index];
      if (!arg.startsWith("-"))
      {
        break;
      }
      else
      {
        if (arg.length() == 2)
        {
          // handle -- 
          if (arg.equals("--"))
            break;

          // handle -[A-z]
          isShort = true;
          optKey = arg.substring(1);
        }
        else if (arg.length() == 1)
        {
          // has only "-"
          break;
        }
        else
        {
          optKey = arg.substring(2);
        }

        //int eq;
        //eq = opt.find('=');
        int eq = optKey.indexOf("=");

        if (eq > 0)
        {
          // opt= "FOO=BAR" => opt="FOO" , value="BAR"
          optValue = optKey.substring(eq + 1);
          optKey = optKey.substring(0, eq);
          hasValue = true;
        }

        if (isShort)
        {
          // Handle a series of short options.
          for (char c : optKey.toCharArray())
          {
            OptionSpecImpl entry = null;
            for (OptionSpecImpl opt : options_)
            {
              if (opt.accepts("-" + optKey))
              {
                entry = opt;
                break;
              }
            }
            if (entry.requiresArgument())
              throw new OptionException(String.format("Argument required for opt (%s)", optKey));
            out.put(entry, entry.getDefaultValue());
          }
          continue;
        }

        OptionSpecImpl entry = null;
        for (OptionSpecImpl opt : options_)
        {
          if (opt.accepts("--" + optKey))
          {
            entry = opt;
            break;
          }
        }

        // User specified a value that does not exist for this program
        if (entry == null)
        {
          throw new OptionException(String.format("Invalid option. (%s)", optKey));
        }

        // If the option required a value we must look for either
        //  --option=value or  --option value forms
        Object value = entry.getDefaultValue();
        if (entry.acceptsArguments())
        {
          if (hasValue)
          {
            ;
          }
          else if (index < argc - 1)
          {
            index++;
            // iter -> second.value = argv[index];
            optValue = argv[index];
          }
          else
          {
            throw new OptionException(String.format("Option requires argument. (%s)", optKey));
          }

          if (optValue == null && entry.requiresArgument())
            throw new OptionException(String.format("Argument required for opt (%s)", optKey));
          value = ClassUtilities
                  .newValueOf(entry.getArgumentClass())
                  .valueOf(optValue);
        }
        else
        {
          if (optValue != null)
            throw new OptionException(String.format("Argument specified for option (%s)", optKey));
        }
        out.put(entry, value);
      }
    }

    // Handle required arguments
    for (OptionSpecImpl opt : options_)
    {
      if (opt.isRequired() && !out.has(opt))
        throw new OptionException("Required option " + opt.options().get(0) + " not specified");
    }

    String[] arguments_ = new String[0];
    if (index < argc)
    {
      arguments_ = new String[argc - index];
      for (int i = 0; i < argc - index; ++i)
      {
        arguments_[i] = String.valueOf(argv[index + i]);
      }
    }

    out.setArguments(Arrays.asList(arguments_));
    return out;
  }

  public void printHelpOn(PrintStream out)
  {
    if (formatter == null)
      formatter = new DefaultHelpFormatter();
    out.append(formatter.format(options_));
  }

  public void formatHelpWith(HelpFormatter formatter)
  {
    this.formatter = formatter;
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