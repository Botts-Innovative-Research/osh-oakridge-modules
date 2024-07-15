/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.math.distribution;

import gov.llnl.math.SpecialFunctions;
import gov.llnl.utility.UUIDUtilities;

/**
 *
 * @author nelson85
 */
public class GammaDistribution implements Distribution
{
  private static final long serialVersionUID = UUIDUtilities.createLong("GammaDistribution");
  final double shape; // k
  final double scale; // theta

  public GammaDistribution(double shape, double scale)
  {
    this.shape = shape;
    this.scale = scale;
  }

  @Override
  public String toString()
  {
    return "Gamma Distribution shape=" + shape + " scale=" + scale;
  }

  @Override
  public double pdf(double x)
  {
    if (x < 0)
      return 0;
    return Math.exp(-SpecialFunctions.gammaln(shape)
            - shape * Math.log(scale)
            + (shape - 1) * Math.log(x) - x / scale);
  }

  @Override
  public double cdf(double x)
  {
    return SpecialFunctions.gammaP(shape, x / scale);
  }

  @Override
  public double ccdf(double x)
  {
    return SpecialFunctions.gammaQ(shape, x / scale);
  }

  @Override
  public double logccdf(double x)
  {
    // TODO decide at what point the better approximation is given by
    // the continued fraction expansion 
    // https://math.stackexchange.com/questions/1041674/log-of-1-reguralized-incomplete-gamma-function-upper
    // -x+a*log(x)-gammaln(a)+log(1/(x+ (1-a)/(1+ 1/(x+ (2-a)/(1+ 2/x))))))

    return Math.log(ccdf(x));
  }

  @Override
  public double cdfinv(double x)
  {
    throw new UnsupportedOperationException("Not supported yet.");
  }

//  static public void main(String[] args)
//  {
//    GammaDistribution gd = new GammaDistribution(2, 4);
//    System.out.println(gd.cdf(0.5));
//    System.out.println(gd.cdf(1));
//    System.out.println(gd.cdf(2));
//  }
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