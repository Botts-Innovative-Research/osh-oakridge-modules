/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.ernie.impl;

import java.util.function.BiConsumer;
import java.util.function.Function;
import gov.llnl.ernie.analysis.FeaturesDescription;
import java.io.Serializable;

public class FeatureDescriptionImpl<T, R> implements FeaturesDescription.FeatureDescription<T, R>, Serializable
{
  private static final long serialVersionUID = gov.llnl.utility.UUIDUtilities.createLong("FeatureDescriptionImpl-v1");

  private final String name;
  private final Class<R> type;
  private final Function<T, R> getter;
  private final BiConsumer<T, R> setter;

  public FeatureDescriptionImpl(String name, Class<R> type, Function<T, R> getter, BiConsumer<T, R> setter)
  {
    this.name = name;
    this.type = type;
    this.getter = getter;
    this.setter = setter;
  }

  @Override
  public String getName()
  {
    return name;
  }

  @Override
  public Class getType()
  {
    return type;
  }

  public R get(T obj)
  {
    return getter.apply(obj);
  }

  public void set(T obj, R value)
  {
    setter.accept(obj, value);
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