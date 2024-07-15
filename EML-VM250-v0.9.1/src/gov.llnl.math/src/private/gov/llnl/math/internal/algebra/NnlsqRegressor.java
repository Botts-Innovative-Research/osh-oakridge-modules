/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.math.internal.algebra;

import gov.llnl.math.algebra.Nnlsq;
import gov.llnl.utility.HashUtilities;
import gov.llnl.utility.UUIDUtilities;
import gov.llnl.utility.annotation.Internal;
import java.io.Serializable;

//<editor-fold desc="regressor">
/**
 * This represents the working definition for regressors during processing.
 */
@Internal
public class NnlsqRegressor implements Nnlsq.Datum, Serializable
{
  private static final long serialVersionUID = UUIDUtilities.createLong("NnlsqRegressor-v1");

  //<editor-fold desc="list">
  // List element behavior
  NnslqRegressorSet set;
  NnlsqRegressor prev;
  NnlsqRegressor next;
  //</editor-fold>
  //<editor-fold desc="inputs">
  public int id;
  public long hash;
  public double[] regressor;
  /**
   * The regressor times the weighting coefficients.
   */
  public double[] regressorWeighted;
  /**
   * Offset to the first element in the regressor list.
   */
  public int offset;
  //</editor-fold>
  //<editor-fold desc="state">
  // State variable
  
  /** 
   * Cycle state last time this regressor was used.  
   * This is used to determine if the state has already been
   * evaluated before. 
   */
  public long lastUsed = -1;
  public double scale = 1.0; // 1/sqrt(A_i^T*A_i)
  public double regressandProjection; // B^T*W*A/sqrt(B^T*W*B)
  public double demand;
  public double demandOffset = 0;
  public double coef; // X
  public double update; // Zp
  public double revised; //
  public int convergenceTag = 0;
  public int constrainTag = 0;
  //</editor-fold>

  NnlsqRegressor(int id, double[] regressor, double[] regressorWeighted, int offset)
  {
    super();
    this.id = id;

    // Create a 64 bit hash code for cycle checking
    int[] hashInput = new int[]
    {
      System.identityHashCode(this), System.identityHashCode(regressor)
    };
    this.hash = HashUtilities.hash(hashInput);
    this.regressor = regressor;
    this.regressorWeighted = regressorWeighted;
    this.offset = offset;
  }

//<editor-fold desc="public api">
  @Override
  public int getId()
  {
    return this.id;
  }

  @Override
  public double getCoef()
  {
    return this.coef;
  }
//</editor-fold>
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