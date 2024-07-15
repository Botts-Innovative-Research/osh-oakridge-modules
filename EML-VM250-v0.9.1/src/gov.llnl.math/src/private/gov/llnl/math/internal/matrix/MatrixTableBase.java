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
import gov.llnl.math.matrix.Matrix;
import static gov.llnl.math.matrix.MatrixAssert.assertViewResize;
import gov.llnl.math.matrix.MatrixIterators;
import gov.llnl.utility.UUIDUtilities;
import gov.llnl.utility.annotation.Internal;

/**
 *
 * @author nelson85
 */
@Internal
public abstract class MatrixTableBase implements Matrix
{
  private static final long serialVersionUID
          = UUIDUtilities.createLong("MatrixTableBase-v1");

  final transient Object origin;
  protected int dim;
  protected double[][] data;

  protected MatrixTableBase(double[][] data, int dim)
  {
    this.origin = this;
    this.data = data;
    this.dim = dim;
  }

  protected MatrixTableBase(Object origin, double[][] data, int dim)
  {
    this.origin = origin;
    this.data = data;
    this.dim = dim;
  }

  @Override
  public void mutable() throws MathExceptions.WriteAccessException
  {
  }

  @Override
  final public Object sync()
  {
    return origin;
  }

//  /**
//   * Access the underlying data. Altering this data may case issues. The
//   * orientation of the data depends on the type of matrix.
//   *
//   * @return the underlying data.
//   */
//  public double[][] toArray()
//  {
//    return this.data;
//  }

  protected static <T extends MatrixTableBase>
          T assignRowTable(T target, Matrix source)
  {
    // Check for self assignment
    if (source == target)
      return target;

    // Check for assignment from view of self
    if (target.sync() == source.sync())
      source = source.copyOf();

    // Resize if not a view
    if (target.sync() == target)
    {
      target.resize(source.rows(), source.columns());
    }
    else
    {
      // Verify size of view    
      assertViewResize(target, source.rows(), source.columns());
    }

    // Copy data into view
    MatrixIterators.VectorIterator iter = MatrixIterators.newRowReadIterator(source);
    while (iter.hasNext())
    {
      System.arraycopy(iter.access(), iter.begin(),
              target.data[iter.index()], 0,
              target.dim);
    }
    return target;
  }

  protected static <T extends MatrixTableBase>
          T assignColumnTable(T target, Matrix source)
  {
    // Check for self assignment
    if (source == target)
      return target;

    // Check for assignment from view of self
    if (target.sync() == source.sync())
      source = source.copyOf();

    // Resize if not a view
    if (target.sync() == target)
    {
      target.resize(source.rows(), source.columns());
    }
    else
    {
      // Verify size of view    
      assertViewResize(target, source.rows(), source.columns());
    }

    // Copy data into view
    MatrixIterators.VectorIterator iter = MatrixIterators.newColumnReadIterator(source);
    while (iter.hasNext())
    {
      System.arraycopy(iter.access(), iter.begin(),
              target.data[iter.index()], 0,
              target.dim);
    }
    return target;
  }

  protected final void allocate(int major, int minor)
  {
    this.dim = minor;
    data = new double[major][];
    for (int i = 0; i < major; ++i)
      data[i] = new double[minor];
  }

  public double[] copyToIndirect(double[] dest, int offset, int index)
  {
    // Indirect copy
    int i0 = offset;
    for (double[] v : this.data)
      dest[i0++] = v[index];
    return dest;
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