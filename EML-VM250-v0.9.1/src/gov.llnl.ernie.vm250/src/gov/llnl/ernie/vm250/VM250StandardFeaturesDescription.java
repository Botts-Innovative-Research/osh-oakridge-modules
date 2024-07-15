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
import java.util.ArrayList;

/**
 *
 * @author guensche1
 */
public class VM250StandardFeaturesDescription extends FeaturesDescriptionImpl<VM250StandardFeatures>
{
  private static final long serialVersionUID = gov.llnl.utility.UUIDUtilities.createLong("VM250StandardFeaturesDescription-v1");
  private final int panels;
  private final int ratios;

  public VM250StandardFeaturesDescription(int panels)
  {
    super("Standard", new ArrayList<>());
    FeatureDescriptionBuilder builder = newBuilder(this.getInternal());

    this.panels = panels;
    this.ratios = 0;

    builder.defineArrayOrdinal("Gross.Panel", (VM250StandardFeatures results) -> results.grossCountMetric, panels, 1);
    builder.add("Gross.All", Double.class,
            (VM250StandardFeatures results) -> results.grossCountMetric[panels],
            (VM250StandardFeatures results, Double v) -> results.grossCountMetric[panels] = v);

    this.buildIndex();
  }

  /**
   * @return the panels
   */
  public int getPanels()
  {
    return panels;
  }

  /**
   * @return the ratios
   */
  public int getRatios()
  {
    return ratios;
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