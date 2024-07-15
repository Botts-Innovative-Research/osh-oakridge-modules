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

import gov.llnl.math.DoubleArray;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

/**
 * Utilities to deal with a stream of double[].
 *
 * Additional methods for this type of stream are in MatrixCollectors.
 *
 * @author nelson85
 */
public class DoubleArrayCollectors
{
  private static final Set<Collector.Characteristics> CHARACTERISTICS
          = Collections.unmodifiableSet(EnumSet.of(
                  Collector.Characteristics.CONCURRENT,
                  Collector.Characteristics.UNORDERED));

  /**
   * Average a series of double[].
   *
   * All arrays must be the same length.
   *
   * @return the element-wise average of a series of double arrays.
   */
  static public Collector<double[], ?, double[]> mean()
  {
    return new Collector<double[], DoubleArrayPtr, double[]>()
    {
      @Override
      public Supplier<DoubleArrayPtr> supplier()
      {
        return () -> new DoubleArrayPtr();
      }

      @Override
      public BiConsumer<DoubleArrayPtr, double[]> accumulator()
      {
        return (p, v) ->
        {
          if (v == null)
            return;
          if (p.n == 0)
            p.values = v.clone();
          else
            DoubleArray.addAssign(p.values, v);
          p.n++;
        };
      }

      @Override
      public BinaryOperator<DoubleArrayPtr> combiner()
      {
        return (p1, p2) ->
        {
          if (p1.n == 0)
            return p2;
          if (p2.n == 0)
            return p1;
          p1.n += p2.n;
          DoubleArray.addAssign(p1.values, p2.values);
          return p1;
        };
      }

      @Override
      public Function<DoubleArrayPtr, double[]> finisher()
      {
        return p ->
        {
          if (p.n == 0)
            return null;
          DoubleArray.divideAssign(p.values, p.n);
          return p.values;
        };
      }

      @Override
      public Set<Collector.Characteristics> characteristics()
      {
        return CHARACTERISTICS;
      }
    };
  }

  private static class DoubleArrayPtr
  {
    double[] values;
    int n = 0;
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