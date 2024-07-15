/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.ernie;

/**
 * Options for the Analysis loader.
 *
 * Example:
 * 
 * {@code  <pre>
 *    DocumentReader<Analysis> reader = DocumentReader.create(Analysis.class);
 *    reader.setProperty(AnalysisOptions.DISABLE_CLASSIFIERS, true);
 *    Analysis analysis=reader.loadFile(Paths.get("analysis.xml"));
 * </pre> }
 *
 * @author nelson85
 */
public class AnalysisOptions
{
  /**
   * Boolean flag to the analysis reader to disable reading of classifier during
   * the loading process.
   *
   * If true the classifiers will not be read.
   */
  public static final String DISABLE_CLASSIFIERS = "http://ernie.llnl.gov#disable_classifiers";
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