/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.math.wavelet;

import gov.llnl.math.matrix.Matrix;

/**
 *
 * @author nelson85
 */
public interface WaveletTransform
{

  // FIXME should it be possible to get the family/length from the transform?
  /**
   * Compute the wavelet transform.
   *
   * @param signal
   * @param scale is the requested scale of the wavelet. If the scale is greater
   * than possible given the length than the resulting transform will truncate
   * the the largest possible scale. This must be a positive integer.
   *
   * @return wavelet coefficients in which each column stores a specific scale.
   * @throws IllegalArgumentException if the scale is zero or negative.
   */
  Matrix forward(double[] signal, int scale) throws IllegalArgumentException;

  /**
   * Reconstructs the signal from the wavelet coefficients.
   *
   * @param waveletCoef is the matrix of coefficients resulting from a wavelet
   * transform.
   * @return the reconstructed signal at each scale.
   */
  Matrix reconstruct(Matrix waveletCoef);

  /**
   * Smooth a signal using wavelets with a specified scale.
   *
   * This decomposed the signal using the specified wavelet then reconstructs it
   *
   * @param inData
   * @param waveletScale is the scale of wavelet to smooth to. This must be a
   * positive integer.
   * @return
   */
  double[] smooth(double[] inData, int waveletScale);

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