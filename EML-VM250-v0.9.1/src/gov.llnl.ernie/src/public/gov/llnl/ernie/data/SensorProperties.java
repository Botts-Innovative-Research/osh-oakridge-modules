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
import java.io.Serializable;
import java.time.Duration;

/**
 * The sensor properties describe the physical hardware used to record the data.
 *
 * The panel properties are available in the lane as well as from the data
 * recorded by the sensor (SensorMeasurement and SensorBackground).
 *
 * @author nelson85
 */
public interface SensorProperties extends Serializable
{
  /**
   * Get the energy boundaries for the channels. There will be one more edge
   * than channel.
   *
   * @return the edges for the channels in keV.
   */
  EnergyScale getEnergyScale();
  
  Duration getSamplePeriod();
  
  /**
   * Get the expected time between samples for this sensor.
   *
   * @return the period in seconds.
   */
  default double getSamplePeriodSeconds()
  {
    return getSamplePeriod().toNanos()*1e-9;
  }

  /**
   * Get the number of channels in the sensor.
   *
   * @return number of channels.
   */
  int getNumberOfChannels();

  /**
   * Get the position of the sensor relative to the center of the lane.
   *
   * @return the x position
   * @throws IllegalStateException if this is a combined sensor.
   */
  Vector3 getOrigin() throws IllegalStateException;

  /**
   * Get the width of the panel (side-side).
   *
   * @return the height
   * @throws IllegalStateException if this is a combined sensor.
   */
  double getWidth() throws IllegalStateException;

  /**
   * Get the height of the panel (top - bottom).
   *
   * @return the height
   * @throws IllegalStateException if this is a combined sensor.
   */
  double getHeight() throws IllegalStateException;

  /**
   * Get the thickness of the panel for computing side contributions.
   *
   * @return the height
   * @throws IllegalStateException if this is a combined sensor.
   */
  double getThickness() throws IllegalStateException;
  
  double getSideCollimator(); 
 
  /**
   * Get if this a composition panel. A composition panel will not have a valid
   * position.
   *
   * @return
   */
  boolean isCombined();

  /**
   * Factors that describe the changes in spectral shape as a function of
   * suppression.
   *
   * @return
   */
  double[] getEnergyFactors();
  
  SensorView getSensorView();

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