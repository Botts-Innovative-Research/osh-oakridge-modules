/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.ernie.impl;

import gov.llnl.utility.UUIDUtilities;
import gov.llnl.ernie.data.SensorProperties;
import gov.llnl.ernie.data.EnergyScale;
import gov.llnl.ernie.data.SensorView;
import gov.llnl.math.euclidean.Vector3;
import java.time.Duration;

/**
 *
 * @author guensche1
 */
public class SensorPropertiesCombined implements SensorProperties
{
  private static final long serialVersionUID = UUIDUtilities.createLong("SensorPropertiesCombined-v1");

  SensorProperties base;


  public SensorPropertiesCombined(SensorProperties base)
  {
    this.base=base;
  }

  /**
   * Get if this a composition panel. A composition panel will not have a valid
   * position.
   *
   * @return
   */
  @Override
  public boolean isCombined()
  {
    return true;
  }

  /**
   * Get the number of channels in the panel.
   *
   * @return number of channels.
   */
  @Override
  public int getNumberOfChannels()
  {
    return base.getNumberOfChannels();
  }

  /**
   * Get the energy boundaries for the channels. There will be one more edge
   * than channel.
   *
   * @return the edges for the channels in keV.
   */
  @Override
  public EnergyScale getEnergyScale()
  {
    return base.getEnergyScale();
  }

  /**
   * Get the position of the panel relative to the center of the lane.
   *
   * @return the x position
   * @throws IllegalStateException if this is a combined panel
   */
  @Override
  public Vector3 getOrigin() throws IllegalStateException
  {
      throw new IllegalStateException("Combined panels do not have position.");
  }

  @Override
  public double getWidth() throws IllegalStateException
  {
      throw new IllegalStateException("Combined panels do not have height.");
  }

  /**
   * Get the height of the panel (top - bottom)
   *
   * @return the height
   * @throws IllegalStateException if this is a combined panel
   */
  @Override
  public double getHeight() throws IllegalStateException
  {
      throw new IllegalStateException("Combined panels do not have height.");
  }

  @Override
  public double getThickness() throws IllegalStateException
  {
      throw new IllegalStateException("Combined panels do not have thickness.");
  }

  @Override
  public double[] getEnergyFactors()
  {
    return base.getEnergyFactors();
  }


  @Override
  public Duration getSamplePeriod()
  {
    return base.getSamplePeriod();
  }

  @Override
  public SensorView getSensorView()
  {
    return null;
  }

  /**
   * @return the sideCollimator
   */
  @Override
  public double getSideCollimator()
  {
    return base.getSideCollimator();
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