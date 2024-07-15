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

/**
 * Representation of a detector which is sufficiently small that it is always in
 * the far field.
 *
 * @author nelson85
 */

@ReaderInfo(SensorFaceSmallReader.class)
public class SensorFaceSmall implements SensorFace
{
  double area;
  Vector3 origin;
  Versor orientation;

  public SensorFaceSmall(double area, Vector3 origin, Versor orientation)
  {
    this.area = area;
    this.origin = origin;
    this.orientation = orientation;
  }

  SensorFaceSmall()
  {
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
    System.out.println(v2);
    Vector3 v3 = orientation.inv().rotate(v2);
    System.out.println(v3);
    if (v3.getX() < 0)
      return 0;
    double r = v3.norm();
    double cos = v3.getX() / r;
    return cos / r / r * area;
  }

  @Override
  public double getArea()
  {
    return area;
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