/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.math.cluster;

import gov.llnl.math.DoubleArray;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collector;
import gov.llnl.math.cluster.WeightedCollector.Accumulator;

/**
 *
 * @author nelson85
 */
public class WeightedCollector implements Collector<double[], Accumulator, double[]>
{

  ToDoubleFunction<double[]> kernel;

  public static WeightedCollector of(ToDoubleFunction<double[]> kernel)
  {
    return new WeightedCollector(kernel);
  }

  WeightedCollector(ToDoubleFunction<double[]> kernel)
  {
    this.kernel = kernel;
  }

  @Override
  public Supplier<Accumulator> supplier()
  {
    return () -> new Accumulator();
  }

  @Override
  public BiConsumer<Accumulator, double[]> accumulator()
  {
    return Accumulator::accept;
  }

  @Override
  public BinaryOperator<Accumulator> combiner()
  {
    return Accumulator::combine;
  }

  @Override
  public Function<Accumulator, double[]> finisher()
  {
    return Accumulator::finish;
  }

  @Override
  public Set<Characteristics> characteristics()
  {
    return Collections.unmodifiableSet(EnumSet.of(Characteristics.UNORDERED));
  }

  public class Accumulator
  {
    double[] v;
    double w;
    int l = 0;

    void accept(double[] u)
    {
      double d = kernel.applyAsDouble(u);
//      System.out.println(d);
      if (d * l < w * 1e-6)
      {
        return;
      }
      l++;
      if (Double.isNaN(d))
        throw new RuntimeException();
      if (v == null)
        v = DoubleArray.copyOf(u);
      else
        DoubleArray.addAssignScaled(v, u, d);
      w += d;
    }

    Accumulator combine(Accumulator a)
    {
      DoubleArray.addAssign(v, a.v);
      w += a.w;
      return this;
    }

    double[] finish()
    {
      return DoubleArray.divideAssign(v, w);
    }
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