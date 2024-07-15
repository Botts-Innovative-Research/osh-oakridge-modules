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

/**
 *
 * @author nelson85
 */
public interface DoubleIndexSpliterator
{
  int characteristics();

  long estimateSize();

  DoubleIndexSpliterator trySplit();

  boolean tryAdvance(DoubleIndexConsumer action);

  boolean tryAdvanceReverse(DoubleIndexConsumer action);

  void range(int begin, int end);

  @SuppressWarnings("empty-statement")
  default void forEachRemaining(DoubleIndexConsumer action)
  {
    while (tryAdvance(action));
  }

  @SuppressWarnings("empty-statement")
  default void forEachRemainingReverse(DoubleIndexConsumer action)
  {
    while (tryAdvanceReverse(action));
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