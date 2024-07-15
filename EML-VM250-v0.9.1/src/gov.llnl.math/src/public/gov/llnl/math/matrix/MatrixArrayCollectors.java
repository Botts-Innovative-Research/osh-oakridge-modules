/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gov.llnl.math.matrix;

import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

/**
 * Utilities to operate on Stream&lt;Matrix&gt;.
 *
 * @author nelson85
 */
public class MatrixArrayCollectors
{

  /**
   * Sums a series of matrix together.
   *
   * All matrix must be the same size.
   *
   * @param <T>
   * @return
   */
  public static <T> Collector<Matrix, ?, Matrix> sum()
  {
    return new Collector<Matrix, MatrixPtr, Matrix>()
    {
      @Override
      public BiConsumer<MatrixPtr, Matrix> accumulator()
      {
        return (MatrixPtr p, Matrix m) ->
        {
          if (p.matrix == null)
            p.matrix = m.copyOf();
          else
            MatrixOps.addAssign(p.matrix, m);
        };
      }

      @Override
      public Set<Collector.Characteristics> characteristics()
      {
        return Collections.unmodifiableSet(EnumSet.of(Collector.Characteristics.UNORDERED));
      }

      @Override
      public BinaryOperator<MatrixPtr> combiner()
      {
        return (MatrixPtr p1, MatrixPtr p2) ->
        {
          MatrixOps.addAssign(p1.matrix, p2.matrix);
          return p1;
        };
      }

      @Override
      public Function<MatrixPtr, Matrix> finisher()
      {
        return (MatrixPtr p) -> p.matrix;
      }

      @Override
      public Supplier<MatrixPtr> supplier()
      {
        return () -> new MatrixPtr();
      }
    };
  }

  /**
   * Collect a set of matrix by concatenating as columns.
   *
   * @param <T>
   * @return
   */
  public static <T> Collector<Matrix, ?, Matrix> hcat()
  {
    return new Collector<Matrix, List<double[]>, Matrix>()
    {
      @Override
      public BiConsumer<List<double[]>, Matrix> accumulator()
      {
        return (List<double[]> t, Matrix u) ->
        {
          for (int i = 0; i < u.columns(); ++i)
          {
            t.add(u.copyColumn(i));
          }
        };
      }

      @Override
      public Set<Collector.Characteristics> characteristics()
      {
        return Collections.emptySet();
      }

      @Override
      public BinaryOperator<List<double[]>> combiner()
      {
        return (List<double[]> t, List<double[]> u) ->
        {
          t.addAll(u);
          return t;
        };
      }

      @Override
      public Function<List<double[]>, Matrix> finisher()
      {
        return (columns) -> MatrixFactory.newColumnMatrix(columns);
      }

      @Override
      public Supplier<List<double[]>> supplier()
      {
        return () -> new LinkedList<>();
      }
    };

  }

  /**
   * Mutable representation to use of the accumulator.
   */
  private static class MatrixPtr
  {
    Matrix matrix = null;
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