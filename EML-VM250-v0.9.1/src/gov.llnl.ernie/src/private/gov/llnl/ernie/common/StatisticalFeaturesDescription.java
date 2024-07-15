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
import gov.llnl.ernie.analysis.FeaturesDescription;

/**
 *
 * @author nelson85
 */
public class StatisticalFeaturesDescription extends FeaturesDescriptionImpl<StatisticalFeatures>
{
  private static final long serialVersionUID = gov.llnl.utility.UUIDUtilities.createLong("StatisticalFeaturesDescription-v1");

  public StatisticalFeaturesDescription(FeaturesDescription ef)
  {
    super("Statistics", new ArrayList<>());
    FeatureDescriptionBuilder builder = newBuilder(this.getInternal());

    String[] intensityLabels =
    {
      "IMean", "IStdDev", "ISkew", "IKurt", "IStdRatio"
    };
    String[] positionLabels =
    {
      "XMean", "XStdDev", "XSkew", "XKurt"
    };

    builder.defineArrayLabeled("All.", (StatisticalFeatures f) -> f.allIntensityStats,
            intensityLabels);
    builder.defineArrayLabeled("Peak.", (StatisticalFeatures f) -> f.peakIntensityStats,
            intensityLabels);
    builder.defineArrayLabeled("Peak.", (StatisticalFeatures f) -> f.peakPositionStats,
            positionLabels);

    builder.defineArrayLabeled("Front.", (StatisticalFeatures f) -> f.frontIntensityStats,
            intensityLabels);
    builder.defineArrayLabeled("Front.", (StatisticalFeatures f) -> f.frontPositionStats,
            positionLabels);

    builder.defineArrayLabeled("Rear.", (StatisticalFeatures f) -> f.rearIntensityStats,
            intensityLabels);
    builder.defineArrayLabeled("Rear.", (StatisticalFeatures f) -> f.rearPositionStats,
            positionLabels);

    builder.add("Split.Position", Double.class, (StatisticalFeatures f) -> f.splitDistance,
            (f, d) -> f.splitDistance = d);
    builder.add("Split.Intensity", Double.class, (StatisticalFeatures f) -> f.splitIntensity,
            (f, d) -> f.splitIntensity = d);
    builder.add("Split.DeltaWidth", Double.class, (StatisticalFeatures f) -> f.splitDeltaWidth,
            (f, d) -> f.splitDeltaWidth = d);

    if (ef != null)
    {
      builder.includeFeatures("Peak.", ef, (StatisticalFeatures f) -> f.allPeakFeatures);
      builder.includeFeatures("Front.", ef, (StatisticalFeatures f) -> f.frontPeakFeatures);
      builder.includeFeatures("Rear.", ef, (StatisticalFeatures f) -> f.rearPeakFeatures);
    }

    builder.add("SpreadX", Double.class,
            (StatisticalFeatures f) -> f.spreadX,
            (StatisticalFeatures f, Double v) -> f.spreadX = v);
    builder.add("Spread3D", Double.class,
            (StatisticalFeatures f) -> f.spread3d,
            (StatisticalFeatures f, Double v) -> f.spread3d = v);
    builder.add("SpreadI", Double.class,
            (StatisticalFeatures f) -> f.spreadI,
            (StatisticalFeatures f, Double v) -> f.spreadI = v);
    builder.add("SplitDip", Double.class,
            (StatisticalFeatures f) -> f.splitDip,
            (StatisticalFeatures f, Double v) -> f.splitDip = v);

    builder.add("IStdDevIMeanRatio", Double.class,
            (StatisticalFeatures f) -> f.IStdDevIMeanRatio,
            (StatisticalFeatures f, Double v) -> f.IStdDevIMeanRatio = v);

    this.buildIndex();
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