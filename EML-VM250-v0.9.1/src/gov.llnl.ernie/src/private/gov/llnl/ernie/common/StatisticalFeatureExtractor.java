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

import gov.llnl.ernie.ErniePackage;
import gov.llnl.ernie.analysis.FeatureExtractor;
import gov.llnl.math.matrix.Matrix;
import gov.llnl.math.matrix.MatrixFactory;
import gov.llnl.math.IntegerArray;
import gov.llnl.math.matrix.MatrixOps;
import gov.llnl.ernie.data.Record;
import gov.llnl.ernie.data.SensorBackground;
import gov.llnl.ernie.data.SensorMeasurement;
import gov.llnl.ernie.analysis.Features;
import gov.llnl.ernie.analysis.FeaturesDescription;
import gov.llnl.ernie.data.SensorPosition;
import gov.llnl.math.DoubleArray;
import gov.llnl.utility.xml.bind.Reader;

/**
 *
 * @author nelson85
 */
@Reader.Declaration(pkg = ErniePackage.class, name = "statisticalFeatureExtractor")
public class StatisticalFeatureExtractor implements FeatureExtractor
{
  // FIXME there are very slight differences between the output of this routine
  // and matlab.  I believe the source to be in the backgroundExtractor
  // However, for now they are small enough to disregard.
  private StatisticalFeaturesDescription description;
  private PeakFeatureExtractor peakFeatureExtractor = null;
  private int padding;
  private int minSamples;  // smallest # of samples to use for computing front/rear/peak statistics
//  private EnergyExtractor2 energyExtractor;

//<editor-fold desc="settings" defaultstate="collapsed">
  /**
   * @return the padding
   */
  public int getPadding()
  {
    return padding;
  }

  /**
   * @param padding the padding to set
   */
  @Reader.Attribute(name = "padding", type = int.class, required = true)
  public void setPadding(int padding)
  {
    this.padding = padding;
  }

  /**
   * @return the minSamples
   */
  public int getMinSamples()
  {
    return minSamples;
  }

  /**
   * @param minSamples the padding to set
   */
  @Reader.Attribute(name = "minSamples", type = int.class, required = true)
  public void setMinSamples(int minSamples)
  {
    this.minSamples = minSamples;
  }

  @Reader.Element(name = "peakFeatureExtractor", required = false)
  public void setPeakFeatureExtractor(PeakFeatureExtractor ee)
  {
    this.peakFeatureExtractor = ee;
  }

  public PeakFeatureExtractor getPeakFeatureExtractor()
  {
    return this.peakFeatureExtractor;
  }
//</editor-fold>

  @Override
  public void initialize()
  {
    FeaturesDescription contents = null;
    if (peakFeatureExtractor != null)
    {
      peakFeatureExtractor.initialize();
      contents = peakFeatureExtractor.getGroupDescription();
    }
    this.description = new StatisticalFeaturesDescription(contents);
  }

  @Override
  public FeaturesDescription getDescription()
  {
    return description;
  }

  @Override
  public Features newFeatures()
  {
    StatisticalFeatures out = new StatisticalFeatures(description);
    if (peakFeatureExtractor != null)
    {
      out.allPeakFeatures = peakFeatureExtractor.newFeatures();
      out.frontPeakFeatures = peakFeatureExtractor.newFeatures();
      out.rearPeakFeatures = peakFeatureExtractor.newFeatures();
    }
    return out;
  }

  @Override
  public StatisticalFeatures compute(Record record)
  {
    StatisticalFeatures result = new StatisticalFeatures(description);
    compute(result, record);
    return result;
  }

//<editor-fold desc="internal" defaultstate="collapsed">
  public void compute(StatisticalFeatures result, Record record)
  {
    // Get common
    int occupancyStart = record.getVPSMeasurement().getGammaOccupancyStart();
    int occupancyEnd = record.getVPSMeasurement().getGammaOccupancyEnd();
    double dt = record.getLane().getGammaSensorProperties().get(0).getSamplePeriodSeconds();

    // Note this is in 1s, but the samples are in 0.1.  Need to account for that.
    SensorMeasurement combinedMeasurement = record.getCombinedGammaMeasurement(SensorPosition.ALL);
    SensorBackground combinedBackground = record.getCombinedGammaBackground(SensorPosition.ALL);
    int n = combinedMeasurement.size();

    //  Compute the excess counts over expected at each point in time for the combined panel
    int[] combinedGrossInt = combinedMeasurement.getCountsRange(0, combinedMeasurement.size());
    Matrix combinedNet = MatrixFactory.createColumnVector(IntegerArray.promoteToDoubles(combinedGrossInt));
    MatrixOps.subtractAssign(combinedNet,
            MatrixFactory.wrapColumnVector(combinedBackground.getCountsRange(0, combinedGrossInt.length)));
    double[] cnf = combinedNet.flatten();

    // Intensity-Statistics
    //  Take the statistics for the period in which the vehicle was present.
    //  Only modification to the statistics is to correct the mean by
    //  subtracting the background in the first 40 samples
    result.allIntensityStats = StatisticalFeatureExtractorUtilities.computeIntensityStatistics(cnf,
            occupancyStart,
            occupancyEnd, dt);

    // Use the maximum to find the peak in time
    StatisticalPeakLocation peakLocation = StatisticalPeakLocation.findPeakLocation(
            record, occupancyStart, occupancyEnd, minSamples);
    double[] position = record.getVehicleMotion().getPositions(combinedMeasurement);

    // We need to the position for the detector at each gamma sample to compute the
    // weighted statistics
    double[] cnf2 = DoubleArray.apply(cnf, p -> p > 0 ? p : 0);
    int peakStart = Math.max(peakLocation.peakStartTime - padding, 0);
    int peakEnd = Math.min(peakLocation.peakEndTime + padding, combinedNet.rows());
    result.peakPositionStats = StatisticalFeatureExtractorUtilities.computeWeightedStatistics(
            position,
            cnf2,
            peakStart,
            peakEnd);

    // Extract spectrum (units counts per 0.1s in 9 channels)
    if (this.peakFeatureExtractor != null)
    {
      result.allPeakFeatures = peakFeatureExtractor.extract(record, peakLocation.peakStartTime, peakLocation.peakEndTime);
    }

    // Compute the split for front and back.
    int split = (int) StatisticalFeatureExtractorUtilities.computeSplit(
            combinedNet.flatten(),
            occupancyStart,
            occupancyEnd);
    result.split = split;

    // Find the best peak in the regions defined by the split.
    StatisticalPeakLocation peakLocation2 = StatisticalPeakLocation.findPeakLocation(
            record, occupancyStart, split + 1, minSamples);
    StatisticalPeakLocation peakLocation3 = StatisticalPeakLocation.findPeakLocation(
            record, split - 1, occupancyEnd, minSamples);

    // Compute the statistics for the front half
    int start = Math.max(peakLocation2.peakStartTime - padding, 0);
    int end = Math.max(peakLocation2.peakEndTime, start + minSamples);
    result.frontPositionStats = StatisticalFeatureExtractorUtilities.computeWeightedStatistics(
            position,
            cnf2,
            start,
            end);
    result.frontIntensityStats = StatisticalFeatureExtractorUtilities.computeIntensityStatistics(cnf,
            occupancyStart,
            Math.max(split, Math.min(occupancyEnd, occupancyStart + minSamples)),
            dt);

    // Compute the statistics for the rear half
    end = Math.min(peakLocation3.peakEndTime + padding, combinedNet.rows());
    start = Math.min(peakLocation3.peakStartTime, end - minSamples);
    result.rearPositionStats = StatisticalFeatureExtractorUtilities.computeWeightedStatistics(
            record.getVehicleMotion().getPositions(combinedMeasurement),
            cnf2,
            start,
            end);
    result.rearIntensityStats = StatisticalFeatureExtractorUtilities.computeIntensityStatistics(cnf,
            Math.min(split, Math.max(occupancyStart, occupancyEnd - minSamples)),
            occupancyEnd,
            dt);

    result.peakIntensityStats = StatisticalFeatureExtractorUtilities.computeIntensityStatistics(cnf,
            peakStart,
            peakEnd,
            dt);

    // Extract spectrum (units counts per 0.1s in 9 channels)
    if (this.peakFeatureExtractor != null)
    {
      result.frontPeakFeatures = peakFeatureExtractor.extract(record,
              peakLocation2.peakStartTime, peakLocation2.peakEndTime);
      result.rearPeakFeatures = peakFeatureExtractor.extract(record,
              peakLocation3.peakStartTime, peakLocation3.peakEndTime);
    }

    if (this.peakFeatureExtractor != null)
    {
      PeakFeatures tmp = peakFeatureExtractor.extract(record,
              Math.max(result.split - padding, 0),
              Math.min(result.split + padding, occupancyEnd));
      result.splitIntensity = tmp.getPeakStatistics()[0];
    }
    // Compute split features
    result.splitDistance = position[result.split];
    result.splitDeltaWidth = result.rearPositionStats[0] + result.rearPositionStats[1] - result.peakPositionStats[1];

    // Spread and SplitDip
    double dx = result.rearPositionStats[0] - result.frontPositionStats[0];
    double dy = result.rearPeakFeatures.peakStatistics[1] - result.frontPeakFeatures.peakStatistics[1];
    double dz = result.rearPeakFeatures.peakStatistics[2] - result.frontPeakFeatures.peakStatistics[2];

    double totalIntensity = result.allPeakFeatures.peakStatistics[0];
    double frontIntensity = result.frontPeakFeatures.peakStatistics[0];
    double rearIntensity = result.rearPeakFeatures.peakStatistics[0];
    double meanIntensity = (totalIntensity + frontIntensity + rearIntensity) / 3;

    result.spreadX = dx;
    result.spread3d = Math.sqrt(dx * dx + dy * dy + dz * dz);
    if (meanIntensity != 0)
    {
      result.spreadI = ((Math.max(Math.max(totalIntensity, frontIntensity), rearIntensity)
              - Math.min(Math.min(totalIntensity, frontIntensity), rearIntensity))
              / meanIntensity);
      result.splitDip = ((result.splitIntensity
              - Math.min(Math.min(totalIntensity, frontIntensity), rearIntensity))
              / meanIntensity);
    }

    result.IStdDevIMeanRatio = result.peakIntensityStats[1] / Math.sqrt(
            result.peakIntensityStats[0] * result.peakIntensityStats[0] + 1);
  }

//</editor-fold>
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