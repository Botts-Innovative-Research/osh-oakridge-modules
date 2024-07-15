/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package support;

import gov.llnl.math.MathExceptions.WriteAccessException;
import gov.llnl.math.matrix.Matrix;

/**
 *
 * @author nelson85
 */
public class MatrixTestReadOnly extends MatrixTestProxy
{
  public MatrixTestReadOnly(Matrix m)
  {
    super(m);
  }

  @Override
  public void set(int r, int c, double v) throws WriteAccessException
  {
    throw new WriteAccessException();
  }

  @Override
  public void mutable() throws WriteAccessException
  {
    throw new WriteAccessException();
  }

  @Override
  public void assignRow(double[] v, int r) throws WriteAccessException
  {
    throw new WriteAccessException();
  }

  @Override
  public void assignColumn(double[] v, int r) throws WriteAccessException
  {
    throw new WriteAccessException();
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