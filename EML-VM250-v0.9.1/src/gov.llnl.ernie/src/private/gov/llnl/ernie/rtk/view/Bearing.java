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

/**
 * Utility class for computing a bearing with vectors.
 *
 * @author nelson85
 */
public class Bearing
{
  /**
   * Compute the bearing to an object relative to our travel.
   *
   * @param course is the direction we are traveling.
   * @param object is the direction to the object relative to our position.
   * @return the angle to toward the object.
   */
  public static double compute(Vector3 course, Vector3 object)
  {
    double vx = course.getX();
    double vy = course.getY();
    double r = Math.sqrt(vx * vx + vy * vy);

    // There is no bearing if we are not moving.
    if (r == 0)
      return 0;

    vx /= r;
    vy /= r;
    double ox = object.getX();
    double oy = object.getY();
    double cos = ox * vx + oy * vy;
    double sin = oy * vx - ox * vy;
    return Math.atan2(sin, cos);
  }

//  static public void main(String[] args)
//  {
//    System.out.println(compute(Vector3.of(2, 0, 0), Vector3.of(4, 0, 0)));
//    System.out.println(compute(Vector3.of(2, 0, 0), Vector3.of(0, 4, 0)));
//    System.out.println(compute(Vector3.of(2, 0, 0), Vector3.of(0, -4, 0)));
//    
//        System.out.println(compute(Vector3.of(0, 2, 0), Vector3.of(4, 0, 0)));
//    System.out.println(compute(Vector3.of(0, 2, 0), Vector3.of(0, 4, 0)));
//    System.out.println(compute(Vector3.of(0, 2, 0), Vector3.of(0, -4, 0)));
//  }
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