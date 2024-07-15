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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author nelson85
 */
public class InitializerDefault implements Initializer
{
  /**
   * Predefined immutable clusters from previous execution.
   */
  List<Cluster> predefined = new ArrayList<>();

  public void addPredefined(List<double[]> clusters)
  {
    clusters.stream()
            .map((p) -> newPredefined(p))
            .forEach((p) -> predefined.add(p));
  }

  /**
   * Helper for addPredefined.
   *
   * @param v
   * @return
   */
  private Cluster newPredefined(double[] v)
  {
    Cluster out = new Cluster(v);
    out.fixed = true;
    return out;
  }

  /**
   * Creates an initial population of clusters.
   *
   * This method is not great as it tends to build clusters near the extremes
   * which have to be hacked off later.
   *
   * @param samples
   * @param n
   * @return
   */
  public List<Cluster> initialize(KMeans kmeans, List<Sample> samples, int n)
  {
    Cluster[] clusters = new Cluster[n];
    // We need at least one free cluster, though it is going to have a
    // hard time if we don't give it at least a few free clusters.
    if (n <= this.predefined.size())
      throw new RuntimeException("Requested clusters cannot be achieved.");

    // Copy in predefined clusters
    for (int i = 0; i < this.predefined.size(); ++i)
    {
      clusters[i] = this.predefined.get(i);
      clusters[i].template = kmeans.normalizer.apply(clusters[i].template);
    }

    // Find a points distant from an existing cluster to create a new one.
    List<Cluster> listClusters = Arrays.asList(clusters);
    for (int i0 = this.predefined.size(); i0 < n; ++i0)
    {
      // Find the most distant from each cluster pulling randomly
      double q1 = -Double.MAX_VALUE;
      Sample worst = samples.get(0);
      // FIXME determine the number of samples to draw
      for (int i1 = 0; i1 < 100; ++i1)
      {
        Sample sample = samples.get(kmeans.random.nextInt(samples.size()));
        ClusterUtilities.assignToCluster(listClusters.subList(0, i0), sample, kmeans.metric);
        // Find the most outstanding
        if (sample.distance > q1)
        {
          q1 = sample.distance;
          worst = sample;
        }
      }
      clusters[i0] = new Cluster(worst.getTemplate());
    }
    return new ArrayList(Arrays.asList(clusters));
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