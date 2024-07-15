/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.math.graph;

import gov.llnl.utility.io.ReaderException;
import gov.llnl.utility.xml.bind.ObjectReader;

/**
 *
 * @author nelson85
 */
public abstract class GraphReaderBase<T> extends ObjectReader<T>
{
  int maxVariable = -1;

//  public GraphReaderBase(Order order, int options, String name, PackageResource schema)
//  {
//    super(order, options, name, schema);
//  }

  //<editor-fold desc="query">
  public void setDefaultQueryLookup()
  {
    this.setQueryLookup(new QueryLookupInterface.DefaultQueryLookup());
  }

  // The QueryLookup must in scope throughout the apply or load calls
  // does not assume ownership
  public void setQueryLookup(QueryLookupInterface qli)
  {
    if (qli == null)
      throw new RuntimeException("null query lookup function");
    queryIdMap = qli;
  }
  QueryLookupInterface<Integer> queryIdMap;

  protected int getQueryId(String name) throws ReaderException
  {
    if (queryIdMap == null)
      throw new ReaderException("Query id map not set");
    if (name == null)
      throw new ReaderException("Node must have id attribute.");
    int id = queryIdMap.lookup(name);
    if (id == GraphQuery.QUERY_ID_NONE)
      throw new ReaderException("Unable to find mapping for " + name);
    if (id > this.maxVariable)
      maxVariable = id;
    return id;
  }
//</editor-fold>

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