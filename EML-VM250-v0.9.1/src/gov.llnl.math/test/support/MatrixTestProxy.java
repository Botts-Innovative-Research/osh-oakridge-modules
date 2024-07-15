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
import gov.llnl.math.MathExceptions.ResizeException;
import gov.llnl.math.MathExceptions.WriteAccessException;
import gov.llnl.math.matrix.Matrix;

/**
 *
 * @author nelson85
 */
public class MatrixTestProxy implements Matrix.WriteAccess
{
  Matrix proxy;

  public MatrixTestProxy(Matrix matrix)
  {
    this.proxy = matrix;
  }

  @Override
  public int rows()
  {
    return proxy.rows();
  }

  @Override
  public int columns()
  {
    return proxy.columns();
  }

  @Override
  public void set(int r, int c, double v) throws WriteAccessException
  {
    proxy.set(r, c, v);
  }

  @Override
  public double get(int r, int c)
  {
    return proxy.get(r, c);
  }

  @Override
  public void mutable() throws MathExceptions.WriteAccessException
  {
    proxy.mutable();
  }

  @Override
  public Object sync()
  {
    return proxy.sync();
  }

  @Override
  public boolean resize(int rows, int columns) throws ResizeException
  {
    return proxy.resize(rows, columns);
  }

  @Override
  public Matrix assign(Matrix source) throws ResizeException, WriteAccessException
  {
    proxy.assign(source);
    return this;
  }

  @Override
  public void assignRow(double[] in, int index) throws WriteAccessException
  {
    if (!(proxy instanceof Matrix.WriteAccess))
      throw new WriteAccessException();
    ((Matrix.WriteAccess) proxy).assignRow(in, index);
  }

  @Override
  public void assignColumn(double[] in, int index) throws WriteAccessException
  {
    ((Matrix.WriteAccess) proxy).assignColumn(in, index);
  }

  @Override
  public double[] copyRowTo(double[] dest, int offset, int index)
  {
    return proxy.copyRowTo(dest, offset, index);
  }

  @Override
  public double[] copyColumnTo(double[] dest, int offset, int index)
  {
    return proxy.copyColumnTo(dest, offset, index);
  }

  @Override
  public Matrix copyOf()
  {
    return proxy.copyOf();
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