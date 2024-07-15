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
 *
 * @author nelson85
 */
public class ExtentFeatures implements Features
{
  private static final long serialVersionUID = gov.llnl.utility.UUIDUtilities.createLong("ExtentFeatures-v2");
  public double peakLocationX;
  public double maxIntensity;

  // full width at 1/4, 1/2 and 3/4 max, computed by starting at the peak and working out:
  public double[] innerWidths;
  // same widths, computed starting at beginning / end of scan and working in:
  public double[] outerWidths;
  // ratios in/out for each FW*M:
  public double[] ratios;
  // FW3QM.In / FW1QM.Out:
  public double topBottom_ratio;
  // location of max relative to FWHM_in:
  public double peakVsFWHM;
  // intensity / sqrt(FWHM**2 + 0.1):
  public double intensityFWHMRatio;
  // triangle intensity ratio tries to differentiate more peaked sources from NORM
  public double triangleIntensityRatio;


  // auditing:
  public double[] smoothed;
  public int maxidx;
  public int[] innerIndices, outerIndices;

  public ExtentFeatures()
  {
    innerWidths = new double[3];
    outerWidths = new double[3];
    ratios = new double[3];
  }

  @Override
  public FeaturesDescription getDescription()
  {
    return ExtentFeatureExtractor.DESCRIPTION;
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