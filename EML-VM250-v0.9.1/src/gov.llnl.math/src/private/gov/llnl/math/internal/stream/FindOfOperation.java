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
import java.util.function.DoublePredicate;

/**
 *
 * @author nelson85
 */
public class FindOfOperation implements DoubleIndexStream.Operation<DoubleIndex, DoubleIndexImpl>
{
  boolean done_ = false;
  DoublePredicate predicate;
  boolean reverse;

  public FindOfOperation(DoublePredicate predicate, boolean reverse)
  {
    this.predicate = predicate;
    this.reverse = reverse;
  }

  @Override
  public void accept(DoubleIndexImpl a, int index, double value)
  {
    if (predicate.test(value))
    {
      a.index_ = index;
      a.value_ = value;
      done_ = true;
    }
  }

  @Override
  public DoubleIndexImpl combine(DoubleIndexImpl a, DoubleIndexImpl b)
  {
    return null;
  }

  public boolean done()
  {
    return done_;
  }

  @Override
  public DoubleIndexImpl accumulator()
  {
    return new DoubleIndexImpl(-1, 0);
  }

  @Override
  public boolean hasCharacteristic(Class cls)
  {
    return (cls == DoubleIndexStream.Operation.Predicated.class) || 
            ((cls == DoubleIndexStream.Operation.Reverse.class)&&reverse);
  }

  @Override
  public DoubleIndex finish(DoubleIndexImpl a)
  {
    return a;
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