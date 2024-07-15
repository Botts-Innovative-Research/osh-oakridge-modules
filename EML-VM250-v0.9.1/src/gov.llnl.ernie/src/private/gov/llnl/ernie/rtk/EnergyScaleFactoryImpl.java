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
import gov.llnl.math.DoubleUtilities;
import gov.llnl.utility.annotation.Internal;

@Internal
public class EnergyScaleFactoryImpl extends EnergyScaleFactory
{
  @Override
  protected EnergyScale createNewScale(double[] bins)
  {
    return new EnergyBinsImpl(bins);
  }

//  @Override
//  protected EnergyPairsScale createNewScale(int channels, Collection<ChannelEnergyPair> pairs)
//  {
//    return new EnergyPairsScaleImpl(channels, pairs);
//  }

  @Override
  protected EnergyScale createNewSqrtScale(double begin, double end, int steps)
  {
    double[] values = new double[steps + 1];
    double begin2 = Math.sqrt(begin);
    double end2 = Math.sqrt(end);
    for (int i = 0; i < steps + 1; i++)
    {
      values[i] = DoubleUtilities.sqr(begin2 + (end2 - begin2) / steps * i);
    }
    return EnergyScaleFactory.newScale(values);
  }

  @Override
  protected EnergyScale createNewLinearScale(double begin, double end, int steps)
  {
    double[] values = new double[steps + 1];
    for (int i = 0; i < steps + 1; i++)
    {
      values[i] = begin + (end - begin) / steps * i;
    }
    return EnergyScaleFactory.newScale(values);
  }

  @Override
  protected EnergyScale createNewScaledScale(EnergyScale energyScale, double scaleFactor)
  {
//    if (energyScale instanceof EnergyPairsScale)
//    {
//      EnergyPairsScale pairsScale = (EnergyPairsScale) energyScale;
//      int channels = pairsScale.getChannels();
//      ArrayList<ChannelEnergyPair> tmp = new ArrayList<>(pairsScale);
//      ChannelEnergyUtilities.scaleChannels(tmp, scaleFactor);
//      return new EnergyPairsScaleImpl(channels, tmp);
//    }
    double[] values = new double[energyScale.getChannels() + 1];
    for (int i = 0; i < values.length; i++)
    {
      values[i] = energyScale.getEnergyOfEdge(scaleFactor * i);
    }
    return EnergyScaleFactory.newScale(values);
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