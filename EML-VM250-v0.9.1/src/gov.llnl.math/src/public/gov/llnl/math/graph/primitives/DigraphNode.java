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
public class DigraphNode<NodeType extends NodeBase, EdgeType extends EdgeBase> extends NodeBase
{

  public DigraphNode()
  {
  }

  public void dispose()
  {
  }

  // Sizes of the edges
  public int getNumParents()
  {
    return parents_.size();
  }

  public int getNumChildren()
  {
    return children_.size();
  }

  public int getNumEdges()
  {
    return parents_.size() + children_.size();
  }

  // Interface to create node linkages (Should be private)
  public void appendParent(EdgeType edge)
  {
    edge.setNode(this, true);
    parents_.append(edge.getConnection(false));
  }

  public void appendChild(EdgeType edge)
  {
    edge.setNode(this, false);
    children_.append(edge.getConnection(true));
  }

  // Access to edges
  public NodeEdgeList<NodeType, EdgeType> getParents()
  {
    return parents_;
  }

  public NodeEdgeList<NodeType, EdgeType> getChildren()
  {
    return children_;
  }

  NodeEdgeList<NodeType, EdgeType> parents_;
  NodeEdgeList<NodeType, EdgeType> children_;
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