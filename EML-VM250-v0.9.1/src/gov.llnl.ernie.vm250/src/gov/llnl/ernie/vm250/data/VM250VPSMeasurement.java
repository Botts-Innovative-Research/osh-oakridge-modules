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

import gov.llnl.ernie.data.VPSMeasurement;
import gov.llnl.ernie.data.VPSProperties;
import gov.llnl.utility.UUIDUtilities;
import java.io.Serializable;
import java.time.Instant;

/**
 *
 * @author guensche1
 */
public class VM250VPSMeasurement implements VPSMeasurement, Serializable
{
  private static final long serialVersionUID = UUIDUtilities.createLong("VM250VPSMeasurement-v1");

  private final VM250Record outer;

  public VM250VPSMeasurement(final VM250Record outer)
  {
    this.outer = outer;
  }

  @Override
  public VPSProperties getVPSProperties()
  {
    return outer.getLane().getVPSProperties();
  }

  @Override
  public boolean getDirectionTraveled()
  {
    // TODO is this reasonable as VM250 doesn't appear to have any direction field in the database?
    // getDirectionTraveled() is currently not called by anything.
    return true;
  }

  @Override
  public int getGammaOccupancyStart()
  {
    return outer.getInternal().getSetupInfo().getIntervals();
  }

  @Override
  public int getGammaOccupancyEnd()
  {
    return outer.getInternal().getLength()
            - outer.getInternal().getSetupInfo().getOccupancyHoldin();
  }

  @Override
  public int getNeutronOccupancyStart()
  {
    return 0;
  }

  @Override
  public int getNeutronOccupancyEnd()
  {
    VM250RecordInternal internal = outer.getInternal();
    return internal.panelData[0].size_n_;
  }

  @Override
  public Instant getOccupancyStart()
  {
    throw new UnsupportedOperationException("Required by interface VPSMeasurement but unused.");
  }

  @Override
  public Instant getOccupancyEnd()
  {
    throw new UnsupportedOperationException("Required by interface VPSMeasurement but unused.");
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