/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.ernie.rtk.view;

import gov.llnl.math.euclidean.Vector3;
import gov.llnl.math.euclidean.Vector3Ops;
import gov.llnl.math.euclidean.Versor;
import gov.llnl.utility.xml.bind.ReaderInfo;

@ReaderInfo(SensorFaceRectangularReader.class)
public class SensorFaceRectangular implements SensorFace
{
  double width, height;
  Vector3 origin;
  Versor orientation;

  public SensorFaceRectangular()
  {
  }

  public SensorFaceRectangular(double width, double height, Vector3 origin, Versor orientation)
  {
    this.width = width;
    this.height = height;
    this.origin = origin;
    this.orientation = orientation;
  }

  @Override
  public Vector3 getOrigin()
  {
    return origin;
  }

  @Override
  public Versor getOrientation()
  {
    return orientation;
  }

  @Override
  public double computeSolidAngle(Vector3 v)
  {
    Vector3 v2 = Vector3Ops.subtract(v, origin);
    Vector3 v3 = orientation.inv().rotate(v2);

    double vx = v3.getX();
    double vy = v3.getY();
    double vz = v3.getZ();
    if (vx < 0)
      return 0;

    // Integration of surface for equation
    //   getX/(getX^2+getY^2+getZ^2)^(3/2) over the surface
    return f(vx, vy - width / 2, vz - height / 2)
            - f(vx, vy + width / 2, vz - height / 2)
            - f(vx, vy - width / 2, vz + height / 2)
            + f(vx, vy + width / 2, vz + height / 2);
  }

  static public double f(double x, double y, double z)
  {
    return Math.atan(y * z / x / Math.sqrt(x * x + y * y + z * z));
  }

  @Override
  public double getArea()
  {
    return width * height;
  }

  public String toString()
  {
    return String.format("SensorFaceRectangular(%.2f,%.2f)", this.width, this.height);
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