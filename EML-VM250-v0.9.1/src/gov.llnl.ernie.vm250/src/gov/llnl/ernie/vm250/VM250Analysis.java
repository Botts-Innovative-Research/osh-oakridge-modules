/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.ernie.vm250;

import gov.llnl.ernie.vm250.data.VM250AnalysisSource;
import edu.cmu.cs.auton.classifiers.Output;
import gov.llnl.ernie.Analysis;
import gov.llnl.ernie.ErniePackage;
import gov.llnl.ernie.data.AnalysisResult;
import gov.llnl.ernie.impl.AnalysisResultImpl;
import gov.llnl.ernie.analysis.FeatureExtractor;
import gov.llnl.ernie.data.Record;
import gov.llnl.ernie.classifier.Classifier;
import gov.llnl.ernie.analysis.AnalysisException;
import gov.llnl.ernie.analysis.AnalysisPreprocessor;
import gov.llnl.ernie.neutron.NeutronLocationEstimator;
import gov.llnl.utility.InitializeException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import gov.llnl.ernie.analysis.Features;
import gov.llnl.ernie.common.ExtentFeatures;
import gov.llnl.ernie.common.GammaNSigmaFeatures;
import gov.llnl.ernie.common.StatisticalFeatures;
import gov.llnl.ernie.vm250.data.VM250Record;
import gov.llnl.utility.xml.bind.ReaderInfo;
import java.util.Collections;
import java.util.HashMap;

/**
 *
 * @author nelson85
 */
@ReaderInfo(VM250AnalysisReader.class)
public class VM250Analysis implements Analysis
{
  private final static Logger LOGGER = ErniePackage.LOGGER;

//<editor-fold desc="settings">
  private boolean verbose = false;
  private double threshold = 0.5;
  private double nSigmaThreshold = -1;
  private double backgroundRatioThreshold = -1;
//</editor-fold>
//<editor-fold desc="members" defaultstate="collapsed">
  // Support functions
  public VM250BackgroundEstimator backgroundEstimator = new VM250BackgroundEstimator();
  // Preprocessors
  private VM250BackgroundEstimator backgroundPreprocessor;
  private List<AnalysisPreprocessor> preprocessors = new ArrayList<>();

  // Feature extractors
  private List<FeatureExtractor> extractors = new ArrayList<>();

  // Classifiers
  Classifier classifier;
  private Map<String, String> settings = new HashMap<>();

//  double metrics[]; // FIXME should have separate metrics for each classifier
//</editor-fold>
//<editor-fold desc="ctor" defaultstate="collapsed">
  public VM250Analysis()
  {
  }

  public void initialize() throws InitializeException
  {
    // Verify that all requirements are satisfied to work.
    // Call initialize on all required components
    for (AnalysisPreprocessor p : preprocessors)
    {
      p.initialize(this);
    }

    for (FeatureExtractor p : this.extractors)
    {
      p.initialize();
    }
  }

//</editor-fold>
  // Ernie viewer has a list of records to call in order,
  // it then directs the analysis to
  //  1) read a record
  //  2) call each of the extract features
  //  3) Call cmu predict code
  //  Then the viewer will get each of the results needed and push it to the views
  /**
   * Analyze a record.
   *
   * This will produce the features and the classification.
   *
   * @param record
   * @return
   * @throws AnalysisException
   */
  @Override
  public AnalysisResult processRecord(Record record) throws AnalysisException
  {
    // Verify record exists
    if (record == null)
    {
      throw new AnalysisException("Record is null");
    }

    AnalysisResultImpl result = (AnalysisResultImpl)allocateResult();

    // Load the current record
    result.currentRecord = record;

    // Make sure there is a lane for this vehicle
    if (record.getLane() == null)
    {
      throw new AnalysisException(String.format("Record is in unknown lane: dataSourceId=%d, rpmId=%d",
              record.getContextualInfo().getScanID(), record.getLane().getRpmId()));
    }

    if (record.getVendorAnalysis() == null)
    {
      throw new AnalysisException("Record does not have segment results");
    }

    // For now we will just depend on the original neutron alarm
    result.setNeutronAlarm(record.getVendorAnalysis().isNeutronAlarm());
    if (result.getNeutronAlarm() && record.getVehicleMotion() != null)
    {
      result.neutronLocation = NeutronLocationEstimator.compute(record);
    }

    // Watch for birds or other bad scans that force us to fallback
    if (irregularVehicle(record))
    {
      executeFallback(result, record);
      return result;
    }

    // Extract features and run the classifier
    try
    {
      prepare(record);
      if (record.bad())
      {
        throw new AnalysisException("Bad record");
      }

      extractFeatures(result, record, this.extractors);
      executeClassifier(result);
    }
    catch (AnalysisException e)
    {
      LOGGER.log(Level.INFO, "Analysis failed on record '" + record.getContextualInfo().getScanID() + "' - Executing fallback...");
      executeFallback(result, record);
    }
    return result;
  }

//<editor-fold desc="internal" defaultstate="collapsed">
  /**
   * (internal) Verify if a vehicle has transitions that match a vehicle
   * present. This returns true if any of the vps beams were not broken during
   * the encounter. This is a screen against birds and trash passing through the
   * portal.
   *
   * @param record
   * @return true if the vehicle is irregular
   */
  public boolean irregularVehicle(Record record)
  {
    // FIXME not sure if this concept applies to VM250

    return false;
  }

  @Override
  public AnalysisResult allocateResult()
  {
    AnalysisResultImpl AR = new AnalysisResultImpl();
    AR.setThreshold(threshold);
    return AR;
  }

  @Override
  public void prepare(Record record) throws AnalysisException
  {
    for (AnalysisPreprocessor pp : this.preprocessors)
    {
      try
      {
       pp.compute(record);
      }
      catch(Exception ex)
      {
        LOGGER.log(Level.SEVERE, "Failure in preprocessor: " + pp.getClass(), ex);
      }
    }
  }

  /**
   * (internal) Extracts the features for a record. Requires the vehicle has a
   * valid motion profile and vehicle type. It computes the background estimate
   * and fails if it cannot be computed. It then calls each of the feature
   * extractors. If any of the feature extractors fails, it calls the fall back
   * routine.
   *
   * @param results
   * @param record
   * @param extractors
   * @throws gov.llnl.ernie.analysis.AnalysisException
   */
  @Override
  public void extractFeatures(AnalysisResult results, Record record, Collection<FeatureExtractor> extractors)
          throws AnalysisException
  {
    AnalysisResultImpl results2 = (AnalysisResultImpl) results;
    for (FeatureExtractor featureExtractor : this.extractors)
    {
      Features features = featureExtractor.compute(record);
      if (features == null)
      {
        throw new AnalysisException("Unable to produce features from " + featureExtractor);
      }
      results2.addFeatures(features);
    }
    results2.setValid(true);
  }

  /**
   * (internal) This is the fallback called if there is a problem with analysis.
   * If there is a problem, the vendor alarm flags are used to set the alarm
   * output. The standard features and the background are populated for the
   * display, but the values are not used for determining if there is an alarm.
   *
   * @param result2
   * @param record
   */
  public void executeFallback(AnalysisResult result2, Record record)
  {
    AnalysisResultImpl result = (AnalysisResultImpl) result2;
    VM250Record typedRecord = (VM250Record) record;
    result.setFallback(true);
    result.setFaultMessage(String.join(",", record.getBadReasons()));
    if (record.getVendorAnalysis().isAlarm())
    {
      result.setProbabilityInvestigate(1);
      result.setProbabilityRelease(0);
    }
    else
    {
      result.setProbabilityInvestigate(0);
      result.setProbabilityRelease(1);
    }
//    result.clearFeatures();
    // FIXME
    // failure here is ignored.
    typedRecord.gammaBackgroundGross = backgroundPreprocessor.computeDefaultBackground(record);
  }

  /**
   * (internal) Calls the CMU classifier. Will return without a result if the
   * CMU Classifier_VM250 is not loaded.
   *
   * @throws gov.llnl.ernie.analysis.AnalysisException
   */
  public void executeClassifier(AnalysisResultImpl result) throws AnalysisException
  {
    if (classifier == null || !classifier.valid())
    {
      throw new AnalysisException("bad classifier");
    }

    // Call the classifier on the full data set assuming there is only one source
    Output CMU_result = classifier.predict(result.getFeatures());
//    metrics = classifier.getCMU_metrics(CMU_result);
    VM250AnalysisSource sourceFull = new VM250AnalysisSource(
            classifier.getOutputLabels(),
            CMU_result.getClassLogLikelihoods(),
            result.findFeatures(StatisticalFeatures.class),
            result.findFeatures(ExtentFeatures.class)
    );
    sourceFull.setClassifierUsed("full");
    result.sourceFull = sourceFull;
    // There is only one source so we use joint[0] position for display
    result.addSource(result.sourceFull);
    result.setProbabilityInvestigate(sourceFull.getProbabilityInvestigate());
    result.setProbabilityRelease(sourceFull.getProbabilityRelease());
    
    // also check if we've exceeded either 'force alarm' threshold:
    GammaNSigmaFeatures nsigma = result.findFeatures(GammaNSigmaFeatures.class);
    if (((this.nSigmaThreshold > 0) && (nsigma.getMaxAllPanels() > this.nSigmaThreshold))
            ||
        ((this.backgroundRatioThreshold > 0) && (nsigma.getMaxBgRatio() > this.backgroundRatioThreshold)))
    {
      result.setProbabilityInvestigate(1);
      result.setProbabilityRelease(0);
    }
  }
//</editor-fold>
//<editor-fold desc="accessors" defaultstate="collapsed">

  @Override
  public double getThreshold()
  {
    return this.threshold;
  }

  /**
   * Adjust the probability required to investigate. Range between 0 and 1. The
   * larger the factor the more evidence required to investigate.
   *
   * This is called automatically by initialize.
   *
   * @param threshold
   */
  @Override
  public void setThreshold(double threshold)
  {
    this.threshold = threshold;
  }
  
  public double getNSigmaThreshold()
  {
    return this.nSigmaThreshold;
  }
  
  /**
   * Adjust the N-Sigma threshold for forcing ERNIE to investigate. Used to
   * ensure very bright sources (possibly above regulatory limits) are not
   * released.
   * 
   * @param value 
   */
  public void setNSigmaThreshold(double value)
  {
    this.nSigmaThreshold = value;
  }

  public double getBackgroundRatioThreshold()
  {
    return this.backgroundRatioThreshold;
  }
  
  /**
   * Adjust the gross / background threshold for forcing ERNIE to investigate.
   * Alternative way to ensure very bright sources (possibly above regulatory limits)
   * are not released.
   * 
   * @param value 
   */
  public void setBackgroundRatioThreshold(double value)
  {
    this.backgroundRatioThreshold = value;
  }

  /**
   * Set the primary classifier. Normally this happens when initializing, but
   * may need to override the original with another version.
   *
   * @param cls = new primary classifier
   */
  @Override
  public void setClassifier(Classifier cls)
  {
    this.classifier = cls;
  }

  @Override
  public String getModelName()
  {
    return classifier.getModelName();
  }

  @Override
  public List<FeatureExtractor> getFeatureExtractors()
  {
    return extractors;
  }

  public void setPreprocessors(List<AnalysisPreprocessor> preprocessors)
  {
    this.preprocessors.addAll(preprocessors);
    for (AnalysisPreprocessor v : preprocessors)
    {
      if (v instanceof VM250BackgroundEstimator)
      {
        this.backgroundPreprocessor = (VM250BackgroundEstimator) v;
      }
    }
  }

  public void setFeatureExtractors(List<FeatureExtractor> featureExtractors)
  {
    this.extractors.addAll(featureExtractors);
  }

  /**
   * @return the verbose
   */
  public boolean isVerbose()
  {
    return verbose;
  }

  /**
   * @return the backgroundPreprocessor
   */
  public VM250BackgroundEstimator getBackgroundPreprocessor()
  {
    return backgroundPreprocessor;
  }

  /**
   * @return the preprocessors
   */
  @Override
  public List<AnalysisPreprocessor> getPreprocessors()
  {
    return preprocessors;
  }

  /**
   * @return the extractors
   */
  public List<FeatureExtractor> getExtractors()
  {
    return extractors;
  }

  /**
   * @return the classifier
   */
  public Classifier getClassifier()
  {
    return classifier;
  }

//</editor-fold>
  @Override
  public void addSetting(String key, String value)
  {
    this.settings.put(key, value);
  }

  @Override
  public Map<String, String> getSettings()
  {
    // FIXME we should copy in thresholds as well so that we have a complete
    // list
    return this.settings;
  }

  @Override
  public List<FeatureExtractor> getFallbackExtractors()
  {
    return Collections.EMPTY_LIST;
  }
  
  public Class<? extends gov.llnl.ernie.Fault> getFaultClass()
  {
    return VM250Record.Fault.class;
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