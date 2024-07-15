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

import gov.llnl.ernie.data.EnergyScale;


/**
 *
 * @author nelson85
 * @param <Type> is the type of array holding the data. It should be an array of
 * primitives type such as int[], float[], or double[].
 */
public interface Spectrum<Type> extends RadiationData<Type>
{
  /**
   * Get the size of the spectrum.
   *
   * @return the number of channels
   */
  int size();

  /**
   * Set the energy bins associated with the spectrum.
   *
   * @param bins
   */
  void setEnergyScale(EnergyScale bins);

  /**
   * Get the energy bins associated with the spectrum.
   *
   * @return the energy bins associated with the spectrum, or null if no scale
   * is set.
   */
  EnergyScale getEnergyScale();

  /**
   * Get the data storage type for this spectrum.
   *
   * @return the type of the data storage.
   */
  Class getCountClass();

  /**
   * Get the counts that are below the valid energy scale.
   *
   * @return
   */
  double getUnderRangeCounts();

  /**
   * Get the counts that are above the valid energy scale.
   *
   * @return
   */
  double getOverRangeCounts();

  /**
   * Get the total counts in the region of interest.
   *
   * @param roi
   * @return
   */
  double getCounts(RegionOfInterest roi);

  double getRate(RegionOfInterest roi);

  default int getMinimumValidChannel()
  {
    return 0;
  }

  default int getMaximumValidChannel()
  {
    return size();
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