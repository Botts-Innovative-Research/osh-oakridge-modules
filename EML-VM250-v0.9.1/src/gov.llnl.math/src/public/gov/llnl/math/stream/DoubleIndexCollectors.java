/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.math.stream;

import gov.llnl.math.internal.stream.*;
import gov.llnl.math.stream.DoubleIndexStream.Operation;
import java.util.OptionalDouble;

/**
 *
 * @author nelson85
 */
public class DoubleIndexCollectors
{
  static public Operation<DoubleIndex, ?> maximum()
  {
    return new MaximumOperation();
  }

  static public Operation<DoubleIndex, ?> minimum()
  {
    return new MinimumOperation();
  }

  static public Operation<OptionalDouble, ?> variance()
  {
    return new VarianceOperation();
  }

  static public Operation<Void, ?> forEach(DoubleIndexConsumer consumer)
  {
    return new Operation<Void, Void>()
    {
      @Override
      public void accept(Void a, int index, double value)
      {
        consumer.accept(index, value);
      }

      @Override
      public Void accumulator()
      {
        return null;
      }

      @Override
      public boolean hasCharacteristic(Class cls)
      {
        return (cls == Parallel.class);
      }

      @Override
      public Void combine(Void a, Void b)
      {
        return null;
      }

      @Override
      public Void finish(Void a)
      {
        return null;
      }
    };
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