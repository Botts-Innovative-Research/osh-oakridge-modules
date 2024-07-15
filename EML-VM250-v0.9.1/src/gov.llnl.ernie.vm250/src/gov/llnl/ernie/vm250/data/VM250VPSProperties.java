/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.ernie.vm250.data;

import gov.llnl.ernie.data.VPSProperties;
import gov.llnl.utility.UUIDUtilities;
import java.io.Serializable;

/**
 * Properties of the vehicle presence sensors (VPS). This will mostly be an
 * opaque type as each vendor system is different. The vendor specific version
 * will contain the specifics of the vendor implementation. This structure is
 * the base class for those implementations.
 */
public class VM250VPSProperties implements VPSProperties, Serializable
{
  private static final long serialVersionUID = UUIDUtilities.createLong("VM250VPSProperties-v1");
  private final double vpsDistance;
  private final int numberBeams;

  public VM250VPSProperties(double distance, int beams)
  {
    this.vpsDistance = distance;
    this.numberBeams = beams;
  }

  /**
   * TODO REFACTOR what is VPS Distance?
   *
   * Distance from leading to trailing beams in meters
   *
   * @return
   */
  @Override
  public double getBeamDistance()
  {
    return vpsDistance;
  }

  // TODO REFACTOR hard code to 4 in 
  @Override
  public int getNumberBeams()
  {
    return numberBeams;
  }

  @Override
  public int[] getBeamOrder()
  {
    return new int[]{0,1,2,3};
  }

  @Override
  public boolean[] getDisabled()
  {
    return new boolean[4];
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