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
import java.util.Spliterator;

/**
 *
 * @author nelson85
 */
public class DoubleIndexSpliteratorImpl implements DoubleIndexSpliterator
{
  double[] values;
  int begin;
  int end;

  public DoubleIndexSpliteratorImpl(double[] values, int begin, int end)
  {
    this.values = values;
    this.begin = begin;
    this.end = end;
  }

  @Override
  public int characteristics()
  {
    return Spliterator.ORDERED;
  }

  @Override
  public long estimateSize()
  {
    return end - begin;
  }

  @Override
  public void range(int begin, int end)
  {
    this.begin = Math.max(this.begin, begin);
    this.end = Math.min(this.end, end);
  }

  @Override
  public boolean tryAdvance(DoubleIndexConsumer action)
  {
    if (begin >= end)
      return false;
    action.accept(begin, values[begin]);
    begin++;
    return true;
  }

  @Override
  public boolean tryAdvanceReverse(DoubleIndexConsumer action)
  {
    if (begin >= end)
      return false;
    end--;
    action.accept(end, values[end]);
    return true;
  }

  @Override
  public DoubleIndexSpliterator trySplit()
  {
    if (end - begin < 4)
      return null;
    int split = (end + begin) / 2;
    int b0 = begin;
    this.begin = split;

    return new DoubleIndexSpliteratorImpl(values, b0, split);
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