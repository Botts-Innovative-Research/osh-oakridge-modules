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

import gov.llnl.ernie.analysis.Features;
import gov.llnl.ernie.analysis.FeaturesDescription;

/**
 * TODO REFACTOR add class description
 *
 * @author guensche1
 */
public class TransformFeatures implements Features
{
  private static final long serialVersionUID = gov.llnl.utility.UUIDUtilities.createLong("TransformFeatures-v1");
  private final TransformFeaturesDescription description;
  final double[] pcaFeatures;
  final double[] fftFeatures;

  public TransformFeatures(TransformFeaturesDescription fgd)
  {
    this.description = fgd;
    pcaFeatures = new double[fgd.nPcaFeatures];
    fftFeatures = new double[fgd.nFftFeatures];
  }

  /**
   * @param pcaFeatures should have length = 12
   * @param fftFeatures should have length = 5
   */
  public TransformFeatures(TransformFeaturesDescription fgd, double[] pcaFeatures, double[] fftFeatures)
  {
    this(fgd);
    if (pcaFeatures != null)
    {
      System.arraycopy(pcaFeatures, 0, this.pcaFeatures, 0, pcaFeatures.length);
    }
    if (fftFeatures != null)
    {
      System.arraycopy(fftFeatures, 0, this.fftFeatures, 0, fftFeatures.length);
    }
  }

  /**
   * @return the pcaFeatures
   */
  public double[] getPcaFeatures()
  {
    return pcaFeatures;
  }

  /**
   * @return the fftFeatures
   */
  public double[] getFftFeatures()
  {
    return fftFeatures;
  }

  public void setPcaFeatures(double[] pcaFeatures)
  {
    System.arraycopy(pcaFeatures, 0, this.pcaFeatures, 0, pcaFeatures.length);
  }

  public void setFftFeatures(double[] fftFeatures)
  {
    System.arraycopy(fftFeatures, 0, this.fftFeatures, 0, fftFeatures.length);
  }

  @Override
  public FeaturesDescription getDescription()
  {
    return description;
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