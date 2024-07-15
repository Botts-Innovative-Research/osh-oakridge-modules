/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.math.random;

import gov.llnl.math.DoubleArray;

/**
 *
 * @author nelson85
 */
public class MultinomialRandom extends RandomFactory
{

  public MultinomialRandom()
  {
    super(getDefaultGenerator());
  }

  public MultinomialRandom(RandomGenerator random)
  {
    super(random);
  }

  public int[] draw(double[] means, int k)
  {
    BinomialRandom b = new BinomialRandom(this.getGenerator());
    int n = means.length;
    double s = DoubleArray.sum(means);
    int[] out = new int[n];
    for (int i = 0; i < n; ++i)
    {
      double m = means[i];
      if (m <= 0)
        continue;
      double p = m / s;
      int n1 = (int) b.newVariable(k, p).next();
      out[i] = n1;
      k -= n1;
      s -= m;
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