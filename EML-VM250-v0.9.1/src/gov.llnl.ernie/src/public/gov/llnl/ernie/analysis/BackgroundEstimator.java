/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.ernie.analysis;

import gov.llnl.ernie.data.Record;
import gov.llnl.math.matrix.Matrix;
import java.io.Serializable;

/**
 * Estimator for background.
 *
 * This is a portion of the analysis algorithm called during the preprocessing
 * stage.
 *
 * The background estimator is called after the vehicle motion and vehicle class
 * are determined. The background template is supplied by the vehicle class and
 * then mapped through the vehicle motion into a specific for the portal.
 *
 * The lane parameters from the record are used to determine the spectral shape
 * as a function as suppression.
 *
 * @author nelson85
 */
public interface BackgroundEstimator extends Serializable
{
  public static final int TARGET_LENGTH = 128;
  
  /**
   * Compute the gross counts as a function of time for each sensor.
   *
   * This method is used as a fallback when the vehicle class cannot be
   * determined.
   *
   *
   * @param record
   * @return a matrix with size gamma samples by panels.
   */
  Matrix.ColumnAccess computeDefaultBackground(Record record);

  /**
   * Estimate background suppression for a record using specified background
   * model.
   *
   *
   * @param record
   * @param backgroundModelIndex (0=average, 1=high, 2=low)
   * @return
   */
  Matrix.ColumnAccess computeBackground(Record record, int backgroundModelIndex);

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