/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.ernie.rtk;

import gov.llnl.utility.Expandable;
import java.time.Instant;

/**
 *
 * @author seilhan3
 */
public interface RadiationData<Type> extends Expandable
{
  /**
   * Get the live time for the measurement. Units are in seconds. For most
   * systems the live time is simply given as the real time. In those cases the
   * true live time is equal to to the real time minus the counts times the dead
   * time.
   *
   * @return the live time measurement.
   */
  double getLiveTime();

  /**
   * Get the real time for the measurement. Units are in seconds.
   *
   * @return the real time of the measurement.
   */
  double getRealTime();

  /**
   * Get the starting time for this spectrum. The timestamp should be the start
   * time for this spectral data. Timestamps are optional. It will return the
   * null if there is no timestamp for this measurement.
   *
   * @return a date for the start of the measurement.
   */
  Instant getStartTime();

  /**
   * Get the end time for this measurement. This will be calculated from the
   * start time and the real time. It will return null if there is no timestamp
   * associated with this measurement.
   *
   * @return a date for the end of the measurement.
   */
  Instant getEndTime();

  /**
   * Get the count rate for the measurement.
   *
   * @return the count rate computed using the live time.
   */
  double getRate();

  /**
   * Get the total counts in the spectrum.
   *
   * @return
   */
  double getCounts();

  /**
   * Get the title of the spectrum.
   *
   * @return the title or null if not defined.
   */
  String getTitle();

  double[] toDoubles();
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