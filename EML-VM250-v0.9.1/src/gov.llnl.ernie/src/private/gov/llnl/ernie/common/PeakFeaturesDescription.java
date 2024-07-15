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
public class PeakFeaturesDescription extends FeaturesDescriptionImpl<PeakFeatures>
{
  private static final long serialVersionUID = gov.llnl.utility.UUIDUtilities.createLong("PeakFeaturesDescription-v1");

  public PeakFeaturesDescription(FeaturesDescription ef)
  {
    super("Features.Peak.", new ArrayList<>());

    FeatureDescriptionBuilder builder = newBuilder(this.getInternal());
    builder.defineArrayLabeled("", (PeakFeatures f) -> f.getPeakStatistics(),
            "Intensity", "MeanY", "MeanZ");
    builder.add("Significance", Double.class, (PeakFeatures f) -> f.peakSignificance,
            (f, d) -> f.peakSignificance = d);

    // Import energy features
    if (ef != null)
    {
      builder.includeFeatures("", ef, (PeakFeatures f) -> f.getEnergyFeatures());
    }

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