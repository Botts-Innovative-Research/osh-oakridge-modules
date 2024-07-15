/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.ernie.internal.manipulator;

import static gov.llnl.math.DoubleUtilities.sqr;
import gov.llnl.math.MathExceptions.DomainException;
import gov.llnl.math.random.Random48;
import gov.llnl.math.spline.CubicHermiteSpline;
import gov.llnl.math.spline.CubicHermiteSplineFactory;
import java.util.ArrayList;

/**
 *
 * @author nelson85
 */
public class AnisotropicModel
{
  ArrayList<Surface> surfaces = new ArrayList<>();
  Random48 random = new Random48();
  private Vector2 origin;

  static public class Parameters
  {
    public double surfaceDensity; // the density of the source in g/cm^2 (0-5)
    public double surfaceVariability; // how much variablity of the surface density (0-2)
    public double volumeDensity; // the density of the cargo in g/cm^2 (0-5)
    public double volumeVariability; // how much varaiablity in contents density as a function of position (0-2)
    public double granularity; // the granularity of the cargo variablity (m)

    public Parameters(double surfaceDensity, double surfaceVariability,
            double volumeDensity, double volumeVariability,
            double granularity)
    {
      this.surfaceDensity = surfaceDensity;
      this.surfaceVariability = surfaceVariability;
      this.volumeDensity = volumeDensity;
      this.volumeVariability = volumeVariability;
      this.granularity = granularity;
    }
  }

// DoubleMatrix and DoubleArray operations are overkill for this one
  static public class Vector2
  {
    private static Vector2 subtract(Vector2 p2, Vector2 p1)
    {
      return new Vector2(p2.d[0] - p1.d[0], p2.d[1] - p1.d[1]);
    }

    private static double dot(Vector2 V, Vector2 P1)
    {
      return V.d[0] * P1.d[0] + V.d[1] * P1.d[1];
    }

    private static Vector2 scale(Vector2 V, double k2)
    {
      return new Vector2(V.d[0] * k2, V.d[1] * k2);
    }
    double[] d = new double[2];

    private Vector2(double px, double py)
    {
      d[0] = px;
      d[1] = py;
    }
  }

  static public class Surface
  {
    Vector2 p1;
    Vector2 p2;
    CubicHermiteSpline surfaceFunction;
    CubicHermiteSpline volumeFunction;
  }

  public void setSeed(long seed)
  {
    random.setSeed(seed);
  }

  public void defineBox(double bx1, double bx2, double by1, double by2,
          Parameters ap)
  {
    defineSurface(bx1, bx2, by1, by1, ap);
    defineSurface(bx1, bx2, by2, by2, ap);
    defineSurface(bx1, bx1, by1, by2, ap);
    defineSurface(bx2, bx2, by1, by2, ap);
  }

  public void defineSurface(double px1, double px2, double py1, double py2,
          Parameters ap)
  {
    Surface surface = new Surface();
    surface.p1 = new Vector2(px1, py1);
    surface.p2 = new Vector2(px2, py2);
    int variablePoints = (int) Math.floor(Math.sqrt(sqr(px1 - px2) + sqr(py1 - py2)) / ap.granularity);
    if (variablePoints < 4)
    {
      variablePoints = 4;
    }
    double x[] = new double[variablePoints + 1];
    double ySurface[] = new double[variablePoints + 1];
    double yVolume[] = new double[variablePoints + 1];
    for (int i = 0; i <= variablePoints; ++i)
    {
      x[i] = (double) i / (double) variablePoints;
      ySurface[i] = ap.surfaceDensity * Math.exp(Math.log(ap.surfaceVariability + 1) * random.nextDouble());
      yVolume[i] = ap.volumeDensity * Math.exp(Math.log(ap.volumeVariability + 1) * (2 * random.nextDouble() - 1));
    }
    surface.surfaceFunction = CubicHermiteSplineFactory.createNatural(x, ySurface);
    surface.volumeFunction = CubicHermiteSplineFactory.createNatural(x, yVolume);

    this.surfaces.add(surface);
  }

  /**
   * Remove all surfaces from the AnisotropicModel.
   */
  public void clearSurfaces()
  {
    this.surfaces.clear();
  }

  /**
   * Define the origin where the source is located. Positions are relative to
   * the front of the vehicle.
   *
   * @param px distance in m from front of the vehicle
   * @param py distance in m from the center line of vehicle.
   */
  public void defineSource(double px, double py)
  {
    this.origin = new Vector2(px, py);
  }

  /**
   * Compute the number of areal densities of material for this angle.
   *
   * @param angle in radians
   * @return the density of shielding materials in g/cm^2
   */
  public double compute(double angle)
  {
    double l = 0;
    double d = 1e10;
    Vector2 V = new Vector2(-Math.cos(angle), Math.sin(angle));
    for (Surface surface : this.surfaces)
    {
      try
      {
        Vector2 D = Vector2.subtract(surface.p2, surface.p1);
        Vector2 P1 = Vector2.subtract(surface.p1, origin);
        double k1 = Vector2.dot(V, P1);
        double k2 = Vector2.dot(V, D);
        Vector2 V2 = Vector2.scale(V, k2);
        Vector2 V1 = Vector2.scale(V, k1);
        Vector2 R = Vector2.subtract(D, V2);
        double t = Vector2.dot(R, Vector2.subtract(V1, P1))
                / Vector2.dot(R, R); // unitized distance along the line
        double u = k1 + k2 * t; // distance to the surface
        if (t < 0 || t > 1 || u < 0 || u > d)
        {
          continue;
        }
        d = u;
        double var_volume = surface.volumeFunction.applyAsDouble(t);
        double var_surface = surface.surfaceFunction.applyAsDouble(t);
//        double thickness = 1.0 / Math.sqrt(1 - sqr(Vector2.dot(V, D)) / Vector2.dot(D, D));
//        l = var_surface * thickness + var_volume * u;
        l = var_surface + var_volume * u * 100.0; // convert u from m to cm
      }
      catch (DomainException ex)
      {
        throw new RuntimeException(ex);
      }
    }
    return l;
  }

  public double[] compute(double[] angle)
  {
    double[] out = new double[angle.length];
    for (int i = 0; i < out.length; ++i)
    {
      out[i] = compute(angle[i]);
    }
    return out;
  }

  /**
   * Compute the number of areal densities of material for a point relative to
   * the model. Positions are relative to the front of the vehicle.
   *
   * @param px distance in m down the lane.
   * @param py distance in m towards the drivers side.
   * @return the density of shielding materials in g/cm^2
   */
  public double compute(double px, double py)
  {
    double angle = Math.atan2((py - origin.d[1]), -(px - origin.d[0]));
    return compute(angle);
  }

  /**
   * Compute the weighted shielding between two angles. This is used for
   * distributed sources.
   *
   * @param angle1 in radians
   * @param angle2 in radians
   * @return the density of shielding materials in g/cm^2
   */
  public double computeBetween(double angle1, double angle2)
  {
    int n = 20;
    double out = 0;
    for (int i = 0; i < n; i++)
    {
      double angle = angle1 + ((angle2 - angle1) * i) / (n - 1); // CHECK ME, This was 20.0
      out += Math.exp(-compute(angle));
    }
    out /= n;
    return -Math.log(out);
  }

  /**
   * Compute the weighted shielding between two points. This is used for
   * distributed sources.
   *
   * @param px1 distance in m down the lane for point 1.
   * @param py1 distance in m towards the drivers side for point 1.
   * @param px2 distance in m down the lane for point 2.
   * @param py2 distance in m towards the drivers side for point 2.
   * @return the density of shielding materials in g/cm^2
   */
  public double computeBetween(double px1, double py1, double px2, double py2)
  {
    double angle1 = Math.atan2((py1 - origin.d[1]), (px1 - origin.d[0]));
    double angle2 = Math.atan2((py2 - origin.d[1]), (px2 - origin.d[0]));
    return computeBetween(angle1, angle2);
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