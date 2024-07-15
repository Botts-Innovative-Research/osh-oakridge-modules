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

import gov.llnl.math.euclidean.Vector3;

/**
 *
 * @author nelson85
 */
public class QuaternionOps
{
  public static Quaternion multiply(Quaternion q1, Quaternion q2)
  {
    return Quaternion.of(
            q1.getU() * q2.getU() - q1.getI() * q2.getI() - q1.getJ() * q2.getJ() - q1.getK() * q2.getK(),
            q1.getU() * q2.getI() + q1.getI() * q2.getU() + q1.getJ() * q2.getK() - q1.getK() * q2.getJ(),
            q1.getU() * q2.getJ() + q1.getJ() * q2.getU() + q1.getK() * q2.getI() - q1.getI() * q2.getK(),
            q1.getU() * q2.getK() + q1.getK() * q2.getU() + q1.getI() * q2.getJ() - q1.getJ() * q2.getI()
    );
  }

  public static Quaternion multiply(Quaternion q1, Vector3 q2)
  {
    return Quaternion.of(
           -q1.getI() * q2.getX() - q1.getJ() * q2.getY() - q1.getK() * q2.getZ(),
            q1.getU() * q2.getX() + q1.getJ() * q2.getZ() - q1.getK() * q2.getY(),
            q1.getU() * q2.getY() + q1.getK() * q2.getX() - q1.getI() * q2.getZ(),
            q1.getU() * q2.getZ() + q1.getI() * q2.getY() - q1.getJ() * q2.getX()
    );
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