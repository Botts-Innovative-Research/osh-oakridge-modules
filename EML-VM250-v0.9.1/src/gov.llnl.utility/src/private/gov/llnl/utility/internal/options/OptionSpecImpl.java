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

import gov.llnl.utility.options.OptionDescriptor;
import gov.llnl.utility.options.OptionSpecBuilder;
import java.util.List;

/**
 *
 * @author nelson85
 */
public class OptionSpecImpl<T> implements OptionSpecBuilder<T>, OptionDescriptor
{
  List<String> keys;
  String help;
  boolean required;
  boolean hasArgument;
  boolean requiredArgument;
  Class argumentClass = String.class;
  Object defaultValue = null;

  public OptionSpecImpl(List<String> keys, String help)
  {
    this.keys = keys;
    this.help = help;
  }

  @Override
  public OptionSpecImpl<T> required()
  {
    this.required = true;
    return this;
  }

  @Override
  public OptionSpecImpl<T> withRequiredArg()
  {
    this.hasArgument = true;
    this.requiredArgument = true;
    return this;
  }

  @Override
  public OptionSpecImpl<T> withOptionalArg()
  {
    this.hasArgument = true;
    return this;
  }

  @Override
  public OptionSpecImpl<T> defaultsTo(T value)
  {
    this.defaultValue = value;
    return this;
  }

  @Override
  public <T2> OptionSpecImpl<T2> ofType(Class<T2> cls)
  {
    this.argumentClass = cls;
    return (OptionSpecImpl<T2>) this;
  }

  @Override
  public boolean accepts(String optKey)
  {
    for (String s : this.keys)
    {
      if (optKey.equals(s))
        return true;
    }
    return false;
  }

  @Override
  public String description()
  {
    return this.help;
  }

  @Override
  public boolean requiresArgument()
  {
    return this.requiredArgument;
  }

  @Override
  public boolean isRequired()
  {
    return this.required;
  }

  @Override
  public boolean acceptsArguments()
  {
    return this.hasArgument;
  }

  @Override
  public List<String> options()
  {
    return this.keys;
  }

  public Object getDefaultValue()
  {
    return this.defaultValue;
  }

  public Class getArgumentClass()
  {
    return this.argumentClass;
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