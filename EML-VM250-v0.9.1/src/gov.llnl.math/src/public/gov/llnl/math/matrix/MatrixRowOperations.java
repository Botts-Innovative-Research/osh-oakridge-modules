/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.math.matrix;

/**
 * Abstraction for Gauss elimination code.
 */
public interface MatrixRowOperations
{
  /**
   * Add two rows together with a scaling factor.
   *
   * @param r1 is the target row.
   * @param r2 is the source row to add.
   * @param scalar is the scaling factor to apply when adding.
   */
  void addScaledRows(int r1, int r2, double scalar);

  /**
   * Multiply a row by a scalar.
   *
   * @param row is the target row.
   * @param scalar is the factor to multiply by.
   */
  void multiplyAssignRow(int row, double scalar);

  /**
   * Divide a row by a scalar.
   *
   * @param row is the target row.
   * @param scalar is the factor to divide by.
   */
  void divideAssignRow(int row, double scalar);

  /**
   * Swap two rows in the matrix.
   *
   * @param r1
   * @param r2
   */
  void swapRows(int r1, int r2);

  /**
   * Apply the operations to the matrix. This must be called once the row
   * operations are complete.
   */
  void apply();

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