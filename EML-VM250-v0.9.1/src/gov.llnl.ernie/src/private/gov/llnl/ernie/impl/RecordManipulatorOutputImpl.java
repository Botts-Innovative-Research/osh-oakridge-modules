/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.ernie.impl;

import gov.llnl.ernie.data.Record;
import gov.llnl.ernie.internal.manipulator.AnisotropicModel;
import gov.llnl.ernie.manipulator.RecordManipulator;
import gov.llnl.ernie.rtk.DoubleSpectrum;
import gov.llnl.math.matrix.Matrix;

public class RecordManipulatorOutputImpl implements RecordManipulator.Output
{
  private Record record;
  private double grossSNR;
  private double injectedSNR;

  // Auditing
  public Matrix.ColumnAccess compactIntensity;
  public DoubleSpectrum compactSource;  // original source prior to injection
  public Matrix compactSpectrum; // actual source after injection
  public double[] flux;
  transient public AnisotropicModel distributedAnisotopicModel;
  transient public AnisotropicModel compactAnisotropicModel;
  transient public Matrix compactShielding;

  @Override
  public Record getRecord()
  {
    return record;
  }

  @Override
  public double getGrossSNR()
  {
    return this.grossSNR;
  }

  @Override
  public double getInjectedSNR()
  {
    return this.injectedSNR;
  }

  /**
   * @param record the record to set
   */
  public void setRecord(Record record)
  {
    this.record = record;
  }

  /**
   * @param grossSNR the grossSNR to set
   */
  public void setGrossSNR(double grossSNR)
  {
    this.grossSNR = grossSNR;
  }

  /**
   * @param injectedSNR the injectedSNR to set
   */
  public void setInjectedSNR(double injectedSNR)
  {
    this.injectedSNR = injectedSNR;
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