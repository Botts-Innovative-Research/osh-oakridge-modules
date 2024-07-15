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

import java.util.function.ToDoubleBiFunction;

/**
 *
 * @author nelson85
 */
public class KernelFactory
{
  public static ToDoubleBiFunction<double[], double[]> fromMetric(
          ToDoubleBiFunction<double[], double[]> metric, double bandwidth)
  {
    final double factor = 1 / Math.sqrt(2 * Math.PI * bandwidth);
    return (d1, d2) -> factor * Math.exp(-0.5 * (metric.applyAsDouble(d1, d2)+0.1) / bandwidth);
  }

  public static ToDoubleBiFunction<double[], double[]> radius(
          ToDoubleBiFunction<double[], double[]> metric, double width)
  {
    return (d1, d2) -> metric.applyAsDouble(d1, d2)<width?1:0;
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