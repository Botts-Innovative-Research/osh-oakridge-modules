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
 */
public abstract class EnergyScaleFactory
{

  static final EnergyScaleFactory INSTANCE = new EnergyScaleFactoryImpl();

  /**
   * Create a new energy scale with a fixed bin structure.
   *
   * @param bins
   * @return
   */
  public static EnergyScale newScale(double[] bins)
  {
    return INSTANCE.createNewScale(bins);
  }

//  /**
//   * Create a new energy scale from energy pairs.
//   *
//   * @param channels
//   * @param pairs
//   * @return
//   */
//  public static EnergyPairsScale newScale(int channels, Collection<ChannelEnergyPair> pairs)
//  {
//    return INSTANCE.createNewScale(channels, pairs);
//  }
  /**
   * Create a new energy scale with square root binning.
   *
   * This is often used for analysis in which the keeping the relative peak
   * widths the same is desirable.
   *
   * @param begin is the start of the energy scale in keV.
   * @param end is the end of the energy scale in keV.
   * @param steps is the number of energy channels in the energy scale.
   * @return a new energy scale.
   */
  public static EnergyScale newSqrtScale(double begin, double end, int steps)
  {
    return INSTANCE.createNewSqrtScale(begin, end, steps);
  }

  /**
   * Create a new linear scale.
   *
   * @param begin is the start of the energy scale in keV.
   * @param end is the end of the energy scale in keV.
   * @param steps is the number of energy channels in the energy scale.
   * @return a new energy scale.
   */
  public static EnergyScale newLinearScale(double begin, double end, int steps)
  {
    return INSTANCE.createNewLinearScale(begin, end, steps);
  }

  /**
   * Create an energy scale which is scaled from an existing one.
   *
   * This is used as part of the peak stabilizer routines to adjust an existing
   * scale.
   *
   * @param energyScale
   * @param ratio
   * @return
   */
  public static EnergyScale newScaledScale(EnergyScale energyScale, double ratio)
  {
    return INSTANCE.createNewScaledScale(energyScale, ratio);
  }

//<editor-fold desc="implementation">  
  protected abstract EnergyScale createNewScale(double[] bins);

//  protected abstract EnergyPairsScale createNewScale(int channels, Collection<ChannelEnergyPair> pairs);
  protected abstract EnergyScale createNewSqrtScale(double begin, double end, int steps);

  protected abstract EnergyScale createNewLinearScale(double begin, double end, int steps);

  protected abstract EnergyScale createNewScaledScale(EnergyScale energyScale, double scaleFactor);
//</editor-fold> 
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