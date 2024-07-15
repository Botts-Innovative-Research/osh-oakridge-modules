/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.utility;

/**
 *
 * @author albin3
 */
public class MutableDouble extends Number
{
  
  private double value;
  
  public MutableDouble(double value)
  {
    this.value = value;
  }
  
  public MutableDouble()
  {
    value = 0.;
  }
  
  @Override
  public int intValue()
  {
    return (int) value;
  }

  @Override
  public long longValue()
  {
    return (long) value;
  }

  @Override
  public float floatValue()
  {
    return (float) value;
  }

  @Override
  public double doubleValue()
  {
    return value;
  }
  
   public void setValue(double value)
  {
    this.value = value;
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