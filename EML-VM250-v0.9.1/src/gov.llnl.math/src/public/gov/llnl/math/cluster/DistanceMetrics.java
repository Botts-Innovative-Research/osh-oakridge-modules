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

import java.util.function.Supplier;

/**
 * Collection of distance metrics commonly used in KMeans.
 *
 * This is organized into an enum so that we can load the configuration from a
 * configuration file if needed.
 */
public enum DistanceMetrics implements Supplier<DistanceMetric>
{
  DotProduct(ClusterUtilities::metricDotProduct),
  SquaredError(ClusterUtilities::metricSquaredError);
  private DistanceMetric metric;

  DistanceMetrics(DistanceMetric metric)
  {
    this.metric = metric;
  }

  @Override
  public DistanceMetric get()
  {
    return this.metric;
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