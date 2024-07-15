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

import gov.llnl.ernie.analysis.AnalysisException;
import gov.llnl.ernie.ErniePackage;
import gov.llnl.ernie.analysis.FeatureExtractor;
import gov.llnl.math.DoubleArray;
import gov.llnl.math.IntegerArray;
import java.time.Instant;
import java.util.ArrayList;
import gov.llnl.ernie.data.Record;
import gov.llnl.ernie.data.SensorBackground;
import gov.llnl.ernie.data.SensorMeasurement;
import gov.llnl.ernie.analysis.Features;
import gov.llnl.ernie.analysis.FeaturesDescription;
import gov.llnl.ernie.data.SensorPosition;
import gov.llnl.math.DoubleUtilities;
import gov.llnl.utility.xml.bind.Reader;

/**
 *
 * @author mattoon1
 */
@Reader.Declaration(pkg = ErniePackage.class, name = "extentFeatureExtractor")
public class ExtentFeatureExtractor implements FeatureExtractor
{
  static final ExtentFeaturesDescription DESCRIPTION = new ExtentFeaturesDescription();

  @Override
  public void initialize()
  {
    // NOT USED
  }

  @Override
  public FeaturesDescription getDescription()
  {
    return ExtentFeatureExtractor.DESCRIPTION;
  }

  @Override
  public ExtentFeatures compute(Record record) throws AnalysisException
  {
    ExtentFeatures result = new ExtentFeatures();

    // Counts per sample, summed over panels/channels
    SensorMeasurement measurement = record.getCombinedGammaMeasurement(SensorPosition.ALL);
    SensorBackground background = record.getCombinedGammaBackground(SensorPosition.ALL);
    int[] combinedGrossInt = measurement.getCountsRange(0, measurement.size());
    double[] backgroundSubtracted = IntegerArray.promoteToDoubles(combinedGrossInt);

    // Subtract the background, then remove negative values from result
    DoubleArray.subtractAssign(backgroundSubtracted, background.getCountsRange(0, background.size()));
    DoubleArray.assignMaxOf(backgroundSubtracted, 0);

    // 1-second moving-average smoothing. Also compute max position:
    // FIXME: assumes 5 samples per second
    int start, stop, nsteps, maxidx = 0;
    double sum, maxval = -1;
    double[] smoothed = new double[backgroundSubtracted.length];
    for (int i = 0; i < smoothed.length; i++)
    {
      start = Math.max(i - 2, 0);
      stop = Math.min(i + 2, backgroundSubtracted.length - 1);
      nsteps = stop - start + 1;
      sum = 0;
      for (int j = start; j <= stop; j++)
      {
        sum += backgroundSubtracted[j];
      }
      smoothed[i] = sum / nsteps;
      if (smoothed[i] > maxval)
      {
        maxval = smoothed[i];
        maxidx = i;
      }
    }

    Instant peakTime = measurement.getTime(maxidx);

    result.peakLocationX = record.getVehicleMotion().getPosition(peakTime);
    result.maxIntensity = maxval;
    
    result.maxidx = maxidx;
    result.smoothed = smoothed;
    if (maxval == 0)  // nothing left after background subtraction
    {
      return result;
    }

    double[] position = record.getVehicleMotion().getPositions(measurement);

    // search outward from maxidx to find position of each threshold
    ArrayList<Integer> abovePeak = new ArrayList<>();
    ArrayList<Integer> belowPeak = new ArrayList<>();

    extractInnerThresholdPositions(maxidx, smoothed, belowPeak, abovePeak);

    result.innerWidths[0] = position[abovePeak.get(2)] - position[belowPeak.get(2)];
    result.innerWidths[1] = position[abovePeak.get(1)] - position[belowPeak.get(1)];
    result.innerWidths[2] = position[abovePeak.get(0)] - position[belowPeak.get(0)];
    
    result.peakVsFWHM = (result.peakLocationX - position[belowPeak.get(1)]) / 
            Math.sqrt( DoubleUtilities.sqr(result.innerWidths[1]) + 0.1 );
    
    // for auditing:
    result.innerIndices = new int[6];
    for (int idx=0; idx<3; idx++)
    {
      result.innerIndices[idx] = belowPeak.get(2-idx);
      result.innerIndices[idx+3] = abovePeak.get(idx);
    }

    // Now search inward from beginning / end of scan:
    abovePeak.clear();
    belowPeak.clear();
    extractOuterThresholdPositions(maxidx, smoothed, belowPeak, abovePeak);

    result.outerWidths[0] = position[abovePeak.get(0)] - position[belowPeak.get(0)];
    result.outerWidths[1] = position[abovePeak.get(1)] - position[belowPeak.get(1)];
    result.outerWidths[2] = position[abovePeak.get(2)] - position[belowPeak.get(2)];
    
    // for auditing:
    result.outerIndices = new int[6];
    for (int idx=0; idx<3; idx++)
    {
      result.outerIndices[idx] = belowPeak.get(idx);
      result.outerIndices[idx+3] = abovePeak.get(2-idx);
    }

    for (int idx = 0; idx < 3; idx++)
    {
      result.ratios[idx] = result.innerWidths[idx] / result.outerWidths[idx];
    }

    result.topBottom_ratio = result.innerWidths[2] / result.outerWidths[0];
    result.intensityFWHMRatio = result.maxIntensity / Math.sqrt(
            result.innerWidths[1]*result.innerWidths[1] + 0.1);

    // triangle intensity ratio
    double numerator = ((0.5*result.innerWidths[0]) - (0.25*result.innerWidths[1]));
    double denominator = Math.sqrt(
            DoubleUtilities.sqr(result.innerWidths[0] - result.innerWidths[1]) + 1);
    result.triangleIntensityRatio = numerator / denominator;

    return result;
  }

//<editor-fold desc="internal" defaultstate="collapsed">
  /**
   * Find locations (in time space) where counts fall off to 0.75, 0.5 and 0.25
   * of the peak. Resulting indices are stored in belowPeak and abovePeak
   *
   * @param maxidx location of max counts
   * @param counts counts per time step
   * @param belowPeak
   * @param abovePeak
   */
  void extractInnerThresholdPositions(int maxidx, double[] counts,
          ArrayList<Integer> belowPeak, ArrayList<Integer> abovePeak)
  {
    int N = counts.length;
    double maxval = counts[maxidx];

    double[] thresholds = new double[]
    {
      0.75 * maxval, 0.5 * maxval, 0.25 * maxval
    };

    int idx = maxidx;
    int tidx = 0;
    while (true)
    {
      idx += 1;
      if (idx >= N)
      {
        while (abovePeak.size() < 3)
        {
          abovePeak.add(N - 1);
        }
        break;
      }
      while ((tidx < 3) && (counts[idx] <= thresholds[tidx]))
      {
        abovePeak.add(idx);
        tidx += 1;
      }
      if (tidx > 2)
      {
        break;
      }
    }

    // now search downward for the same thresholds
    idx = maxidx;
    tidx = 0;
    while (true)
    {
      idx -= 1;
      if (idx < 0)
      {
        while (belowPeak.size() < 3)
        {
          belowPeak.add(0);
        }
        break;
      }
      while ((tidx < 3) && (counts[idx] <= thresholds[tidx]))
      {
        belowPeak.add(idx);
        tidx += 1;
      }
      if (tidx > 2)
      {
        break;
      }
    }
  }

  /**
   * Find locations (in time space) where counts grow to 0.25, 0.5 and 0.75 of
   * the peak. Resulting indices are stored in belowPeak and abovePeak
   *
   * @param maxidx location of max counts
   * @param counts counts per time step
   * @param belowPeak
   * @param abovePeak
   */
  void extractOuterThresholdPositions(int maxidx, double[] counts,
          ArrayList<Integer> belowPeak, ArrayList<Integer> abovePeak)
  {
    int N = counts.length;
    double maxval = counts[maxidx];

    double[] thresholds = new double[]
    {
      0.25 * maxval, 0.5 * maxval, 0.75 * maxval
    };

    int idx = 0;
    int tidx = 0;
    while (true)
    {
      idx += 1;
      if (idx >= maxidx)
      {
        while (belowPeak.size() < 3)
        {
          belowPeak.add(idx - 1);
        }
        break;
      }
      while ((tidx < 3) && (counts[idx] > thresholds[tidx]))
      {
        belowPeak.add(idx - 1);
        tidx += 1;
      }
      if (tidx > 2)
      {
        break;
      }
    }

    // now search for same thresholds starting at end of scan:
    idx = N - 1;
    tidx = 0;
    while (true)
    {
      idx -= 1;
      if (idx <= maxidx)
      {
        while (abovePeak.size() < 3)
        {
          abovePeak.add(idx + 1);
        }
        break;
      }
      while ((tidx < 3) && (counts[idx] > thresholds[tidx]))
      {
        abovePeak.add(idx + 1);
        tidx += 1;
      }
      if (tidx > 2)
      {
        break;
      }
    }

  }
//</editor-fold>

  @Override
  public Features newFeatures()
  {
    return new ExtentFeatures();
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