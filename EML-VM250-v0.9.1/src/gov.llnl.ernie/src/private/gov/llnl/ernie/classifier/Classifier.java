/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.ernie.classifier;

import edu.cmu.cs.auton.classifiers.RandomForestClassifier;
import edu.cmu.cs.auton.classifiers.InOut;
import edu.cmu.cs.auton.classifiers.Output;
import gov.llnl.ernie.ErniePackage;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import gov.llnl.ernie.analysis.Features;
import gov.llnl.ernie.analysis.FeaturesDescription;
import java.util.logging.Level;

/**
 *
 * @author mattoon1
 */
public class Classifier
{

  private String featureNames[];
  RandomForestClassifier CMU_Analyzer;
  String output_labels[];
  boolean verbose;

  public String getModelName()
  {
    return CMU_Analyzer.getModelName();
  }

  public static class Setter
  {
    public void set(double value)
    {
    }
  }

  /**
   * Create new Classifier using specified machine learning model, metrics, and
   * verbosity
   *
   * @param machine_learning_model
   * @param machine_learning_metrics
   * @param Verbose
   */
  public Classifier(String machine_learning_model, String machine_learning_metrics, boolean Verbose)
  {
    initialize(machine_learning_model, machine_learning_metrics, Verbose);
  }

  /**
   * private helper method for creating new Classifiers
   */
  private void initialize(String machine_learning_model, String machine_learning_metrics, boolean Verbose)
  {
    this.verbose = Verbose;
    if (verbose)
    {
      System.out.println("Load classifier from " + machine_learning_model + " and " + machine_learning_metrics);
    }
    try
    {
      CMU_Analyzer = InOut.loadModelFile(machine_learning_model);
      CMU_Analyzer.setTable(InOut.loadMetricsFile(machine_learning_metrics));
    }
    catch (IOException ex)
    {
      throw new RuntimeException("Unable to load cmu machine learning model", ex);
    }
    if (verbose)
    {
      System.out.println("Complete");
    }
    String[] _featureNames = CMU_Analyzer.getFeatureNames();
    output_labels = CMU_Analyzer.getOutputValueNames();
    int length = _featureNames.length;
    if (_featureNames[length - 1].equals("LABEL"))
    {
      featureNames = new String[length - 1];
      System.arraycopy(_featureNames, 0, featureNames, 0, length - 1);
    }
    else
    {
      featureNames = _featureNames;
    }
  }

  /**
   * @return output labels from CMU classifier
   */
  public String[] getOutputLabels()
  {
    return CMU_Analyzer.getOutputValueNames();
  }

  /**
   *
   * @return double[] inputFeatures
   */
  public double[] generateFeaturesArray(Collection<Features> features)
  {

    Map<String, Double> amap = new HashMap<>();
    amap.put("SegmentInfo.FoldID", -1.0);    // dummy value for fold ID
    for (Features group : features)
    {
      FeaturesDescription description = group.getDescription();
      for (FeaturesDescription.FeatureDescription feature : description.getFeatureDescriptions())
      {
        String name = feature.getName();
        String fname = String.format("Features.%s.%s", description.getPrefix(), name);
        amap.put(fname,
                (double) group.get(name, Double.class));
      }
    }

    // convert map values to double array in correct order:
    int nValues = getFeatureNames().length;
    double[] inputFeatures = new double[nValues];

    String[] names = getFeatureNames();
    for (int i = 0; i < nValues; i++)
    {
      Double value = amap.get(names[i]);
      if (value==null)
      {
        ErniePackage.LOGGER.log(Level.SEVERE, "missing feature "+names[i]);
        continue;
      }
      inputFeatures[i] = amap.get(names[i]);
    }

    if (verbose)
    {
      // for debugging:
      for (int i = 0; i < nValues; i++)
      {
        System.out.print(String.format("%s,", getFeatureNames()[i]));
      }
      System.out.println();
      for (int i = 0; i < nValues; i++)
      {
        System.out.print(String.format("%f,", inputFeatures[i]));
      }
      System.out.println();
      // end debug stuff
    }

    return inputFeatures;
  }

  public Output predict(Collection<Features> featureGroups)
  {
    return this.predict(this.generateFeaturesArray(featureGroups));
  }

  public Output predict(double[] features)
  {
    // call CMU predictor, store results internally
    Output CMU_result;
    CMU_result = CMU_Analyzer.classifyQuery(features);
    return CMU_result;
  }

  public boolean valid()
  {
    return this.CMU_Analyzer != null;
  }

  public void apply(Output CMU_result, HashMap<String, Setter> outputTranslation)
  {
    double logLikelihoods[] = CMU_result.getClassLogLikelihoods();
    for (int i = 0; i < output_labels.length; i++)
    {
      outputTranslation.get(output_labels[i]).set(logLikelihoods[i]);
    }

  }

  public double[] getCMU_metrics(Output CMUresult)
  {
    double _metrics[] = new double[7];
    _metrics[0] = CMUresult.getMeanEntropy();
    _metrics[1] = CMUresult.getDotProductSum();
    _metrics[2] = CMUresult.getInboundsScore();
    _metrics[3] = CMUresult.getConsistency();
    _metrics[4] = CMUresult.getAccuracy().getExpectedAccuracy();
    _metrics[5] = CMUresult.getAccuracy().getExpectedAccuracyUpper();
    _metrics[6] = CMUresult.getAccuracy().getExpectedAccuracyLower();
    return _metrics;
  }

  /**
   * @return the featureNames
   */
  public String[] getFeatureNames()
  {
    return featureNames;
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