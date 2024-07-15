/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.ernie.vm250.data;

import gov.llnl.ernie.Analysis.SourceType;
import gov.llnl.ernie.common.ExtentFeatures;
import gov.llnl.ernie.common.StatisticalFeatures;
import gov.llnl.ernie.impl.AnalysisSourceImpl;
import gov.llnl.math.DoubleArray;

/**
 *
 * @author nelson85
 */
public class VM250AnalysisSource extends AnalysisSourceImpl
{

  public VM250AnalysisSource(String[] outputLabels, double[] LogLikelihoods,
          StatisticalFeatures statistical, ExtentFeatures extent)
  {
    // Use CMU info to predict the class
    setSourceProbabilities(outputLabels, LogLikelihoods);
    // estimate source position
    double fwhm = extent.innerWidths[1];
    this.x1 = extent.peakLocationX - fwhm/2;
    this.x2 = extent.peakLocationX + fwhm/2;
    this.y = statistical.peakPositionStats[1];
    this.z = statistical.peakPositionStats[2];
  }
  
  /**
   * Alternate constructor for use in AnalysisFromCsv
   * @param outputLabels
   * @param LogLikelihoods
   * @param x1
   * @param x2
   * @param y
   * @param z 
   */
  public VM250AnalysisSource(String[] outputLabels, double[] LogLikelihoods,
          double x1, double x2, double y, double z)
  {
    // Use CMU info to predict the class
    setSourceProbabilities(outputLabels, LogLikelihoods);
    this.x1 = x1;
    this.x2 = x2;
    this.y = y;
    this.z = z;
  }

  protected final void setSourceProbabilities(String[] outputLabels, double[] LogLikelihoods)
  {
    // Renormalize probabilities to one
    double sum = 0;
    for (int i = 0; i < LogLikelihoods.length; i++)
    {
      sum += Math.exp(LogLikelihoods[i]);
    }
    if (outputLabels.length == 6)
    {
      for (int i = 0; i < outputLabels.length; ++i)
      {
        double value = Math.exp(LogLikelihoods[i]) / sum;
        if ("NonEmitting".equals(outputLabels[i]))
        {
          probabilityNonEmitting = value;
        }
        else if ("NORM".equals(outputLabels[i]))
        {
          probabilityNORM = value;
        }
        else if ("Medical".equals(outputLabels[i]))
        {
          probabilityMedical = value;
        }
        else if ("Industrial".equals(outputLabels[i]))
        {
          probabilityIndustrial = value;
        }
        else if ("Fissile".equals(outputLabels[i]))
        {
          probabilityFissile = value;
        }
        else if ("Contamination".equals(outputLabels[i]))
        {
          probabilityContamination = value;
        }
        else
        {
          throw new RuntimeException("Unexpected label returned by classifier");
        }
      }
      probabilityRelease = probabilityNORM + probabilityNonEmitting;
      probabilityInvestigate = (probabilityFissile + probabilityContamination + probabilityMedical + probabilityIndustrial);
    }
    else if (outputLabels.length == 2)
    {
      for (int i = 0; i < outputLabels.length; ++i)
      {
        double value = Math.exp(LogLikelihoods[i]) / sum;
        if ("Release".equals(outputLabels[i]))
        {
          probabilityRelease = value;
        }
        else if ("Investigate".equals(outputLabels[i]))
        {
          probabilityInvestigate = value;
        }
        else
        {
          throw new RuntimeException("Unexpected label returned by classifier");
        }
      }
    }
    else
    {
      throw new RuntimeException(
              String.format("Expected 2- or 6-class classifier, received %d classes",
                      outputLabels.length));
    }
  }

  public SourceType getSourceType()
  {
    // If it is release use the larger of NORM and nonemitting
    if (probabilityRelease > probabilityInvestigate)
    {
      return (probabilityNonEmitting > probabilityNORM) ? SourceType.NONEMITTING : SourceType.NORM;
    }
    // If it is investigate
    double[] investigate = new double[]
    {
      probabilityMedical, probabilityIndustrial, probabilityFissile, probabilityContamination
    };
    switch (DoubleArray.findIndexOfMaximum(investigate))
    {
      case 0:
        return SourceType.MEDICAL;
      case 1:
        return SourceType.INDUSTRIAL;
      case 2:
        return SourceType.FISSILE;
      case 3:
        return SourceType.CONTAMINATION;
      default:
        throw new RuntimeException();
    }
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