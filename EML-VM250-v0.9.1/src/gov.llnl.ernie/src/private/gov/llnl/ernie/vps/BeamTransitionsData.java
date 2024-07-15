/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.ernie.vps;

/**
 * Representation of all of the VPS data from a transit.
 *
 * This is an intermediate form of the data computed from raw data.
 *
 * FIXME: this code is a mess. We have both representations in time and distance
 * sharing the same data structure. Some functions only work on the distance
 * structure, others only work on the time structure.
 *
 * It also had both data and calculation methods in the same structure.
 *
 * @author nelson85
 */
public class BeamTransitionsData implements java.io.Serializable
{
  private static final long serialVersionUID = gov.llnl.utility.UUIDUtilities.createLong("BeamTransitionsData-v1");
  // This is a list of transitions for each vps sensor

  // We have 4 Transition lists one for each vps
  // each vps has a list of transitions
  public BeamTransitionList entries[];

  public BeamTransitionsData()
  {
    entries = new BeamTransitionList[4];
    for (int i = 0; i < 4; ++i)
    {
      entries[i] = new BeamTransitionList();
    }
  }

  public BeamTransitionsData(BeamTransitionList[] b)
  {
    this.entries = b;
  }

  /**
   * Copy constructor for BeamTransitionData.
   *
   * Produces a deep copy.
   *
   * @param vps
   */
  public BeamTransitionsData(BeamTransitionsData vps)
  {
    entries = new BeamTransitionList[4];
    for (int i = 0; i < 4; ++i)
    {
      entries[i] = new BeamTransitionList(vps.entries[i]);
    }
  }

  /**
   * Convert the list into an array for plots and dump to disk.
   *
   * @return
   */
  public double[][] toArray()
  {
    double[][] out = new double[4][];
    for (int i = 0; i < 4; ++i)
    {
      out[i] = entries[i].toArray();
    }
    return out;
  }

  public void fromArray(double[][] d)
  {
    if (d == null)
      throw new NullPointerException("Input array is null");
    for (int i = 0; i < 4; ++i)
    {
      if (d[i] == null)
        throw new NullPointerException("Beam data is null");
      this.entries[i].fromArray(d[i]);
    }
  }

  public int size()
  {
    return entries.length;
  }

  // Only operates on distance list
  public double getVehicleLength()
  {
    double len = 0;
    for (BeamTransitionList entrie : entries)
    {
      len = Math.max(len, entrie.getVehicleLength());
    }
    return len;
  }

  public BeamTransitionList getBeam(int which)
  {
    return entries[which];
  }

  public void setVps(int i, BeamTransitionList decode)
  {
    entries[i] = decode;
  }

  public void clear()
  {
    for (BeamTransitionList entry : this.entries)
    {
      entry.clear();
    }
  }

  /**
   * Find the first transition time (excluding glitches)
   *
   * @return
   */
  public double getFirstTime()
  {
    double time = Double.MAX_VALUE;
    for (BeamTransitionList beam : this.entries)
    {
      time = Math.min(beam.getFirstTime(), time);
    }
    return time;
  }

  /**
   * Utility to get the last transition time.
   *
   * Used by the new motion extractor to compute the length of the time base.
   *
   * @return the time of the last falling transition.
   */
  public double getLastTime()
  {
    double time = 0;
    for (BeamTransitionList beam : this.entries)
    {
      time = Math.max(beam.getLastTime(), time);
    }
    return time;
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