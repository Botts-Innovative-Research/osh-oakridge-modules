/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.ernie.vm250;

import gov.llnl.utility.UUIDUtilities;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

/**
 * Support class used to represent the velocity of a vehicle as a function of
 * time.
 *
 * This is primarily to support record manipulation.
 *
 * @author nelson85
 */
public class VelocityPairs implements Serializable
{
  private static final long serialVersionUID = UUIDUtilities.createLong("VelocityPairs-v1");
  List<Entry> entries = new LinkedList<>();

  public void add(double t1, double v1)
  {
    entries.add(new Entry(t1, v1));
  }

  public double getPosition(double time)
  {
    // linear interpolate from the first
    if (time < entries.get(0).time)
    {
      Entry e0 = entries.get(0);
      double v0 = e0.velocity;
      double t0 = e0.time;
      double p0 = e0.position;
      return v0 * (time - t0) + p0;
    }

    // linear interpolate from the last
    if (time > entries.get(entries.size() - 1).time)
    {
      Entry e0 = entries.get(entries.size() - 1);
      double v0 = e0.velocity;
      double t0 = e0.time;
      double p0 = e0.position;
      return v0 * (time - t0) + p0;
    }

    for (int i = 0; i < entries.size() - 1; ++i)
    {
      Entry e0 = entries.get(i);
      Entry e1 = entries.get(i + 1);
      if (time >= e0.time && time <= e1.time)
      {
        double accel = (e1.velocity - e0.velocity) / (e1.time - e0.time);
        return (accel / 2 * (time - e0.time) * (time - e0.time) + e0.velocity * (time - e0.time)) + e0.position;
      }
    }
    // Should never get here/
    return 0;
  }

  public int size()
  {
    return entries.size();
  }

  static public class Entry
  {
    public double time;
    public double velocity;
    public double position = 0;

    Entry(double time, double velocity)
    {
      this.time = time;
      this.velocity = velocity;
    }
  }

  public void clear()
  {
    entries.clear();
  }

  public void augmentPositions()
  {
    double position = 0;
    double v0 = entries.get(0).velocity;
    double t0 = entries.get(0).time;
    for (Entry entry : entries)
    {
      if (t0 == entry.time)
      {
        // do nothing
      }
      else
      {
        // Linear interpolate for velocity
        double t1 = entry.time;
        double v1 = entry.velocity;
        double accel = (v1 - v0) / (t1 - t0);
        position += accel / 2 * (t1 - t0) * (t1 - t0) + v0 * (t1 - t0);
        entry.position = position;
        v0 = v1;
        t0 = t1;
      }
    }
  }

  public Entry get(int i)
  {
    return entries.get(i);
  }

  public Entry first()
  {
    return entries.get(0);
  }

  public Entry last()
  {
    return entries.get(entries.size() - 1);
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