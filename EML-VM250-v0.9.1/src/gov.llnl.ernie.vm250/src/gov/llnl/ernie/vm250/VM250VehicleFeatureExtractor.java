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

import gov.llnl.ernie.analysis.FeatureExtractor;
import gov.llnl.ernie.common.VehicleFeatures;
import gov.llnl.ernie.data.Record;
import gov.llnl.ernie.analysis.Features;
import gov.llnl.ernie.analysis.FeaturesDescription;
import gov.llnl.utility.xml.bind.Reader;

/**
 * Extracts Vehicle features from a {@link VM250Record} Record
 *
 * @author guensche1
 */
@Reader.Declaration(pkg = ErnieVM250Package.class,
        name = "vehicleFeatureExtractor",
        referenceable = true)
public class VM250VehicleFeatureExtractor implements FeatureExtractor
{
  final static VM250VehicleFeaturesDescription DESCRIPTION = new VM250VehicleFeaturesDescription();

  @Override
  public void initialize()
  {
    // NOT USED
  }

  @Override
  public FeaturesDescription getDescription()
  {
    return DESCRIPTION;
  }

  @Override
  public Features newFeatures()
  {
    return new VehicleFeatures(DESCRIPTION);
  }

  @Override
  public Features compute(Record record)
  {
    VehicleFeatures features = new VehicleFeatures(DESCRIPTION);
    features.setVehicleLength(record.getVehicleMotion().getVehicleLength());
    features.setLaneWidth(record.getLane().getLaneWidth());
    return features;
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