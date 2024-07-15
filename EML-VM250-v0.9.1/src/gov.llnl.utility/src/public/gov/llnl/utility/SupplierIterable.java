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

/**
 * Use a supplier as a iterable. This can only traverse the supplier once. It
 * stops on the first null.
 *
 * @author nelson85
 */
public class SupplierIterable<T> implements Iterable<T>
{
  final Supplier<T> supplier;
  T next_;

  public SupplierIterable(Supplier<T> supplier)
  {
    this.supplier = supplier;
  }

  @Override
  public Iterator<T> iterator()
  {

    return new Iterator<T>()
    {
      @Override
      public boolean hasNext()
      {
        next_ = supplier.get();
        return (next_ != null);
      }

      @Override
      public T next()
      {
        return next_;
      }

    };
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