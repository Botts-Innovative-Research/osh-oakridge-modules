/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.math.internal.matrix;

import gov.llnl.math.DoubleArray;
import gov.llnl.math.matrix.Matrix;
import gov.llnl.math.matrix.MatrixFactory;
import gov.llnl.math.matrix.MatrixRowOperations;
import gov.llnl.utility.annotation.Debug;
import gov.llnl.utility.annotation.Internal;

/**
 *
 * @author nelson85
 */
@Internal
public class MatrixRowOpsVector implements MatrixRowOperations
{
  double[] data;
  int offset;

  @Debug
  public Matrix getObj()
  {
    return MatrixFactory.wrapColumnVector(DoubleArray.copyOfRange(data, offset, data.length));
  }

  public MatrixRowOpsVector(double[] values, int offset)
  {
    this.data = values;
    this.offset = offset;
  }

  @Override
  public void addScaledRows(int r1, int r2, double scalar)
  {
    data[r1 + offset] += scalar * data[r2 + offset];
  }

  @Override
  public void swapRows(int r1, int r2)
  {
    double tmp = data[r1 + offset];
    data[r1 + offset] = data[r2 + offset];
    data[r2 + offset] = tmp;
  }

  @Override
  public void multiplyAssignRow(int row, double scalar)
  {
    data[row + offset] *= scalar;
  }

  @Override
  public void divideAssignRow(int row, double scalar)
  {
    data[row + offset] /= scalar;
  }

  @Override
  public void apply()
  {
    // Not used.  Applied as called.
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