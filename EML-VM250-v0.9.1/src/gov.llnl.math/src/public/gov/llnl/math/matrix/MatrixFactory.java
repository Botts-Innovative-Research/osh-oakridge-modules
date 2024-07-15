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

import gov.llnl.math.MathExceptions.SizeException;
import gov.llnl.math.internal.matrix.MatrixRowOpsColumnAccess;
import gov.llnl.math.internal.matrix.MatrixRowOpsRowAccess;
import gov.llnl.math.internal.matrix.MatrixRowOpsVector;
import java.util.Collection;

/**
 *
 */
public class MatrixFactory
{

  private static void assertRectangular(double[][] v) throws SizeException
  {
    // Check the major dimension
    int d1 = v.length;
    if (d1 == 0)
      throw new SizeException("zero dimension matrix");

    // Check the minor dimension
    int d2 = v[0].length;
    if (d2 == 0)
      throw new SizeException("zero dimension matrix");

    for (double[] v1 : v)
    {
      if (d2 != v1.length)
        throw new SizeException("dimension mismatch");
    }
  }

  /**
   * Produce a copy of a matrix. The layout of the matrix will depend on the
   * orientation of the original matrix. Row and Column access is preserved. The
   * copy is always a fully writable matrix.
   *
   * @param m
   * @return a new matrix which holds the same data as the input.
   */
  public static Matrix newMatrix(Matrix m)
  {
    // Keep the access policy if it has one.
    if (m instanceof Matrix.ColumnAccess)
      return newColumnMatrix(m);
    if (m instanceof Matrix.RowAccess)
      return newRowMatrix(m);

    // Otherwise select the best representation.
    if (m.rows() > m.columns())
      return newColumnMatrix(m);
    return newRowMatrix(m);
  }

  public static Matrix newMatrix(int rows, int columns)
  {
    if (rows >= columns)
      return new MatrixColumnTable(rows, columns);
    return new MatrixRowTable(rows, columns);
  }

  public static Matrix.ColumnAccess newColumnMatrix(int rows, int columns)
  {
    if (columns == 1)
      return new MatrixColumnArray(rows, columns);
    return new MatrixColumnTable(rows, columns);
  }

  public static Matrix.ColumnAccess newColumnMatrix(double[]... columns)
  {
    return new MatrixColumnTable(columns, columns, columns[0].length);
  }

  public static Matrix.ColumnAccess newColumnMatrix(Collection<double[]> columns)
  {
    double[][] data = columns.toArray(new double[0][]);
    return new MatrixColumnTable(data, data, data[0].length);
  }

  public static Matrix.RowAccess newRowMatrix(Collection<double[]> rows)
  {
    double[][] data = rows.toArray(new double[0][]);
    return new MatrixRowTable(data, data, data[0].length);
  }

  public static Matrix.RowAccess newRowMatrix(double[]... rows)
  {
    return new MatrixRowTable(rows, rows, rows[0].length);
  }

  public static Matrix.ColumnAccess newColumnMatrix(Matrix matrix)
  {
    return new MatrixColumnTable(matrix);
  }

  public static Matrix.RowAccess newRowMatrix(int rows, int columns)
  {
    if (rows == 1)
      return new MatrixRowArray(rows, columns);
    return new MatrixRowTable(rows, columns);
  }

  public static Matrix.RowAccess newRowMatrix(Matrix matrix)
  {
    return new MatrixRowTable(matrix);
  }

  public static Matrix createRowVector(double[] v)
  {
    return wrapRowVector(v).copyOf();
  }

  public static MatrixRowArray wrapRowVector(double[] v)
  {
    try
    {
      return new MatrixRowArray(v, v, 1, v.length);
    }
    catch (SizeException ex)
    {
      throw new RuntimeException(ex);
    }
  }

  /**
   * Create a column vector initialized to a set of values.
   *
   * The matrix is a copy and thus will not modify the original.
   *
   * @param v
   * @return a column vector initialized to the value specified.
   */
  public static MatrixColumnArray createColumnVector(double[] v)
  {
    return wrapColumnVector(v).copyOf();
  }

  /**
   * Wrap a double array as a column vector.
   *
   * @param v
   * @return a new column vector backed by the array. The matrix is a view and
   * cannot be resized.
   */
  public static MatrixColumnArray wrapColumnVector(double[] v)
  {
    try
    {
      return new MatrixColumnArray(v, v, v.length, 1);
    }
    catch (SizeException ex)
    {
      throw new RuntimeException(ex);
    }
  }

  /**
   * Wrap a double array as a matrix. Data is interpreted in column major order.
   *
   * @param v
   * @param r
   * @param c
   * @return a new matrix backed the array. The matrix is a view and cannot be
   * resized.
   * @throws SizeException if the length of v does not match the number of
   * requested rows and columns.
   */
  public static Matrix wrapArray(double[] v, int r, int c) throws SizeException
  {
    if (r * c != v.length)
      throw new SizeException();
    if (r == 1)
      return new MatrixRowArray(v, v, r, c);
    return new MatrixColumnArray(v, v, r, c);
  }

  /**
   * Create a new matrix initialized to values stored in an array. Data is
   * interpreted in column major order.
   *
   * @param v
   * @param r
   * @param c
   * @return a new matrix initialized by the array.
   * @throws SizeException
   */
  public static Matrix createFromArray(double[] v, int r, int c) throws SizeException
  {
    return wrapArray(v, r, c).copyOf();
  }

  public interface MatrixNew
  {
    Matrix create(int r, int c);
  }

  public static Matrix createFromArray(double[] v, int r, int c, MatrixNew supplier) throws SizeException
  {
    return supplier.create(r, c).assign(wrapArray(v, r, c));
  }


  /**
   * Creates a matrix from a list of arrays. This is useful when working with
   * MATLAB.
   *
   * @param v
   * @param layout should be true if the data is row major (C convention) or
   * false if data is column major.
   * @return a new matrix initialized from the array.
   * @throws SizeException
   */
  public static Matrix createFromArray(double[][] v, boolean layout) throws SizeException
  {
    return wrapFromArray(v, layout).copyOf();
  }

  /**
   * Creates a matrix from a list of arrays. This is useful when working with
   * MATLAB. SUPERSEDED this does not produce an access type, better to use
   * wrapRows or wrapColumns.
   *
   * @param v
   * @param layout should be true if the data is row major (C convention) or
   * false if data is column major.
   * @return a new matrix backed by the array. The matrix is a view and cannot
   * be resized.
   * @throws SizeException
   */
  public static Matrix wrapFromArray(double[][] v, boolean layout) throws SizeException
  {
    assertRectangular(v);
    int d1 = v.length;
    int d2 = v[0].length;

    // Determine the orientation
    if (layout == true)
    {
      // Row major  v[r][c]
      return new MatrixRowTable(v, v, d2);
    }
    else
    {
      // Column major v[c][r]
      return new MatrixColumnTable(v, v, d2);
    }
  }

  public static Matrix.RowAccess wrapRows(double[][] v) throws SizeException
  {
    assertRectangular(v);
    return new MatrixRowTable(v, v, v[0].length);
  }

  public static Matrix.ColumnAccess wrapColumns(double[][] v) throws SizeException
  {
    assertRectangular(v);
    return new MatrixColumnTable(v, v, v[0].length);
  }

  /**
   * Ensures that a matrix for read access has row access. If the matrix does
   * not have row access, it will produce a copy.
   *
   * @param matrix
   * @return the input if matrix already has row access, or a copy if it does
   * not.
   */
  public static Matrix.RowAccess asRowMatrix(Matrix matrix)
  {
    if (matrix instanceof Matrix.RowAccess)
      return (Matrix.RowAccess) matrix;
    return newRowMatrix(matrix);
  }

  /**
   * Ensures that a matrix for read access has column access. If the matrix does
   * not have column access, it will produce a copy.
   *
   * @param matrix
   * @return the input if matrix already has column access, or a copy if it does
   * not.
   */
  public static Matrix.ColumnAccess asColumnMatrix(Matrix matrix)
  {
    if (matrix instanceof Matrix.ColumnAccess)
      return (Matrix.ColumnAccess) matrix;
    return newColumnMatrix(matrix);
  }

  public static MatrixRowOperations createRowOperations(Matrix matrix)
  {
    if (matrix instanceof Matrix.ColumnAccess
            && matrix instanceof Matrix.WriteAccess)
      return new MatrixRowOpsColumnAccess((Matrix.ColumnAccess) matrix);
    if (matrix instanceof Matrix.RowAccess
            && matrix instanceof Matrix.WriteAccess)
      return new MatrixRowOpsRowAccess((Matrix.RowAccess) matrix);
    throw new UnsupportedOperationException("Row operations not supported for matrix of type "
            + matrix.getClass());
  }

  public static MatrixRowOperations createRowOperations(double[] v)
  {
    return new MatrixRowOpsVector(v, 0);
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