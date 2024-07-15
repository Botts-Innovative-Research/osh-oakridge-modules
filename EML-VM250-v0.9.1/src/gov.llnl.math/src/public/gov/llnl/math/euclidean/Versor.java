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

import gov.llnl.math.internal.euclidean.VersorInv;
import gov.llnl.math.internal.euclidean.VersorImpl;
import gov.llnl.math.internal.euclidean.VersorReader;
import gov.llnl.math.internal.euclidean.VersorWriter;
import gov.llnl.utility.xml.bind.ReaderInfo;
import gov.llnl.utility.xml.bind.WriterInfo;

/**
 * Specialty representation of a unit length quaternion.
 *
 * Versors are used to represent a rotation in a Euclidian space.
 *
 * @author nelson85
 */
@ReaderInfo(VersorReader.class)
@WriterInfo(VersorWriter.class)
public interface Versor extends Quaternion
{
  public static final Versor ZERO = Versor.of(Vector3.AXIS_X, 0);

  /**
   * Create a versor.
   *
   * @param axis is the direction to rotate about.
   * @param angle is the angle to rotate.
   * @return a new versor.
   */
  public static Versor of(Vector3 axis, double angle)
  {
    return new VersorImpl(axis, angle);
  }

  /**
   * Create a versor.
   *
   * FIXME remove this.
   *
   * @param x is the x dimension of the versor to rotate about.
   * @param y is the y dimension of the versor to rotate about.
   * @param z is the z dimension of the versor to rotate about.
   * @param angle is the angle to rotate.
   * @return a new versor.
   */
  public static Versor of(double x, double y, double z, double angle)
  {
    double r = Math.sqrt(x * x + y * y + z * z);
    if (r == 0)
    {
      x = 1;
      angle = 0;
      r = 1;
    }
    double q = Math.sin(angle / 2);
    return new VersorImpl(Math.cos(angle / 2), q * x / r, q * y / r, q * z / r);
  }

  /**
   * Produce a versor that rotates in the opposite direction.
   *
   * @return
   */
  default Versor inv()
  {
    return new VersorInv(this);
  }

  /**
   * Rotate a vector by a versor.
   *
   * @param v
   * @return
   */
  default Vector3 rotate(Vector3 v)
  {
    if (getU() == 0)
      return Vector3.of(v.getX(), v.getY(), v.getZ());
    Quaternion q = QuaternionOps.multiply(this, v);
    Quaternion q2 = QuaternionOps.multiply(q, this.inv());
    return Vector3.of(q2.getI(), q2.getJ(), q2.getK());
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