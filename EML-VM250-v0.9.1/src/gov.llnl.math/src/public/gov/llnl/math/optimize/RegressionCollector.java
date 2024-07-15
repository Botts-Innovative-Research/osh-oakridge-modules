/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.math.optimize;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

/**
 *
 * @author nelson85
 */
public class RegressionCollector
{

  /**
   * Converts a regression method to a collector.
   *
   * @param <Model>
   * @param <Method>
   * @param method
   * @return
   */
  public static <Model extends DoubleUnaryOperator, Method extends Regression<Model>> Collector<RegressionPoint, Method, Model>
          as(Method method)
  {
    return new Collector<RegressionPoint, Method, Model>()
    {
      @Override
      public Supplier<Method> supplier()
      {
        return () -> method;
      }

      @Override
      public BiConsumer<Method, RegressionPoint> accumulator()
      {
        return (Method method, RegressionPoint datum) -> method.add(datum.getX(), datum.getY(), datum.getLambda());
      }

      @Override
      public BinaryOperator<Method> combiner()
      {
        return null;
      }

      @Override
      public Function<Method, Model> finisher()
      {
        return (Method m) -> m.compute();
      }

      @Override
      public Set<Collector.Characteristics> characteristics()
      {
        return EnumSet.of(Characteristics.UNORDERED);
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