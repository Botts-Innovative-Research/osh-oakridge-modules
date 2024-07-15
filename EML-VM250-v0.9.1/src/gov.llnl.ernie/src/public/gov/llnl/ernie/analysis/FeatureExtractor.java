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
import gov.llnl.utility.InitializeException;

/**
 * Common interface for all classes that operate on a record to extract a set of
 * features.
 *
 * Feature extractors shall not have any state information and must be
 * reentrant.
 *
 * @author guensche1
 */
public interface FeatureExtractor extends InfoTable
{
  /**
   * Initialize the feature extractor.
   *
   * Verifies that all required resources are set and sets up any required data
   * structures.
   *
   * @throws InitializeException if a prerequisite has not been met.
   */
  @Override
  void initialize() throws InitializeException;

  /**
   * Creates a feature group to hold the data produced by the extractor.
   *
   * This is used when retrieving a store features.
   *
   * @return
   */
  public Features newFeatures();

  /**
   * Compute {@link Features}
   *
   * @param record
   * @return
   * @throws AnalysisException
   */
  public Features compute(Record record) throws AnalysisException;

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