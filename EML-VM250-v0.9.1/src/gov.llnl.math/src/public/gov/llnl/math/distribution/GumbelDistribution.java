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

import gov.llnl.math.MathExceptions;
import static gov.llnl.math.SpecialFunctions.erfinv;
import gov.llnl.math.matrix.Matrix;
import gov.llnl.math.matrix.MatrixFactory;
import gov.llnl.math.matrix.MatrixOps;
import gov.llnl.utility.UUIDUtilities;
import java.util.Arrays;

/**
 *
 * @author jurgenson2
 */
public class GumbelDistribution implements Distribution
{
  private static final long serialVersionUID = UUIDUtilities.createLong("GumbelDistribution");
  final public double mu;
  final public double beta;

  public GumbelDistribution(double mu, double beta)
  {
    this.mu = mu;
    this.beta = beta;
  }

  @Override
  public String toString()
  {
    return "Gumbel Distribution(mu=" + mu + ", beta=" + beta + ")";
  }

  @Override
  public double cdf(double x)
  {
    return Math.exp(-Math.exp(-(x - mu) / beta));
  }

  // convert percentage to metric
  @Override
  public double cdfinv(double pct)
  {
    if (pct <= 0)
      return Double.NEGATIVE_INFINITY;
    if (pct >= 1)
      return Double.POSITIVE_INFINITY;
    return -beta * Math.log(-Math.log(pct)) + mu;
  }

  @Override
  public double ccdf(double x)
  {
    return -Math.expm1(-Math.exp(-(x - mu) / beta));
  }

  public double ccdfinv(double pct)
  {
    if (pct <= 0)
      return Double.POSITIVE_INFINITY;
    if (pct >= 1)
      return Double.NEGATIVE_INFINITY;
    return -beta * Math.log(-Math.log1p(-pct)) + mu;
  }

  @Override
  public double logccdf(double x)
  {
    // Expansion is needed if we are far out on the tails.
    if ((x - mu) > 10 * beta)
    {
      double u = Math.exp(-(x - mu) / beta);
      return -(x - mu) / beta + Math.log(1 - u / 2 + u * u / 6 + u * u * u / 24);
    }
    return Math.log(ccdf(x));
  }

  public double logccdfinv(double pct)
  {
    // special case when exp
    if (pct < -20)
    {
      // Log(-Log(1-e^x)) => x+(e^x)/2-(e^2x)/8 for x<-20
      double u = Math.exp(pct)/2;
      return -beta * (pct + u - u * u / 2) + mu;
    }
    return ccdfinv(Math.exp(pct));
  }

  @Override
  public double pdf(double x)
  {
    double z = (x - mu) / beta;
    return Math.exp(-(z + Math.exp(-z))) / beta;
  }

  public double significance(double x)
  {
    return ccdf(x);
  }

  public double[] significance(double x[])
  {
    double[] out = new double[x.length];
    for (int i = 0; i < x.length; ++i)
    {
      out[i] = significance(x[i]);
    }
    return out;
  }

  // FIXME these are great ideas, but the CDF gets too close to 1
  // We need to work through the math and make expansions of the erfinv
  // functions around one to get this correct.
  public double convertToNormal(double x)
  {
    double sqrt2 = 1.4142135623730951000;
    double d = cdf(x);
    return sqrt2 * erfinv(2 * d - 1);
  }

  public static GumbelDistribution fit(double[] in2, double p1, double p2)
  {
    try
    {
      double beta = 0.;
      double mu = 0.;
      double[] in = in2.clone();
      Arrays.sort(in);
      double lenin = in.length;
      int i1 = (int) Math.floor(lenin * p1);

      int i2 = (int) Math.floor(lenin * p2);
//      double A1 = 0, A2 = 0, A3 = 0, f1, B1 = 0, B2 = 0, y1;
//      for (int i = i1; i < i2; i++)
//      {
//        y1 = -Math.log(-Math.log((i+1)  / (lenin+2)));
//        f1 = in[i];
//        System.out.println(in[i]+" "+y1);
//        A1 = A1 + y1 * y1;
//        A2 = A2 + y1;
//        A3 = A3 + 1;
//        B1 = B1 + y1 * f1;
//        B2 = B2 + f1;
//      }
//      System.out.println(A1+" "+A2+" "+A3+" "+B1+" "+B2);

      double A1 = 0, A2 = 0, A3 = 0, f1, B1 = 0, B2 = 0, y1;
      for (int i = i1; i < i2; i++)
      {
        y1 = -Math.log(-Math.log((i + 1) / (lenin + 2)));
        f1 = in[i];
        //System.out.println(in[i]+" "+y1+(i+1)/(lenin+2));
        A1 = A1 + f1 * f1;
        A2 = A2 - f1;
        A3 = A3 + 1;
        B1 = B1 + y1 * f1;
        B2 = B2 - y1;
      }
      //System.out.println(p1+" "+p2+" "+i1+" "+i2+" "+A1+" "+A2+" "+A3+" "+B1+" "+B2);

      Matrix A = MatrixFactory.wrapArray(new double[]
      {
        A1, A2, A2, A3
      }, 2, 2);
      Matrix B = MatrixFactory.createColumnVector(new double[]
      {
        B1, B2
      });
      B = MatrixOps.divideLeft(A, B);
//      DoubleMatrix A = new DoubleMatrix(new double[]
//      {
//        A1, A2, A2, A3
//      }, 2, 2);
//      DoubleMatrix B = new DoubleMatrix(new double[]
//      {
//        B1, B2
//      }, 2, 1);
//
//      DoubleMatrix.solveDestructive(B, A);

      beta = 1.0 / B.get(0, 0);
      mu = B.get(1, 0) * beta;
      return new GumbelDistribution(mu, beta);
    }
    catch (MathExceptions.SizeException ex)
    {
      throw new RuntimeException(ex);
    }

  }

  /**
   * Create a new Gumbel distribution on a new time base. For example a gumbel
   * distribution computed with 1/100 of samples, the original distribution is
   * found with gd.changeTimeBase(100).
   *
   * @param downsample
   * @return
   */
  public GumbelDistribution changeTimeBase(double downsample)
  {
    return new GumbelDistribution(this.mu - this.beta * Math.log(downsample), this.beta);

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