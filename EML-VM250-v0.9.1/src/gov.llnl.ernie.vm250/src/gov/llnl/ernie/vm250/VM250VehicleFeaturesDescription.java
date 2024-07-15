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

import gov.llnl.ernie.impl.FeaturesDescriptionImpl;
import gov.llnl.ernie.common.VehicleFeatures;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author nelson85
 */
public class VM250VehicleFeaturesDescription extends FeaturesDescriptionImpl<VehicleFeatures>
{
  private static final long serialVersionUID = gov.llnl.utility.UUIDUtilities.createLong("VM250VehicleFeaturesDescription-v1");
  final static List<FeatureDescription> FEATURES = new ArrayList<>();

  static
  {
    FeatureDescriptionBuilder builder = newBuilder(FEATURES);
    builder.add("Length", Double.class,
            VehicleFeatures::getVehicleLength,
            VehicleFeatures::setVehicleLength);
    builder.add("LaneWidth", Double.class,
            VehicleFeatures::getLaneWidth,
            VehicleFeatures::setLaneWidth);
  }

  public VM250VehicleFeaturesDescription()
  {
    super("Vehicle", FEATURES);
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