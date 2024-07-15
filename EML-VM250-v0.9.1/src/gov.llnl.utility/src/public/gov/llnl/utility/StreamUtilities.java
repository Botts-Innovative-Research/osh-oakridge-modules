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

import java.util.Iterator;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 *
 * @author nelson85
 */
public class StreamUtilities
{

  /**
   * Constructs a stream from a supplier. The stream ends when the supplier
   * produces a null.
   *
   * @param producer
   * @return
   */
  public static <T> Stream<T> from(Supplier<T> producer)
  {
    Iterator<T> data = new Iterator<T>()
    {
      T value = producer.get();

      @Override
      public boolean hasNext()
      {
        return value != null;
      }

      @Override
      public T next()
      {
        T out = value;
        value = producer.get();
        return out;
      }
    };
    Iterable<T> iterable = () -> data;
    return StreamSupport.stream(iterable.spliterator(), false);
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