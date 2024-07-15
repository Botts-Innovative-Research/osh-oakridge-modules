/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.ernie.manipulator;

import gov.llnl.ernie.data.Record;
import gov.llnl.math.matrix.Matrix;

/**
 *
 * @author nelson85
 */
public interface RecordManipulator
{

  /**
   * Apply a manipulation to a record. This is the main method for this class.
   * It alters velocity and injects sources as directed by the manipulation
   * object. The method produces a new record with that same meta fields as the
   * original. It does not try to update the alarm flags, thus the alarm flags
   * are irregular.
   *
   * @param record is the base record to manipulate.
   * @param manipulation
   * @return a new record with the manipulation applied.
   * @throws ManipulationException when the alteration of velocity has failed.
   */
  Output applyManipulation(Record record, Manipulation manipulation)
          throws ManipulationException;

  Manipulation createManipulation(ManipulationDescription description);

  InjectionSourceLibrary getLibrary();

  /**
   * Direct injection of gammaData into a record.
   *
   * This is used as part of the reinjection process.
   *
   * @param record
   * @param position
   * @param gammaData
   */
  void injectGamma(Record record, double[] position, Matrix[] gammaData);

  public interface Output
  {
    Record getRecord();

    public double getGrossSNR();

    public double getInjectedSNR();
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