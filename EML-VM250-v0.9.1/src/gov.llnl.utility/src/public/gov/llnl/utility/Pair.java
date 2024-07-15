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

import gov.llnl.utility.xml.bind.Reader;
import java.util.Map;

/**
 *
 * @author nelson85
 * @param <Key>
 * @param <Value>
 */
//@Reader.Declaration(pkg = UtilityPackage.class, name = "pair")
public class Pair<Key, Value> implements Map.Entry<Key, Value>
{
  Key key = null;
  Value value = null;

  protected Pair()
  {
  }

  public Pair(Key key, Value value)
  {
    this.key = key;
    this.value = value;
  }

  @Override
  public Key getKey()
  {
    return key;
  }

  @Override
  public Value getValue()
  {
    return value;
  }

  @Override
  //  @Reader.Attribute(name = "value", required = true, type = String.class)
  public Value setValue(Value v)
  {
    return this.value = v;
  }

//  @Reader.Attribute(name = "key", required = true, type = String.class)
  protected Key setKey(Key k)
  {
    return this.key = k;
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