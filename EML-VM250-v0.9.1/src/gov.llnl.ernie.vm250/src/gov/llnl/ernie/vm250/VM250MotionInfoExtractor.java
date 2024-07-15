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

import gov.llnl.ernie.analysis.AnalysisException;
import gov.llnl.ernie.ErniePackage;
import gov.llnl.ernie.analysis.FeatureExtractor;
import gov.llnl.ernie.data.Record;
import gov.llnl.ernie.analysis.Features;
import gov.llnl.ernie.analysis.FeaturesDescription;
import gov.llnl.ernie.common.MotionInfo;
import gov.llnl.ernie.common.MotionInfoDescription;
import gov.llnl.utility.xml.bind.Reader;

/**
 *
 * @author mattoon1
 */
@Reader.Declaration(pkg = ErnieVM250Package.class, name = "motionInfoExtractor")
public class VM250MotionInfoExtractor implements FeatureExtractor
{
  static final MotionInfoDescription DESCRIPTION = new MotionInfoDescription();

  @Override
  public void initialize()
  {
    // NOT USED
  }

  @Override
  public FeaturesDescription getDescription()
  {
    return VM250MotionInfoExtractor.DESCRIPTION;
  }

  @Override
  public MotionInfo compute(Record record) throws AnalysisException
  {
    MotionInfo result = new MotionInfo();
    
    result.setInitialVelocity(record.getVehicleMotion().getVelocityInitial());

    return result;
  }

  @Override
  public Features newFeatures()
  {
    return new MotionInfo();
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