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
import gov.llnl.math.matrix.Matrix;
import static gov.llnl.math.matrix.MatrixAssert.assertViewResize;
import gov.llnl.math.matrix.MatrixColumnTable;
import gov.llnl.math.matrix.MatrixRowTable;
import gov.llnl.utility.annotation.Internal;

/**
 * Base classes for proxies that provide views to a matrix. Used by MatrixView
 * methods.
 */
@Internal
public abstract class MatrixProxy implements Matrix
{
  protected Matrix proxy;

  protected MatrixProxy(Matrix matrix)
  {
    this.proxy = matrix;
  }

//<editor-fold desc="basic" defaultstate="collapsed">
  @Override
  public void mutable() throws MathExceptions.WriteAccessException
  {
    proxy.mutable();
  }

  @Override
  public Object sync()
  {
    return this.proxy;
  }

//</editor-fold>  
//<editor-fold desc="assign/copy" defaultstate="collapsed">
  @Override
  public boolean resize(int rows, int columns) throws ResizeException
  {
    assertViewResize(this, rows, columns);
    return false;
  }

  @Override
  public Matrix copyOf()
  {
    if (rows() >= columns())
      return new MatrixColumnTable(this);
    return new MatrixRowTable(this);
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