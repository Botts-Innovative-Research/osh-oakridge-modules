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

import support.MatrixTestGenerator;
import gov.llnl.math.MathExceptions.MathException;
import gov.llnl.math.MathExceptions.SizeException;
import org.testng.annotations.Test;

/**
 *
 * @author nelson85
 */
public class MatrixAssertNGTest
{
  MatrixTestGenerator tg = new MatrixTestGenerator();

  public MatrixAssertNGTest()
  {
  }

  /**
   * Test of assertSquare method, of class MatrixAssert.
   *
   * @throws java.lang.Exception
   */
  @Test
  public void testAssertSquare() throws Exception
  {
    MatrixAssert.assertSquare(MatrixFactory.newColumnMatrix(2, 2));
  }

  /**
   * Test of assertSquare method, of class MatrixAssert.
   *
   * @throws java.lang.Exception
   */
  @Test(expectedExceptions = SizeException.class)
  public void testAssertSquare_SizeException() throws Exception
  {
    MatrixAssert.assertSquare(MatrixFactory.newColumnMatrix(2, 3));
  }

  /**
   * Test of assertSizeEquals method, of class MatrixAssert.
   *
   * @throws java.lang.Exception
   */
  @Test
  public void testAssertSizeEquals() throws Exception
  {
    MatrixAssert.assertSizeEquals(MatrixFactory.newColumnMatrix(2, 3), 6);
  }

  /**
   * Test of assertSizeEquals method, of class MatrixAssert.
   *
   * @throws java.lang.Exception
   */
  @Test(expectedExceptions = SizeException.class)
  public void testAssertSizeEquals_SizeException() throws Exception
  {
    MatrixAssert.assertSizeEquals(MatrixFactory.newColumnMatrix(2, 3), 9);
  }

  /**
   * Test of assertEqualSize method, of class MatrixAssert.
   *
   * @throws java.lang.Exception
   */
  @Test
  public void testAssertEqualSize() throws Exception
  {
    MatrixAssert.assertEqualSize(MatrixFactory.newColumnMatrix(2, 3),
            MatrixFactory.newColumnMatrix(2, 3));
  }

  @Test(expectedExceptions = SizeException.class)
  public void testAssertEqualSize_SizeException() throws Exception
  {
    MatrixAssert.assertEqualSize(MatrixFactory.newColumnMatrix(2, 3),
            MatrixFactory.newColumnMatrix(4, 1));
  }

  /**
   * Test of assertColumnsEqualsRows method, of class MatrixAssert.
   *
   * @throws java.lang.Exception
   */
  @Test
  public void testAssertColumnsEqualsRows() throws Exception
  {
    MatrixAssert.assertColumnsEqualsRows(MatrixFactory.newColumnMatrix(2, 3),
            MatrixFactory.newColumnMatrix(3, 2));
  }

  @Test(expectedExceptions = SizeException.class)
  public void testAssertColumnsEqualsRows_SizeException() throws Exception
  {
    MatrixAssert.assertEqualSize(MatrixFactory.newColumnMatrix(2, 3),
            MatrixFactory.newColumnMatrix(1, 4));
  }

  /**
   * Test of assertRowsEqual method, of class MatrixAssert.
   *
   * @throws java.lang.Exception
   */
  @Test
  public void testAssertRowsEqual_Matrix_Matrix() throws Exception
  {
    MatrixAssert.assertRowsEqual(MatrixFactory.newColumnMatrix(4, 2),
            MatrixFactory.newColumnMatrix(4, 1));
  }

  @Test(expectedExceptions = SizeException.class)
  public void testAssertRowsEqual_Matrix_Matrix_SizeException() throws Exception
  {
    MatrixAssert.assertRowsEqual(MatrixFactory.newColumnMatrix(4, 2),
            MatrixFactory.newColumnMatrix(3, 1));
  }

  /**
   * Test of assertRowsEqual method, of class MatrixAssert.
   *
   * @throws java.lang.Exception
   */
  @Test
  public void testAssertRowsEqual_Matrix_int() throws Exception
  {
    MatrixAssert.assertRowsEqual(MatrixFactory.newColumnMatrix(4, 2), 4);
  }

  @Test(expectedExceptions = SizeException.class)
  public void testAssertRowsEqual_Matrix_int_SizeException() throws Exception
  {
    MatrixAssert.assertRowsEqual(MatrixFactory.newColumnMatrix(4, 2), 3);
  }

  /**
   * Test of assertColumnsEqual method, of class MatrixAssert.
   *
   * @throws java.lang.Exception
   */
  @Test
  public void testAssertColumnsEqual_Matrix_int() throws Exception
  {
    MatrixAssert.assertColumnsEqual(MatrixFactory.newColumnMatrix(2, 4), 4);
  }

  @Test(expectedExceptions = SizeException.class)
  public void testAssertColumnsEqual_Matrix_int_SizeException() throws Exception
  {
    MatrixAssert.assertColumnsEqual(MatrixFactory.newColumnMatrix(2, 4),
            MatrixFactory.newColumnMatrix(1, 3));
  }

  /**
   * Test of assertColumnsEqual method, of class MatrixAssert.
   *
   * @throws java.lang.Exception
   */
  @Test
  public void testAssertColumnsEqual_Matrix_Matrix() throws Exception
  {
    MatrixAssert.assertColumnsEqual(MatrixFactory.newColumnMatrix(2, 4),
            MatrixFactory.newColumnMatrix(1, 4));
  }

  @Test(expectedExceptions = SizeException.class)
  public void testAssertColumnsEqual_Matrix_Matrix_SizeException() throws Exception
  {
    MatrixAssert.assertColumnsEqual(MatrixFactory.newColumnMatrix(4, 2),
            MatrixFactory.newColumnMatrix(4, 1));
  }

  /**
   * Test of assertNotViewResize method, of class MatrixAssert.
   *
   * @throws java.lang.Exception
   */
  @Test
  public void testAssertNotViewResize() throws Exception
  {
  }

  /**
   * Test of assertViewResize method, of class MatrixAssert.
   *
   * @throws java.lang.Exception
   */
  @Test
  public void testAssertViewResize() throws Exception
  {
  }

  /**
   * Test of assertSymmetric method, of class MatrixAssert.
   *
   * @throws java.lang.Exception
   */
  @Test
  public void testAssertSymmetric() throws Exception
  {
    Matrix.ColumnAccess m1 = tg.newMatrix(2, 2);
    MatrixOps.addAssign(m1, m1.transpose());
    MatrixAssert.assertSymmetric(m1);
  }

  @Test(expectedExceptions = SizeException.class)
  public void testAssertSymmetric_SizeException() throws Exception
  {
    Matrix.ColumnAccess m1 = MatrixFactory.newColumnMatrix(2, 3);
    MatrixAssert.assertSymmetric(m1);
  }

  @Test(expectedExceptions = MathException.class)
  public void testAssertSymmetric_MathException() throws Exception
  {
    Matrix.ColumnAccess m1 = tg.newMatrix(3, 3);
    MatrixAssert.assertSymmetric(m1);
    System.out.println("Incorrect");
  }

  /**
   * Test of assertVector method, of class MatrixAssert.
   *
   * @throws java.lang.Exception
   */
  @Test
  public void testAssertVector() throws Exception
  {
    MatrixAssert.assertVector(tg.newMatrix(5, 1));
    MatrixAssert.assertVector(tg.newMatrix(1, 5));
  }

  @Test(expectedExceptions = SizeException.class)
  public void testAssertVector_SizeException() throws Exception
  {
    MatrixAssert.assertVector(tg.newMatrix(3, 3));
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