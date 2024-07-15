/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.math.signal;

import gov.llnl.math.DoubleArray;

/**
 *
 * @author nelson85
 */
public class FilterUtilities
{

  public static double[] applyReverse(Filter filter, double[] in)
  {
    return DoubleArray.reverseAssign(filter.apply(new IterateDoublesReverse(in)));
  }

  public static double[] applySymmetric(Filter filter, double[] in)
  {
    double[] out = filter.apply(new IterateDoublesForward(in));
    return DoubleArray.reverseAssign(filter.apply(new IterateDoublesReverse(out)));
  }

  public static class IterateDoublesForward implements Filter.IterateDoubles
  {
    double[] memory;
    int begin;
    int end;
    int n;

    public IterateDoublesForward(double[] in)
    {
      memory = in;
      begin = 0;
      end = in.length;
      n = 0;
    }

    @Override
    public boolean hasNext()
    {
      return n != end;
    }

    @Override
    public double next()
    {
      int current = n;
      ++n;
      return memory[current];
    }

    @Override
    public int size()
    {
      return end - begin;
    }
  }

  public static class IterateDoublesReverse implements Filter.IterateDoubles
  {
    double[] memory;
    int begin;
    int end;
    int n;

    public IterateDoublesReverse(double[] in)
    {
      memory = in;
      begin = 0;
      end = in.length;
      n = end - 1;
    }

    @Override
    public boolean hasNext()
    {
      return n >= begin;
    }

    @Override
    public double next()
    {
      int current = n;
      --n;
      return memory[current];
    }

    @Override
    public int size()
    {
      return end - begin;
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