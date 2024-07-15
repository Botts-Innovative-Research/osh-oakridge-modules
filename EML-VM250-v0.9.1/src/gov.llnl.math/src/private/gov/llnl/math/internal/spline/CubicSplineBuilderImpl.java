/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.math.internal.spline;

import gov.llnl.math.matrix.special.MatrixTriDiagonal;
import gov.llnl.math.spline.CubicHermiteSpline;
import gov.llnl.math.spline.CubicHermiteSplineFactory;
import gov.llnl.utility.annotation.Debug;
import java.util.ArrayList;
import java.util.Collections;

/**
 *
 * @author nelson85
 */
@Debug
public class CubicSplineBuilderImpl implements CubicHermiteSpline.CubicSplineBuilder
{
  @Debug
  public ArrayList<ControlConstraint> constraints = new ArrayList<>();

  public void addControl(double x, double y)
  {
    constraints.add(new ControlConstraint(x, y, 0, false));
  }

  public void addControl(double x, double y, double m)
  {
    constraints.add(new ControlConstraint(x, y, m, true));
  }

  public CubicHermiteSpline construct()
  {
    // Sort the control points
    Collections.sort(constraints, (ControlConstraint c1, ControlConstraint c2) -> Double.compare(c1.x, c2.x));
    int n = constraints.size();
    double[] dy = new double[n - 1];
    double[] dx = new double[n - 1];
    double[] d = new double[n];
    double[] l0 = new double[n - 1];
    double[] l1 = new double[n - 1];
    double[] q = new double[n];
    for (int i = 0; i < n - 1; ++i)
    {
      dx[i] = constraints.get(i + 1).x - constraints.get(i).x;
      dy[i] = constraints.get(i + 1).y - constraints.get(i).y;
    }
    // Slope constraining equations for bound and unbound points
    double u0 = 0;
    double v0 = 0;
    for (int i = 0; i < n; ++i)
    {
      double u1 = 0;
      double v1 = 0;
      if (i < n - 1)
      {
        u1 = 1 / dx[i];
        v1 = dy[i] * u1 * u1;
      }
      ControlConstraint c1 = constraints.get(i);
      if (c1.constrainedSlope)
      {
        d[i] = 1;
        q[i] = c1.m;
      }
      else
      {
        if (i > 0)
          l0[i - 1] = u0;
        d[i] = 2 * (u0 + u1);
        if (i < n - 1)
          l1[i] = u1;
        q[i] = 3 * (v0 + v1);
      }
      u0 = u1;
      v0 = v1;
    }
  
    // Solve for constant acceleration (natural spline)
/*    for (int i = 0; i < n - 1; i++)
    {
      double f = l0[i] / d[i];
      d[i + 1] -= f * l0[i];
      q[i + 1] -= f * q[i];
    }
    // Back substitution
    q[n - 1] /= d[n - 1];
    for (int i = 1; i < n; i++)
    {
      q[n - i - 1] = (q[n - i - 1] - q[n - i] * l1[n - i - 1]) / d[n - i - 1];
    }
*/
    MatrixTriDiagonal td = MatrixTriDiagonal.wrap(l0, d, l1);
    double[] m = td.divideLeft(q);
    
    double[] x= new double[n];
    double[] y= new double[n];
    for (int i = 0; i < n; ++i)
    {
      if (Double.isNaN(q[i]))
      {
        throw new RuntimeException("NaN");
      }
      ControlConstraint c1 = constraints.get(i);
      x[i]=c1.x;
      y[i]=c1.y;
    }
    
    return CubicHermiteSplineFactory.create(x,y,m);
  }
  
}
//</editor-fold>


/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */