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
 * @param <EdgeType>
 */
public class GraphNode<EdgeType extends EdgeBase> extends NodeBase
{
//    typedef GraphNode<EdgeT> SelfType;
//    typedef EdgeT EdgeType;
//    typedef NodeEdgeList<EdgeType> EdgeList;
//    typedef typename EdgeList::SizeType SizeType;
  public GraphNode()
  {
  }

  public void dispose()
  {
    edges_ = null;
  }

  // Sizes of the edges
  public int getNumEdges()

  {
    return edges_.size();
  }

  // Interface to create node linkages (Should be private)
  public void appendEdge(EdgeType edge)
  {
    edges_.append(edge.getConnectionFrom(this));
  }

  // Access to edges
  public NodeEdgeList getEdges()
  {
    return edges_;
  }

  private NodeEdgeList edges_ = new NodeEdgeList();
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