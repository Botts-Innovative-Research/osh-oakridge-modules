/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.ernie.common;

import gov.llnl.ernie.impl.FeaturesDescriptionImpl;
import java.util.ArrayList;
import java.util.List;

/**
 * Description of the Extent feature.
 *
 * This maps all of the variable into feature names.
 *
 * @author nelson85
 */
public class ExtentFeaturesDescription extends FeaturesDescriptionImpl<ExtentFeatures>
{
  private static final long serialVersionUID = gov.llnl.utility.UUIDUtilities.createLong("ExtentFeaturesDescription-v2");
  final static List<FeatureDescription> FEATURES = new ArrayList<>();

  static
  {
    FeatureDescriptionBuilder builder = newBuilder(FEATURES);
    builder.add("XLocation", Double.class,
            (ExtentFeatures tf) -> tf.peakLocationX,
            (ExtentFeatures tf, Double v) -> tf.peakLocationX = v);
    builder.add("PeakIntensity", Double.class,
            (ExtentFeatures tf) -> tf.maxIntensity,
            (ExtentFeatures tf, Double v) -> tf.maxIntensity = v);
    
    builder.add("FWQM.In", Double.class,
            (ExtentFeatures tf) -> tf.innerWidths[0],
            (ExtentFeatures tf, Double v) -> tf.innerWidths[0] = v);
    builder.add("FWHM.In", Double.class,
            (ExtentFeatures tf) -> tf.innerWidths[1],
            (ExtentFeatures tf, Double v) -> tf.innerWidths[1] = v);
    builder.add("FW3QM.In", Double.class,
            (ExtentFeatures tf) -> tf.innerWidths[2],
            (ExtentFeatures tf, Double v) -> tf.innerWidths[2] = v);

    builder.add("FWQM.Out", Double.class,
            (ExtentFeatures tf) -> tf.outerWidths[0],
            (ExtentFeatures tf, Double v) -> tf.outerWidths[0] = v);
    builder.add("FWHM.Out", Double.class,
            (ExtentFeatures tf) -> tf.outerWidths[1],
            (ExtentFeatures tf, Double v) -> tf.outerWidths[1] = v);
    builder.add("FW3QM.Out", Double.class,
            (ExtentFeatures tf) -> tf.outerWidths[2],
            (ExtentFeatures tf, Double v) -> tf.outerWidths[2] = v);

    builder.add("FWQM.Ratio", Double.class,
            (ExtentFeatures tf) -> tf.ratios[0],
            (ExtentFeatures tf, Double v) -> tf.ratios[0] = v);
    builder.add("FWHM.Ratio", Double.class,
            (ExtentFeatures tf) -> tf.ratios[1],
            (ExtentFeatures tf, Double v) -> tf.ratios[1] = v);
    builder.add("FW3QM.Ratio", Double.class,
            (ExtentFeatures tf) -> tf.ratios[2],
            (ExtentFeatures tf, Double v) -> tf.ratios[2] = v);
    
    builder.add("FWTopBottom.Ratio", Double.class,
            (ExtentFeatures tf) -> tf.topBottom_ratio,
            (ExtentFeatures tf, Double v) -> tf.topBottom_ratio = v);
            
    builder.add("PeakVsFWHM", Double.class,
            (ExtentFeatures tf) -> tf.peakVsFWHM,
            (ExtentFeatures tf, Double v) -> tf.peakVsFWHM = v);
    builder.add("IntensityFWHMRatio", Double.class,
            (ExtentFeatures tf) -> tf.intensityFWHMRatio,
            (ExtentFeatures tf, Double v) -> tf.intensityFWHMRatio = v);
    builder.add("TriangleIntensityRatio", Double.class,
            (ExtentFeatures tf) -> tf.triangleIntensityRatio,
            (ExtentFeatures tf, Double v) -> tf.triangleIntensityRatio = v);

  }

  public ExtentFeaturesDescription()
  {
    super("Extent", FEATURES);
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