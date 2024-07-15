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
import java.util.Random;

/**
 *
 * @author nelson85
 */
public class InitializerRandom implements Initializer
{
  /**
   * Creates an initial population of clusters.
   *
   * @param samples
   * @param n
   * @return
   */
  @Override
  public List<Cluster> initialize(KMeans algo, List<Sample> samples, int n)
  {
    Random random = new Random();
    try
    {
      if (samples.size() < n)
        throw new RuntimeException("Insufficient samples for split");
      Cluster[] clusters = new Cluster[n];
      for (int i = 0; i < n; ++i)
      {
        clusters[i] = new Cluster(null);
        clusters[i].template = samples.get(i).getTemplate();
      }
      for (Sample sample : samples)
      {
        sample.current = clusters[random.nextInt(n)];
        sample.distance = 0;
      }
      ArrayList out = new ArrayList(Arrays.asList(clusters));
      algo.rebuildClusters(out, samples);
      return out;
    }
    catch (Exception ex)
    {
      throw new RuntimeException("samples " + samples.size() + " " + n, ex);
    }
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