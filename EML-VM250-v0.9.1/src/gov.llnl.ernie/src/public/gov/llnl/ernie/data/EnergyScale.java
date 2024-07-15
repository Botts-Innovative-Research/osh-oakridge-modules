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

import gov.llnl.ernie.rtk.EnergyScaleReader;
import gov.llnl.utility.Expandable;
import gov.llnl.utility.xml.bind.ReaderInfo;
import java.io.Serializable;

/**
 *
 *
 * @author nelson85
 */
@ReaderInfo(EnergyScaleReader.class)
public interface EnergyScale extends Expandable, Serializable
{
  /**
   * Get the number of channels in the spectrum.
   *
   * @return
   */
  int getChannels();

  /**
   * Get the energy corresponding to an edge.
   *
   * @param edge
   * @return the energy boundary for this edge.
   */
  double getEdge(int edge);

  /**
   * Get the energy associated with the fractional bin.
   *
   * @param edge
   * @return
   */
  double getEnergyOfEdge(double edge);

  /**
   * Convert the energy scale to bin centers. This is primarily used for
   * plotting purpose.
   *
   * @return the centers for each channel.
   */
  double[] getCenters();

  /**
   * Get the energy edges. There is one more edge than channel.
   *
   * @return the edge for each channel.
   */
  double[] getEdges();

  /**
   * Find the bin which contains this energy. Bins are defined as being
   * inclusive of their lower edge.
   *
   * @param energy
   * @return the edge number for the first edge equal to or below this energy.
   */
  int findBinFloor(double energy);

  /**
   * Find the bin which is above this energy.
   *
   * @param energy
   * @return the edge number for the first bin equal to or above this energy.
   */
  int findBinCeiling(double energy);

  /**
   * Find the fractional bin associated with this energy with. The returned bin
   * has an edge origin, if you need a center origin, subtract 0.5
   *
   * @param energy
   * @return fractional bins
   */
  double findBin(double energy);
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