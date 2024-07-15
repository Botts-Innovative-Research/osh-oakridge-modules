/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.utility;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

/**
 *
 * @author nelson85
 */
public class CollectionUtilities
{

  // Replace with Java 8, FU Matlab
  public interface Accessor<T, T2>
  {
    T get(T2 t2);
  }

  static public <T, T2> Collection<T> adaptor(Collection<T2> base, Accessor<T, T2> accessor)
  {
    return new AdaptorCollection<>(base, accessor);
  }

  static class AdaptorCollection<T, T2> implements Collection<T>
  {
    Collection<T2> base;
    Accessor<T, T2> accessor;

    AdaptorCollection(Collection<T2> base, Accessor<T, T2> accessor)
    {
      this.base = base;
      this.accessor = accessor;
    }

    @Override
    public int size()
    {
      return base.size();
    }

    @Override
    public boolean isEmpty()
    {
      return base.isEmpty();
    }

    @Override
    public boolean contains(Object o)
    {
      return find(o) != null;
    }

    @Override
    public Iterator<T> iterator()
    {
      return Iterators.<T, T2>adaptor(base.iterator(), accessor);
    }

    @Override
    public Object[] toArray()
    {
      return new ArrayList<>(this).toArray();
    }

    @Override
    public <T> T[] toArray(T[] ts)
    {
      return (T[]) new ArrayList<>(this).toArray(ts);
    }

    @Override
    public boolean add(T e)
    {
      throw new UnsupportedOperationException("Not supported.");
    }

    final Iterator<T2> find(Object o)
    {
      Iterator<T2> iter;
      for (iter = base.iterator(); iter.hasNext();)
      {
        T2 next = iter.next();
        if (next.equals(o))
          return iter;
      }
      return null;
    }

    @Override
    public boolean remove(Object o)
    {
      Iterator<T2> iter = find(o);
      if (iter == null)
        return false;
      iter.remove();
      return true;
    }

    @Override
    public void clear()
    {
      base.clear();
    }

//<editor-fold desc="unsupported">
    @Override
    public boolean containsAll(Collection<?> clctn)
    {
      throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public boolean addAll(Collection<? extends T> clctn)
    {
      throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public boolean removeAll(Collection<?> clctn)
    {
      throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public boolean retainAll(Collection<?> clctn)
    {
      throw new UnsupportedOperationException("Not supported.");
    }
//</editor-fold>
  }

  /**
   * Function to get an object
   *
   * @param collection
   * @return
   */
  public static Object[] toArray(Collection collection)
  {
    Object[] out = new Object[collection.size()];
    int i = 0;
    for (Object obj : collection)
    {
      out[i++] = obj;
    }
    return out;
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