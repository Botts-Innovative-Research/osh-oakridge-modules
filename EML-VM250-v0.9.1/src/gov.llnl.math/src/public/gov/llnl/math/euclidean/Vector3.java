/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.math.euclidean;

import gov.llnl.math.internal.euclidean.Vector3Impl;
import gov.llnl.math.internal.euclidean.Vector3Reader;
import gov.llnl.math.internal.euclidean.Vector3Writer;
import gov.llnl.utility.xml.bind.ReaderInfo;
import gov.llnl.utility.xml.bind.WriterInfo;

/**
 *
 * @author nelson85
 */
@ReaderInfo(Vector3Reader.class)
@WriterInfo(Vector3Writer.class)
public interface Vector3
{
  final static Vector3 ZERO = Vector3.of(0, 0, 0);
  final static Vector3 AXIS_X = Vector3.of(1, 0, 0);
  final static Vector3 AXIS_Y = Vector3.of(0, 1, 0);
  final static Vector3 AXIS_Z = Vector3.of(0, 0, 1);

  /** Create a vector3.
   *
   * @param x is the x dimension of the vector.
   * @param y is the y dimension of the vector.
   * @param z is the z dimension of the vector.
   * @return a new vector.
   */
  static Vector3 of(double x, double y, double z)
  {
    return new Vector3Impl(x, y, z);
  }

  double getX();

  double getY();

  double getZ();

  default double norm()
  {
    double vx = getX();
    double vy = getY();
    double vz = getZ();
    return Math.sqrt(vx * vx + vy * vy + vz * vz);
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