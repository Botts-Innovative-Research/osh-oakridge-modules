/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.math.optimize;

import gov.llnl.math.MathExceptions;
import java.util.function.DoubleUnaryOperator;

/**
 * Base class to compute regression onto a model.
 *
 * @param <Model>
 */
public interface Regression<Model extends DoubleUnaryOperator>
{

  /**
   * Get the total number of regression points added thus far.
   *
   * @return the number of added elements.
   */
  int getNumObservations();

  /**
   * Add a point to the regression without weighting.
   *
   * @param x
   * @param y
   */
  void add(double x, double y);

  /**
   * Add a point to the regression with a weight lambda.
   *
   * @param x
   * @param y
   * @param lambda is the weight to apply to the measurement.
   */
  void add(double x, double y, double lambda);

  /**
   * Compute the slope and intercept the represents the linear regression for
   * this point.
   *
   * @return an array holding the slope and offset.
   * @throws MathExceptions.ConvergenceException
   */
  Model compute()
          throws MathExceptions.ConvergenceException;

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