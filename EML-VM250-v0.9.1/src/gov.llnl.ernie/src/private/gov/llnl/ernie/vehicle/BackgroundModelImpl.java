/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.ernie.vehicle;

import gov.llnl.ernie.data.VehicleClass;
import gov.llnl.math.matrix.Matrix;
import gov.llnl.math.matrix.MatrixColumnTable;
import gov.llnl.math.matrix.MatrixOps;

/**
 * Implementation of VehicleClass.BackgroundModel.
 *
 * Holds a matrix containing the coefficients for the background.
 *
 * @author nelson85
 */
public class BackgroundModelImpl implements VehicleClass.BackgroundModel
{
  MatrixColumnTable matrix;

  @Override
  public int size()
  {
    return getMatrix().columns();
  }

  @Override
  public double[] get(int panel)
  {
    return getMatrix().accessColumn(panel);
  }

  @Override
  public MatrixColumnTable toMatrix()
  {
    return getMatrix();
  }

  @Override
  public boolean equals(Object other)
  {
    if (!(other instanceof VehicleClass.BackgroundModel))
    {
      return false;
    }

    if (this == other)
    {
      return true;
    }

    return MatrixOps.equivalent(toMatrix(), ((VehicleClass.BackgroundModel) other).toMatrix());
  }

  /**
   * @return the matrix
   */
  public MatrixColumnTable getMatrix()
  {
    return matrix;
  }

  /**
   * @param matrix the matrix to set
   */
  public void setMatrix(Matrix matrix)
  {
    this.matrix = new MatrixColumnTable(matrix);
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