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
import gov.llnl.math.signal.FilterUtilities.IterateDoublesForward;

/**
 *
 * @author nelson85
 */
public class FilterFIR implements Filter
{
  double[] b;

  public FilterFIR(double[] a)
  {
    this.b = DoubleArray.copyOf(a);
  }

  @Override
  public double[] apply(double[] in)
  {
    return apply(new IterateDoublesForward(in));
  }

  @Override
  public double[] apply(IterateDoubles iter)
  {
    double[] out = new double[iter.size()];
    RollingBuffer in = new RollingBuffer(b.length);
    int i = 0;
    while (iter.hasNext())
    {
      in.add(iter.next());
      double sum = 0;
      for (int j = 0; j < b.length; ++j)
        sum += b[j] * in.get(j);
      out[i++] = sum;
    }
    return out;
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