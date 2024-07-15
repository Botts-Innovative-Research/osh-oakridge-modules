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

import gov.llnl.math.stream.DoubleIndexSpliterator;
import java.util.Spliterator;
import java.util.function.IntConsumer;

/**
 *
 * @author nelson85
 */
class DoubleIndexSpliteratorOfIndices implements Spliterator.OfInt
{
  DoubleIndexSpliterator source;

  DoubleIndexSpliteratorOfIndices(DoubleIndexSpliterator source)
  {
    this.source = source;
  }

  @Override
  public int characteristics()
  {
    return source.characteristics();
  }

  @Override
  public long estimateSize()
  {
    return source.estimateSize();
  }

  @Override
  public boolean tryAdvance(IntConsumer arg0)
  {
    return source.tryAdvance((i, d) -> arg0.accept(i));
  }

  @Override
  public Spliterator.OfInt trySplit()
  {
    DoubleIndexSpliterator out = source.trySplit();
    if (out == null)
      return null;
    return new DoubleIndexSpliteratorOfIndices(out);
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