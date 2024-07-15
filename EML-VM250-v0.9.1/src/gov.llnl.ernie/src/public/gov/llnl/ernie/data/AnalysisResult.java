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

import gov.llnl.ernie.Analysis.RecommendedAction;
import java.util.Collection;
import gov.llnl.ernie.analysis.Features;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author nelson85
 */
public interface AnalysisResult
{
//<editor-fold desc="results" defaultstate="collapsed">
  /**
   * Returns true if the Ernie algorithm processed the record successfully.
   *
   * @return true if this structure contains a valid result, or false if the
   * analysis failed.
   */
  boolean isValid();

  /**
   * Get the reason for a failure.
   *
   * This should be an empty string for a successful analysis. It should produce
   * a reason if the result was not valid.
   *
   * @return
   */
  String getFaultMessage();

  /**
   * Determine if the analysis went to fallback.
   *
   * @return true if fallback was executed on this record being processed; i.e.
   * analysis failed.
   */
  boolean isFallback();

  /**
   * Determine the recommended action for ERNIE. Returns true if this is either
   * a neutron alarm or the analysis completed and indicates an alarm, or the
   * fallback indicate the existing portal settings indicate an alarm.
   *
   * @return
   */
  RecommendedAction getRecommendedAction();

  /**
   * Get the probability of investigate.
   *
   * This only has a proper value if Ernie completed analysis successfully. For
   * general use, method 'getRecommendedAction' should be used instead.
   *
   * @return
   */
  double getProbabilityInvestigate();

  /**
   * Get the probability of release.
   *
   * This only has a proper value if Ernie completed analysis successfully. For
   * general use, method 'getRecommendedAction' should be used instead.
   *
   * @return
   */
  double getProbabilityRelease();

//  /**
//   * Returns the maximum of the gamma gross alarm metrics.
//   *
//   * (FIXME obsolete) This was used to drive the display of the SAIC results. As
//   * the threshold are no longer correct, its output is irregular.
//   *
//   * @return
//   */
//  @Deprecated
//  boolean getGammaAlarm();

  /**
   * Returns the maximum of the neutron alarm metrics.
   *
   * (FIXME obsolete) This was used to drive the display of the SAIC results. As
   * the threshold are no longer correct, its output is irregular.
   *
   * @return
   */
  @Deprecated
  boolean getNeutronAlarm();

//</editor-fold>
//<editor-fold desc="sources" defaultstate="collapsed">
  /**
   * Returns number of sources detected
   *
   * @return
   */
  int getNumberOfSources();

  /**
   * The Total Source detected.
   *
   * FIXME revisit this interface.
   *
   * @return
   */
  AnalysisSource getFullSource();

  /**
   * @param i index of source to return
   * @return
   */
  AnalysisSource getSource(int i);

//</editor-fold>
//<editor-fold desc="audit" defaultstate="collapsed">
  /**
   *
   * @return will not be null.
   */
  AnalysisContextualInfo getAnalysisContextualInfo();

  /**
   *
   * @return will not be null.
   */
  ScanContextualInfo getScanContextualInfo();

  /**
   * Results from the underlying portal.
   *
   * This will be a copy of the values from the Record.
   *
   * @return will not be null.
   */
  VendorAnalysis getVendorAnalysis();

  /**
   * Results from the underlying portal.
   *
   * This will be a copy of the values from the Record.
   *
   * @return will not be null.
   */
  VehicleInfo getVehicleInfo();

  /**
   * Gives the last record called with processRecord. For use by the display.
   *
   * @return the record or null if the record is not stored.
   */
  Record getRecord();

//</editor-fold>
//<editor-fold desc="features" defaultstate="collapsed">
  /**
   * Get all features computed by the analysis.
   *
   * @return
   */
  Collection<Features> getFeatures();

  default Map<String, Object> getFeatureMap()
  {
    Map<String, Object> out = new HashMap<>();
    for (Features f: getFeatures())
    {
      out.putAll(f.toMap());
    }
    return out;
  }

  /**
   * Retrieves a specific feature from the features computed.
   *
   * @param <T>
   * @param cls
   * @return
   */
  <T extends Features> T getFeatureGroup(Class<T> cls);
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