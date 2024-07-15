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

import gov.llnl.ernie.internal.data.VPSPropertiesReader;
import gov.llnl.utility.xml.bind.ReaderInfo;

/**
 * Description of Vehicle Presence Sensors.
 *
 * This is intended to give a generic description of the vehicle detection
 * system used in a portal. Currently it is oriented towards the requirements of
 * the RPM8 system that uses optical beams rather the an generic structure
 * describing beams, ground loops, radar, camera or other methods of tracking
 * vehicles.
 *
 * The primary use of this class to define the data types for the motion
 * extraction in a generic fashion, but we do not currently have a generic
 * motion extract to define requirements. Thus this is just a placeholder until
 * we have enough system defined to require such a definition.
 *
 * FIXME rather than having one distance we should have each beam described as a
 * location in terms of x along the road and a height above the road.
 *
 * @author nelson85
 */
@ReaderInfo(VPSPropertiesReader.class)
public interface VPSProperties
{
  int getNumberBeams();

  /**
   * Get the beam distance.
   *
   * @return the beam distance in meters.
   */
  double getBeamDistance();

  // We will need a structure that defines which beams form a pair.
  // For the RPM8 this would be 2 pairs (0,1) and (2,3).

  public int[] getBeamOrder();
  
  public boolean[] getDisabled();
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