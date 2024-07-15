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

import gov.llnl.ernie.analysis.FeatureExtractor;
import gov.llnl.ernie.analysis.AnalysisException;
import gov.llnl.ernie.spectral.EnergyExtractor2.HypothesisResult;
import gov.llnl.ernie.spectral.JointSeparation.ComplementaryScan;
import gov.llnl.math.DoubleArray;
import static gov.llnl.math.DoubleUtilities.clamp;
import static gov.llnl.math.DoubleUtilities.sqr;
import gov.llnl.math.MathExceptions.SizeException;
import gov.llnl.math.matrix.Matrix;
import gov.llnl.math.matrix.MatrixFactory;
import gov.llnl.math.matrix.MatrixOps;
import java.io.Serializable;
import gov.llnl.utility.CoreDump;
import gov.llnl.ernie.data.Record;
import gov.llnl.utility.xml.bind.ReaderInfo;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import gov.llnl.ernie.data.SensorMeasurement;
import gov.llnl.utility.InitializeException;
import gov.llnl.ernie.data.Lane;
import gov.llnl.ernie.data.SensorPosition;
import gov.llnl.ernie.math.Utility;
import gov.llnl.math.euclidean.Vector3;
import java.util.stream.Stream;

/**
 * Extract the sources by decomposing spectrally using matched filters.
 *
 * This is currently hardwired for the 4 panel RPM8 configuration. We can
 * replace this with a general purpose system when needed.
 *
 * @author nelson85
 */
@ReaderInfo(JointFeatureExtractorReader.class)
public class JointFeatureExtractor implements Serializable, FeatureExtractor
{
  private static final long serialVersionUID
          = gov.llnl.utility.UUIDUtilities.createLong("FeatureExtractorJoint-v1");

  public JointSeparation js;
  public double[][] targets;
  public EnergyExtractor2 energyExtractor;
  int nPanels = 0;
  boolean fallback = false;
  private JointFeaturesDescription featureDescription;
  private String[] clusterLabels;

  public JointFeatureExtractor()
  {
  }

  public void setPanels(int nPanels)
  {
    this.nPanels = nPanels;
  }

  public void setFallback(boolean fallback)
  {
    this.fallback = fallback;
  }

  public void setEnergyExtractor(EnergyExtractor2 energyExtractor)
  {
    this.energyExtractor = energyExtractor;
    String[] clusterLabels = Stream.of(energyExtractor.getHypothesisTitles())
            .map(w -> w.concat(".corr")).toArray(String[]::new);
    this.clusterLabels = clusterLabels;
  }

  @Override
  public void initialize() throws InitializeException
  {
    if (this.nPanels == 0)
    {
      throw new InitializeException("Panels is not set");
    }

    js = new JointSeparation(nPanels);
    int n = energyExtractor.hypothesisTests.size();
    targets = new double[n][];
    for (int i = 0; i < n; i++)
    {
      Matrix mean = energyExtractor.hypothesisTests.get(i).mean;
      targets[i] = mean.copyColumnTo(new double[mean.rows()], 0, 0);
    }
    if (energyExtractor == null)
    {
      throw new InitializeException("Energy extractor is not set");
    }
  }

  @Override
  public JointFeaturesDescription getDescription()
  {
    if (this.featureDescription == null)
      this.featureDescription = new JointFeaturesDescription(this.fallback, this.clusterLabels);
    return this.featureDescription;
  }

  @Override
  public JointFeatures compute(Record record2) throws AnalysisException
  {
    try
    {
      // Create an output
      JointFeatures out = new JointFeatures(this.getDescription());

      // Perform joint separation
      JointSeparation.Results jsr = js.separate(record2, targets);
      out.separationResults = jsr;

      // We can extract the sources directly
      out.sources[0].spectrum = jsr.pass3.src1;
      out.sources[1].spectrum = jsr.pass3.src2;

      // DEBUG
      if (jsr.pass4 == null)
      {
        return out;
      }

      // Estimate the lane position
      Lane lane = record2.getLane();
      double laneWidth = lane.getLaneWidth();

      for (int i = 0; i < 2; i++)
      {
        // Estimate the x position
        int u = jsr.pass4.scan[i].timeIndex;
        Instant t = record2.getGammaMeasurements().get(0).getTime(u);

        // FIXME Calib pass4 scan contains information required ot compute the extent
        double[] intensity = jsr.pass4.scan[i].intensity;
        double sumBottom = Utility.sum(intensity, lane.getGammaPanels(SensorPosition.BOTTOM));
        double sumTop = Utility.sum(intensity, lane.getGammaPanels(SensorPosition.TOP));
        double sumRight = Utility.sum(intensity, lane.getGammaPanels(SensorPosition.RIGHT));
        double sumLeft = Utility.sum(intensity, lane.getGammaPanels(SensorPosition.LEFT));
        double ratio1 = out.sources[i].ratio1 = (sumBottom - sumTop) / (sumBottom + sumTop + 1);
        double ratio2 = out.sources[i].ratio2 = (sumRight - sumLeft) / (sumRight + sumLeft + 1);

        out.sources[i].position = lane.convertGammaRatios(ratio1, ratio2);
        double xPosition = 0;
        if (!fallback)
        {
          xPosition = record2.getVehicleMotion().getPositions(
                  record2.getCombinedGammaMeasurement(SensorPosition.ALL)
          )[out.separationResults.pass4.scan[i].timeIndex];
        }
        out.sources[i].position = Vector3.of(xPosition,
                out.sources[i].position.getY(), out.sources[i].position.getZ());

        // Compute the extent of the source
        if (!fallback)
        {
          ExtentResult er = extractExtent(record2, out.separationResults.pass4.scan[i]);
          out.sources[i].length = er.length;
          out.sources[i].x1 = er.x1;
          out.sources[i].x2 = er.x2;
        }

        // Estimate the source intensity
        out.sources[i].intensity = JointFeatureExtractor.extractIntensity(
                jsr.pass4.scan[i].intensity, out.sources[i].position, laneWidth);
      }

      // Extract the energy features
      Discriminator.Result dr = Discriminator.process(targets,
              out.sources[0].spectrum, out.sources[1].spectrum, jsr.meanBackgroundSpectrum);
      out.sources[0].correlationEnergyFeatures = dr.energy1;
      out.sources[1].correlationEnergyFeatures = dr.energy2;

      // Compute chisqr from background (found to be a necessary feature)
      HypothesisResult hr = new HypothesisResult();
      ComplementaryScan scan = jsr.pass4.scan[1];
      energyExtractor.backgroundHypothesisTest.evaluate(hr,
              scan.sampleSpectrum, scan.bkgSpectrum, scan.bkgSpectrum2, JointSeparation.timeWindow);
      out.sources[1].bkgHypothesis = hr.distance;
      scan = jsr.pass4.scan[0];
      energyExtractor.backgroundHypothesisTest.evaluate(hr,
              scan.sampleSpectrum, scan.bkgSpectrum, scan.bkgSpectrum2, JointSeparation.timeWindow);
      out.sources[0].bkgHypothesis = hr.distance;

      // Compute energy ratios
      extractRatios(jsr, out.sources[0], jsr.pass4.scan[0], jsr.pass3.src1);
      extractRatios(jsr, out.sources[1], jsr.pass4.scan[1], jsr.pass3.src2);

      // Put most intense source first:
      // if (out.sources[1].intensity > out.sources[0].intensity)
      //   out.swapSources();
      return out;
    }
    // Catch all exceptions that warrent a core or simply Exception
    catch (AnalysisException ex)
    {
      // Create a core dump file condition on a System property.
      CoreDump dump = new CoreDump("gov.llnl.ernie.FeatureExtractorJoint", false);

      // Add each of the required items to produce the problem
      dump.add("FeatureExtractorJoint", this);
      dump.add("record", record2);
      dump.add("exception", ex);

      // Each of the items will be saved in the core file
      dump.write("FEJ");

      // Rethrow the exception to keep the control flow
      throw ex;
    }
  }

//<editor-fold desc="internal" defaultstate="collapsed">
  public void extractRatios(JointSeparation.Results jsr, JointSource results, ComplementaryScan scan, double[] src)
  {
    try
    {
      // Estimate ratio tests for each source
      double[] sample = new double[9];
      double[] bkgRate = new double[9];

      int t = scan.timeIndex;
      double c = 0;
      for (int i = 0; i < nPanels; i++)
      {
        DoubleArray.addAssign(sample, jsr.ws[i].backgroundEstimate[t]);
        DoubleArray.addAssign(bkgRate, jsr.ws[i].backgroundSpectrum);
        c += scan.counts[i][t];
      }
//      DoubleArray.addScaled(sample, sample, src, c);
      DoubleArray.addAssignScaled(sample, src, c);
      DoubleArray.divideAssign(bkgRate, 10);
      int i = 0;
      results.ratioEnergyFeatures = new double[energyExtractor.ratioTests.size()];
      for (EnergyExtractor2.RatioTest test : energyExtractor.ratioTests)
      {
        results.ratioEnergyFeatures[i++] = test.evaluate(sample, bkgRate);
      }

      // Compute PCA features
      Matrix normalizedEstimate = MatrixFactory.createColumnVector(src);
      MatrixOps.normColumns2(normalizedEstimate);

      //PCA Coherence
//      DoubleMatrix.subtractAssign(normalizedEstimate, energyExtractor.dataMean);
      MatrixOps.subtractAssign(normalizedEstimate, energyExtractor.dataMean);

      Matrix tmp = MatrixOps.multiply(energyExtractor.pcaTransform, energyExtractor.coh);

      //  pcaFeatures=T*(S/|S|);
      Matrix pca = MatrixOps.multiply(tmp, normalizedEstimate);
      results.pcaEnergyFeatures = pca.flatten();
    }
    catch (SizeException ex)
    {
      throw new RuntimeException(ex);
    }
  }

//<editor-fold desc="interpolation tables" defaultstate="collapsed">
  final static double K_Y = 0.00019264;
  final static double[] W_Y =
  {
    0.39515193,
    -0.39495929,
    0.07196249,
    -0.08002089
  };

  final static double K_Z = 1.17770511;
  final static double[] W_Z =
  {
    -2.59171455,
    3.76941966,
    2.55433242,
    0.14014855
  };

  final static double K_S = 2.81580605;
  final static double[] W_S =
  {
    10.24239674,
    1.37886045,
    3.58898349,
    -0.93241856
  };
//</editor-fold>

  public static Vector3 extractLanePosition(double[] panelIntensities, double laneWidth, double x)
  {
    // FIXME This is hard coded for RPM8

    // Use interpolation tables to extract the position
    double[] normIntensity = DoubleArray.normColumns1(DoubleArray.assignMaxOf(panelIntensities.clone(), 0));
    double y = clamp(laneWidth * (W_Y[0] * (normIntensity[0] + normIntensity[2])
            + W_Y[1] * (normIntensity[1] + normIntensity[3])
            + W_Y[2] * sqr(normIntensity[0] - normIntensity[2])
            + W_Y[3] * sqr(normIntensity[1] - normIntensity[3])), -2.0, 2.0);
    double z = clamp((W_Z[0] * (normIntensity[0] + normIntensity[1])
            + W_Z[1] * (normIntensity[2] + normIntensity[3])
            + W_Z[2] * (sqr(normIntensity[0] - normIntensity[1]) - sqr(normIntensity[2] - normIntensity[3]))
            + W_Z[3] * sqr(y / laneWidth)) + K_Z, 0.3, 3);
    return Vector3.of(x, y, z);
  }

  /**
   * Computes the intensity given the extracted counts in each panel.
   *
   * @param panelIntensity
   * @param pos
   * @param laneWidth
   * @return
   */
  public static double extractIntensity(double[] panelIntensity, Vector3 pos, double laneWidth)
  {
    // Use interpolation table to compute the scaling factor
    // Scale increases as the measurement gets closer to the panels.
    double scale = W_S[0] * sqr(pos.getY() / laneWidth)
            + W_S[1] * sqr(sqr(pos.getY() / laneWidth))
            + W_S[2] * pos.getZ()
            + W_S[3] * sqr(pos.getZ())
            + K_S;

    // There is a minimum scaling that is acceptable
    scale = Math.max(scale, 2.5);

    // Compute the total counts in all panels
    double sum = 0;
    for (int i = 0; i < panelIntensity.length; i++)
    {
      sum += Math.max(panelIntensity[i], 0);
    }

    // The intensity estimate is the counts in all divided by the scale.
    return sum / scale;
  }

  static public final double EXTENT_BIAS = 10;

  static public final double[] EXTENT_COEF = new double[]
  {
    1.1454,
    -12.5322,
    9.4860,
    2.1771,
    8.3762,
    -8.7259,
    0.2414
  };

  @Override
  public JointFeatures newFeatures()
  {
    return new JointFeatures(this.getDescription());
  }

  public static class ExtentResult implements Serializable
  {
    double x1, x2;
    double length;
  }

  /**
   * Computes the effective extent of the object to determine how long it is.
   * Operates in time domain based on flatness during the interval the source is
   * determines as present. The converts to distance using the record motion
   * profile.
   *
   * @param record
   * @param scan
   * @return
   */
  public static ExtentResult extractExtent(Record record, ComplementaryScan scan)
  {
    int t1 = scan.timeIndex;
    double mx1 = scan.metrics[t1];

    // Find the window of analysis
    double thresh = Math.sqrt(mx1 * mx1 + 4) / 4;
    int t2 = t1;
    while (t2 > 0 && scan.metrics[t2] > thresh)
    {
      t2--;
    }
    t2++;

    int t3 = t1;
    while (t3 < scan.metrics.length && scan.metrics[t3] > thresh)
    {
      t3++;
    }

    // Compute metrics
    int c1 = 0, c2 = 0;
    double s2 = 0;
    for (int i = t2; i < t3; i++)
    {
      double s1 = Math.max(scan.metrics[i], 0);
      if (s1 > 0.5 * mx1)
      {
        c1++;
      }
      if (s1 > 0.75 * mx1)
      {
        c2++;
      }
      s2 += s1;
    }
    double m1 = ((double) c1) / (t3 - t2);
    double m2 = ((double) c2) / (t3 - t2);
    double m3 = s2 / (t3 - t2) / mx1;

    // Interpolate from the metrics to extent
    double Q = Math.min(Math.max(EXTENT_COEF[0]
            + EXTENT_COEF[1] * m1
            + EXTENT_COEF[2] * m2
            + EXTENT_COEF[3] * m3
            + EXTENT_COEF[4] * m1 * m1
            + EXTENT_COEF[5] * m2 * m2
            + EXTENT_COEF[6] * m3 * m3,
            0.10), 1);
    double extent = (t3 - t2) * Q - EXTENT_BIAS;

    // Bad interpolation for non-sources can generate negative numbers here.
    // Such a number is meaningless and must be capped.
    if (extent < 0)
    {
      extent = 0;
    }

    // Convert the extent into times
    double outT1 = t1 - extent / 2;
    double outT2 = t1 + extent / 2;

    if (outT1 < t2)
    {
      outT2 = outT2 + t2 - outT1;
      outT1 = t2;
    }
    if (outT2 > t3)
    {
      outT1 = outT1 + t3 - outT2;
      outT2 = t3;
    }

    // Convert times into distances
    ExtentResult result = new ExtentResult();
    SensorMeasurement measurement = record.getCombinedGammaMeasurement(SensorPosition.ALL);
    Instant i1 = getTime(measurement, outT1);
    Instant i2 = getTime(measurement, outT2);
    result.x1 = record.getVehicleMotion().getPosition(i1);
    result.x2 = record.getVehicleMotion().getPosition(i2);
    result.length = result.x2 - result.x1;
    return result;
  }

  static Instant getTime(SensorMeasurement panel, double t)
  {
    Instant p0 = panel.getTime((int) (t));
    int ms = (int) ((t - (int) t) * 100);
    return p0.plus(ms, ChronoUnit.MILLIS);
  }

//  public static void main(String[] args) throws Exception, SQLException
//  {
//    Database_RPM8 dbc = new Database_RPM8();
//    RPM8RecordInternal record = dbc.getRecord(671561404);
//    JointFeatureExtractor fej = new JointFeatureExtractor();
//    JointFeatureExtractor.JointFeatures res = fej.extract(record);
//
//    CoreDump dump = new CoreDump("gov.llnl.ernie.JointFeatureExtractor", true);
//
//    // Add each of the required items to produce the problem
//    dump.add("JointFeatureExtractor", fej);
////        dump.add("FeatureExtractorJointFeatures", res);
//    dump.add("record", record);
//
//    // Each of the items will be saved in the core file
//    dump.write("FEJ");
//  }
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