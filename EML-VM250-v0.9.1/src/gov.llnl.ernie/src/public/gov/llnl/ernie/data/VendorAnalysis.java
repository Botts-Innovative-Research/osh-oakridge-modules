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

/**
 *
 * @author nelson85
 */
public interface VendorAnalysis
{
  /**
   * Check if this vehicle alarmed.
   *
   * @return
   */
  public boolean isAlarm();

  /**
   * Check if there was a reported gamma alarm.
   *
   * @return true if there was a gamma alarm.
   */
  public boolean isGammaAlarm();

  /**
   * Check if there was a reported neutron alarm.
   *
   * @return true if there was a neutron alarm.
   */
  public boolean isNeutronAlarm();

  public boolean isScanError();

  /**
   * Get the number of segments used in processing.
   *
   * For some vendors, the record may be broken into multiple segments.
   * Individual segments may be analyzed.
   *
   * FIXME how do we denote if an analysis was for a completed scan or a partial
   * one.
   *
   * @return
   */
  public int getNumSegments();

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