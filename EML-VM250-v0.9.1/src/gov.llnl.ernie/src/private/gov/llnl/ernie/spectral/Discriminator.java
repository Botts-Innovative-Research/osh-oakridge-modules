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

import gov.llnl.math.DoubleArray;
import java.io.Serializable;

/**
 * Computes the linear discriminate features between the two extracted sources
 * and the set of targets that we searched for.
 *
 * The result is a set of energy coefficients for each source.
 *
 * @author nelson85
 */
public class Discriminator implements Serializable
{
  private static final long serialVersionUID
          = gov.llnl.utility.UUIDUtilities.createLong("Discriminator-v1");

  public static class Result implements Serializable
  {
    public double[] energy1;
    public double[] energy2;
  }

  static public Result process(double[][] targets, double[] src1, double[] src2, double[] bkg)
  {
    int m = targets.length;
    int n = bkg.length;
    Result result = new Result();
    result.energy1 = new double[m];
    result.energy2 = new double[m];

    // Compute our covariance
    double[] isigma = new double[n];
    for (int i = 0; i < n; i++)
    {
      isigma[i] = 1.0 / (bkg[i] + 1e-5);
    }
    isigma[0] /= 5;   // FIXME magic numbers
    isigma[8] /= 10;  // FIXME magic numbers

    // Weight the sources by the covariance
    double[] src1W = src1.clone();
    double[] src2W = src2.clone();
    double[] bkgW = bkg.clone();
    DoubleArray.multiplyAssign(src1W, isigma);
    DoubleArray.multiplyAssign(src2W, isigma);
    DoubleArray.multiplyAssign(bkgW, isigma);

    double f0 = DoubleArray.multiplyInner(bkgW, bkg);
    double u1 = DoubleArray.multiplyInner(bkgW, src1);
    double u2 = DoubleArray.multiplyInner(bkgW, src2);
    double w1 = DoubleArray.multiplyInner(src1W, src1) - u1 * u1 / f0;
    double w2 = DoubleArray.multiplyInner(src2W, src2) - u2 * u2 / f0;

    // T=I-B*(B'*W*B)^-1*B*W
    // E_i = S_i*W*T*Y/ ( \sqrt(S_i*W*T*S_i)*\sqrt(Y*W*T*Y) )
    for (int i = 0; i < m; i++)
    {
      double[] target = targets[i];
      double[] targetW = targets[i].clone();
      DoubleArray.multiplyAssign(targetW, isigma);

      double v0 = DoubleArray.multiplyInner(target, bkgW);
      double v1 = DoubleArray.multiplyInner(target, src1W);
      double v2 = DoubleArray.multiplyInner(target, src2W);

      double d0 = DoubleArray.multiplyInner(target, targetW) - v0 * v0 / f0;
      result.energy1[i] = (v1 - v0 * u1 / f0) / Math.sqrt(w1 * d0);
      result.energy2[i] = (v2 - v0 * u2 / f0) / Math.sqrt(w2 * d0);
    }
    return result;
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