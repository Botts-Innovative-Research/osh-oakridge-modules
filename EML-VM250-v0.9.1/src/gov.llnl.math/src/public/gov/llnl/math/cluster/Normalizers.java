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
import java.util.function.UnaryOperator;

/**
 * Collection of normalizers used in the KMeans algorithm.
 *
 * A normalizer is applied to a feature vector to condition it to work with
 * the distance metric. It is applied to convert the initial samples into
 * feature vectors and each time a cluster center is updated.
 *
 * This is organized into an enum so that we can load the configuration from a
 * configuration file if needed.
 */
public enum Normalizers implements Supplier<UnaryOperator<double[]>>
{
  None((p) -> p),
  L1(ClusterUtilities::l1),
  L2(ClusterUtilities::l2),
  CENTER(ClusterUtilities::center);

  private UnaryOperator<double[]> normalizer;

  Normalizers(UnaryOperator<double[]> normalizer)
  {
    this.normalizer = normalizer;
  }

  @Override
  public UnaryOperator<double[]> get()
  {
    return this.normalizer;
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