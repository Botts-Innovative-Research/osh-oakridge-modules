/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gov.llnl.math.test;

import gov.llnl.math.graph.GraphQuery;
import gov.llnl.math.graph.ProbabilityGraph;
import gov.llnl.math.graph.ProbabilityGraphReader;
import gov.llnl.math.graph.QueryLookupInterface.ListQueryLookup;
import gov.llnl.math.graph.ViterbiProbabilityGraph;
import gov.llnl.utility.io.ReaderException;
import gov.llnl.utility.xml.bind.DocumentReader;
import java.io.IOException;
import java.nio.file.Paths;

/**
 *
 * @author nelson85
 */
public class TestProbabilityGraph
{
  public static void main(String[] args) throws ReaderException, IOException
  {
    ProbabilityGraphReader reader = new ProbabilityGraphReader();
    DocumentReader<ProbabilityGraph> pgr = DocumentReader.create(reader);

    reader.setDefaultQueryLookup();
    reader.setQueryLookup(new ListQueryLookup("fred", "george", "bob"));
    reader.setConditionLookup(new ListQueryLookup("sally"));
    ProbabilityGraph pg = pgr.loadFile(Paths.get("graph.xml"));
    pg.dump(System.out);

    double f = 0.2;
    double p1;
    GraphQuery query = pg.allocateQuery();
    query.setTrue(1);
    p1 = pg.computeProbability(query);
    System.out.println("100 =" + p1);
    query.setCondition(0, 0);
    p1 = pg.computeProbability(query);
    System.out.println("100 =" + p1);
    query.setCondition(0, 1);
    p1 = pg.computeProbability(query);
    System.out.println("100 =" + p1);
    query.clearConditions();
    query.setTrue(2);
    double p2 = pg.computeProbability(query);
    System.out.println("110 =" + p2);
    query.clear();
    query.setTrue(1);
    query.setPartial(2, f);
    double p3 = pg.computeProbability(query);
    System.out.println("1p0 =" + p3);
    ViterbiProbabilityGraph vpg = new ViterbiProbabilityGraph();
    ViterbiProbabilityGraph.Output vpgo = new ViterbiProbabilityGraph.Output();
    vpg.propogate(vpgo, pg, query);
    System.out.println("viterbi " + vpgo.totalProbability);
    System.out.println("truth " + ((1 - f) * p1 + f * p2));
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