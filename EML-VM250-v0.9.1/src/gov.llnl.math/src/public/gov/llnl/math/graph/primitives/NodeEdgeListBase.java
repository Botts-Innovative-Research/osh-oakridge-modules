/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.math.graph.primitives;

/**
 *
 * @author nelson85
 */
public class NodeEdgeListBase
{
  public NodeEdgeListBase()
  {
    size_ = 0;
    head_ = null;
  }

  public int size()
  {
    return size_;
  }

  public void append(ConnectionBase e)
  {
    size_++;
    if (head_ == null)
    {
      head_ = e;
      return;
    }
    // Track to end of the list and append
    ConnectionBase e2 = head_;
    while (e2.getNext() != null)
    {
      e2 = e2.getNext();
    }
    e2.setNext(e);
  }

  int size_;
  ConnectionBase head_;
};


/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */