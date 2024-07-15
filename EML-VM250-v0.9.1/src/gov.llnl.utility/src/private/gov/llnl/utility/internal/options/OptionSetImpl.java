/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.utility.internal.options;

import gov.llnl.utility.options.OptionSet;
import gov.llnl.utility.options.OptionSpec;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author nelson85
 */
public class OptionSetImpl implements OptionSet
{
  Map<OptionSpec, Object> specified_ = new TreeMap<>((p1, p2) -> Integer.compare(p1.hashCode(), p2.hashCode()));
  List<String> arguments_;

  public OptionSpec find(String option)
  {
    if (!option.startsWith("-"))
    {
      if (option.length() == 1)
        option = "-" + option;
      else
        option = "--" + option;
    }
    for (OptionSpec opts : this.specified_.keySet())
    {
      if (opts.accepts(option))
        return opts;
    }
    return null;
  }

  @Override
  public boolean has(String option)
  {
    return has(find(option));
  }

  @Override
  public boolean has(OptionSpec spec)
  {
    if (spec == null)
      return false;
    return specified_.containsKey(spec);
  }

  @Override
  public <T> T valueOf(OptionSpec<T> spec)
  {
    if (spec == null)
      return null;
    return (T) specified_.get(spec);
  }

  @Override
  public List<String> nonOptionArguments()
  {
    return arguments_;
  }

  @Override
  public Object valueOf(String flag)
  {
    return valueOf(find(flag));
  }

  public void put(OptionSpecImpl entry, Object value)
  {
    this.specified_.put(entry, value);
  }

  public void setArguments(List<String> asList)
  {
    this.arguments_ = asList;
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