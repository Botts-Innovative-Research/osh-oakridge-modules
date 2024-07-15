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
import gov.llnl.math.graph.ProbabilityGraphNode.Condition;
import gov.llnl.utility.Try;
import gov.llnl.utility.io.ReaderException;
import gov.llnl.utility.xml.bind.ObjectReader;
import gov.llnl.utility.xml.bind.Reader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.xml.sax.Attributes;

/**
 *
 * @author nelson85
 */

@Reader.Declaration(pkg = MathPackage.class, name = "probabilityGraph",
        cls = ProbabilityGraph.class,
        document = true, referenceable = true)
@Reader.Attribute(name = "probability", type = Double.class)
public class ProbabilityGraphReader extends GraphReaderBase<ProbabilityGraph>
{
  double defaultProbability = 0.5;
  LinkedList<NodeSpecification> nodeSpecifications;
  Map<Integer, ProbabilityGraphNode> nodeMap;
  int maxCondition = -1;

//  public ProbabilityGraphReader()
//  {
//    super(Order.FREE, Options.DOCUMENT, "probabilityGraph",
//            MathPackage.getInstance());
//  }
  @Override
  public ProbabilityGraph start(Attributes attributes) throws ReaderException
  {
    this.maxCondition = -1;
    this.maxVariable = -1;
    String prob = attributes.getValue("probability");
    if (prob != null)
      this.defaultProbability = Double.parseDouble(prob);
    nodeSpecifications = new LinkedList<>();
    return new ProbabilityGraph();
  }

  @Override
  public ProbabilityGraph end() throws ReaderException
  {
    ProbabilityGraph pg = getObject();

    // Pass 1: Allocate all of the nodes
    processPass1(pg, nodeSpecifications);

    // Pass 2: Create connections
    processPass2(pg, nodeSpecifications);

    pg.setConditionCount(maxCondition + 1);
    pg.setVariableCount(maxVariable + 1);

    // Pass 3: Normalize the graph
    ProbabilityGraphNormalizer norm = new ProbabilityGraphNormalizer();
    norm.process(pg);
    return null;
  }

  @Override
  public ElementHandlerMap getHandlers() throws ReaderException
  {
    ReaderBuilder<ProbabilityGraphReader> builder = this.newBuilder().using(this);
    builder.element("node")
            .reader(new NodeSpecificationReader())
            .call(ProbabilityGraphReader::add);
    builder.element("define_condition")
            .callString(Try.promote(ProbabilityGraphReader::defineCondition)).noid();
    builder.section(new NodeGroupSection());
    return builder.getHandlers();
  }

  void add(NodeSpecification ns)
  {
    this.nodeSpecifications.add(ns);
  }

  void defineCondition(String condition) throws ReaderException
  {
    if (this.conditionIdMap == null)
      throw new RuntimeException("Condition map is not set.");
    int id = getConditionId(condition);
    if (id > maxCondition)
      maxCondition = id;
  }

//<editor-fold desc="conditions">
  QueryLookupInterface<Integer> conditionIdMap;

  public void setDefaultConditionLookup()
  {
    this.setQueryLookup(new QueryLookupInterface.DefaultQueryLookup());
  }

  public void setConditionLookup(QueryLookupInterface qli)
  {
    conditionIdMap = qli;
  }

  private int getConditionId(String name) throws ReaderException
  {
    if (name == null)
      throw new ReaderException("Node must have id attribute.");
    int id = conditionIdMap.lookup(name);
    if (id == GraphQuery.QUERY_ID_NONE)
      throw new ReaderException("Unable to find mapping for " + name);
    return id;
  }
//</editor-fold>
//<editor-fold desc="contents">

  public void setDefaultProbability(double d)
  {
    this.defaultProbability = d;
  }

  static class NodeSpecification
  {
    String idName;
    int id;
    List<Integer> depends = new LinkedList<>();
    double[] probability = null;
    List<Condition> conditions = new ArrayList<>();

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

    public void addCondition(Condition condition)
    {
      this.conditions.add(condition);
    }

    private Condition[] getConditions()
    {
      if (conditions.isEmpty())
        return null;
      return conditions.toArray(new Condition[conditions.size()]);
    }
  }


  @Reader.Declaration(pkg = MathPackage.class, name = "probabiblityNode",
          order = Reader.Order.SEQUENCE, referenceable = true)
  @Reader.Attribute(name = "name", type = String.class)
  @Reader.Attribute(name = "probability", type = Double.class)
  public class NodeSpecificationReader extends ObjectReader<NodeSpecification>
  {

    @Override
    public NodeSpecification start(Attributes attributes) throws ReaderException
    {
      String idName = attributes.getValue("name");
      if (idName == null)
        throw new ReaderException("fail to get id");
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
      NodeSpecification ns = getObject();
      if (ns.probability == null)
        ns.setProbability(new double[]
        {
          1 - defaultProbability, defaultProbability
        });
      return null;
    }

    @Override
    public ElementHandlerMap getHandlers() throws ReaderException
    {
      ReaderBuilder<NodeSpecification> builder = this.newBuilder();
      builder.reader(new ConditionReader())
              .call(NodeSpecification::addCondition).optional();
      builder.using(this).element("depends")
              .callString(NodeSpecificationReader::addDependency).optional().unbounded();
      builder.element("probability")
              .call(NodeSpecification::setProbability, double[].class);
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

  @Reader.Attribute(name = "probability", type = Double.class, required = true)
  class NodeGroupSection extends Section
  {
    double last;

    public NodeGroupSection()
    {
      super(Order.FREE, "subgroup");
    }

    @Override
    public ProbabilityGraph start(Attributes attributes) throws ReaderException
    {
      last = defaultProbability;
      String value = attributes.getValue("probability");
      defaultProbability = Double.parseDouble(value);
      return null;
    }

    @Override
    public ProbabilityGraph end() throws ReaderException
    {
      defaultProbability = last;
      return null;
    }

    @Override
    public ElementHandlerMap getHandlers() throws ReaderException
    {
      ReaderBuilder<ProbabilityGraphReader> builder = this.newBuilder().using(ProbabilityGraphReader.this);
      builder.element("node").reader(new NodeSpecificationReader())
              .call(ProbabilityGraphReader::add);
      return builder.getHandlers();
    }
  }


  @Reader.Declaration(pkg = MathPackage.class, name = "condition",
          order = Reader.Order.ALL,
          referenceable = true)
  @Reader.Attribute(name = "on", type = String.class)
  @Reader.Attribute(name = "ratio", type = Double.class, required = true)
  public class ConditionReader extends ObjectReader<Condition>
  {

    @Override
    public Condition start(Attributes attributes) throws ReaderException
    {
      String on = attributes.getValue("on");
      String ratio = attributes.getValue("ratio");
      return new Condition(getConditionId(on), Double.parseDouble(ratio.trim()));
    }

    @Override
    public ElementHandlerMap getHandlers() throws ReaderException
    {
      return null;
    }

    @Override
    public Class<Condition> getObjectClass()
    {
      return Condition.class;
    }
  }
//</editor-fold>
//<editor-fold desc="internal">

  ProbabilityGraphNode lookupNode(int name)
  {
    return nodeMap.get(name);
  }

  void registerNode(String name, int id, ProbabilityGraphNode node)
  {
    if (nodeMap.get(id) != null)
      throw new RuntimeException("Duplicate node " + name);
    nodeMap.put(id, node);
  }

  void processPass1(ProbabilityGraph pg, List<NodeSpecification> root)
  {
    nodeMap = new TreeMap<>();
    for (NodeSpecification ns : root)
    {
      String idName = ns.idName;
      int id = ns.id;
      ProbabilityGraphNode node = pg.allocateNode();
      node.setQueryId(id);
      registerNode(idName, id, node);
      node.setConditions(ns.getConditions());
      node.setName(ns.idName);
    }
  }

  void processPass2(ProbabilityGraph pg, List<NodeSpecification> root) throws ReaderException
  {
    for (NodeSpecification ns : root)
    {
      ProbabilityGraphNode node = lookupNode(ns.id);
      if (node == null)
        throw new ReaderException("internal consistency check failed");
      node.setAsVariableNode();

      // Add the dependencies
      if (!ns.depends.isEmpty())
      {
        ProbabilityGraphNode factorNode = pg.allocateNode();
        // First link is to the dependent node
        pg.link(node, factorNode);
        for (int parent : ns.depends)
          pg.link(this.lookupNode(parent), factorNode);
        factorNode.setAsFactorNode();
        node = factorNode;
      }

      // Set up factor table
      if (ns.probability.length != node.getFactorTableSize())
        throw new ReaderException("Probability table size mismatch on "
                + ns.idName + ", got "
                + ns.probability.length + " expected " + node.getFactorTableSize());
      for (int i = 0; i < ns.probability.length; i += 2)
      {
        double d1 = ns.probability[i];
        double d2 = ns.probability[i + 1];
        if (Math.abs(d1 + d2 - 1) > 1e-12 || d1 < 0 || d2 < 0)
          throw new ReaderException("Bad probability table");
      }
      node.setFactorTable(ns.probability);
    }
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