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
import gov.llnl.math.matrix.Matrix;
import gov.llnl.math.matrix.MatrixFactory;
import gov.llnl.math.matrix.MatrixOps;
import gov.llnl.math.stream.DoubleArrayCollectors;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleBiFunction;
import java.util.function.UnaryOperator;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * Perform K-Means clustering.
 *
 * This was made to serve ERNIE where we have the special constraint that we
 * need to retain a set of predefined classes and augment additional classes.
 * Thus we have custom KMeans implementation.
 *
 * This algorithm tries to minimize the distance between cluster members.
 *
 * KMeans requires three user supplied methods:
 * <ul>
 * <li>Normalizer - a function which takes the template and transforms it into a
 * vector suitable for measuring distance.
 *
 * <li>DistanceMetric - a function which measures the distance between a sample
 * and the cluster.
 *
 * <li>Collector - a class which takes a set of members assigned to a cluster
 * and computes the new cluster center. As this function is the same a normal
 * stream operation, we will use the stream collection.
 * </ul>
 *
 * @author nelson85
 */
public class KMeans
{
//<editor-fold desc="parameters" defaultstate="collapsed">
  double rebuildFraction = 1.0;
  double convergenceFraction = 0.01;

  Initializer initializer = new InitializerDefault();
  Splitter splitter = new SplitterDefault();
  Predicate<Cluster> removeRule = null;
  Predicate<Cluster> splitRule = null;
  Predicate<Sample> outlierRule = null;

  int maxIterations = 100;
  int splitIterations = Integer.MAX_VALUE;
  int removeIterations = Integer.MAX_VALUE;

  /**
   * How many clusters we are attempting to assign.
   */
  int requestedClusters = 10;

  /**
   * Random generator used to creating samples.
   */
  Random random = new Random();

  /**
   * Metric for measuring distance from cluster sample.
   */
  ToDoubleBiFunction<double[], double[]> metric = DistanceMetrics.SquaredError.get();

  /**
   * Method to apply to samples and cluster centers to the normalized vector.
   *
   * This is matched with the metric function. Some metrics require the data in
   * the cluster to be normalized a specific way.
   *
   */
  UnaryOperator<double[]> normalizer = Normalizers.L2.get();

  /**
   * Collector for forming the cluster center.
   *
   * Most times averaging will be acceptable, but other procedures may be
   * possible.
   */
  Collector<double[], ?, double[]> collector = DoubleArrayCollectors.mean();

//</editor-fold>
//<editor-fold desc="inputs" defaultstate="collapsed">
  /**
   * @param maxIterations the maxIterations to set
   */
  public void setMaximumIterations(int maxIterations)
  {
    this.maxIterations = maxIterations;
  }

  /**
   * @param splitIterations the splitIterations to set
   */
  public void setSplitIterations(int splitIterations)
  {
    this.splitIterations = splitIterations;
  }

  /**
   * @param removeIterations the removeIterations to set
   */
  public void setRemoveIterations(int removeIterations)
  {
    this.removeIterations = removeIterations;
  }

  /**
   * @param requestedClusters the numClusters to set
   */
  public void setRequestedClusters(int requestedClusters)
  {
    this.requestedClusters = requestedClusters;
  }

  /**
   * Set the random number generator to use.
   *
   * This defaults to the standard random number generator and should only need
   * to be set when we require deterministic operation.
   *
   * @param random the random number generator to set.
   */
  public void setRandom(Random random)
  {
    this.random = random;
  }

  /**
   * Set the distance metric for clustering.
   *
   * @param metric the metric to set.
   */
  public void setMetric(ToDoubleBiFunction<double[], double[]> metric)
  {
    this.metric = metric;
  }

  public void setMetric(Supplier<? extends ToDoubleBiFunction<double[], double[]>> metric)
  {
    this.metric = metric.get();
  }

  /**
   * Set the normalizer for samples and cluster centers.
   *
   * @param normalizer the normalizer to set
   */
  public void setNormalizer(UnaryOperator<double[]> normalizer)
  {
    this.normalizer = normalizer;
  }

  public void setNormalizer(Supplier<? extends UnaryOperator<double[]>> normalizer)
  {
    this.normalizer = normalizer.get();
  }

  /**
   * Set the aggregator for updating the cluster center.
   *
   * @param collector the collector to set
   */
  public void setCollector(Collector<double[], ?, double[]> collector)
  {
    this.collector = collector;
  }

  /**
   * @param convergenceFraction the convergenceFraction to set
   */
  public void setConvergenceFraction(double convergenceFraction)
  {
    this.convergenceFraction = convergenceFraction;
  }

  /**
   * @param rebuildFraction the rebuildFraction to set
   */
  public void setRebuildFraction(double rebuildFraction)
  {
    this.rebuildFraction = rebuildFraction;
  }

  /**
   * @param initializer the initializer to set
   */
  public void setInitializer(Initializer initializer)
  {
    this.initializer = initializer;
  }

  /**
   * @param splitter the splitter to set
   */
  public void setSplitter(Splitter splitter)
  {
    this.splitter = splitter;
  }

  /**
   * @param removeRule the removeRule to set
   */
  public void setRemoveRule(Predicate<Cluster> removeRule)
  {
    this.removeRule = removeRule;
  }

  /**
   * @param splitRule the splitRule to set
   */
  public void setSplitRule(Predicate<Cluster> splitRule)
  {
    this.splitRule = splitRule;
  }

  /**
   * @param outlierRule the outlierRule to set
   */
  public void setOutlierRule(Predicate<Sample> outlierRule)
  {
    this.outlierRule = outlierRule;
  }

  /**
   * @return the initializer
   */
  public Initializer getInitializer()
  {
    return initializer;
  }

  /**
   * @return the splitter
   */
  public Splitter getSplitter()
  {
    return splitter;
  }

  public Predicate<Cluster> getSplitRule()
  {
    return this.splitRule;
  }

//</editor-fold>
//<editor-fold desc="execute" defaultstate="collapsed">
  int sampleId;

  public Clustering execute(List<double[]> entries)
  {
    // Convert entries into samples
    List<Sample> samples = ClusterUtilities.createSamples(entries);

    // Make sure the samples are properly normalized
    samples.stream().forEach(p -> normalizer.apply(p.getTemplate()));

    // We need to make sure there is no structure in the data
    Collections.shuffle(samples, random);

    // If we require a fixed number of clusters, we need to
    // split the set here.
    List<Cluster> clusters = initializer.initialize(this, samples, requestedClusters);

    Clustering out = iterateCluster(clusters, samples, this.maxIterations,
            removeRule,
            splitRule);

    // Resort the samples by distance
    out.samples.sort((p1, p2) -> Double.compare(p1.distance, p2.distance));
    return out;
  }

  /**
   * Updates the clusters based on the samples for a specified number of
   * iterations.
   *
   * @param clusters
   * @param samples
   * @param maxIterations
   * @param removeRule
   * @param splitRule
   * @return
   */
  public Clustering iterateCluster(List<Cluster> clusters, List<Sample> samples,
          int maxIterations,
          Predicate<Cluster> removeRule,
          Predicate<Cluster> splitRule)
  {
    final int REFRESH = 100;
    boolean quit = false;
    int counter = REFRESH;

    // Assign each sample to a cluster
    ClusterUtilities.assignAllToCluster(clusters, samples, this.metric);

    // Loop to achieve convergence
    for (int iterations = 0; iterations < maxIterations; ++iterations)
    {
      // Back assign cluster
      rebuildClusters(clusters, samples);

      // Compose statistics
      ClusterUtilities.buildStatistics(clusters, samples, outlierRule);

      // Decide to drop or create new classes here
      clusters.sort((p1, p2) -> -Integer.compare(p1.members, p2.members));

      if (counter == 0)
      {
        System.out.println("CONSIDER " + samples.size());

        // Remove clusters based on a rule (should be customizable)
        if (removeRule != null
                && iterations < this.removeIterations)
        {
          clusters.removeIf(removeRule);
          counter = REFRESH;
        }

        // Split clusters based on a rule (should be customizable)
        if (splitter != null && splitRule != null
                && iterations < this.splitIterations)
        {

          List<Cluster> splitClusters = clusters.stream()
                  .filter(splitRule)
                  .collect(Collectors.toList());

          if (!splitClusters.isEmpty())
          {
            clusters.removeAll(splitClusters);

            // We should read the clusters that were split
            // FIXME what if we get too many clusters
            splitClusters.forEach(p -> clusters.addAll(splitter.split(this, p, samples)));
            ClusterUtilities.buildStatistics(clusters, samples, outlierRule);

            counter = REFRESH;
            continue;
          }
        }
      }

      // Assign each sample to a cluster
      int changes = ClusterUtilities.assignAllToCluster(clusters, samples, metric);
      if (counter == 0 && changes < samples.size() * convergenceFraction)
      {
        break;
      }
      System.out.println(counter + " Changes " + changes);
      if (changes == 0 && counter < REFRESH)
        counter = 0;

      if (counter > 0)
        counter--;
    }

    return new Clustering(clusters, samples);
  }

  public void computeDistance(Cluster cluster1, List<Sample> outliers)
  {
    for (Sample sample : outliers)
    {
      double dist = this.metric.applyAsDouble(cluster1.template, sample.getTemplate());
      sample.distance = dist;
    }
  }

  /**
   * Rebuild the clusters from the samples.
   *
   * @param clusters
   * @param samples
   */
  public void rebuildClusters(List<Cluster> clusters, List<Sample> samples)
  {
    // Reject outliers while building cluster centers
    List<Sample> samples2 = new ArrayList<>(samples);
    samples2.sort((p1, p2) -> Double.compare(p1.distance, p2.distance));

    BiConsumer accumulator = collector.accumulator();
    Supplier supplier = collector.supplier();
    Function finisher = collector.finisher();

    // Create a fresh accumulator for each cluster
    for (Cluster cluster : clusters)
    {
      cluster.included = 0;
      cluster.tmp = supplier.get();
    }

    // Accumulate the templates on the clusters
    for (Sample sample : samples2)
    {
      Cluster cluster = sample.current;
      if (cluster.included > cluster.members * rebuildFraction)
        continue;
      cluster.included++;
      accumulator.accept(cluster.tmp, sample.getTemplate());
    }

    // Convert back to a cluster.
    for (Cluster cluster : clusters)
    {
      // Don't update fixed clusters
      if (cluster.fixed)
        continue;

      // Recompute a new cluster center
      double[] center = (double[]) finisher.apply(cluster.tmp);

      // If for some reason we don't get any samples in a cluster, don't
      // change the center
      if (center == null)
        continue;

      cluster.template = this.normalizer.apply(center);

    }
  }

  class MergeTmp
  {
    Cluster cluster1;
    Cluster cluster2;
    double distance;
    private double[] proposed;
    private double mergeIncrease;
    private int outliersBefore;
    private int outliersAfter;
    private int total;

    private MergeTmp(Cluster cluster1, Cluster cluster2, double distance)
    {
      this.cluster1 = cluster1;
      this.cluster2 = cluster2;
      this.distance = distance;
    }

    public double evaluateMerge(List<Sample> samples)
    {
      this.proposed = DoubleArray.multiply(cluster1.template, cluster1.members);
      DoubleArray.addAssignScaled(proposed, cluster2.template, cluster2.members);
      proposed = normalizer.apply(proposed);

      // Check to see if we have increased outliers by much
      this.outliersBefore = cluster1.outliers + cluster2.outliers;
      this.outliersAfter = 0;

      for (Sample sample : samples)
      {
        if (sample.current != cluster1 && sample.current != cluster2)
          continue;
        double old = sample.distance;
        sample.distance = metric.applyAsDouble(proposed, sample.getTemplate());
        if (outlierRule.test(sample))
          outliersAfter++;
        sample.distance = old;
      }

      this.total = cluster1.members + cluster2.members;
      this.mergeIncrease = (outliersAfter - outliersBefore) / (double) total;
      return this.mergeIncrease;
    }
  }

  private List<MergeTmp> computeMergeDistances(List<Cluster> clustersIn)
  {
    List<MergeTmp> tmp = new LinkedList<>();
    for (int i0 = 0; i0 < clustersIn.size(); ++i0)
      for (int i1 = i0 + 1; i1 < clustersIn.size(); ++i1)
      {
        Cluster cluster1 = clustersIn.get(i0);
        Cluster cluster2 = clustersIn.get(i1);
        double distance = this.metric.applyAsDouble(cluster1.template, cluster2.template);
        if (tmp.size() < 20)
        {
          tmp.add(new MergeTmp(cluster1, cluster2, distance));
          tmp.sort((c1, c2) -> -Double.compare(c1.distance, c2.distance));
        }
        else if (distance < tmp.get(0).distance)
        {
          tmp.remove(0);
          tmp.add(new MergeTmp(cluster1, cluster2, distance));
          tmp.sort((c1, c2) -> -Double.compare(c1.distance, c2.distance));
        }
      }
    return tmp;

  }

  public List<Cluster> mergeClusters(List<Cluster> clustersIn,
          List<Sample> samples)
  {
    List<Cluster> clustersOut = new LinkedList<>(clustersIn);

    while (true)
    {
      List<MergeTmp> tmp = computeMergeDistances(clustersOut);
      for (MergeTmp t : tmp)
      {
        t.evaluateMerge(samples);
      }
      tmp.sort((c1, c2) -> Double.compare(c1.mergeIncrease, c2.mergeIncrease));
      MergeTmp best = tmp.get(0);

      System.out.println("Merge distance: " + best.distance);
      System.out.println("before: " + best.outliersBefore);
      System.out.println("after: " + best.outliersAfter);
      System.out.println("change: " + best.mergeIncrease);

      if (best.mergeIncrease < 0.02)
      {
        clustersOut.remove(best.cluster1);
        clustersOut.remove(best.cluster2);
        Cluster newCluster = new Cluster(best.proposed);
        clustersOut.add(newCluster);
        ClusterUtilities.assignAllToCluster(clustersOut, samples, this.metric);

        // Compose statistics
        ClusterUtilities.buildStatistics(clustersOut, samples, outlierRule);
        continue;
      }
      break;
    }
    return clustersOut;
  }

  public double[][] checkMergeClusters(List<Cluster> clustersIn,
          List<Sample> samples)
  {
    int n = clustersIn.size();
    Matrix out = MatrixFactory.newMatrix(n, n);

    for (int i0 = 0; i0 < n; ++i0)
    {
      for (int i1 = i0 + 1; i1 < n; ++i1)
      {
        Cluster nearest1 = clustersIn.get(i0);
        Cluster nearest2 = clustersIn.get(i1);

        double[] proposed = DoubleArray.multiply(nearest1.template, nearest1.members);
        DoubleArray.addAssignScaled(proposed, nearest2.template, nearest2.members);
        proposed = this.normalizer.apply(proposed);

        // Check to see if we have increased outliers by much
        int total = nearest1.members + nearest2.members;
        int outliersBefore = nearest1.outliers + nearest2.outliers;
        int outliersAfter = 0;
        for (Sample sample : samples)
        {
          if (sample.current != nearest1 && sample.current != nearest2)
            continue;
          double old = sample.distance;
          sample.distance = this.metric.applyAsDouble(proposed, sample.getTemplate());
          if (this.outlierRule.test(sample))
            outliersAfter++;
          sample.distance = old;
        }
        out.set(i0, i1, (outliersAfter - outliersBefore) / (double) total);
        out.set(i1, i0, (outliersAfter - outliersBefore) / (double) total);

      }
    }
    return MatrixOps.toArray(out);
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