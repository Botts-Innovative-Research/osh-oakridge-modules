/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.utility.internal;

import gov.llnl.utility.annotation.Internal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * View for iterating in reverse order.
 */
@Internal
public class ReverseCollection<T> implements Collection<T>
{
  private final List<T> list;

  public ReverseCollection(List<T> list)
  {
    this.list = list;
  }

  @Override
  public Iterator<T> iterator()
  {
    return new Iterator<T>()
    {
      final ListIterator<T> iter = list.listIterator(list.size());

      @Override
      public boolean hasNext()
      {
        return iter.hasPrevious();
      }

      @Override
      public T next()
      {
        return iter.previous();
      }

      @Override
      public void remove()
      {
        iter.remove();
      }
    };
  }

  @Override
  public int size()
  {
    return this.list.size();
  }

  @Override
  public boolean isEmpty()
  {
    return this.list.isEmpty();
  }

  @Override
  public boolean contains(Object o)
  {
    return this.list.contains(o);
  }

  @Override
  public Object[] toArray()
  {
    Object[] out = new Object[this.size()];
    int i = 0;
    for (Object v : this)
    {
      out[i++] = v;
    }
    return out;
  }

  @Override
  public <T> T[] toArray(T[] a)
  {
    @SuppressWarnings("unchecked")
    List<T> q = new ArrayList(list);
    Collections.reverse(q);
    q.toArray(a);
    return a;
  }

  @Override
  public boolean add(T e)
  {
    list.add(0, e);
    return true;
  }

  @Override
  public boolean remove(Object o)
  {
    return list.remove(o);
  }

  @Override
  public boolean containsAll(Collection<?> c)
  {
    return list.containsAll(c);
  }

  @Override
  public boolean addAll(Collection<? extends T> c)
  {
    for (T element : c)
    {
      list.add(0, element);
    }
    return true;
  }

  @Override
  public boolean removeAll(Collection<?> c)
  {
    return list.removeAll(c);
  }

  @Override
  public boolean retainAll(Collection<?> c)
  {
    return list.retainAll(c);
  }

  @Override
  public void clear()
  {
    list.clear();
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