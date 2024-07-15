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

import gov.llnl.math.MathExceptions;
import gov.llnl.math.MathExceptions.ResizeException;
import gov.llnl.math.MathExceptions.SizeException;
import gov.llnl.math.matrix.Matrix;
import gov.llnl.math.matrix.MatrixAssert;
import static gov.llnl.math.matrix.MatrixAssert.assertNotViewResize;

/**
 *
 * @author nelson85
 */
public abstract class MatrixArrayBase implements Matrix
{
  final transient Object origin;
  protected int rows;
  protected int columns;
  protected double[] data;

  /**
   * Construct a new matrix. Allocates memory to fit.
   *
   * @param rows
   * @param columns
   */
  protected MatrixArrayBase(int rows, int columns)
  {
    this.origin = this;
    this.rows = rows;
    this.columns = columns;
    this.data = new double[rows * columns];
  }

  /**
   * Construct a matrix pointing to existing memory.
   *
   * @param data
   * @param rows
   * @param columns
   */
  protected MatrixArrayBase(double[] data, int rows, int columns)
  {
    this.origin = this;
    this.rows = rows;
    this.columns = columns;
    this.data = data;
  }

  /**
   * Constructs a new matrix as a view of other memory.
   *
   * @param origin
   * @param data
   * @param rows
   * @param columns
   * @throws SizeException
   */
  protected MatrixArrayBase(Object origin, double[] data, int rows, int columns)
          throws MathExceptions.SizeException
  {
    if (rows * columns != data.length)
      throw new MathExceptions.SizeException();

    this.origin = origin;
    this.rows = rows;
    this.columns = columns;
    this.data = data;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  final public int rows()
  {
    return rows;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  final public int columns()
  {
    return columns;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object sync()
  {
    return origin;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean resize(int rows, int columns) throws ResizeException
  {
    // Short cut if no change
    if (rows == rows() && columns == columns())
      return false;

    assertNotViewResize(this);

    // Allocate fresh memory
    this.rows = rows;
    this.columns = columns;
    this.data = new double[rows * columns];
    return true;
  }

  /**
   * Access to the raw data in the matrix.
   *
   * @return the array backing the data.
   */
  public double[] toArray()
  {
    return data;
  }

//<editor-fold desc="special" defaultstate="collapsed">
  /**
   * Alters the shape of the matrix. The size of the memory must fit the new
   * layout. This operation will disrupt any existing views.
   *
   * @param rows is the desired number of rows.
   * @param columns is the desired number of columns.
   * @throws SizeException
   */
  public void reshape(int rows, int columns) throws SizeException
  {
    MatrixAssert.assertSizeEquals(this, rows * columns);
    this.rows = rows;
    this.columns = columns;
  }

//</editor-fold>
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