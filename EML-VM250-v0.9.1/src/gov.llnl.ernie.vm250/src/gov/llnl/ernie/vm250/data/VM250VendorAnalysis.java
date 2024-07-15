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


import gov.llnl.ernie.data.VendorAnalysis;

/*
 * Copyright 2018, Lawrence Livermore National Security, LLC.
 * All rights reserved
 * 
 * Terms and conditions are given in "Notice" file.
 */

/**
 *
 * @author guensche1
 */
public class VM250VendorAnalysis implements VendorAnalysis
{
  private VM250RecordInternal record;

  public VM250VendorAnalysis(VM250RecordInternal record)
  {
    this.record = record;
  }
  
  @Override
  public boolean isAlarm()
  {
    return isGammaAlarm() || isNeutronAlarm();
  }

  @Override
  public boolean isGammaAlarm()
  {
    return record.getSegmentDescription().isAnyGammaAlarm();
  }

  @Override
  public boolean isNeutronAlarm()
  {
    return record.getSegmentDescription().isAnyNeutronAlarm();
  }

  @Override
  public boolean isScanError()
  {
    // FIXME probably need to add other reasons to go to scan error
    return !record.getSegmentDescription().isRealOccupancy();
  }

  @Override
  public int getNumSegments()
  {
    // FIXME what should this be?
    return 1;
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