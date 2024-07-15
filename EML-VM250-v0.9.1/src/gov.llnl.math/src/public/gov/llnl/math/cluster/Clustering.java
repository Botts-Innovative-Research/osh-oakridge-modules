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
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;

//</editor-fold>
//<editor-fold desc="inner classes" defaultstate="collapsed">
/**
 * Result after applying clustering.
 */
public class Clustering
{
  public List<Cluster> clusters;
  public List<Cluster> reject;
  public List<Sample> samples;

  Clustering(List<Cluster> clusters, List<Sample> samples)
  {
    this.clusters = clusters;
    this.samples = samples;
  }

  /**
   * Get the centers of each cluster.
   *
   * @return
   */
  public double[][] getClusterCenters()
  {
    return clusters.stream().map((p) -> p.template).toArray((p) -> new double[p][]);
  }

  /**
   * Get the list of outliers from the classes.
   *
   * @param outlierRule
   * @return a list of outlier found.
   */
  public List<Sample> findOutliers(Predicate<Sample> outlierRule)
  {
    LinkedList<Sample> output = new LinkedList<>();
    for (Sample sample : this.samples)
    {
      if (outlierRule.test(sample))
      {
        output.add(sample);
      }
    }
    return output;
  }

  public void sortByMembers()
  {
    this.clusters.sort(ClusterOrder.members);
  }

  /**
   * Remove any cluster that fails certain criteria.
   *
   * @param removeRule
   */
  public void removeRejects(Predicate<Cluster> removeRule)
  {
    this.reject = new ArrayList<>();
    this.clusters.stream().filter(removeRule).forEach((e) -> this.reject.add(e));
    this.clusters.removeAll(reject);
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