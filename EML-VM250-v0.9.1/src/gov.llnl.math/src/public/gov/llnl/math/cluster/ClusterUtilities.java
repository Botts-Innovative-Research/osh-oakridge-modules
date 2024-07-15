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

import gov.llnl.math.DoubleArray;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToDoubleBiFunction;
import java.util.function.UnaryOperator;

/**
 *
 * @author nelson85
 */
public class ClusterUtilities
{
//<editor-fold desc="distance metrics">
  public static double[] l1(double[] v)
  {
    return DoubleArray.normColumns1(v.clone());
  }

  public static double[] l2(double[] v)
  {
    return DoubleArray.normColumns2(v.clone());
  }

  public static double[] center(double[] v)
  {
    double mn = DoubleArray.min(v);
    double mx = DoubleArray.max(v);
    if (mx == mn)
      mx = mn + 1;
    return DoubleArray.multiplyAssign(DoubleArray.addAssign(v.clone(), -(mn + mx) / 2), mx - mn);
  }

  public static double metricDotProduct(double[] d1, double[] d2)
  {
    return 1 - DoubleArray.multiplyInner(d1, d2);
  }

  public static double metricSquaredError(double[] d1, double[] d2)
  {
    double s = 0;
    for (int i = 0; i < d1.length; ++i)
    {
      s += (d1[i] - d2[i]) * (d1[i] - d2[i]);
    }
    return s / d1.length;
  }

  static double entropyBinary(double[] v)
  {
    double n = v.length;
    int q = 0;
    for (double d : v)
    {
      if (d < 0)
        q++;
    }
    if (q == n || q == 0)
      return 0;
    return -q / n * Math.log(q / n) - (n - q) / n * Math.log((n - q) / n);
  }
//</editor-fold>
//<editor-fold desc="cluster lists">

  /**
   * Used to populate the statistics.Needed to decide when to drop a cluster.
   *
   *
   * @param clusters
   * @param samples
   * @param outlierRule
   */
  public static void buildStatistics(List<Cluster> clusters, List<Sample> samples, Predicate<Sample> outlierRule)
  {
    // Zero all the statistics
    for (Cluster cluster : clusters)
    {
      cluster.members = 0;
      cluster.outliers = 0;
      cluster.meanDistance = 0.0;
      cluster.varDistance = 0.0;
    }

    // Build the statistics
    for (Sample sample : samples)
    {
      Cluster cluster = sample.current;
      cluster.members++;
      if (outlierRule != null && outlierRule.test(sample))
        cluster.outliers++;
      cluster.meanDistance += sample.distance;
      cluster.varDistance += sample.distance * sample.distance;
    }

    // Update the stddev
    for (Cluster cluster : clusters)
    {
      cluster.meanDistance /= cluster.members;
      cluster.varDistance = cluster.varDistance / cluster.members - cluster.meanDistance * cluster.meanDistance;
    }
  }

  /**
   * Assign a sample to a cluster.
   *
   * Updates the currently assigned cluster and distance in the sample.
   *
   * @param clusters
   * @param sample
   * @return the cluster the sample was assigned to.
   */
  public static Cluster assignToCluster(List<Cluster> clusters, Sample sample, ToDoubleBiFunction<double[], double[]> metric)
  {
    double min = Double.MAX_VALUE;
    for (Cluster cluster : clusters)
    {
      double dist = metric.applyAsDouble(cluster.template, sample.getTemplate());
      if (dist < min)
      {
        sample.current = cluster;
        min = dist;
      }
    }
    sample.distance = min;
    return sample.current;
  }

  /**
   * Assign a set of samples to one of a list of clusters.
   *
   * @param clusters
   * @param samples
   * @param metric
   * @return the number of items that were reassigned
   */
  public static int assignAllToCluster(
          List<Cluster> clusters,
          List<Sample> samples,
          ToDoubleBiFunction<double[], double[]> metric)
  {
    int changes = 0;
    for (Sample sample : samples)
    {
      sample.previous = sample.current;
      assignToCluster(clusters, sample, metric);
      if (sample.previous != sample.current)
        changes++;
    }
    return changes;
  }

  public static List<Sample> createSamples(List<double[]> entries)
  {
    Function<double[], Sample> creator = new Function<double[], Sample>()
    {
      int id = 0;

      @Override
      public Sample apply(double[] t)
      {
        return new Sample(id++, t);
      }
    };

    return Arrays.asList(entries.stream().map(creator).toArray(p -> new Sample[p]));
  }

  /**
   * Populates each cluster with a list of members.
   *
   * @param clusters
   * @param samples
   */
  public static void tallyMembers(List<Cluster> clusters, List<Sample> samples)
  {
    clusters.forEach(p -> p.memberList = new LinkedList<>());
    samples.forEach(p -> p.current.memberList.add(p));
  }

  /**
   * Gradient ascent to find the maximum density.
   *
   * @param template
   * @param samples
   * @param densityFunction
   * @param normalizer
   */
  static public double[] findMaximumDensity(
          double[] template,
          List<Sample> samples,
          ToDoubleBiFunction<double[], double[]> densityFunction,
          UnaryOperator<double[]> normalizer)
  {
//    {
//      final double[] template2 = normalizer.apply(template);
//      samples = samples.stream().filter(d -> densityFunction.applyAsDouble(template2, d.getTemplate()) > 1e-10)
//              .collect(Collectors.toList());
//    }

    // Compute the local minimum
    for (int i = 0; i < 20; ++i)
    {
      final double[] template2 = normalizer.apply(template);
      WeightedCollector accumulator = new WeightedCollector(
              (double[] d) -> densityFunction.applyAsDouble(template2, d));
      double[] next = samples.stream().map(p -> p.getTemplate()).collect(accumulator);
      next = normalizer.apply(next);
      double dist = ClusterUtilities.metricSquaredError(next, template2);
      System.out.println(dist + " " + computeDensity(template, samples, densityFunction));
      template = next;
      if (dist < 1e-6)
        break;
    }
    return template;
  }

  // Testing function to see how far out we need to look to do the clustering cuts.
  static public double[] getDensity(
          double[] template,
          List<Sample> samples,
          ToDoubleBiFunction<double[], double[]> densityFunction,
          UnaryOperator<double[]> normalizer)
  {
    final double[] template2 = normalizer.apply(template);
    return samples.stream()
            .mapToDouble(d -> densityFunction.applyAsDouble(template2, d.getTemplate()))
            .toArray();
  }

  static public double computeDensity(
          double[] template,
          List<Sample> samples,
          ToDoubleBiFunction<double[], double[]> densityFunction)
  {
    double density = 0;
    for (Sample sample : samples)
    {
      density += densityFunction.applyAsDouble(template, sample.getTemplate());
    }
    return density;
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