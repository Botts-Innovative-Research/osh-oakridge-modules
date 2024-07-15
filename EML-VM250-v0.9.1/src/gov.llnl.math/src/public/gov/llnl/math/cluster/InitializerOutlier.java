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
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Specialized rule for trying to make a new cluster using the outliers.
 *
 * @author nelson85
 */
public class InitializerOutlier implements Initializer
{
  Random rand = new Random();

  @Override
  public List<Cluster> initialize(KMeans algo, List<Sample> samples, int n)
  {
    Cluster current = samples.get(0).current;
    List<Cluster> out = new ArrayList<>();
    // Existing cluster goes out.
    out.add(current);
    int k1 = samples.size();

    for (int j = 0; j < 4; ++j)
    {

      List<Sample> outliers = samples.stream()
              .filter(algo.outlierRule)
              .collect(Collectors.toList());
      int k2 = outliers.size();

      double quality = (double) k2 / (double) k1;
//      System.out.println(k1 + " " + k2 + " " + quality);

      if (algo.splitRule.test(current) == false)
        return out;

      List<Cluster> potential = new ArrayList<>();
      for (int i = 0; i < 40; ++i)
      {
        int t = rand.nextInt(outliers.size());
        double[] template1 = algo.normalizer.apply(outliers.get(t).getTemplate());
        Cluster cluster1 = new Cluster(template1);
        algo.computeDistance(cluster1, outliers);
        cluster1.members = k2 - (int) outliers.stream().filter(algo.outlierRule).count();
        potential.add(cluster1);
      }
      potential.sort((c1, c2) -> -Integer.compare(c1.members, c2.members));
      Cluster best = potential.get(0);
      out.add(best);
      ClusterUtilities.assignAllToCluster(out, outliers, algo.metric);
      ClusterUtilities.buildStatistics(out, samples, algo.outlierRule);
      System.out.println("NEW " + best.members + " " + best.outliers + " " + algo.splitRule.test(best));
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