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

import static gov.llnl.math.DoubleUtilities.cube;
import gov.llnl.utility.UUIDUtilities;
import java.io.Serializable;

/**
 *
 * @author nelson85
 */
public class GammaRandom extends RandomFactory implements Serializable
{
  private static final long serialVersionUID = UUIDUtilities.createLong("GammaRandom");
  NormalRandom normalGenerator = null;

  public GammaRandom()
  {
    super(getDefaultGenerator());
    normalGenerator = new NormalRandom(getGenerator());
  }

  public GammaRandom(RandomGenerator generator)
  {
    super(generator);
    normalGenerator = new NormalRandom(getGenerator());
  }

  /**
   * Generate a Gamma distributed distribution.
   * <p>
   * There are two definitions of the gamma function. One uses the "scale",
   * while the other uses "rate". The scale is one over the rate.
   *
   * @see
   * <a
   * href="http://www.hongliangjie.com/2012/12/19/how-to-generate-gamma-random-variables/">reference</a>
   * @param alpha is the shape
   * @param beta is the rate
   * @return a random draw.
   */
  public double draw(double alpha, double beta)
  {
    RandomGenerator random = getGenerator();

    // Handling alpha less that 1
    if (alpha < 1.0)
    {
      double d = draw(alpha + 1, beta);
      double u = random.nextDouble();
      return d * Math.pow(u, 1.0 / alpha);
    }

    // Marsaglia and Tsang Method
    double d = alpha - 1.0 / 3.0;
    double c = Math.sqrt(9.0 * d);
    while (true)
    {
      double u = random.nextDouble();
      double z = normalGenerator.draw();
      if (z > -c)
      {
        double v = cube(1 + z / c);
        if (Math.log(u) < 0.5 * z * z + d - d * v + d * Math.log(v))
          return d * v * beta;
      }
    }
  }

//  public double[] drawArray(double alpha, double beta, int length)
//  {
//    double[] out = new double[length];
//    for (int i = 0; i < length; i++)
//      out[i] = draw(alpha, beta);
//    return out;
//  }
  public RandomVariable newVariable(double alpha, double beta)
  {
    return () -> draw(alpha, beta);
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