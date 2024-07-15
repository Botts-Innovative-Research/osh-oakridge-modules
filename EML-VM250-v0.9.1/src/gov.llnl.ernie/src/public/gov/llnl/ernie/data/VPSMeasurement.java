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

//</editor-fold>

import java.time.Instant;

/**
 * Structure of the VPS measurement. Mostly opaque structure, will always use
 * the vendor specific routine to translate this. The preprocessor will
 * translate this to a motion profile.
 *
 * This also contains the start and end of occupancy indices.
 */
public interface VPSMeasurement
{
  /**
   * Access the properties of the vps. This should be the same as the
   * Lane.getVPSProperties().
   *
   * @return the vendor specific implementation of the vps properties.
   */
  public VPSProperties getVPSProperties();

  /**
   * Get the direction of travel.
   *
   * @return true if in the forward x direction, false otherwise.
   */
  public boolean getDirectionTraveled();

  public Instant getOccupancyStart();
  
  public Instant getOccupancyEnd();
 
  /**
   * Get the sample index for the first gamma measurements containing the
   * vehicle. Front of vehicle should pass through the portal in this time
   * slice.
   *
   * @return the sample index for the gamma measurements.
   */
  public int getGammaOccupancyStart();

  /**
   * Get the sample index for the first gamma measurements past the vehicle.
   *
   * @return the sample index for the gamma measurements.
   */
  public int getGammaOccupancyEnd();

  /**
   * Get the sample index for the first neutron measurements containing the
   * vehicle. Front of vehicle should pass through the portal in this time
   * slice.
   *
   * @return the sample index for the neutron measurements.
   */
  public int getNeutronOccupancyStart();

  /**
   * Get the sample index for the first neutron measurements past the vehicle.
   *
   * @return the sample index for the neutron measurements.
   */
  public int getNeutronOccupancyEnd();
  // num beams if needed

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