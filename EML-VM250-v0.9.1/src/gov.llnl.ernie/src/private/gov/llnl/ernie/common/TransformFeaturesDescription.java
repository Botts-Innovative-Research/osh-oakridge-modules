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

/**
 * FeatureGroupDescription for {@link TransformFeatures}.
 *
 * @author guensche1
 */
public class TransformFeaturesDescription extends FeaturesDescriptionImpl<TransformFeatures>
{
  private static final long serialVersionUID = gov.llnl.utility.UUIDUtilities.createLong("TransformFeaturesDescription-v1");
  int nPcaFeatures;
  int nFftFeatures;

  public TransformFeaturesDescription(int pc, int fft)
  {
    super("Transforms", new ArrayList());
    this.nPcaFeatures = pc;
    this.nFftFeatures = fft;

    FeatureDescriptionBuilder builder = newBuilder(this.getInternal());
    builder.defineArrayOrdinal("PCA.PC", (TransformFeatures tf) -> tf.pcaFeatures, pc, 1);
    builder.defineArrayOrdinal( "FFT.FFT", (TransformFeatures tf) -> tf.fftFeatures, fft, 1);
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