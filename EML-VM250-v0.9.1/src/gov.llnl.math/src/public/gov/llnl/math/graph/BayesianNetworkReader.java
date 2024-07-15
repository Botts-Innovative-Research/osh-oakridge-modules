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

import gov.llnl.math.MathPackage;
import gov.llnl.utility.io.ReaderException;
import gov.llnl.utility.xml.bind.ObjectReader;
import gov.llnl.utility.xml.bind.Reader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
import org.xml.sax.Attributes;

@Reader.Declaration(pkg = MathPackage.class, name = "bayesianNetwork",
        cls = BayesianNetwork.class,
        document = true, referenceable = true)
public class BayesianNetworkReader extends GraphReaderBase<BayesianNetwork>
{
  LinkedList<NodeSpecification> nodeSpecifications;
  TreeMap<Integer, BayesianNetworkNode> nodeMap;

//  public BayesianNetworkReader()
//  {
//    super(Order.FREE, Options.DOCUMENT,
//            "bayesianNetwork", MathPackage.getInstance());
//  }
  @Override
  public BayesianNetwork start(Attributes attributes) throws ReaderException
  {
    super.maxVariable = -1;
    nodeSpecifications = new LinkedList<>();
    nodeMap = new TreeMap<>();
    return new BayesianNetwork();
  }

  @Override
  public BayesianNetwork end() throws ReaderException
  {
    BayesianNetwork pg = getObject();
    // Pass 1: Allocate all of the nodes
    processPass1(pg, nodeSpecifications);

    // Pass 2: Create connections
    processPass2(pg, nodeSpecifications);

//    pg.setVariableCount(maxVariable + 1);
    return null;
  }

  @Override
  public ElementHandlerMap getHandlers() throws ReaderException
  {
    ReaderBuilder<BayesianNetworkReader> builder = this.newBuilder().using(this);
    builder.element("node")
            .reader(new NodeSpecificationReader())
            .call(BayesianNetworkReader::add);
    return builder.getHandlers();
  }

  public void add(NodeSpecification ns)
  {
    this.nodeSpecifications.add(ns);
  }

//<editor-fold desc="contents">
  static class NodeSpecification
  {
    String idName;
    int id;
    List<Integer> depends = new LinkedList<>();
    double[] probability = null;
    List<ProbabilityGraphNode.Condition> conditions = new ArrayList<>();

    private NodeSpecification(String idName, int id)
    {
      this.idName = idName;
      this.id = id;
    }

    public void setProbability(double[] probability)
    {
      this.probability = probability;
    }

    public void addDepends(int dep)
    {
      this.depends.add(dep);
    }

    public void addCondition(ProbabilityGraphNode.Condition condition)
    {
      this.conditions.add(condition);
    }

    private ProbabilityGraphNode.Condition[] getConditions()
    {
      if (conditions.isEmpty())
        return null;
      return conditions.toArray(new ProbabilityGraphNode.Condition[conditions.size()]);
    }
  }


  @Reader.Declaration(pkg = MathPackage.class, name = "bayesianNode",
          order = Reader.Order.SEQUENCE, referenceable = false)
  @Reader.Attribute(name = "id", required = true)
  @Reader.Attribute(name = "probability")
  public class NodeSpecificationReader extends ObjectReader<NodeSpecification>
  {

    @Override
    public NodeSpecification start(Attributes attributes) throws ReaderException
    {
      String idName = attributes.getValue("id");
      NodeSpecification ns = new NodeSpecification(idName, getQueryId(idName));
      String probability = attributes.getValue("probability");
      if (probability != null)
      {
        double pv = Double.parseDouble(probability);
        ns.setProbability(new double[]
        {
          1 - pv, pv
        });
      }
      return ns;
    }

    @Override
    public NodeSpecification end() throws ReaderException
    {
      return null;
    }

    @Override
    public ElementHandlerMap getHandlers() throws ReaderException
    {
      ReaderBuilder<NodeSpecification> builder = this.newBuilder();
      builder.using(this).element("depends")
              .callString(NodeSpecificationReader::addDependency).optional().unbounded();
      builder.element("probability")
              .contents(double[].class)
              .call(NodeSpecification::setProbability);
      return builder.getHandlers();
    }

    @Override
    public Class<NodeSpecification> getObjectClass()
    {
      return NodeSpecification.class;
    }

    void addDependency(String name)
    {
      try
      {
        getObject().addDepends(getQueryId(name));
      }
      catch (ReaderException ex)
      {
        throw new RuntimeException(ex);
      }
    }
  }

//</editor-fold>
  private void processPass1(BayesianNetwork pg, LinkedList<NodeSpecification> nsl)
  {
    // Pass 1: Allocate all of the nodes
    for (NodeSpecification ns : nsl)
    {
      int id = ns.id;
      BayesianNetworkNode node = pg.allocateNode();
      node.setQueryId(id);
      registerNode(id, node);
    }
  }

  private void processPass2(BayesianNetwork pg, LinkedList<NodeSpecification> nsl)
  {
    for (NodeSpecification ns : nsl)
    {
      BayesianNetworkNode node = lookupNode(ns.id);
      if (node == null)
        throw new RuntimeException("BayesianNetworkReader::processNode: internal consistency check failed");

      if (ns.depends.isEmpty())
      {
        node.setProbabilityTable(ns.probability);
      }
      else
      {
        // Pass 1 set up all the dependencies
        for (Integer parent : ns.depends)
        {
          BayesianNetworkNode parentNode = lookupNode(parent);
          if (parentNode == null)
            throw new RuntimeException("Bad name");

          pg.link(parentNode, node);
        }

        node.setProbabilityTable(ns.probability);
      }
    }
  }

  private BayesianNetworkNode lookupNode(int name)
  {
    return nodeMap.get(name);
  }

  private void registerNode(int name, BayesianNetworkNode node)
  {
    nodeMap.put(name, node);
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