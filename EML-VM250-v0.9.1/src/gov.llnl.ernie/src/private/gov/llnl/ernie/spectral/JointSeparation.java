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

import gov.llnl.ernie.analysis.AnalysisException;
import gov.llnl.ernie.data.Record;
import gov.llnl.math.DoubleArray;
import gov.llnl.math.MathExceptions.SizeException;
import gov.llnl.math.matrix.Matrix;
import gov.llnl.math.matrix.MatrixFactory;
import gov.llnl.math.matrix.MatrixOps;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import gov.llnl.ernie.data.SensorBackground;
import gov.llnl.ernie.data.SensorMeasurement;
import gov.llnl.math.matrix.MatrixViews;

/**
 *
 * @author nelson85
 */
public class JointSeparation implements Serializable
{
  private static final long serialVersionUID
          = gov.llnl.utility.UUIDUtilities.createLong("JointSeparation-v1");
  final int nPanels;

  public static int timeWindow = 10;

  public JointSeparation(int nPanels)
  {
    this.nPanels = nPanels;
  }

  /**
   * Perform a joint separation to extract two sources from time series spectral
   * data. This method will use a number of matched filters to try to locate two
   * spectral shapes with the time series spectral data. It requires that there
   * be a background estimate or why to estimate background. It also requires a
   * set of spectral shapes to perform an initial search on.
   *
   * @param record to be extracted from
   * @param sources are the prototype spectral shapes to search for
   * @throws gov.llnl.ernie.analysis.AnalysisException
   * @return
   */
  public Results separate(Record record, double[][] sources) throws AnalysisException
  {
    Results results = new Results();
    PanelWorkspace[] ws = new PanelWorkspace[nPanels + 1];

    for (int i = 0; i < ws.length; i++)
    {
      ws[i] = new PanelWorkspace();
    }
    results.ws = ws;  // DEBUG

    // Extract the panel data and filter it by the rolling window
    extractPanelData(ws, record);

    // Estimate the background spectrum and suppressed background
    results.meanBackgroundSpectrum = computeBackgroundSpectrum(ws, record);

    // Extract the background as a function of time
    computeBackgroundProfile(ws, record, true);

    // Scan for the presence of a source
    PrimaryScan pass1 = scanForSources(ws, sources, null);
    results.pass1 = pass1;
    PrimaryScan pass1g = scanForSourcesGross(ws);
    results.pass1g = pass1g;
    if (results.pass1g.maxScore > results.pass1.maxScore)
    {
      pass1.bestTarget = pass1g.bestTarget;
    }

    // Estimate a spectral shape for this source
    double[] src1 = extractSource1(ws, pass1.maxTime, pass1.bestTarget);
    results.sourceInitial = src1;
//    if (DoubleUtilities.isnan(DoubleArray.sum(src1)))
    if (Double.isNaN(DoubleArray.sum(src1)))
    {
      throw new RuntimeException("NaN in extractSource1");
    }

    // Scan for the presence of a second source 
    PrimaryScan pass2 = scanForSources(ws, sources, src1);
    results.pass2 = pass2;

    // Perform a joint separation for the two sources
    ExtractSource2Results pass3 = extractSource2(ws, pass1.bestTarget, pass2.bestTarget, src1, pass1.maxScore / 4, pass2.maxScore / 4);
    results.pass3 = pass3;

//    if (DoubleArray.equal(pass3.src1, pass3.src2))
    if (DoubleArray.equivalent(pass3.src1, pass3.src2))
    {
      throw new AnalysisException("source extraction failed");
    }
    // Extract the time profile of the estimated sources
    ComplementaryResults pass4 = scanComplementary(ws, pass3.src1, pass3.src2);
    results.pass4 = pass4;
    return results;
  }

  /**
   * Extract all of the panel data with rolling window filtering.
   *
   * @param record
   */
  public void extractPanelData(PanelWorkspace[] ws, Record record)
  {
    List<SensorMeasurement> gammaMeasurements = record.getGammaMeasurements();
    for (int i = 0; i < nPanels; i++)
    {
      ws[i].sampleData = TimeFilter.filter(gammaMeasurements.get(i)
              .getSpectrumRange(0, gammaMeasurements.get(i).size()), timeWindow);
    }

    ws[nPanels].sampleData = new double[ws[0].sampleData.length][];
    for (int i = 0; i < ws[nPanels].sampleData.length; i++)
    {
      ws[nPanels].sampleData[i] = new double[9];
      for (int j = 0; j < nPanels; j++)
      {
        DoubleArray.addAssign(ws[nPanels].sampleData[i], ws[j].sampleData[i]);
      }
    }
  }

  static final double TYPICAL_SUPPRESSION = 0.7;

  public double[] computeBackgroundSpectrum(PanelWorkspace[] ws, Record record)
  {
    double[] meanBackgroundSpectrum = new double[9];
    ws[nPanels].backgroundSpectrum = new double[9];
    ws[nPanels].suppressedBackgroundSpectrum = new double[9];
    for (int i = 0; i < nPanels; i++)
    {
      ws[i].backgroundSpectrum = record.getGammaBackgrounds().get(i).getExpectedSpectrum();
      ws[i].suppressedBackgroundSpectrum = record.getGammaBackgrounds().get(i)
              .computeExpectedSpectrumSuppressed(TYPICAL_SUPPRESSION, 1.0);

      DoubleArray.addAssign(meanBackgroundSpectrum, ws[i].backgroundSpectrum);
      DoubleArray.addAssign(ws[nPanels].backgroundSpectrum, ws[i].backgroundSpectrum);

      // FIXME the following line is bugged
//      DoubleArray.addAssign(ws[nPanels].suppressedBackgroundSpectrum, ws[i].backgroundSpectrum);
      DoubleArray.addAssign(ws[nPanels].suppressedBackgroundSpectrum, ws[i].suppressedBackgroundSpectrum);
    }
    return meanBackgroundSpectrum;
  }

  public void computeBackgroundProfile(PanelWorkspace[] ws, Record record, boolean useBackgroundEstimator)
  {
    if (record.getVehicleMotion() == null)
    {
      useBackgroundEstimator = false;
    }

    int N = ws[0].sampleData.length;
    if (useBackgroundEstimator == true)
    {
      ArrayList<double[]> be1 = new ArrayList<>();
      for (int i = 0; i < nPanels; ++i)
      {
        SensorBackground gbk = record.getGammaBackgrounds().get(i);
        be1.add(gbk.getCountsRange(0, gbk.size()));

      }
      // Method 1: We has a vehicle class profile that we can extract the suppression estimate from
      Matrix backgroundTemplate = MatrixFactory.newColumnMatrix(be1);

      // For each panel
      for (int i = 0; i < nPanels; ++i)
      {
        ws[i].backgroundEstimate = new double[N][];
        ws[i].suppression = new double[N];
        // For each time
        for (int j = 0; j < N; ++j)
        {
          double suppression = backgroundTemplate.get(j, i) / backgroundTemplate.get(0, i);
          double[] d = record.getGammaBackgrounds().get(i)
                  .computeExpectedSpectrumSuppressed(suppression, 0.1); // FIXME magic number
          ws[i].backgroundEstimate[j] = d;
          ws[i].suppression[j] = suppression;
        }
      }
    }
    else
    {
      // Method 2: We have to make one up on the fly
      // For each panel
      for (int i = 0; i < nPanels; ++i)
      {
        ws[i].backgroundEstimate = new double[N][];
        ws[i].suppression = new double[N];
        // For each time
        for (int j = 0; j < N; ++j)
        {
          double[][] data = ws[i].sampleData;

          double suppression = 1;
          if ((j > timeWindow) && (j + timeWindow < N))
          {
            for (int k = 0; k < 9; ++k)
            {
              suppression = Math.min(suppression, (data[j][k] + 2 * Math.sqrt(data[j][k] + 1)) / ws[i].backgroundSpectrum[k]);
            }
          }
          double[] d = record.getGammaBackgrounds().get(i)
                  .computeExpectedSpectrumSuppressed(suppression, 0.1); // FIXME magic number
          ws[i].backgroundEstimate[j] = d;
          ws[i].suppression[j] = suppression;
        }
      }
    }

    // Apply the time filter
    for (int i = 0; i < nPanels; ++i)
    {
      ws[i].backgroundEstimate = TimeFilter.filter(ws[i].backgroundEstimate, timeWindow);
    }

    ws[nPanels].backgroundEstimate = new double[ws[0].backgroundEstimate.length][];
    for (int i = 0; i < ws[nPanels].backgroundEstimate.length; i++)
    {
      ws[nPanels].backgroundEstimate[i] = new double[9];
      for (int j = 0; j < nPanels; j++)
      {
        DoubleArray.addAssign(ws[nPanels].backgroundEstimate[i], ws[j].backgroundEstimate[i]);
      }
    }
  }

//  public double[][] pass1;
//  public double[][] pass2;
//  public int sourceTime;
//  public int bestTargetIndex;
//  public double maxScore;
//  public double[] bestTarget;
  public static class PrimaryScan implements Serializable
  {
    public int maxTime;
    public int bestTargetIndex;
    public double maxScore = -10000;
    public double[] bestTarget;

    // Audit
    public int maxPanel;
    public double[][] details;

    private PrimaryScan(int i)
    {
      details = new double[i][];
      maxTime = 0;
      bestTargetIndex = -1;
      maxScore = -10000;
      bestTarget = null;
      maxPanel = -1;
    }
  }

  /**
   * This routine searches for a source that matches the targets in the time
   * series spectral data. The background and sample data must already by
   * extracted. It will construct a matched filter for each of the targets and
   * apply it to each of the panels. The largest signal in any panels is assumed
   * to be the location of the source.
   *
   * @param targets
   * @param nuisance is a source to be avoided. Can be null.
   * @return
   * @throws gov.llnl.ernie.analysis.AnalysisException
   */
  public PrimaryScan scanForSources(PanelWorkspace[] ws, double[][] targets, double[] nuisance) throws AnalysisException
  {
    PrimaryScan out = new PrimaryScan(targets.length * ws.length);

    int N = ws[0].sampleData.length;
    int i4 = 0;
    // For each target
    for (int i2 = 0; i2 < targets.length; ++i2)
    {
      if (nuisance != null && DoubleArray.equivalent(targets[i2], nuisance))
      {
        for (int i1 = 0; i1 < 4; i1++)
        {
          out.details[i4++] = new double[ws[i1].sampleData.length];
        }
        continue;
      }

      // For each panel
      for (int i1 = 0; i1 < ws.length; i1++)
      {

        // Create a matched filter.
        MatchedFilter mf = MatchedFilter.create(targets[i2],
                ws[i1].backgroundSpectrum,
                ws[i1].suppressedBackgroundSpectrum,
                nuisance);

        double[] out2 = mf.apply(ws[i1].sampleData);

        // Find the maximum score for this panel and target
//        int i3 = DoubleArray.findMaxIndex(out2);
        int i3 = DoubleArray.findIndexOfMaximum(out2);

        // RPM8RecordInternal what source template and time index was maximum for 
        // the source extraction.
        if (out.maxScore < out2[i3])
        {
          out.bestTargetIndex = i2;
          out.maxTime = i3;
          out.maxPanel = i1;
          out.maxScore = out2[i3];
          out.bestTarget = targets[i2];
        }

        // Maintain audit
        out.details[i4++] = out2;
      }
    }
    if (out.bestTarget == null)
    {
      throw new RuntimeException("failed to find source");
    }

    return out;
  }

  public PrimaryScan scanForSourcesGross(PanelWorkspace[] ws)
  {
    PrimaryScan out = new PrimaryScan(ws.length);
    // Method 2 Gross counts
    // For each panel

    int i4 = 0;

    for (int i1 = 0; i1 < ws.length; i1++)
    {
      int n = ws[i1].sampleData.length;
      double[] out2 = new double[n];
      for (int i2 = 0; i2 < n; i2++)
      {
        double gross = DoubleArray.sum(ws[i1].sampleData[i2]);
        double bkg = DoubleArray.sum(ws[i1].backgroundEstimate[i2]);
        out2[i2] = (gross - bkg - 2 * Math.sqrt(bkg)) / Math.sqrt(bkg);
      }

      // Find the maximum score for this panel and target
//      int i3 = DoubleArray.findMaxIndex(out2);
      int i3 = DoubleArray.findIndexOfMaximum(out2);

      // RPM8RecordInternal what source template and time index was maximum for 
      // the source extraction.
      if (out.maxScore < out2[i3])
      {
        out.bestTargetIndex = -1;
        out.maxTime = i3;
        out.maxPanel = i1;
        out.maxScore = out2[i3];
        out.bestTarget = DoubleArray.subtractAssign(
                ws[i1].sampleData[i3].clone(),
                ws[i1].backgroundEstimate[i3]);
      }

      // Maintain audit
      out.details[i4++] = out2;
    }

    return out;
  }

  public class ComplementaryScan implements Serializable
  {
    /**
     * Per panel scan data (panel,time)
     */
    public double[][] counts;

    // metrics of detection by time
    public double[] metrics;

    // panel data at the peak metric
    public double[] intensity;

    // time index of peak metric
    public int timeIndex;

    // matched filter efficiency number  (counts per projection)
    public double[] mfScale;

    // Raw data for peak metric time so we can test for background
    public double[] sampleSpectrum;
    public double[] bkgSpectrum;
    public double[] bkgSpectrum2;

    public double getMaxMetric()
    {
      return metrics[timeIndex];
    }

    public ComplementaryScan()
    {
      counts = new double[nPanels][];
      mfScale = new double[nPanels];
    }
  }

  public class ComplementaryResults implements Serializable
  {
    public ComplementaryScan[] scan = new ComplementaryScan[2];

    public ComplementaryResults()
    {
      scan[0] = new ComplementaryScan();
      scan[1] = new ComplementaryScan();
    }
  }

//  /**
//   * Compute the correlation between two sources. This will be used to compute
//   * the energy features.
//   *
//   * @param src1
//   * @param src2
//   * @return
//   */
//  public double computeCorrelation(double[] src1, double[] src2)
//  {
//    double[] src1n = src1.clone();
//    double[] src2n = src2.clone();
////    double mb = DoubleArray.computeInnerProduct(meanBackgroundSpectrum, meanBackgroundSpectrum, 9, 0, 0);
////    double ms1 = DoubleArray.computeInnerProduct(meanBackgroundSpectrum, src1, 9, 0, 0);
////    double ms2 = DoubleArray.computeInnerProduct(meanBackgroundSpectrum, src2, 9, 0, 0);
////    DoubleArray.subtractScaled(src1n, src1n, meanBackgroundSpectrum, ms1 / mb);
////    DoubleArray.subtractScaled(src2n, src2n, meanBackgroundSpectrum, ms2 / mb);
////    double c1 = DoubleArray.computeInnerProduct(src1n, src1, 9, 0, 0);
////    double c2 = DoubleArray.computeInnerProduct(src2n, src2, 9, 0, 0);
////    double c3 = DoubleArray.computeInnerProduct(src1n, src2, 9, 0, 0);
//
//    double mb = DoubleArray.multiplyInner(meanBackgroundSpectrum, 0, meanBackgroundSpectrum, 0, 9);
//    double ms1 = DoubleArray.multiplyInner(meanBackgroundSpectrum, 0, src1, 0, 9);
//    double ms2 = DoubleArray.multiplyInner(meanBackgroundSpectrum, 0, src2, 0, 9);
//
//    DoubleArray.addAssignScaled(src1n, meanBackgroundSpectrum, -(ms1 / mb));
//    DoubleArray.addAssignScaled(src2n, meanBackgroundSpectrum, -(ms2 / mb));
//
//    double c1 = DoubleArray.multiplyInner(src1n, 0, src1, 0, 9);
//    double c2 = DoubleArray.multiplyInner(src2n, 0, src2, 0, 9);
//    double c3 = DoubleArray.multiplyInner(src1n, 0, src2, 0, 9);
//
//    return c3 / Math.sqrt(c1 * c2);
//  }
  /**
   * Extract the time profile for two sources. If the second source spectrum
   * strongly interferes with the extraction of the first when the efficiency of
   * the matched filter is computed, the complementary signal is not computed.
   *
   * @param src1
   * @param src2
   * @return
   * @throws AnalysisException
   */
  public ComplementaryResults scanComplementary(PanelWorkspace[] ws, double[] src1, double[] src2)
          throws AnalysisException
  {
    ComplementaryResults out = new ComplementaryResults();

    int N = ws[0].sampleData.length;
    int i3 = 0;

    // double corr = computeCorrelation(src1, src2);
    // System.out.println("Corr "+corr);
    for (int i1 = 0; i1 < nPanels; i1++)
    {
      MatchedFilter mf1;
      MatchedFilter mf1a;
      MatchedFilter mf1b;
      // Create two matched filters for source1.
      mf1a = MatchedFilter.create(src1,
              ws[i1].backgroundSpectrum,
              ws[i1].suppressedBackgroundSpectrum);
      mf1b = MatchedFilter.create(src1,
              ws[i1].backgroundSpectrum,
              ws[i1].suppressedBackgroundSpectrum,
              src2);

      // Compare the two possible matched filters.  If there is a large loss
      // by using complementary matched filters, then we need to limit that lose.
      // In most cases there is only one source, so this is the best procedure.
      if (1.5 * mf1a.scale < mf1b.scale)
      {
        mf1 = mf1a;
      }
      else
      {
        mf1 = mf1b;
      }

      // Create a matched filter for the lesser source.
      MatchedFilter mf2 = MatchedFilter.create(src2,
              ws[i1].backgroundSpectrum,
              ws[i1].suppressedBackgroundSpectrum,
              src1);

      // Apply the matched filter to the data.
      out.scan[0].counts[i3] = mf1.getCounts(ws[i1].sampleData);
      out.scan[1].counts[i3] = mf2.getCounts(ws[i1].sampleData);

      out.scan[0].mfScale[i3] = mf1.scale;
      out.scan[1].mfScale[i3] = mf2.scale;

      i3++;
    }

    for (int i = 0; i < 2; i++)
    {
      out.scan[i].metrics = complementaryMetric(ws, out.scan[i]);
//      out.scan[i].timeIndex = DoubleArray.findMaxIndex(out.scan[i].metrics);
      out.scan[i].timeIndex = DoubleArray.findIndexOfMaximum(out.scan[i].metrics);
      out.scan[i].intensity = extractColumn(out.scan[i].counts, out.scan[i].timeIndex);
    }

    extractRawSpectrum(ws, out.scan[0]);
    extractRawSpectrum(ws, out.scan[1]);

    return out;
  }

  public void extractRawSpectrum(PanelWorkspace[] ws, ComplementaryScan scan)
  {
    int t = scan.timeIndex;
    scan.sampleSpectrum = new double[9];
    scan.bkgSpectrum = new double[9];
    scan.bkgSpectrum2 = new double[9];
    for (int i = 0; i < nPanels; i++)
    {
      DoubleArray.addAssign(scan.sampleSpectrum, ws[i].sampleData[t]);
      DoubleArray.addAssign(scan.bkgSpectrum, ws[i].backgroundSpectrum);
      DoubleArray.addAssign(scan.bkgSpectrum2, ws[i].suppressedBackgroundSpectrum);
    }
    // Sample is in counts and bkg is in rate.
    DoubleArray.divideAssign(scan.bkgSpectrum, 10);
    DoubleArray.divideAssign(scan.bkgSpectrum2, 10);
  }

  public static double[] extractColumn(double[][] d, int index)
  {
    double[] out = new double[d.length];
    for (int i = 0; i < out.length; ++i)
    {
      out[i] = d[i][index];
    }
    return out;
  }

  public double[] complementaryMetric(PanelWorkspace[] ws, ComplementaryScan scan2)
  {
    double[][] scan = scan2.counts;
    double[] scale = scan2.mfScale;
    int n = ws[0].sampleData.length;
    double[] out = new double[n];
    for (int i = 0; i < n; i++)
    {
      double mx = 0;
      double t1 = 0;
      double t2 = 0;
      for (int j = 0; j < nPanels; j++)
      {
        double sm = DoubleArray.sum(ws[j].sampleData[i]);
        t1 += scan[j][i] / scale[j];
        t2 += sm;
        mx = Math.max(scan[j][i] / Math.sqrt(sm) / scale[j], mx);
      }
      out[i] = Math.max(t1 / Math.sqrt(t2), mx);
    }
    return out;
  }

  /**
   * Testing routine for MATLAB.
   *
   * @param targets
   * @return
   */
  public double[][] scanTest(PanelWorkspace[] ws, double[] targets)
  {
    double[][] out2 = new double[4][];
    int N = ws[0].sampleData.length;
    int i4 = 0;
    for (int i1 = 0; i1 < 4; i1++)
    {
      double[] out = new double[N];
      MatchedFilter mf = MatchedFilter.create(targets, ws[i1].backgroundSpectrum, ws[i1].suppressedBackgroundSpectrum);
      for (int i3 = 0; i3 < N; ++i3)
      {
        out[i3] = mf.apply(ws[i1].sampleData[i3]);

      }
      out2[i4++] = out;
    }
    return out2;
  }

  /**
   * Computes a spectral shape that best eliminates a source at a position. The
   * extracted source will be used to perform a second pass that eliminates this
   * strongest source. We use this form rather then the full excess counts
   * extraction because we have assumed there is a second source, then those
   * excess counts will be assigned the first source if we performed a full
   * extraction.
   *
   * @param position is the time index of the source to be extracted
   * @param target is the spectral template of the suspected source.
   * @return
   */
  public double[] extractSource1(PanelWorkspace[] ws, int position, double[] target)
  {
    try
    {
      double[] out = new double[9];
      double[] aveBackground = new double[9];

      double cum = 0;
      // For each of the panels, extract the estimated counts in background and the source
      for (int i = 0; i < nPanels; ++i)
      {
        double[] sample = ws[i].sampleData[position];
        double[] background = ws[i].backgroundEstimate[position];
        DoubleArray.addAssign(aveBackground, background);
        MatchedFilter mf1 = MatchedFilter.createDirect(target, new double[][]
        {
          background
        }, background, background);  // mf1 selects the target, rejecting background
        MatchedFilter mf2 = MatchedFilter.createDirect(background, new double[][]
        {
          target
        }, background, background);  // mf2 selects background, rejecting target
        // Counts in source and counts in panel
        double k1 = Math.max(mf1.getCounts(sample), 0);
        double k2 = Math.max(mf2.getCounts(sample), 0);

        // Compute the inverse to solve each of the channels
        double sb = DoubleArray.sum(background);
        k2 = (sb + k2) / 2;
        cum += k1 * k1;
        for (int j = 0; j < 9; j++)
        {
          out[j] += k1 * (sample[j] - k2 * background[j] / sb);
        }
      }

      if (cum == 0)
      {
        return target.clone();
      }

      DoubleArray.divideAssign(out, cum);

      // if background was over-suppressed, add some back to get a positive spectrum
      double correctionFactor = 0;
      for (int i = 0; i < 9; i++)
      {
        if (out[i] <= 0 && aveBackground[i] <= 0)
        {
          out[i] = 0;
        }
        if (out[i] >= 0 || aveBackground[i] <= 0)
        {
          continue;
        }
        correctionFactor = Math.max(-out[i] / aveBackground[i], correctionFactor);
      }

      if (correctionFactor > 0.01)
      {
        return DoubleArray.assign(out, target);
      }
      if (correctionFactor > 0)
      {
        out = DoubleArray.addAssignScaled(out, aveBackground, correctionFactor);
      }
      return out;
    }
    catch (SizeException ex)
    {
      throw new RuntimeException(ex);
    }
  }

  public class ExtractSource2Results implements Serializable
  {
    public double[][] audit1 = new double[4][];
    public double[][] audit2 = new double[4][];
    public double[][] audit3 = new double[4][];
    public double[][] audit4 = new double[4][];
    public double[] src2;
    public double[] src1;
    public double limit1;
    public double limit2;
  }

  /**
   * Performs a joint separation in spectral space. The extraction is based on
   * the number of excess counts computed at each time step.
   *
   * @param target1 is the source template found in the first pass.
   * @param target2 is the source template found in the second pass.
   * @param previous was the original estimate of the source in the first pass.
   * @param limit1 is the score range to include of times representing the first
   * source
   * @param limit2 is the score range to include the times representing the
   * second source.
   * @return
   * @throws AnalysisException
   */
  public ExtractSource2Results extractSource2(PanelWorkspace[] ws,
          double[] target1, double[] target2,
          double[] previous,
          double limit1, double limit2) throws AnalysisException
  {
    // FIXME, this is currently operating on the rolling window data as if
    // it were independent.  This is not ideal and may be able to be improved.
    try
    {
      ExtractSource2Results results = new ExtractSource2Results();

      results.limit1 = limit1;
      results.limit2 = limit2;

      double[] t1 = target1.clone();
      double[] t2 = target2.clone();
      if (DoubleArray.equivalent(t1, t2) == true)
      {
        t1 = previous.clone();
      }
      DoubleArray.normColumns1(t1);
      DoubleArray.normColumns1(t2);

      Matrix V1 = MatrixFactory.newMatrix(2, 2);
      Matrix V2 = MatrixFactory.newMatrix(9, 2);

      for (int i = 0; i < nPanels; i++)
      {
        // Compute the correlation matrix between the outputs of the matched filters 
        // and the targets with unit counts.  This matrix will be used to 
        // convert the scores into counts.
        Matrix D = MatrixFactory.newMatrix(2, 2);
        MatchedFilter mf1 = MatchedFilter.create(t1, ws[i].backgroundSpectrum, ws[i].suppressedBackgroundSpectrum);
        MatchedFilter mf2 = MatchedFilter.create(t2, ws[i].backgroundSpectrum, ws[i].suppressedBackgroundSpectrum, previous);

        // Compute the correlation matrix
        D.set(0, 0, mf1.apply(t1));
        D.set(0, 1, mf1.apply(t2));
        D.set(1, 0, mf2.apply(t1));
        D.set(1, 1, mf2.apply(t2));

        Matrix iD = MatrixOps.invert(D);
        int N = ws[i].sampleData.length;

        if (Double.isNaN(D.get(0, 0)))
        {
          mf1.dump(System.out);
          System.out.println("iD: ");
          MatrixOps.dump(System.out, iD);
          System.out.println();
        }

        // Set up for auditing
        double[] audit1 = new double[N];
        double[] audit2 = new double[N];
        double[] audit3 = new double[N];
        double[] audit4 = new double[N];
        double[] c = new double[2];

        // Scan the data
        for (int j = 0; j < N; j++)
        {
          // Alias the source the background
          double[] s = ws[i].sampleData[j];
          double[] b = ws[i].backgroundEstimate[j];

          // Apply the matched filters
          c[0] = mf1.apply(s);
          c[1] = mf2.apply(s);
          audit1[j] = c[0];
          audit2[j] = c[1];

          // If neither source is present at this time slice we can skip it.
          if (c[0] < limit1 && c[1] < limit2)
          {
            audit3[j] = -1;
            audit4[j] = -1;
            continue;
          }

          // Compute a running sum of the correlation between the 
          // extracted counts and the spectrum of excess counts at each 
          // time step.
          double[] v = MatrixOps.multiply(iD, c);
          audit3[j] = v[0];
          audit4[j] = v[1];
          if (v[0] < 0 && v[1] < 0)
          {
            continue;
          }

          DoubleArray.assignMaxOf(v, 0);
          double[] tmp = s.clone();

          // FIXME - special handling will be required here if the background 
          // track is just an estimate. 
          DoubleArray.subtractAssign(tmp, b);
          DoubleArray.assignMaxOf(tmp, 0);
          MatrixOps.addAssign(V1, MatrixOps.multiplyVectorOuter(v, v));
          MatrixOps.addAssign(V2, MatrixOps.multiplyVectorOuter(tmp, v));
        }

        // Maintain the audit log.
        results.audit1[i] = audit1;
        results.audit2[i] = audit2;
        results.audit3[i] = audit3;
        results.audit4[i] = audit4;
      }

      // Use the correlation matrix and the correlation between excess
      // counts to estimate the spectrum
      MatrixOps.addAssign(MatrixViews.diagonal(V1), 1e-6);
      Matrix R = MatrixOps.multiply(V2, MatrixOps.invert(V1));
      results.src1 = R.copyColumnTo(new double[R.rows()], 0, 0);
      results.src2 = R.copyColumnTo(new double[R.rows()], 0, 1);

      // Watch out for a failed extraction
      double d1 = DoubleArray.sum(results.src1);
      double d2 = DoubleArray.sum(results.src2);
      if (d1 == 0)
      {
        results.src1 = previous;
      }
      if (d2 == 0)
      {
        results.src2 = t2;
      }

//      if (DoubleUtilities.isnan(d1) || DoubleUtilities.isnan(d2))
      if (Double.isNaN(d1) || Double.isNaN(d2))
      {
        throw new AnalysisException("NaN in extractSources2");
      }
      // Normalize the results to unit counts
      DoubleArray.normColumns1(results.src1);
      DoubleArray.normColumns1(results.src2);

      // We could compute the uncertainties in each of the channels, but 
      // this is a lot of book keeping that we don't have time for right now.
      //-
      // We could check to see how the scores for the two extract sources 
      // compare with our initial guess to see if our initial guess was better.
      // This would have some preformance gains for the capability of the algorithm
      // but is currently outside of our time budget.
      return results;
    }
    catch (SizeException ex)
    {
      throw new RuntimeException(ex); // cannot happen
    }
  }

  //remove
  //@Debug public Results results;
  // Working memory for each of the panels
  public class PanelWorkspace implements Serializable
  {
    public static final long serialVersionUID = 1L;
    public double[][] sampleData;  // time averaged data
    public double[] backgroundSpectrum;
    public double[] suppressedBackgroundSpectrum;
    public double[] suppression;
    public double[][] backgroundEstimate;
  }

  public class Results implements Serializable
  {
    public static final long serialVersionUID = 2L;
    public PrimaryScan pass1;
    public PrimaryScan pass1g;
    public PrimaryScan pass2;
    public ExtractSource2Results pass3;
    public ComplementaryResults pass4;
    public double[] sourceInitial;
    public PanelWorkspace ws[]; // DEBUG
    public double[] meanBackgroundSpectrum;
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