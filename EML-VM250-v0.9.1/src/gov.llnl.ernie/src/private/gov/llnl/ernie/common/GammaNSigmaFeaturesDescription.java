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
 *
 * @author nelson85
 */
public class GammaNSigmaFeaturesDescription extends FeaturesDescriptionImpl<GammaNSigmaFeatures>
{
  private static final long serialVersionUID = gov.llnl.utility.UUIDUtilities.createLong("GammaNSigmaFeaturesDescription-v1");
  final static List<FeatureDescription> FEATURES = new ArrayList<>();

  static
  {
    FeatureDescriptionBuilder builder = newBuilder(FEATURES);
    builder.add("Gross.All", Double.class,
            (GammaNSigmaFeatures tf) -> tf.maxAllPanels,
            (GammaNSigmaFeatures tf, Double v) -> tf.maxAllPanels = v);
    builder.add("Gross.Master", Double.class,
            (GammaNSigmaFeatures tf) -> tf.maxMaster,
            (GammaNSigmaFeatures tf, Double v) -> tf.maxMaster = v);
    builder.add("Gross.Slave", Double.class,
            (GammaNSigmaFeatures tf) -> tf.maxSlave,
            (GammaNSigmaFeatures tf, Double v) -> tf.maxSlave = v);
  }

  public GammaNSigmaFeaturesDescription()
  {
    super("NSigma", GammaNSigmaFeaturesDescription.FEATURES);
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