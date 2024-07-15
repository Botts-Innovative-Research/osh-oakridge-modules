/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.ernie.spectral;

import gov.llnl.ernie.analysis.AnalysisException;
import gov.llnl.math.DoubleArray;
import gov.llnl.math.MathExceptions.SizeException;
import gov.llnl.math.matrix.Matrix;
import gov.llnl.math.matrix.MatrixFactory;
import static gov.llnl.math.matrix.MatrixFactory.wrapColumnVector;
import gov.llnl.math.matrix.MatrixOps;
import java.io.PrintStream;
import java.io.Serializable;
import static java.lang.Double.isNaN;

/**
 * This code is specialized to the ERNIE application. Not for general use.
 *
 * @author nelson85
 */
public class MatchedFilter implements Serializable
{
  private static final long serialVersionUID
          = gov.llnl.utility.UUIDUtilities.createLong("MatchedFilter-v1");
  public double[] coefficients;
  public double efficiency;
  public double scale;
  public double[] target;

  public double computeBias(double[] bkg)
  {
//    return DoubleArray.multiplyInner(coefficients, 0, bkg, 0, coefficients.length)
//            / Math.sqrt(DoubleArray.sum(bkg));
    return DoubleArray.multiplyInner(coefficients, bkg) / Math.sqrt(DoubleArray.sum(bkg));

  }

  public double apply(double[] data)
  {
    double s = DoubleArray.sum(data);
    if (s == 0)
    {
      return 0;
    }
    return DoubleArray.multiplyInner(coefficients, data) / Math.sqrt(s);
  }

  public double apply(double[] data, double[] bkg)
  {
    return DoubleArray.multiplyInner(coefficients, data) / Math.sqrt(DoubleArray.sum(bkg));
  }

  public double getCounts(double[] data)
  {
    double out = DoubleArray.multiplyInner(coefficients, data);
    return scale * out;
  }

  public double[] getCounts(double[][] data)
  {
    double[] out = new double[data.length];
    for (int i = 0; i < data.length; ++i)
    {
      out[i] = getCounts(data[i]);
    }
    return out;
  }

  public double[] apply(double[][] data)
  {
    double[] out = new double[data.length];
    for (int i = 0; i < data.length; ++i)
    {
      out[i] = apply(data[i]);
    }
    return out;
  }

  public double[] apply(double[][] data, double[][] bkg)
  {
    double[] out = new double[data.length];
    for (int i = 0; i < data.length; ++i)
    {
      out[i] = apply(data[i], bkg[i]);
    }
    return out;
  }

  /**
   * Create a matched filter to remove a specific background.
   *
   * @param target the target for the matched filter.
   * @param bkg1 the primary background to be removed.
   * @param bkg2 a secondary background that is removed only if there is a
   * positive bias.
   * @return a new matched filter for this target
   */
  static public MatchedFilter create(double[] target, double[] bkg1, double[] bkg2)
  {
    try
    {
      MatchedFilter mf = new MatchedFilter();
      mf.target = target;

      mf.coefficients = MatchedFilter.computeMatchedFilterCoefficients(target,
              MatrixFactory.createColumnVector(bkg1), bkg1, bkg1);
      double bias = mf.computeBias(bkg2);

      // It is bias is too positive then we need to remove the second component as well
      if (bias > 0.2)
      {
        Matrix B2 = MatrixFactory.createFromArray(new double[][]
        {
          bkg1, bkg2
        }, false);
        mf.coefficients = MatchedFilter.computeMatchedFilterCoefficients(target, B2, bkg1, bkg1);
      }

      // Compute the bias
//      mf.scale = DoubleArray.sum(target) / DoubleArray.productSum(mf.coefficients, mf.target);
      mf.scale = DoubleArray.sum(target) / DoubleArray.multiplyInner(mf.coefficients, mf.target);
      return mf;
    }
    catch (SizeException ex)
    {
      throw new RuntimeException(ex);
    }
  }

  /**
   * Create a matched filter to remove a specific background.
   *
   * @param target the target for the matched filter.
   * @param bkg1 the primary background to be removed.
   * @param bkg2 a secondary background that is removed only if there is a
   * positive bias.
   * @param nuisance a nuisance source that must also be removed
   * @return a new matched filter for this target
   * @throws AnalysisException
   */
  static public MatchedFilter create(double[] target,
          double[] bkg1,
          double[] bkg2,
          double[] nuisance)
          throws AnalysisException
  {
    try
    {
      MatchedFilter mf = new MatchedFilter();
      mf.target = target;

      Matrix B1;
      if (nuisance != null)
      {
        B1 = MatrixFactory.createFromArray(new double[][]
        {
          bkg1, nuisance
        }, false);
      }
      else
      {
        B1 = MatrixFactory.createFromArray(new double[][]
        {
          bkg1
        }, false);
      }

      mf.coefficients = MatchedFilter.computeMatchedFilterCoefficients(target, B1, bkg1, bkg1);
      double bias = mf.computeBias(bkg2);

      // It is bias is too positive then we need to remove the second component as well
      if (bias > 0.2)
      {
        Matrix B2;
        if (nuisance != null)

        {
          B2 = MatrixFactory.createFromArray(new double[][]
          {
            bkg1, bkg2, nuisance
          }, false);
        }
        else
        {
          B2 = MatrixFactory.createFromArray(new double[][]
          {
            bkg1, bkg2
          }, false);
        }

        mf.coefficients = MatchedFilter.computeMatchedFilterCoefficients(target, B2, bkg1, bkg1);
      }

      // Compute the bias
//      mf.scale = DoubleArray.sum(target) / DoubleArray.productSum(mf.coefficients, mf.target);
      mf.scale = DoubleArray.sum(target) / DoubleArray.multiplyInner(mf.coefficients, mf.target);
//      if (isnan(mf.scale))
      if (isNaN(mf.scale))
      {
        throw new AnalysisException("nan in matched filter");
      }
      return mf;
    }
    catch (SizeException ex)
    {
      throw new RuntimeException(ex);
    }
  }

  /**
   * Create a matched filter to remove a specific set of nuisances.
   *
   * @param target the target for the matched filter.
   * @param nuisance a nuisance source that must also be removed
   * @param mean
   * @param variance
   * @return a new matched filter for this target
   */
  static public MatchedFilter createDirect(double[] target, double[][] nuisance,
          double mean[], double variance[])
  {
    try
    {
      MatchedFilter mf = new MatchedFilter();
      mf.target = target;

      Matrix B1 = MatrixFactory.createFromArray(nuisance, false);
      mf.coefficients = MatchedFilter.computeMatchedFilterCoefficients(target, B1, mean, variance);
      mf.scale = DoubleArray.sum(target)
              / DoubleArray.multiplyInner(mf.coefficients, mf.target);

      return mf;
    }
    catch (SizeException ex)
    {
      throw new RuntimeException(ex);
    }
  }

  static public double[] computeMatchedFilterCoefficients(double[] target,
          Matrix nuisance, double[] mean, double[] sigma)
  {
    double[] isigma = new double[sigma.length];
    for (int i = 0; i < sigma.length; ++i)
    {
      isigma[i] = 1 / (sigma[i] + 1e-6);
    }

    // Special handling for the first and last channels
    //isigma[0]/=2;
    //isigma[8]/=2;
    //  coef=target'*isigma*(I-B*inv(B'*isigma*B)*B'*isigma);
    double[] T1 = target.clone();
    DoubleArray.multiplyAssign(T1, isigma);
    double[] U1 = MatrixOps.multiply(T1, nuisance);
    Matrix N2 = nuisance.copyOf();
    MatrixOps.multiplyAssignRows(N2, wrapColumnVector(isigma));

    Matrix Q = MatrixOps.multiply(N2.transpose(), nuisance);
    Matrix iQ = MatrixOps.invert(Q);
    double[] U2 = MatrixOps.multiply(U1, iQ);
    double[] T2 = MatrixOps.multiply(N2, U2);
    DoubleArray.subtractAssign(T1, T2);

    double vcoef = 0;
    for (int i = 0; i < T1.length; ++i)
    {
      vcoef += T1[i] * T1[i] * mean[i];
    }
    vcoef /= DoubleArray.sum(mean);
    if (vcoef != 0)
    {
      DoubleArray.divideAssign(T1, Math.sqrt(vcoef));
    }
    return T1;
  }

  void dump(PrintStream out)
  {
    out.println("coefficients: " + DoubleArray.toString(coefficients));
    out.println("efficiency: " + efficiency);
    out.println("scale: " + scale);
    out.println("target: " + DoubleArray.toString(target));
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