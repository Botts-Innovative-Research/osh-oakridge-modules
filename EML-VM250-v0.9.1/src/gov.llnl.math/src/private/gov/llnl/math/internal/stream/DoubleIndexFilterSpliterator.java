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

import gov.llnl.math.stream.DoubleIndexConsumer;
import gov.llnl.math.stream.DoubleIndexSpliterator;
import java.util.function.DoublePredicate;

/**
 *
 * @author nelson85
 */
public class DoubleIndexFilterSpliterator implements DoubleIndexSpliterator
{
  DoubleIndexSpliterator source;
  DoublePredicate filter;

  public DoubleIndexFilterSpliterator(DoubleIndexSpliterator source, DoublePredicate filter)
  {
    this.source = source;
    this.filter = filter;
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
  public void range(int begin, int end)
  {
    source.range(begin, end);
  }

  @Override
  public boolean tryAdvance(DoubleIndexConsumer action)
  {
    return source.tryAdvance((i, d) ->
    {
      if (filter.test(d))
        action.accept(i, d);
    });
  }

  @Override
  public boolean tryAdvanceReverse(DoubleIndexConsumer action)
  {
    return source.tryAdvanceReverse((i, d) ->
    {
      if (filter.test(d))
        action.accept(i, d);
    });
  }

  @Override
  public DoubleIndexSpliterator trySplit()
  {
    DoubleIndexSpliterator out = source.trySplit();
    if (out == null)
      return null;
    return new DoubleIndexFilterSpliterator(out, filter);
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