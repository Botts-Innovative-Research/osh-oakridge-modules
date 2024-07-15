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
import gov.llnl.utility.UUIDUtilities;
import java.util.ArrayList;
import java.util.List;

/**
 * Description for Joint Features.
 *
 * Currently this is just a stub. Joint features contains two set of feature
 * vectors, and we don't use the top level for anything.
 *
 * @author nelson85
 */
public class JointFeaturesDescription extends FeaturesDescriptionImpl<JointFeatures>
{
  private static final long serialVersionUID
          = UUIDUtilities.createLong("JointFeaturesDescription-v1");
  final JointSourceDescription sourceDescription;

  public JointFeaturesDescription(boolean fallback, String[] clusterLabels)
  {
    super(fallback ? "Fallback.Joint" : "Joint", new ArrayList<>());
    List<FeatureDescription> features = this.getInternal();
    this.sourceDescription = new JointSourceDescription(fallback, clusterLabels);
    FeatureDescriptionBuilder builder = newBuilder(features);
    builder.includeFeatures("Joint1.", sourceDescription, (JointFeatures jf) -> jf.sources[0]);
    builder.includeFeatures("Joint2.", sourceDescription, (JointFeatures jf) -> jf.sources[1]);
    this.buildIndex();
  }

  public JointSourceDescription getJointSourceDescription()
  {
    return sourceDescription;
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