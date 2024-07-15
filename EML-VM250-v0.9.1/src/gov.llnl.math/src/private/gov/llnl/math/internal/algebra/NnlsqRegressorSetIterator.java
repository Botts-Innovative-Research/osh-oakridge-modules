/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.math.internal.algebra;

import java.util.Iterator;

/**
 *
 * @author nelson85
 */
public class NnlsqRegressorSetIterator implements Iterator<NnlsqRegressor>
{
  NnlsqRegressor current;
  NnlsqRegressor last;
  boolean forward = true;

  NnlsqRegressorSetIterator(NnlsqRegressor head, boolean forward)
  {
    super();
    this.current = head;
    this.forward = forward;
  }

  @Override
  public boolean hasNext()
  {
    return current != null;
  }

  @Override
  public NnlsqRegressor next()
  {
    last = current;
    if (forward)
      current = current.next;
    else
      current = current.prev;
    return last;
  }

  @Override
  public void remove()
  {
    if (last == null)
      throw new IllegalStateException();
    last.set.remove(last);
    last = null;
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