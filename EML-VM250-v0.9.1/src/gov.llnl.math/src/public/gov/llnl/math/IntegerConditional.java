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

import java.util.function.IntPredicate;

/**
 *
 * @author nelson85
 */
public interface IntegerConditional extends IntPredicate 
{
//  /**
//   * Returns true if the condition is met.
//   *
//   * @param value
//   * @return true if condition met.
//   */
//  boolean test(int value);

  public static class GreaterThan implements IntegerConditional
  {
    private final int cond;

    public GreaterThan(int cond)
    {
      this.cond = cond;
    }

    @Override
    public boolean test(int value)
    {
      return value > cond;
    }
  }

  public static class GreaterThanEqual implements IntegerConditional
  {
    private final int cond;

    public GreaterThanEqual(int cond)
    {
      this.cond = cond;
    }

    @Override
    public boolean test(int value)
    {
      return value >= cond;
    }
  }

  public static class LessThan implements IntegerConditional
  {
    private final int cond;

    public LessThan(int cond)
    {
      this.cond = cond;
    }

    @Override
    public boolean test(int value)
    {
      return value < cond;
    }
  }

  public static class LessThanEqual implements IntegerConditional
  {
    private final int cond;

    public LessThanEqual(int cond)
    {
      this.cond = cond;
    }

    @Override
    public boolean test(int value)
    {
      return value <= cond;
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