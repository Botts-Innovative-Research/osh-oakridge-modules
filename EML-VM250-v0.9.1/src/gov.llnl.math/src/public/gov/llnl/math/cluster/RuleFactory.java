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

import java.util.function.DoublePredicate;
import java.util.function.Predicate;

/**
 *
 * @author nelson85
 */
public class RuleFactory
{
  /**
   * Create a split rule requiring a minimum number of members
   * to split and the cluster is too uncorrelated.
   *
   * @param error
   * @param members
   * @return
   */
  static public Predicate<Cluster> splitOnMeanDistance(double error, int members)
  {
    return p -> (p.meanDistance > error && p.members > members);
  }

  /** Create a split rule if the number of outliers is too large.
   *
   * @param maxOutliers
   * @param minMembers
   * @return
   */
  static public Predicate<Cluster> splitOnOutlierCount(int maxOutliers, int minMembers)
  {
    return p -> (p.outliers > maxOutliers && p.members > minMembers);
  }

    /** Create a split rule if the number of outliers is too large.
   *
   * @param outlierFraction
   * @param minMembers
   * @return
   */
  static public Predicate<Cluster> splitOnOutlierFraction(double outlierFraction, int minMembers)
  {
    return p -> (p.outliers > outlierFraction*p.members && p.members > minMembers);
  }

  static public Predicate<Cluster> removeIfInsufficientMembers(int members)
  {
    return p -> p.members < members;
  }

  static public Predicate<Cluster> removeIfInsufficientValid(int members)
  {
    return p -> p.members-p.outliers < members;
  }

  static public Predicate<Sample> outlierOnDistance(double threshold)
  {
    return p -> p.distance > threshold;
  }

  static public DoublePredicate lessThan(double value)
  {
    return p->p<value;
  }

  static public DoublePredicate greaterThan(double value)
  {
    return p->p<value;
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