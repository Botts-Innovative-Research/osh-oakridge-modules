/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.ernie.manipulator;

import gov.llnl.ernie.impl.FeaturesDescriptionImpl;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author guensche1
 */
public class ManipulationDescription extends FeaturesDescriptionImpl<Manipulation>
{
  private static final long serialVersionUID = 
          gov.llnl.utility.UUIDUtilities.createLong("ManipulationDescription-v1");
  private static final List<FeatureDescription> FEATURE_LIST = new ArrayList<>();

  static
  {
    FeatureDescriptionBuilder builder = newBuilder(FEATURE_LIST);
    builder.add("velocityChanged", Boolean.class,
            (Manipulation m) -> m.isAlterVelocity(),
            (Manipulation m, Boolean val) -> m.alterVelocity = val);
    builder.add("PointSourceInjected", Boolean.class,
            (Manipulation m) -> m.isInjectCompact(),
            (Manipulation m, Boolean val) -> m.injectCompact = val);
    builder.add("DistSourceInjected", Boolean.class,
            (Manipulation m) -> m.isInjectDistributed(),
            (Manipulation m, Boolean val) -> m.injectDistributed = val);
    builder.add("StartVelocity", Double.class,
            (Manipulation m) -> m.getSpeedStart(),
            (Manipulation m, Double val) -> m.speedStart = val);
    builder.add("EndVelocity", Double.class,
            (Manipulation m) -> m.getSpeedEnd(),
            (Manipulation m, Double val) -> m.speedEnd = val);
    builder.add("PointIntensity", Double.class,
            (Manipulation m) -> m.getCompactIntensity(),
            (Manipulation m, Double val) -> m.compactIntensity = val);
    builder.add("PointX", Double.class,
            (Manipulation m) -> m.getCompactPx(),
            (Manipulation m, Double val) -> m.compactPx = val);
    builder.add("PointY", Double.class,
            (Manipulation m) -> m.getCompactPy(),
            (Manipulation m, Double val) -> m.compactPy = val);
    builder.add("PointZ", Double.class,
            (Manipulation m) -> m.getCompactPz(),
            (Manipulation m, Double val) -> m.compactPz = val);
    builder.add("PointSNum", String.class,
            (Manipulation m) -> m.getCompactSourceId(),
            (Manipulation m, String val) -> m.compactSourceId = val);
    builder.add("DistIntensity1", Double.class,
            (Manipulation m) -> m.getDistributedIntensity1(),
            (Manipulation m, Double val) -> m.distributedIntensity1 = val);
    builder.add("DistIntensity2", Double.class,
            (Manipulation m) -> m.getDistributedIntensity2(),
            (Manipulation m, Double val) -> m.distributedIntensity2 = val);
    builder.add("DistX1", Double.class,
            (Manipulation m) -> m.getDistributedPx1(),
            (Manipulation m, Double val) -> m.distributedPx1 = val);
    builder.add("DistX2", Double.class,
            (Manipulation m) -> m.getDistributedPx2(),
            (Manipulation m, Double val) -> m.distributedPx2 = val);
    builder.add("DistY", Double.class,
            (Manipulation m) -> m.getDistributedPy(),
            (Manipulation m, Double val) -> m.distributedPy = val);
    builder.add("DistZ", Double.class,
            (Manipulation m) -> m.getDistributedPz(),
            (Manipulation m, Double val) -> m.distributedPz = val);
    builder.add("DistSNum", String.class,
            (Manipulation m) -> m.getDistributedSourceId(),
            (Manipulation m, String val) -> m.distributedSourceId = val);
    builder.add("CargoModel", Integer.class,
            (Manipulation m) -> m.getCargoModelId(),
            (Manipulation m, Integer val) -> m.cargoModelId = val);
    builder.add("SourceType", Integer.class,
            (Manipulation m) -> m.getSourceType(),
            (Manipulation m, Integer val) -> m.sourceType = val);
    builder.add("GammaLeakage", Double.class,
            (Manipulation m) -> m.getGammaLeakage(),
            (Manipulation m, Double val) -> m.gammaLeakage = val);
    builder.add("NeutronLeakage", Double.class,
            (Manipulation m) -> m.getNeutronLeakage(),
            (Manipulation m, Double val) -> m.neutronLeakage = val);
  }

  /**
   * Expecting prefix to be either "Manipulation." or "Manipulation2."
   *
   * @param prefix
   */
  public ManipulationDescription(String prefix)
  {
    super(prefix, FEATURE_LIST);
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