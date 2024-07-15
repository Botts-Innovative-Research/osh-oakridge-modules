/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.ernie.io;

import gov.llnl.ernie.data.Lane;
import gov.llnl.ernie.internal.io.LaneDatabaseReader;
import gov.llnl.utility.xml.bind.ReaderInfo;

/**
 * Database holding the properties of each lane.
 *
 * @author nelson85
 */
@ReaderInfo(LaneDatabaseReader.class)
public interface LaneDatabase
{
  /**
   * Completes setting the properties of a lane.
   *
   * Uses the portal and rpm id to set the properties of the lane. For most
   * lanes this will be the same for all portals of a given type, but it can
   * differ if we compute different properties for each lane.
   *
   * The supplied lane is modified.
   *
   * @param lane
   */
  void lookup(Lane lane);
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