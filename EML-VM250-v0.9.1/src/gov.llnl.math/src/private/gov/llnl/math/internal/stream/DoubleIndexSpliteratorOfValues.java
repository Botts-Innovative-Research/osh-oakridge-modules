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
import java.util.function.DoubleConsumer;

/**
 *
 * @author nelson85
 */
class DoubleIndexSpliteratorOfValues implements Spliterator.OfDouble
{
  DoubleIndexSpliterator source;

  DoubleIndexSpliteratorOfValues(DoubleIndexSpliterator source)
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
  public boolean tryAdvance(DoubleConsumer arg0)
  {
    return source.tryAdvance((i, d) -> arg0.accept(d));
  }

  @Override
  public Spliterator.OfDouble trySplit()
  {
    DoubleIndexSpliterator out = source.trySplit();
    if (out == null)
      return null;
    return new DoubleIndexSpliteratorOfValues(out);
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