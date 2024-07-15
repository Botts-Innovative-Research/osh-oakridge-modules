/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.ernie.spectral;

import gov.llnl.ernie.impl.FeaturesDescriptionImpl;
import gov.llnl.math.euclidean.Vector3;
import java.util.ArrayList;

/**
 *
 * @author nelson85
 */
public class JointSourceDescription extends FeaturesDescriptionImpl<JointSource>
{
  private static final long serialVersionUID = gov.llnl.utility.UUIDUtilities.createLong("JointSourceDescription-v1");

  public JointSourceDescription(boolean fallback, String[] clusterLabels)
  {
    super(getName(fallback), new ArrayList<>());
    FeatureDescriptionBuilder builder = newBuilder(this.getInternal());
    if (!fallback)
      builder.add("PointX", Double.class,
              (JointSource js) -> js.position.getX(),
              (JointSource js, Double v) -> js.position = modifyX(js.position, v));
    builder.add("PointY", Double.class,
            (JointSource js) -> js.position.getY(),
            (JointSource js, Double v) -> js.position = modifyY(js.position, v));
    builder.add("PointZ", Double.class,
            (JointSource js) -> js.position.getZ(),
            (JointSource js, Double v) -> js.position = modifyZ(js.position, v));

    if (!fallback)
      builder.add("Length", Double.class,
              (JointSource js) -> (js.x2 - js.x1),
              (JointSource js, Double v) ->
      {
        js.x1 = js.position.getX() - v / 2;
        js.x2 = js.position.getX() + v / 2;
      });

    builder.add("Intensity", Double.class,
            (JointSource js) -> js.intensity,
            (JointSource js, Double v) -> js.intensity = v);
    builder.add("BkgChisqr", Double.class,
            (JointSource js) -> js.bkgHypothesis,
            (JointSource js, Double v) -> js.bkgHypothesis = v);

    builder.defineArrayOrdinal("PCA", (JointSource x) -> x.pcaEnergyFeatures, 4);
    builder.defineArrayOrdinal("Ratio", (JointSource x) -> x.ratioEnergyFeatures, 13);
    builder.defineArrayLabeled("", (JointSource x) ->
            {
              double[] arr = x.correlationEnergyFeatures;
              if (arr == null)
              {
                arr = new double[clusterLabels.length];
              }
              return arr;
            },
            clusterLabels);
    this.buildIndex();
  }
  
  private static String getName(boolean fallback)
  {
    if (fallback)
      return "Fallback.Joint";
    else
      return "Joint";
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

  private static Vector3 modifyZ(Vector3 position, Double v)
  {
    if (position == null)
      return Vector3.of(0, 0, v);
    return Vector3.of(position.getX(), position.getY(), v);
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