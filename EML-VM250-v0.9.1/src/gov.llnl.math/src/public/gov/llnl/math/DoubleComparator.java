/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.math;

/**
 *
 * @author nelson85
 */
public interface DoubleComparator
{

  /**
   * Compares two doubles.
   *
   * @param t1
   * @param t2
   * @return a negative integer, zero, or a positive integer as the first
   * argument is less than, equal to, or greater than the second.
   */
  int compare(double t1, double t2);

  class Absolute implements DoubleComparator
  {
    @Override
    final public int compare(double t1, double t2)
    {
      t1 = (t1 > 0) ? t1 : -t1;
      t2 = (t2 > 0) ? t2 : -t2;
      return Double.compare(t1, t2);
    }
  }

  class Positive implements DoubleComparator
  {
    @Override
    final public int compare(double t1, double t2)
    {
      return Double.compare(t1, t2);
    }
  }

  class Negative implements DoubleComparator
  {
    @Override
    public final int compare(double t1, double t2)
    {
      return -Double.compare(t1, t2);
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