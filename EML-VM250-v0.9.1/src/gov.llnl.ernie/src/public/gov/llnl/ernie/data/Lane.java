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
import java.util.List;

/**
 * A lane represents all of the information about a portal.
 *
 * This included all data relating to the location and physical dimensions as
 * well a descriptions of the hardware required for analysis of its data.
 *
 * @author nelson85
 */
public interface Lane extends Serializable
{

  /**
   * @return an identifier for the port or terminal.
   *
   * TODO REFACTOR old dataSourceId maps to portId
   */
  long getPortId();

  /**
   * @return an identifier for the rpm. This indicates which portal if there are
   * multiple portals in the terminal.
   */
  long getRpmId();

  /**
   * @return TODO REFACTOR what is LaneConveyance?
   */
  String getLaneConveyance();

  /**
   * @return TODO REFACTOR what does this represent? What kind of values are
   * possible?
   */
  String getLaneType();

  /**
   * @return TODO REFACTOR what is LaneVector?
   */
  String getLaneVector();

  /**
   * @return height
   */
  double getLaneHeight();

  /**
   * Get the width of the lane.
   *
   * @return the lane width in meters.
   */
  double getLaneWidth();

  boolean isSecondary();

  /**
   * Get the properties of the vps. This will be a covariant return with the
   * specific vendor implementation.
   *
   * @return the properties of the vps.
   */
  VPSProperties getVPSProperties();

  /**
   * Access the specifics about the gamma sensors.
   *
   * @return a list containing the gamma sensor properties.
   */
  List<SensorProperties> getGammaSensorProperties();

  /**
   * Access the specifics about the neutron sensors.
   *
   * @return a list containing the neutron sensor properties.
   */
  List<SensorProperties> getNeutronSensorProperties();

  Vector3 convertGammaRatios(double ratio1, double ratio2);

  Vector3 convertNeutronRatios(double ratio1, double ratio2);

  int[] getGammaPanels(SensorPosition position);

  int[] getNeutronPanels(SensorPosition position);
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