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

import java.util.LinkedList;

/**
 *
 * @author nelson85
 * @param <NodeType>
 * @param <EdgeType>
 */
public abstract class GraphBase<NodeType extends NodeBase, EdgeType extends EdgeBase>
{
  public GraphBase()
  {
  }

  public void dispose()
  {
  }

  // Methods for loader
  public int getNumNodes()
  {
    return nodes.size();
  }

  public int getNumEdges()
  {
    return edges_.size();
  }

//    public int getNode(int id)
//    {
//      return nodes_.get(id);
//    }
//    public EdgeType getEdge(int id)
//    {
//      return edges_.get(id);
//    }
  public abstract NodeType allocateNode();

  public abstract EdgeType allocateEdge();

  protected NodeType addNode(NodeType node)
  {
    node.setId(nodes.size());
    nodes.addLast(node);
    return node;
  }

  protected EdgeType addEdge(EdgeType edge)
  {
    edge.setId(edges_.size());
    edges_.addLast(edge);
    return edge;
  }

  public void clear()
  {
    // FIXME
    nodes.clear();
    edges_.clear();
  }

  public LinkedList<NodeType> nodes()
  {
    return nodes;
  }

  public LinkedList<EdgeType> edges()
  {
    return edges_;
  }

  abstract EdgeType link(NodeType parent, NodeType child);
  protected LinkedList<NodeType> nodes = new LinkedList<>();
  protected LinkedList<EdgeType> edges_ = new LinkedList<>();
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