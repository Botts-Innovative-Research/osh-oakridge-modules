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
import java.util.LinkedList;
import java.util.List;
import java.util.function.DoublePredicate;
import java.util.function.DoubleUnaryOperator;
import java.util.function.ToDoubleBiFunction;
import java.util.function.ToDoubleFunction;
import java.util.function.UnaryOperator;

/**
 * Internal representation of a cluster.
 *
 * These elements may be accessed as part of the output to assess quality and
 * interpret the result.
 */
public class Cluster
{
// Cluster parameters
  int id;
  // The template for the cluster.
  public double[] template;

  // True if the cluster center is fixed in position.
  boolean fixed = false;

// Statistics
  // The number of members in the cluster.
  public int members;
  public int outliers;

  // The mean of the distance between the members assigned to the cluster.
  public double meanDistance;
  // The variance of the distance between the members assigned to the cluster.
  public double varDistance;

// Workspace variables
  // How many items are included in the aggregation.
  int included;

  // Space used to compose the aggregate from the samples.
  Object tmp;
  public List<Sample> memberList = null;
  public List<Cluster> neighbors = null;

  public Cluster(double[] p)
  {
    this.template = p;
  }

  public List<Sample> findMembers(List<Sample> samples)
  {
    return Arrays.asList(samples.stream().filter(p -> p.current == this).toArray(p -> new Sample[p]));
  }

  public List<Sample> filter(List<Sample> samples, DoublePredicate criteria, ToDoubleBiFunction<double[], double[]> metric)
  {
    return Arrays.asList(samples.stream().filter(p -> criteria.test(metric.applyAsDouble(template, p.getTemplate())))
            .toArray(p -> new Sample[p]));
  }

  /**
   * Get a list of all the clusters that are nearby.
   *
   * @param clusters
   * @param metric
   * @param d
   */
  public void findNeighbors(List<Cluster> clusters, ToDoubleBiFunction<double[], double[]> metric, double d)
  {
    this.neighbors = new LinkedList<>();
    for (Cluster cluster : clusters)
    {
      if (cluster == this)
        continue;
      double d1 = metric.applyAsDouble(this.template, cluster.template);
      if (d1 < d)
        this.neighbors.add(cluster);
    }
  }

  /**
   * Collect all the neighbors in a region.
   *
   * Assumes that the member list has been populated.
   *
   * @return
   */
  public List<Sample> collectNeighborSamples()
  {
    LinkedList<Sample> out = new LinkedList<>();
    out.addAll(this.memberList);
    neighbors.forEach(p -> out.addAll(p.memberList));
    return out;
  }

  public double computeEntropy(ToDoubleFunction<double[]> metric)
  {
    double out = 0;
    int n = this.template.length;
    double[] v = new double[this.memberList.size()];
    for (int i = 0; i < n; ++i)
    {
      int j = 0;
      for (Sample member : this.memberList)
      {
        v[j++] = member.getTemplate()[i];
      }
      out += metric.applyAsDouble(v);
    }
    return out;
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