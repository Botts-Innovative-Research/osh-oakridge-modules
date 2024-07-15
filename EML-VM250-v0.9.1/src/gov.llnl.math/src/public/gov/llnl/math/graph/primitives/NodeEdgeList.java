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
public class NodeEdgeList<NodeType extends NodeBase, EdgeType extends EdgeBase> extends NodeEdgeListBase
        implements java.lang.Iterable<Connection<NodeType, EdgeType>>
{
  public class Iterator implements java.util.Iterator<Connection<NodeType, EdgeType>>
  {
    ConnectionBase current;

    @Override
    public boolean hasNext()
    {
      return current != null;
    }

    @Override
    public Connection<NodeType, EdgeType> next()
    {
      ConnectionBase next = current;
      current = next.getNext();
      return (Connection<NodeType, EdgeType>) next;
    }

    public Iterator(ConnectionBase connection)
    {
      current = connection;
    }

    @Override
    public void remove()
    {
      throw new UnsupportedOperationException("Not supported yet.");
    }
  }

  public Iterator iterator()
  {
    return new Iterator(this.head_);
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