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

import gov.llnl.utility.UUIDUtilities;
import java.io.Serializable;

/**
 *
 * @author nelson85
 */
public class NormalRandom extends RandomFactory implements Serializable
{
    private static final long serialVersionUID = UUIDUtilities.createLong("NormalRandom");
  private double y1, y2;
  private boolean reuse = false;

  public NormalRandom()
  {
    super(getDefaultGenerator());
  }

  public NormalRandom(RandomGenerator generator)
  {
    super(generator);
  }

  /**
   * Box-Muller transform for generating two random numbers.
   *
   * @return a normal random variable from N(0,1)
   */
  public double draw()
  {
    RandomGenerator random = getGenerator();
    if (reuse)
    {
      reuse = false;
      return y2;
    }
    reuse = true;
    double x1, x2, w;
    do
    {
      x1 = 2.0 * random.nextDouble() - 1.0;
      x2 = 2.0 * random.nextDouble() - 1.0;
      w = x1 * x1 + x2 * x2;
    }
    while (w >= 1.0);

    w = Math.sqrt((-2.0 * Math.log(w)) / w);
    y1 = x1 * w;
    y2 = x2 * w;
    return y1;
  }

  public RandomVariable newVariable(double mean, double std)
  {
    return () -> std * draw() + mean;
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