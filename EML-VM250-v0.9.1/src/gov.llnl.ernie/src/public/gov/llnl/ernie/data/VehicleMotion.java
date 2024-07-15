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

import java.time.Instant;

//</editor-fold>
//<editor-fold desc="Derived" defaultstate="collapsed">
/**
 * Information on the position of the vehicle relative to the center of the
 * portal as a function of time.
 *
 * FIXME should we not use instant as the time unit for looking up the position?
 * As we have systems with different time bases for gamma and neutron we need a
 * more generic system of time.
 */
public interface VehicleMotion
{
  /**
   * Check to see if the motion profile is valid.
   *
   * @return true is valid, false otherwise.
   */
  boolean isGood();

  /**
   * Compute the position of the vehicle at a specific point in time.
   *
   * The distance is relative to the center of the portal in meters.
   *
   * @param time
   * @return
   */
  double getPosition(Instant time);

  /**
   * Convert measurement into position by time.
   *
   * @param measurement
   * @return
   */
  default double[] getPositions(SensorMeasurement measurement)
  {
    double[] out = new double[measurement.size()];
    for (int i = 0; i < measurement.size(); ++i)
    {
      out[i] = getPosition(measurement.getTime(i));
    }
    return out;
  }

  /**
   * Get the computed length of the vehicle.
   *
   * @return
   */
  double getVehicleLength();

  /**
   * Get the velocity of the vehicle as it enters the portal.
   *
   * @return in m/s
   */
  double getVelocityInitial();

  /**
   * Get the velocity of the vehicle as it leaves the portal.
   *
   * @return in m/s
   */
  double getVelocityFinal();

  boolean isReverse();

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