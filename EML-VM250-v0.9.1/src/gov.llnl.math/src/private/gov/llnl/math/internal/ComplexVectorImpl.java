/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.math.internal;

import gov.llnl.math.ComplexVector;
import gov.llnl.utility.annotation.Internal;

/**
 *
 * @author nelson85
 */
public class ComplexVectorImpl implements ComplexVector
{
  double[] real;
  double[] imag;

  @Internal
  public ComplexVectorImpl(double[] real, double[] img)
  {
    this.real = real;
    this.imag = img;
  }

  @Override
  public double[] getReal()
  {
    return real;
  }

  @Override
  public double[] getImag()
  {
    return imag;
  }

  @Override
  public double[] getAbs()
  {
    int n = real.length;
    double[] out = new double[n];
    for (int i = 0; i < n; i++)
    {
      out[i] = Math.sqrt(this.real[i] * this.real[i] + this.imag[i] * this.imag[i]);
    }
    return out;
  }

  @Override
  public int size()
  {
    return real.length;
  }

//  public void resize(int n)
//  {
//    real = new double[n];
//    imag = new double[n];
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