/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.utility.internal.xml.bind;

import gov.llnl.utility.annotation.Internal;
import gov.llnl.utility.xml.bind.WriterContext;

/**
 *
 * @author nelson85
 */
@Internal
public class MarshallerOptionsProxy implements WriterContext.MarshallerOptions
{
  WriterContext.MarshallerOptions parent;
  String key;
  Object obj;

  public MarshallerOptionsProxy(WriterContext.MarshallerOptions options, String key, Object obj)
  {
    this.parent = options;
    this.key = key;
    this.obj = obj;
  }

  @Override
  public <Type> Type get(String key, Class<Type> cls, Type defaultValue)
  {
    if (key.equals(this.key))
      return cls.cast(obj);
    return parent.get(key, cls, defaultValue);
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