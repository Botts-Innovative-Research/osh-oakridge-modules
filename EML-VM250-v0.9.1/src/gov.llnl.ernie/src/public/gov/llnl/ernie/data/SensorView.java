/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.ernie.data;

import gov.llnl.math.euclidean.Vector3;
import gov.llnl.math.euclidean.Vector3Ops;
import gov.llnl.math.euclidean.Versor;

/**
 * Calculator for field of view during sensor encounters.
 * 
 * @author nelson85
 */
public interface SensorView
{
  /**
   * Get the origin of the sensor.
   *
   * This will be used as the offset for any source.
   *
   * @return
   */
  Vector3 getOrigin();

  /**
   * Get the orientation of the sensor.
   *
   * @return
   */
  Versor getOrientation();

  /**
   * Get the area of the detector in terms of solid area.
   *
   * Most often this will be used in ratiometric terms. For example if we know
   * that a sensor produces 100 cps from a source a 1m, we compute the counts at
   * a new position as 100 times the solid angle at the new position divided by
   * the solid angle at 1m.
   *
   * @param v
   * @return
   */
  double computeSolidAngle(Vector3 v);

  /**
   * Get the solid angle at a distance in front of the sensor.
   *
   * This is used to compute the normalization term for relative measurements.
   *
   * @param distance
   * @return
   */
  default double computeDefaultSolidAngle(double distance)
  {
    // Create a vector at reference distance from the origin
    Vector3 base = Vector3.of(distance, 0, 0);

    // Rotate it with respect to the detector
    base = getOrientation().rotate(base);

    // Move it in front of the detector
    base = Vector3Ops.add(base, getOrigin());

    // Compute the view to the sensor
    return computeSolidAngle(base);
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