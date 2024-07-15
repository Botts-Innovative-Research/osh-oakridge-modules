/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.math.cluster;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

//</editor-fold>
//<editor-fold desc="internal" defaultstate="collapsed">
public class SplitterDefault implements Splitter
{
  Initializer initializer = new InitializerRandom();

  @Override
  public List<Cluster> split(KMeans algo, Cluster cluster, List<Sample> samples)
  {
    // Don't split a fixed cluster
    if (cluster.fixed)
    {
      return Arrays.asList(cluster);
    }

    // Find the sample assigned to the cluster
    List<Sample> samplesIn = samples.stream().filter((p) -> p.current == cluster)
            .collect(Collectors.toList());

    System.out.println("PRE " + cluster.members + " " + cluster.outliers + " " + (double) cluster.outliers / (double) cluster.members);
    System.out.println("SAMPLES IN " + samplesIn.size());

    // Create new clusters in the set
    List<Cluster> clusters = initializer.initialize(algo, samplesIn, 2);
    ClusterUtilities.assignAllToCluster(clusters, samplesIn, algo.metric);
    ClusterUtilities.buildStatistics(clusters, samplesIn, algo.outlierRule);

    for (Cluster c : clusters)
      System.out.println("MID " + c.members + " " + c.outliers + " " + (double) c.outliers / (double) c.members);

    // Cluster the sub problem
    Clustering out = algo.iterateCluster(clusters, samplesIn, 50, null, null);

    for (Cluster c : out.clusters)
      System.out.println("POST " + c.members + " " + c.outliers + " " + (double) c.outliers / (double) c.members);

    // Return the new clusters
    return out.clusters;
  }

  /**
   * @return the initializer
   */
  public Initializer getInitializer()
  {
    return initializer;
  }

  /**
   * @param initializer the initializer to set
   */
  public void setInitializer(Initializer initializer)
  {
    this.initializer = initializer;
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