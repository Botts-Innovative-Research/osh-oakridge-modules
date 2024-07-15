/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.math.spline;

import gov.llnl.math.MathExceptions;
import gov.llnl.math.MathExceptions.DomainException;
import gov.llnl.math.function.Function;
import gov.llnl.utility.UUIDUtilities;
import java.io.Serializable;
import java.util.List;

/**
 *
 * @author nelson85
 * @param <PointType>
 */
public abstract class Spline<PointType extends Spline.ControlPoint> implements Function, Serializable
{
  private static final long serialVersionUID = UUIDUtilities.createLong("Spline");
  EndBehavior endBehavior = EndBehavior.LINEAR;

  @Deprecated
  public double[] interpolate(double in[]) throws MathExceptions.DomainException
  {
    double[] out = new double[in.length];
    for (int i = 0; i < in.length; i++)
      out[i] = applyAsDouble(in[i]);
    return out;
  }

  /**
   * Determines if the value is within the domain that is defined without
   * extention.
   *
   * @param d
   * @return true if the value is the defined domain.
   */
  public abstract boolean contains(double d);

  /**
   * Get the interpolated value for scalar. The behavior of the evaluation
   * outside of the domain of the spline depends on the end behavior.
   *
   * @param input is a value to evaluate.
   * @return the spline evaluated at the point input.
   * @throws DomainException if the input is outside of the domain and there is
   * no extrapolation method set.
   */
  @Override
  public abstract double applyAsDouble(double input)
          throws DomainException;

  // PENDING we should generalize this for other functions that have search requirements
  // for domains.
  /**
   * Procedure to accelerate evaluation when the inputs are in order.
   *
   * @param in is an array of values to be evaluated. All of the items in the
   * range from begin to end must be in order.
   * @param begin is the start of the range inclusive.
   * @param end is the end of the range exclusive.
   * @return a new list with the each of the element i
   * @throws DomainException if the input is outside of the domain and there is
   * no extrapolation method set.
   */
  public abstract double[] evaluateRangeOrdered(double[] in, int begin, int end)
          throws DomainException;

  /**
   * Define the behavior of the spline beyond the defined domain.
   *
   * @param eb
   */
  public void setEndBehavior(EndBehavior eb)
  {
    this.endBehavior = eb;
  }

  /**
   * Get the behavior of the spline beyond the defined domain
   *
   * @return the endBehavior
   */
  public EndBehavior getEndBehavior()
  {
    return endBehavior;
  }

  public abstract List<PointType> getControl();

  /**
   * Base class for control points used by all spline functions.
   */
  public static abstract class ControlPoint implements Serializable
  {
    private static final long serialVersionUID = UUIDUtilities.createLong("Spline.ControlPoint");

    public double x;
    public double y;

    public double getX()
    {
      return x;
    }

    public double getY()
    {
      return y;
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