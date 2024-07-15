/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.math.graph.primatives;

/**
 *
 * @author nelson85
 * @param <NodeType>
 */
public class Edge<NodeType extends NodeBase> extends EdgeBase
{
  public Edge()
  {
  }

//  public void setNode(NodeType node, boolean side)
//  {
//    if (side)
//      node1_ = node;
//    else
//      node0_ = node;
//  }
  public NodeType getNode(boolean side)
  {
    if (side)
      return (NodeType) node1_;
    return (NodeType) node0_;
  }

  @Override
  public Connection<NodeType, Edge<NodeType>> getConnection(boolean side)
  {
    return (Connection<NodeType, Edge<NodeType>>) super.getConnection(side);
  }

  @Override
  public NodeType getNodeFrom(ConnectionBase c)
  {
    return (NodeType) super.getNodeFrom(c);
  }

  @Override
  public Connection<NodeType, Edge<NodeType>> getConnectionFrom(NodeBase node)
  {
    return (Connection<NodeType, Edge<NodeType>>) super.getConnectionFrom(node);
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