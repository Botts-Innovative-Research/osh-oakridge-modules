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

import gov.llnl.math.internal.ComplexVectorImpl;

/**
 * Operations for complex vectors.
 *
 * This is a minimal set of operations required for Fourier transform
 * operations. It may expand to addition operations if we require it for wavelet
 * transforms.
 *
 * @author nelson85
 */
public class ComplexVectorOps
{

  static public ComplexVector multiply(ComplexVector a, ComplexVector b)
  {
    double[] ra = a.getReal();
    double[] ia = a.getImag();
    double[] rb = b.getReal();
    double[] ib = b.getImag();
    MathAssert.assertEqualLength(ra, rb);
    
    int n = ra.length;
    double[] ro = new double[n];
    double[] io = new double[n];
    for (int i=0;i<n;++i)
    {
      ro[i]=ra[i]*rb[i]-ia[i]*ib[i];
      io[i]=ra[i]*ib[i]+ia[i]*rb[i];
    }
    return ComplexVector.create(ro, io);
  }

  static public ComplexVector divideAssign(ComplexVectorImpl a, double v)
  {
    DoubleArray.divideAssign(a.getReal(), v);
    DoubleArray.divideAssign(a.getImag(), v);
    return a;
  }

  static public ComplexVector copyOfRange(ComplexVector x, int start, int end)
  {
    return new ComplexVectorImpl(DoubleArray.copyOfRange(x.getReal(), start, end),
            DoubleArray.copyOfRange(x.getImag(), start, end));
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