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
 * @param <NodeType>
 * @param <EdgeType>
 */
public final class Connection<NodeType extends NodeBase, EdgeType extends EdgeBase> extends ConnectionBase
{
  public Connection()
  {
  }

  public int getId()
  {
    return parent_.getId();
  }

  public NodeType getNode()
  {
    return (NodeType) parent_.getNodeFrom(this);
  }

  public EdgeType getEdge()
  {
    return (EdgeType) parent_;
  }

  public Connection<NodeType, EdgeType> getNext()
  {
    return (Connection<NodeType, EdgeType>) next_;
  }
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