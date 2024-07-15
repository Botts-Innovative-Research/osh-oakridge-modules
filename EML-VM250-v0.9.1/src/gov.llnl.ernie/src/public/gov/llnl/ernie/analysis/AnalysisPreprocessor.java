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

import gov.llnl.ernie.Analysis;
import gov.llnl.ernie.data.Record;
import gov.llnl.utility.InitializeException;
import gov.llnl.utility.InitializeInterface;

/**
 * Preprocessors are run prior to feature extraction.
 *
 * The preprocessors augment the record to include any necessary information for
 * feature extraction. This includes extracting the vehicle motion, typing the
 * vehicle. and computing a background estimate.
 *
 * Analysis preprocessors shall not have any state information and must be
 * reentrant.
 *
 * @author nelson85
 */
public interface AnalysisPreprocessor 
{
  /**
   * Initialize the preprocessor.
   *
   * Verifies that all required resources are set and sets up any required data
   * structures.
   *
   * @throws InitializeException if a prerequisite has not been met.
   */
  void initialize(Analysis analysis) throws InitializeException;

  /**
   * Executes the preprocessors and updated the record accordingly.
   *
   * @param record
   */
  void compute(Record record);
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