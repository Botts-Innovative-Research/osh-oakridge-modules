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

import static gov.llnl.math.IntegerArray.colon;
import static gov.llnl.math.IntegerArray.promoteToDoubles;
import org.testng.annotations.Test;

/**
 *
 * @author nelson85
 */
public class MatrixViewsNGTest
{

  public MatrixViewsNGTest()
  {
  }

  /**
   * Test of selectColumn method, of class MatrixViews.
   */
  @Test
  public void testSelectColumn()
  {
  }

  /**
   * Test of selectColumns method, of class MatrixViews.
   */
  @Test
  public void testSelectColumns()
  {
  }

  /**
   * Test of selectColumnRange method, of class MatrixViews.
   */
  @Test
  public void testSelectColumnRange()
  {
  }

  /**
   * Test of selectRow method, of class MatrixViews.
   */
  @Test
  public void testSelectRow()
  {
  }

  /**
   * Test of selectRows method, of class MatrixViews.
   */
  @Test
  public void testSelectRows()
  {
  }

  /**
   * Test of selectRowRange method, of class MatrixViews.
   */
  @Test
  public void testSelectRowRange()
  {
  }

  /**
   * Test of select method, of class MatrixViews.
   */
  @Test
  public void testSelect()
  {
    Matrix in = MatrixFactory.wrapArray(promoteToDoubles(colon(0, 4 * 5)), 4, 5);
    Matrix out;
    out = MatrixViews.select(in, 0, 4, 0, 5);
    System.out.println(out.rows() + " " + out.columns());
  }

  /**
   * Test of transpose method, of class MatrixViews.
   */
  @Test
  public void testTranspose()
  {
  }

  /**
   * Test of diagonal method, of class MatrixViews.
   */
  @Test
  public void testDiag()
  {
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