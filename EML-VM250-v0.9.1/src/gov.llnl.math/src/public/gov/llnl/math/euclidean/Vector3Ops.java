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

/**
 *
 * @author nelson85
 */
public class Vector3Ops
{
  public static Vector3 add(Vector3 v1, Vector3 v2)
  {
    return Vector3.of(v1.getX() + v2.getX(), v1.getY() + v2.getY(), v1.getZ() + v2.getZ());
  }

  public static Vector3 subtract(Vector3 v1, Vector3 v2)
  {
    return Vector3.of(v1.getX() - v2.getX(), v1.getY() - v2.getY(), v1.getZ() - v2.getZ());
  }

  public static Vector3 multiply(Vector3 v1, double s)
  {
    return Vector3.of(v1.getX() * s, v1.getY() * s, v1.getZ() * s);
  }

  public static double multiplyDot(Vector3 v1, Vector3 v2)
  {
    return v1.getX() * v2.getX() + v1.getY() * v2.getY() + v1.getZ() * v2.getZ();
  }

  public static double length(Vector3 v1)
  {
    double x = v1.getX();
    double y = v1.getY();
    double z = v1.getZ();
    return Math.sqrt(x * x + y * y + z * z);
  }

  public static double correlation(Vector3 v1, Vector3 v2)
  {
    double x1 = v1.getX();
    double y1 = v1.getY();
    double z1 = v1.getZ();
    double x2 = v2.getX();
    double y2 = v2.getY();
    double z2 = v2.getZ();

    return (x1 * x2 + y1 * y2 + z1 * z2)
            / Math.sqrt((x1 * x1 + y1 * y1 + z1 * z1) * (x2 * x2 + y2 * y2 + z2 * z2));
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