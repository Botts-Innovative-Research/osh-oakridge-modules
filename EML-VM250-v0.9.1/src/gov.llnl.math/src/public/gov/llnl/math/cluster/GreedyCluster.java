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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleBiFunction;
import java.util.function.UnaryOperator;

/**
 * This is a clustering algorithm developed for ERNIE.
 *
 * The procedure is as follows
 *
 * 1) Partition the space into many small clusters of samples so the problem is
 * manageable.
 *
 * 2) For each cluster find all the points in the nearby domain.
 *
 * 3) With those points, find the maximum density point in each cluster (in some
 * cases the small clusters may share the same local maximum density)
 *
 * 4) Combine the clusters to minimize the total entropy.
 *
 * @author nelson85
 */
public class GreedyCluster
{
  /**
   * Random generator used to creating samples.
   */
  public Random random = new Random();
  private Predicate<Sample> outlierRule = null;

  public DoubleUnaryOperator kernel = null;

  /*
   * Metric for measuring distance from cluster sample.
   */
  private ToDoubleBiFunction<double[], double[]> metric = DistanceMetrics.SquaredError.get();

  private UnaryOperator<double[]> normalizer;

//<editor-fold desc="parameters">
  /**
   * @return the outlierRule
   */
  public Predicate<Sample> getOutlierRule()
  {
    return outlierRule;
  }

  /**
   * @param outlierRule the outlierRule to set
   */
  public void setOutlierRule(Predicate<Sample> outlierRule)
  {
    this.outlierRule = outlierRule;
  }

  /**
   * @return the normalizer
   */
  public UnaryOperator<double[]> getNormalizer()
  {
    return normalizer;
  }

  /**
   * @return the metric
   */
  public ToDoubleBiFunction<double[], double[]> getMetric()
  {
    return metric;
  }

  /**
   * @param metric the metric to set
   */
  public void setMetric(ToDoubleBiFunction<double[], double[]> metric)
  {
    this.metric = metric;
  }

  public void setMetric(Supplier<DistanceMetric> dm)
  {
    this.metric = dm.get();
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

//</editor-fold>
  public Clustering execute(List<Sample> samples)
  {
    // Make sure the samples are properly normalized
    samples.stream().forEach(p -> p.setTemplate(normalizer.apply(p.getTemplate())));

    List<Cluster> clusters = new LinkedList<>();

    // We need to make sure there is no structure in the data
    Collections.shuffle(samples, random);

    // Assign every sample to cluster with the required distance criteria
    // This will partition the space into relatively small domains
    for (Sample sample : samples)
    {
      Cluster best = ClusterUtilities.assignToCluster(clusters, sample, metric);
     
      if (best == null || outlierRule.test(sample))
      {
        best = new Cluster(sample.getTemplate());
        clusters.add(best);
        sample.current = best;
        sample.distance = 0;
      }
    }

    ClusterUtilities.tallyMembers(clusters, samples);

    // The criteria for accepting needs to be based on the bandwidth of the
    // kernel density function
    clusters.forEach(p -> p.findNeighbors(clusters, metric, 0.1));

    // Determine the statistics for each cluster
    ClusterUtilities.buildStatistics(clusters, samples, outlierRule);


    return new Clustering(clusters, samples);
  }

//<editor-fold desc="internal">



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