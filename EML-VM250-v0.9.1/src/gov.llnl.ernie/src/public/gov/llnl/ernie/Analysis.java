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

import gov.llnl.ernie.data.AnalysisResult;
import gov.llnl.ernie.analysis.FeatureExtractor;
import gov.llnl.ernie.analysis.AnalysisException;
import gov.llnl.ernie.analysis.AnalysisPreprocessor;
import gov.llnl.ernie.analysis.VehicleClassifier;
import gov.llnl.ernie.data.Record;
import gov.llnl.ernie.classifier.Classifier;
import gov.llnl.utility.InitializeInterface;
import gov.llnl.utility.io.ReaderException;
import gov.llnl.utility.xml.bind.DocumentReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 *
 * @author nelson85
 */
@DocumentReader.Hooks(AnalysisHooks.class)
public interface Analysis extends InitializeInterface
{
  
  public enum RecommendedAction
  {
    INVESTIGATE(2),
    NONE(0),
    RELEASE(1);

    private int _code;

    RecommendedAction(int code)
    {
      this._code = code;
    }

    public int getValue()
    {
      return _code;
    }
  }

  public enum SourceType
  {
    CONTAMINATION(5),
    FISSILE(4),
    INDUSTRIAL(3),
    INVALID(9),
    MEDICAL(2),
    NEUTRON(8),
    NONE(7),
    NONEMITTING(0),
    NORM(1),
    UNKNOWN(6);
    private int _code;

    SourceType(int code)
    {
      this._code = code;
    }

    public int getValue()
    {
      return _code;
    }
  }

  String getModelName();

  double getThreshold();

  // Ernie viewer has a list of records to call in order,
  // it then directs the analysis to
  //  1) read a record
  //  2) call each of the extract features
  //  3) Call cmu predict code
  //  Then the viewer will get each of the results needed and push it to the views
  AnalysisResult processRecord(Record record) throws AnalysisException;

  /**
   * Set the primary classifier. Normally this happens when initializing, but
   * may need to override the original with another version.
   *
   * @param cls = new primary classifier
   */
  void setClassifier(Classifier cls);

  /**
   * Adjust the probability required to investigate. Range between 0 and 1. The
   * larger the factor the more evidence required to investigate.
   *
   * This is called automatically by initialize.
   *
   * @param threshold
   */
  void setThreshold(double threshold);

  /**
   * Get a map containing all the settings for the analysis.
   *
   * This should include all thresholds as well as the checksum for
   * configuration files.
   *
   * @return
   */
  Map<String, String> getSettings();

  void addSetting(String key, String value);

//<editor-fold desc="feature_builder" defaultstate="collapsed">
  // we need the ability to get the list of feature extractors so that we can transfer to the FeatureBuilder
  List<FeatureExtractor> getFeatureExtractors();
  
  List<FeatureExtractor> getFallbackExtractors();

  List<AnalysisPreprocessor> getPreprocessors();

  AnalysisResult allocateResult();

  void prepare(Record record) throws AnalysisException;

  // we need the ability to run a specific set of feature analysis from the list of available
  void extractFeatures(AnalysisResult results, Record record, Collection<FeatureExtractor> extractors)
          throws AnalysisException;

  /**
   * Get the preprocessor for vehicles.
   *
   * This is used as part of the reinjection of extracted sources.
   *
   * @return
   */
  default VehicleClassifier getVehicleClassifier()
  {
    for (AnalysisPreprocessor pre : this.getPreprocessors())
    {
      if (pre instanceof VehicleClassifier)
      {
        return (VehicleClassifier) pre;
      }
    }
    return null;
  }
//</editor-fold>  

  static Analysis load(Path path) throws ReaderException, IOException, IOException, IOException
  {
    DocumentReader<Analysis> dr = DocumentReader.create(Analysis.class);
    dr.setProperty(DocumentReader.COMPUTE_MD5SUM, true);
    Analysis analysis = dr.loadFile(path);
    Map<String, String> checksums = (Map<String, String>) dr.getProperty(DocumentReader.RESULT_MD5SUM);
    for (Map.Entry<String, String> entry : checksums.entrySet())
    {
      analysis.addSetting(entry.getKey(), entry.getValue());
    }

    return analysis;
  }
  
  Class getFaultClass();
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