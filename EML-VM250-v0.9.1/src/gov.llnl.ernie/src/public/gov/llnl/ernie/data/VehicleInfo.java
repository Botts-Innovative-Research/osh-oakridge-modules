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

import java.io.Serializable;

/**
 *
 * @author nelsonZ
 */
public interface VehicleInfo extends Serializable
{
  /**
   * @return the cabLength
   */
  double getCabLength();

  /** 
   * Get the vehicle id associated with this classification.
   * 
   * @return 
   */
  int getId();

  /**
   * @return the numTrailerAxels
   */
  int getNumTrailerAxels();

  /**
   * @return the passengerAreaEnd
   */
  double getPassengerAreaEnd();

  /**
   * @return the passengerAreaStart
   */
  double getPassengerAreaStart();

  /**
   * @return the payloadAreaEnd
   */
  double getPayloadAreaEnd();

  /**
   * @return the payloadAreaStart
   */
  double getPayloadAreaStart();

  /**
   * @return the trailerEndFromRear
   */
  double getTrailerEndFromRear();

  /**
   * @return the trailerStartFromRear
   */
  double getTrailerStartFromRear();

  int getType();

  /**
   * Get the length of the vehicle.
   * 
   * @return in meters
   */
  double getVehicleLength();

  /**
   * @return the bedOnly
   */
  boolean isBedOnly();

  /**
   * @return the hitchPresent
   */
  boolean isHitchPresent();

  /**
   * @return the isLongHaul
   */
  boolean isLongHaul();

  /**
   * @return the trailerPresent
   */
  boolean isTrailerPresent();

  /**
   * @return the hasWideAxel
   */
  boolean isWideAxel();

  default public boolean isInPassengerArea(double x)
  {
    return !(x < this.getPassengerAreaStart() || x > this.getPassengerAreaEnd());
  }

  default public boolean isInPayloadArea(double x)
  {
    // Payload is referenced from the end
    x = this.getVehicleLength() - x;
    return !(x > this.getTrailerStartFromRear() || x < this.getTrailerEndFromRear());
  }
  
  default double[] getFeatures()
  {
    return null;
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