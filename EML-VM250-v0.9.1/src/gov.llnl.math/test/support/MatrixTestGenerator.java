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

import gov.llnl.math.MathExceptions;
import gov.llnl.math.matrix.Matrix;
import gov.llnl.math.matrix.MatrixColumnArray;
import gov.llnl.math.matrix.MatrixColumnList;
import gov.llnl.math.matrix.MatrixColumnTable;
import gov.llnl.math.matrix.MatrixOps;
import gov.llnl.math.matrix.MatrixRowArray;
import gov.llnl.math.matrix.MatrixRowList;
import gov.llnl.math.matrix.MatrixRowTable;
import gov.llnl.math.random.NormalRandom;
import gov.llnl.math.random.Random48;
import gov.llnl.math.random.RandomUtilities;
import gov.llnl.math.random.RandomVariable;

/**
 *
 * @author nelson85
 */
public class MatrixTestGenerator
{
  public Random48 rand = new Random48(0);
  public RandomVariable nrand = new NormalRandom(rand).newVariable(0, 1);

  int uniformRand(int min, int max)
  {
    return (int) (rand.nextDouble() * (max - min + 1) + min);
  }

  public Matrix[] eachType(Matrix m)
  {
    Matrix[] out = new Matrix[6];
    out[0] = new MatrixColumnArray(m);
    out[1] = new MatrixColumnList(m);
    out[2] = new MatrixColumnTable(m);
    out[3] = new MatrixRowArray(m);
    out[4] = new MatrixRowList(m);
    out[5] = new MatrixRowTable(m);
    return out;
  }

  public double[] newArray(int len)
  {
    return RandomUtilities.drawArray(nrand, len);
  }

  public MatrixColumnArray newMatrix(int rowsMin, int rowsMax, int columnsMin, int columnsMax)
  {
    int rows = uniformRand(rowsMin, rowsMax);
    int columns = uniformRand(columnsMin, columnsMax);
    return newMatrix(rows, columns);
  }

  public MatrixColumnArray newMatrixFixedRows(
          int rows, int columnsMin, int columnsMax)
  {
    int columns = uniformRand(columnsMin, columnsMax);
    return newMatrix(rows, columns);
  }

  public MatrixColumnArray newMatrixFixedColumns(
          int rowsMin, int rowsMax, int columns)
  {
    int rows = uniformRand(rowsMin, rowsMax);
    return newMatrix(rows, columns);
  }

  public MatrixColumnArray newMatrix(int rows, int columns)
  {
    try
    {
      double[] data = RandomUtilities.drawArray(nrand, rows * columns);
      MatrixColumnArray matrix = new MatrixColumnArray(data, rows, columns);
      return matrix;
    }
    catch (MathExceptions.SizeException ex)
    {
      throw new RuntimeException(ex);
    }
  }

  static public void main(String[] args)
  {
    MatrixTestGenerator tg = new MatrixTestGenerator();

    MatrixColumnArray m1 = tg.newMatrix(5, 5);
    MatrixRowArray m2 = new MatrixRowArray(m1);

    System.out.println(MatrixOps.equivalent(m1, m2));
    int i = 1;
    for (int r = 0; r < 5; ++r)
    {
      for (int c = 0; c < 5; ++c)
      {
        m1.set(r, c, i);
        System.out.println(MatrixOps.equivalent(m1, m2));
        m2.set(r, c, i);
        System.out.println(MatrixOps.equivalent(m1, m2));
        i++;
      }
    }
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