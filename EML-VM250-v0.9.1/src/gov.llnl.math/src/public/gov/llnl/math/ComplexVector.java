/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.math;

import gov.llnl.math.MathExceptions.SizeException;
import gov.llnl.math.internal.ComplexVectorImpl;

/**
 * Representation of an array of complex numbers.
 *
 * This class is primarily to support Fourier transforms and wavelets. It is not
 * an immutable class. Java does not support complex numbers natively.
 *
 * @author nelson85
 */
public interface ComplexVector
{
  double[] getAbs();

  /**
   * Get the imaginary portion.
   *
   * @return
   */
  double[] getImag();

  /**
   * Get the real portion.
   *
   * @return
   */
  double[] getReal();

  /**
   * Get the length of the complex vector.
   *
   * @return
   */
  int size();

  /**
   * Create a new complex vector.
   *
   * It is required that at least one of the two input vectors not be null.
   *
   * @param real is the real portion or null if it is zero.
   * @param img is the imaginary portion or null if it is zero.
   * @return a new complex vector.
   * @throws NullPointerException if both inputs are null.
   * @throws SizeException if the length of the real and imaginary parts are
   * different lengths.
   */
  static public ComplexVector create(double[] real, double[] img) throws NullPointerException, SizeException
  {
    if (real == null && img == null)
    {
      throw new NullPointerException("Null inputs");
    }
    if (img == null)
      img = new double[real.length];
    if (real == null)
      real = new double[real.length];
    MathAssert.assertEqualLength(real, img);
    return new ComplexVectorImpl(real, img);
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