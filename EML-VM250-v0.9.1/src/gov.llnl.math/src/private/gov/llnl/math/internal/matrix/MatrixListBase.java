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
import gov.llnl.utility.annotation.Internal;
import java.util.List;

/**
 * Base class for all MatrixList classes.
 */
@Internal
public abstract class MatrixListBase implements Matrix
{
  final transient Object origin;
  protected List<Vector> data;
  protected int dim;

  public static class Vector
  {
    public double[] values;
    public int offset;

    public Vector(double[] values, int offset)
    {
      this.values = values;
      this.offset = offset;
    }
  }

  protected MatrixListBase(List<Vector> data, int dim)
  {
    this.origin = this;
    this.dim = dim;
    this.data = data;
  }

  protected MatrixListBase(Object origin, List<Vector> data, int dim)
  {
    this.origin = origin;
    this.dim = dim;
    this.data = data;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void mutable() throws MathExceptions.WriteAccessException
  {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object sync()
  {
    return origin;
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