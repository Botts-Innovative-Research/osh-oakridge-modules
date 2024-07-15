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

import gov.llnl.ernie.analysis.FeaturesDescription;
import gov.llnl.ernie.impl.FeaturesDescriptionImpl;
import static gov.llnl.ernie.impl.FeaturesDescriptionImpl.newBuilder;
import java.util.ArrayList;

/**
 *
 * @author nelson85
 */
public class BackgroundFeaturesDescription extends FeaturesDescriptionImpl<BackgroundFeatures>
{
  private static final long serialVersionUID = gov.llnl.utility.UUIDUtilities.createLong("StatisticalFeaturesDescription-v1");

  public BackgroundFeaturesDescription(int panels)
  {
    super("Background", new ArrayList<>());
    FeaturesDescriptionImpl.FeatureDescriptionBuilder builder = newBuilder(this.getInternal());
    builder.defineArrayOrdinal("Panel", (BackgroundFeatures f) -> f.panel, panels, 1);
    builder.defineArrayOrdinal("Delta", (BackgroundFeatures f) -> f.delta, panels, 1);
    builder.add("PanelAvg", Double.class, (BackgroundFeatures f) -> f.panelAvg,
            (f, d) -> f.panelAvg = d);
    builder.add("DeltaAvg", Double.class, (BackgroundFeatures f) -> f.deltaAvg,
            (f, d) -> f.deltaAvg = d);
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