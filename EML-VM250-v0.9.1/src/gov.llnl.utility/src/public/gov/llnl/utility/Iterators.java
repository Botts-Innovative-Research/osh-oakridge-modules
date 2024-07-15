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

import gov.llnl.utility.CollectionUtilities.Accessor;
import java.util.Iterator;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Collection of utilities for working with iterators.
 */
public class Iterators
{
  public static <T, T2> Iterator<T> cast(Iterator<T2> iter, Class<T> kls)
  {
    return new CastableIterator<>(iter);
  }

  public static <T, T2 extends T> Iterator<T> cast(Iterator<T2> iter)
  {
    return new CastableIterator<>(iter);
  }

  public static <T> Iterable<T> iterate(Stream<T> t)
  {
    return (Iterable<T>) t::iterator;
  }

  /**
   * Apply an adapter to every element of an Iterator.
   *
   * Adapter is applied on the fly as the elements are traversed.
   *
   * @param <T>
   * @param <T2>
   * @param iter
   * @param adapter
   * @return
   */
  public static <T, T2> Iterator<T> adapt(Iterator<T2> iter, Function<T2, T> adapter)
  {
    return new Iterator<T>()
    {
      @Override
      public boolean hasNext()
      {
        return iter.hasNext();
      }

      @Override
      public T next()
      {
        return adapter.apply(iter.next());
      }
    };
  }

  /**
   * Apply an adapter to every element of an Iterable.
   *
   * Adapter is applied on the fly as the elements are traversed.
   *
   * @param <T>
   * @param <T2>
   * @param iter
   * @param adapter
   * @return
   */
  public static <T, T2> Iterable<T> adapt(Iterable<T2> iter, Function<T2, T> adapter)
  {
    return () -> adapt(iter.iterator(), adapter);
  }

  /**
   * Adaptor for casting the type as it is iterated through.
   *
   * @author nelson85
   * @param <T>
   */
  public static class CastableIterator<T, T2> implements Iterator<T>
  {
    Iterator<T2> iter;

    CastableIterator(Iterator<T2> iter)
    {
      super();
      this.iter = iter;
    }

    @Override
    public boolean hasNext()
    {
      return iter.hasNext();
    }

    @Override
    @SuppressWarnings("unchecked")
    public T next()
    {
      return (T) iter.next();
    }

    @Override
    public void remove()
    {
      iter.remove();
    }
  }

  static public <T, T2> Iterator<T> adaptor(Iterator<T2> iter, Accessor<T, T2> accessor)
  {
    return new AdaptorIterator<>(iter, accessor);
  }

  static class AdaptorIterator<T, T2> implements Iterator<T>
  {
    Iterator<T2> iter;
    CollectionUtilities.Accessor<T, T2> accessor;

    AdaptorIterator(Iterator<T2> iter, Accessor<T, T2> accessor)
    {
      this.iter = iter;
      this.accessor = accessor;
    }

    @Override
    public boolean hasNext()
    {
      return iter.hasNext();
    }

    @Override
    public T next()
    {
      return accessor.get(iter.next());
    }

    @Override
    public void remove()
    {
      iter.remove();
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