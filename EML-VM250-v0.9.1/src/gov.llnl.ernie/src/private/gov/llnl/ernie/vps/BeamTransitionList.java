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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author nelson85
 */
public class BeamTransitionList implements java.io.Serializable, Iterable<BeamTransition>
{
  private static final long serialVersionUID = gov.llnl.utility.UUIDUtilities.createLong("BeamTransitionList-v1");
  public List<BeamTransition> transitions;

  public BeamTransitionList()
  {
    transitions = new LinkedList<>();
  }

  public BeamTransitionList(BeamTransitionList entry)
  {
    transitions = new LinkedList<>();
    for (BeamTransition bt : entry)
    {
      transitions.add(new BeamTransition(bt));
    }
  }

  public void clear()
  {
    transitions.clear();
  }

  public BeamTransition append(double transition, int present, int known)
  {
    BeamTransition b0 = new BeamTransition(transition, present, known);
    transitions.add(b0);
    return b0;
  }

  public void sort()
  {
    transitions.sort((p1, p2) -> Double.compare(p1.transition, p2.transition));
  }

  public int size()
  {
    return transitions.size();
  }

  public BeamTransition get(int i)
  {
    return transitions.get(i);
  }

  public int getFirstRising()
  {
    int i = 0;
    for (BeamTransition entry : transitions)
    {
      if (entry.present == 1 && entry.known == 1)
      {
        return i;
      }
      i++;
    }
    return -1;
  }

  public double getFirstTime()
  {
    double time = Double.MAX_VALUE;
    double t = 0;
    int p = 0;
    for (BeamTransition transition : this)
    {
      if (p == 1 && transition.present == 0 && transition.transition - t > 0.2)
      {
        time = Math.min(time, t);
        break;
      }
      p = transition.present;
      t = transition.transition;
    }
    return time;
  }

  public int getLastFalling()
  {
    // FIXME should look backwards in the list rather than walking
    // the whole list.
    int found = -1;
    int i = 0;
    for (BeamTransition entry : transitions)
    {
      if (entry.present == 0 && entry.known == 1)
      {
        found = i;
      }
      i++;
    }
    return found;
  }

  public double getLastTime()
  {
    double time = 0;
    int p = 0;
    for (BeamTransition transition : this)
    {
      if (p == 1 && transition.present == 0)
        time = Math.max(time, transition.transition);
      p = transition.present;
    }
    return time;
  }

  public double getTransition(int i1)
  {
    return this.transitions.get(i1).transition;
  }

  public double getVehicleLength()
  {
    int i1 = getFirstRising();
    int i2 = getLastFalling();
    if (i1 == -1 || i2 == -1)
    {
      return 0;
    }
    return getTransition(i2) - getTransition(i1);
  }

  /**
   * Trim the data before a given time.
   *
   * This operates only in time. Modifies this beam transition list.
   *
   * @param d
   */
  public void trimBefore(double d)
  {
    while (!transitions.isEmpty() && transitions.get(0).transition < d)
    {
      transitions.remove(0);
    }
  }

  /**
   * Trim the data after a given time.
   *
   * This operates only in time. Modifies this beam transition list.
   *
   * @param d
   */
  public void trimAfter(double d)
  {
    while (!transitions.isEmpty() && transitions.get(transitions.size() - 1).transition > d)
    {
      transitions.remove(transitions.size() - 1);
    }
  }

  public int countRising()
  {
    int count = 0;
    for (BeamTransition entry : transitions)
    {
      if (entry.present == 1)
      {
        count++;
      }
    }
    return count;
  }

  @Override
  public Iterator<BeamTransition> iterator()
  {
    return transitions.iterator();
  }

  public int getState(double d0)
  {
    int p = 0;
    double last = -1000;
    for (BeamTransition t : this.transitions)
    {
      if (d0 < t.transition)
        return p;
      last = t.transition;
      p = t.present;
    }
    return p;
  }

  /**
   * Create a region which is flipped in state.
   *
   * Used for simulation.
   *
   * @param d0 is the start of the flip
   * @param d1 is the end of the flip
   */
  public void flip(double d0, double d1)
  {
    BeamTransition t0 = this.append(d0, 0, 0);
    BeamTransition t1 = this.append(d1, 0, 0);
    this.sort();
    int p0 = 0;
    boolean found = false;
    for (BeamTransition t : this.transitions)
    {
      if (t == t0 || found)
      {
        t.present = 1 - p0;
        found = true;
      }
      p0 = t.present;
    }
  }

  public int getTransitionCount()
  {
    int c = 0;
    int p0 = 0;
    for (BeamTransition t : this.transitions)
    {
      if (t.present != p0)
        c++;
      p0 = t.present;
    }
    return c;
  }

  /**
   * Encode the data to a list.
   *
   * Use +/- for rising and falling occupancy transitions.
   *
   * @return
   */
  public double[] toArray()
  {
    double[] out = new double[this.transitions.size()];
    int i = 0;
    for (BeamTransition t : this.transitions)
    {
      if (t.transition < 0)
        throw new RuntimeException("negative transition found");
      out[i++] = ((t.present == 1) ? t.transition : -t.transition - 0.0001);
    }
    return out;
  }

  /**
   * Corresponding decoder.
   *
   * @param d
   */
  public void fromArray(double[] d)
  {
    this.transitions.clear();
    for (int i = 0; i < d.length; ++i)
    {
      if (d[i] < 0)
      {
        this.transitions.add(new BeamTransition(-d[i], 0, 1));
      }
      else
      {
        this.transitions.add(new BeamTransition(d[i], 1, 1));
      }
    }
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