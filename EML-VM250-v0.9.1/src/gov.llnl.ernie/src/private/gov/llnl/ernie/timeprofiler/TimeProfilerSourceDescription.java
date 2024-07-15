/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.ernie.timeprofiler;

import gov.llnl.ernie.impl.FeaturesDescriptionImpl;
import gov.llnl.math.euclidean.Vector3;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author chandrasekar1
 */
public class TimeProfilerSourceDescription extends FeaturesDescriptionImpl<TimeProfilerSource>
{
  private static final long serialVersionUID = gov.llnl.utility.UUIDUtilities.createLong("JointSourceDescription-v1");
  static List<FeatureDescription> FEATURES = new ArrayList<>();

  static
  {
    FeatureDescriptionBuilder builder = newBuilder(FEATURES);
    builder.add("PointX", Double.class,
            (TimeProfilerSource js) -> js.position.getX(),
            (TimeProfilerSource js, Double v) -> js.position = modifyX(js.position, v));
    builder.add("PointY", Double.class,
            (TimeProfilerSource js) -> js.position.getY(),
            (TimeProfilerSource js, Double v) -> js.position = modifyY(js.position, v));

    builder.add("Length", Double.class,
            (TimeProfilerSource js) -> (js.x2 - js.x1),
            (TimeProfilerSource js, Double v) ->
    {
      js.x1 = js.position.getX() - v / 2;
      js.x2 = js.position.getX() + v / 2;
    });

    builder.add("Intensity", Double.class,
            (TimeProfilerSource js) -> js.intensity,
            (TimeProfilerSource js, Double v) -> js.intensity = v);

//    builder.add("BkgChisqr", Double.class,
//            (JointSource js) -> js.bkgHypothesis,
//            (JointSource js, Double v) -> js.bkgHypothesis = v);
//    builder.defineArrayOrdinal("PCA", (JointSource x) -> x.pcaEnergyFeatures, 4);
//    builder.defineArrayOrdinal("Ratio", (JointSource x) -> x.ratioEnergyFeatures, 13);
//    builder.defineArrayLabeled("", (JointSource x) -> x.correlationEnergyFeatures, EnergyFeaturesDescription.LABELS);
  }

  private static Vector3 modifyX(Vector3 position, Double v)
  {
    if (position == null)
      return Vector3.of(v, 0, 0);
    return Vector3.of(v, position.getY(), position.getZ());
  }

  private static Vector3 modifyY(Vector3 position, Double v)
  {
    if (position == null)
      return Vector3.of(0, v, 0);
    return Vector3.of(position.getX(), v, position.getZ());
  }

  public TimeProfilerSourceDescription()
  {
    super("TimeProfile", FEATURES);
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