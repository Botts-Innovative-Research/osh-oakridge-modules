/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.math.internal.stream;

import gov.llnl.math.stream.DoubleIndex;
import gov.llnl.math.stream.DoubleIndexStream;

/**
 *
 * @author nelson85
 */
@DoubleIndexStream.Operation.Parallel
public class MinimumOperation implements DoubleIndexStream.Operation<DoubleIndex, DoubleIndexImpl>
{
  @Override
  public void accept(DoubleIndexImpl a, int index, double value)
  {
    if (!a.exists() || a.value_ > value)
    {
      a.value_ = value;
      a.index_ = index;
    }
  }

  @Override
  public DoubleIndexImpl accumulator()
  {
    return new DoubleIndexImpl(-1, 0);
  }

  @Override
  public DoubleIndexImpl combine(DoubleIndexImpl a, DoubleIndexImpl b)
  {
    if (!a.exists())
      return b;
    if (!b.exists() || a.value_ < b.value_)
      return a;
    return b;
  }

  @Override
  public DoubleIndex finish(DoubleIndexImpl a)
  {
    return a;
  }

  @Override
  public boolean hasCharacteristic(Class cls)
  {
    return cls == DoubleIndexStream.Operation.Parallel.class;
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