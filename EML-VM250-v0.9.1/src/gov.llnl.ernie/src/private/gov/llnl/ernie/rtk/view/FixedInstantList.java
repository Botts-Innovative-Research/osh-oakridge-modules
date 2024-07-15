/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.ernie.rtk.view;

import gov.llnl.utility.UnmodifiableList;
import gov.llnl.utility.UnmodifiableListIterator;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * Creates a view that represents a set of instants in time.
 *
 * @author nelson85
 */
public class FixedInstantList extends UnmodifiableList<Instant>
{
  int n;
  Instant base;
  Duration step;
  long offset;

  public FixedInstantList(Instant base, Duration step, int steps)
  {
    this.base = base;
    this.n = steps;
    this.offset = step.toNanos();
    this.step = step;
  }

  @Override
  public int size()
  {
    return n;
  }

  @Override
  public boolean isEmpty()
  {
    return n == 0;
  }

  @Override
  public Object[] toArray()
  {
    return new ArrayList<>(this).toArray();
  }

  @Override
  public <T> T[] toArray(T[] ts)
  {
    return new ArrayList<>(this).toArray(ts);
  }

  @Override
  public Instant get(int i)
  {
    if (i < 0 || i >= n)
      throw new IndexOutOfBoundsException();
    return base.plus(offset * i, ChronoUnit.NANOS);
  }

  @Override
  public Iterator<Instant> iterator()
  {
    return this.listIterator();
  }

  @Override
  public ListIterator<Instant> listIterator()
  {
    return this.listIterator(0);
  }

  @Override
  public ListIterator<Instant> listIterator(int i)
  {
    return new UnmodifiableListIterator<Instant>()
    {
      int c = i;

      @Override
      public boolean hasNext()
      {
        return c < n;
      }

      @Override
      public Instant next()
      {
        c++;
        return get(c - 1);
      }

      @Override
      public boolean hasPrevious()
      {
        return c > 0;
      }

      @Override
      public Instant previous()
      {
        c--;
        return get(c);
      }

      @Override
      public int nextIndex()
      {
        return c;
      }

      @Override
      public int previousIndex()
      {
        return c - 1;
      }
    };
  }

  @Override
  public List<Instant> subList(int i, int i1)
  {
    return new FixedInstantList(get(i), step, i1 - i);
  }

  @Override
  public int indexOf(Object o)
  {
    if (!(o instanceof Instant))
      throw new ClassCastException();
    if (o == null)
      throw new NullPointerException();
    try
    {
      long delta = Duration.between(this.base, (Temporal) o).toNanos();
      long u = delta / offset;
      if (u >= n || u < 0 || !get((int) u).equals(o))
        return -1;
      return (int) u;
    }
    catch (ArithmeticException ex)
    {
      return -1;
    }
  }

  @Override
  public int lastIndexOf(Object o)
  {
    return indexOf(o);
  }

  @Override
  public boolean contains(Object o)
  {
    return indexOf(o) != -1;
  }

  static public void main(String[] args)
  {
    for (Instant t : new FixedInstantList(Instant.now(), Duration.ofMillis(100), 5))
    {
      System.out.println(t);
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