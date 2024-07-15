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
import java.util.ArrayList;
import java.util.stream.Stream;

/**
 *
 * @author nelson85
 */
public class EnergyFeaturesDescription extends FeaturesDescriptionImpl
{
  private static final long serialVersionUID
          = gov.llnl.utility.UUIDUtilities.createLong("FeaturesDescriptionImpl-v1");

  final String[] LABELS;
  final int nPcaFeatures;
  final int nRatioFeatures;
  final int nHypothesisFeatures;

  public EnergyFeaturesDescription(int p, int r, String[] labels)
  {
    super("Energy.", new ArrayList<>());

    this.nPcaFeatures = p;
    this.nRatioFeatures = r;
    this.nHypothesisFeatures = labels.length;
    this.LABELS = Stream.of(labels).map(w->w.concat(".chi")).toArray(String[]::new);

    FeatureDescriptionBuilder builder = newBuilder(this.getInternal());
    builder.defineArrayOrdinal("PCA", (EnergyFeatures e) -> e.getPcaFeatures(), nPcaFeatures);
    builder.defineArrayOrdinal("Ratio", (EnergyFeatures e) -> e.getRatioFeatures(), nRatioFeatures);
    builder.defineArrayLabeled("", (EnergyFeatures e) -> e.getHypothesisFeatures(), LABELS);
    builder.add("k", Double.class,
            (EnergyFeatures e) -> e.getK(),
            (EnergyFeatures e, Double d) -> e.k = d);
    builder.add("Background.chi", Double.class,
            (EnergyFeatures e) -> e.getBackground(),
            (EnergyFeatures e, Double d) -> e.background = d);

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