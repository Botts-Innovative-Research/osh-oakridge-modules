/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.math.internal.statistical;

import gov.llnl.math.statistical.Range;

/**
 *
 * @author nelson85
 */
public class RangeImpl implements Range
{
  double begin;
  double end;

  public RangeImpl(double begin, double end)
  {
    this.begin = begin;
    this.end = end;
  }

  @Override
  public boolean contains(double value)
  {
    return (value >= begin) && (value < end);
  }

  @Override
  public boolean intersects(Range range)
  {
    if (range.getEnd() < begin || range.getBegin() >= end)
      return false;
    return true;
  }

  @Override
  public double getBegin()
  {
    return begin;
  }

  @Override
  public double getEnd()
  {
    return end;
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