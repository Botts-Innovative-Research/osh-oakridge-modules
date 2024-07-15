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

import java.util.function.DoublePredicate;

/**
 * Support class for DoubleArray.find.
 */
@FunctionalInterface
public interface DoubleConditional extends DoublePredicate
{

  class GreaterThan implements DoubleConditional
  {
    private final double cond;

    public GreaterThan(double cond)
    {
      this.cond = cond;
    }

    @Override
    public boolean test(double value)
    {
      return value > cond;
    }
  }

  class GreaterThanEqual implements DoubleConditional
  {
    private final double cond;

    public GreaterThanEqual(double cond)
    {
      this.cond = cond;
    }

    @Override
    public boolean test(double value)
    {
      return value >= cond;
    }
  }

  class LessThan implements DoubleConditional
  {
    private final double cond;

    public LessThan(double cond)
    {
      this.cond = cond;
    }

    @Override
    public boolean test(double value)
    {
      return value < cond;
    }
  }

  class LessThanEqual implements DoubleConditional
  {
    private final double cond;

    public LessThanEqual(double cond)
    {
      this.cond = cond;
    }

    @Override
    public boolean test(double value)
    {
      return value <= cond;
    }
  }

  class NotEqual implements DoubleConditional
  {
    double cond;

    public NotEqual(double cond)
    {
      this.cond = cond;
    }

    @Override
    public boolean test(double value)
    {
      return value != cond;
    }

  }

  public static class Equal implements DoubleConditional
  {
    double cond;

    public Equal(double cond)
    {
      this.cond = cond;
    }

    @Override
    public boolean test(double value)
    {
      return value == cond;
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