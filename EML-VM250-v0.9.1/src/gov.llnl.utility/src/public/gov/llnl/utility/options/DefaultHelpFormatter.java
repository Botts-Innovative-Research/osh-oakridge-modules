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

import java.util.List;

/**
 *
 * @author nelson85
 */
public class DefaultHelpFormatter implements HelpFormatter
{
  @Override
  public String format(List<? extends OptionDescriptor> options)
  {
    StringBuilder sb = new StringBuilder();
    for (OptionDescriptor opt : options)
    {
      List<String> keys = opt.options();
      int n = keys.size();
      String args="";
      if (opt.requiresArgument())
        args="=value";
      else if (opt.acceptsArguments())
        args="[=value]";
      if (keys.size() > 1)
      {
        for (int i = 0; i < n - 1; ++i)
        {
          sb.append(String.format("    %-10s", keys.get(i)+args));
          sb.append("\n");
        }
      }

      sb.append(String.format("    %-10s   ", keys.get(n - 1)+args));
      if (opt.isRequired())
      {
        sb.append("(required) ");
      }
      sb.append(opt.description());
      sb.append("\n");
      sb.append("\n");
    }
    return sb.toString();
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