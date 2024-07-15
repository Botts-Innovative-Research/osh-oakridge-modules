/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.ernie.data;

import gov.llnl.ernie.Analysis.SourceType;

/**
 * Analysis Sources give the details about sources found during the analysis.
 *
 * Analysis sources may either be produced for the full scan or may be broken
 * down by individual source if multiple sources can be extracted.
 *
 * @author nelson85
 */
public interface AnalysisSource
{
  /**
   * @return the probabilityRelease
   */
  double getProbabilityRelease();

  /**
   * Get the name of the classifier that produced this source.
   *
   * @return
   */
  String getClassifierUsed();

//<editor-fold desc="position">
  double getPositionX1();

  double getPositionX2();

  double getPositionY();

  double getPositionZ();

  /**
   * Returns true if the source classification indicates a compact source.
   *
   * @return
   */
  boolean isCompact();

  /**
   * Returns true if the source classification indicates a distributed source.
   *
   * @return
   */
  boolean isDistributed();

//</editor-fold>
//<editor-fold desc="sourcetype">
  /**
   * Returns true if the source is most likely non-emitting class.
   *
   * @return
   */
  boolean isNonEmitting();

  SourceType getSourceType();

  /**
   * @return the probabilityNonEmitting
   */
  double getProbabilityNonEmitting();

  /**
   * @return the probabilityContamination
   */
  double getProbabilityContamination();

  /**
   * @return the probabilityFissile
   */
  double getProbabilityFissile();

  /**
   * @return the probabilityIndustrial
   */
  double getProbabilityIndustrial();

  /**
   * @return the probabilityInvestigate
   */
  double getProbabilityInvestigate();

  /**
   * @return the probabilityMedical
   */
  double getProbabilityMedical();

  /**
   * @return the probabilityNORM
   */
  double getProbabilityNORM();
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