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

import gov.llnl.ernie.ErniePackage;
import gov.llnl.math.DoubleArray;
import static gov.llnl.math.DoubleUtilities.sqr;
import gov.llnl.math.MathExceptions.SizeException;
import gov.llnl.math.matrix.Matrix;
import gov.llnl.math.matrix.MatrixFactory;
import gov.llnl.math.matrix.MatrixOps;
import gov.llnl.utility.InitializeException;
import gov.llnl.utility.xml.bind.ReaderInfo;
import gov.llnl.utility.xml.bind.WriterInfo;
import java.util.ArrayList;
import java.io.Serializable;
import java.util.logging.Level;
import gov.llnl.ernie.analysis.FeaturesDescription;

/**
 * This featured extractor converse the energy vector extracted from a source
 * into
 *
 * @author nelson85
 */
@ReaderInfo(EnergyExtractor2Reader.class)
@WriterInfo(EnergyExtractor2Writer.class)
public class EnergyExtractor2 implements Serializable
{
  private static final long serialVersionUID
          = gov.llnl.utility.UUIDUtilities.createLong("EnergyExtractor2-v1");

  public double[] energyFactors;
  public Matrix pcaTransform;
  public Matrix dataMean;
  public Matrix coh;
  public ArrayList<RatioTest> ratioTests = new ArrayList<>();
  public BackgroundHypothesisTest backgroundHypothesisTest
          = new BackgroundHypothesisTest();
  public ArrayList<HypothesisTest> hypothesisTests = new ArrayList<>();
  private EnergyFeaturesDescription description;

  public EnergyExtractor2()
  {
  }

  /**
   * Verify all of the required data structures are set up properly.
   *
   */
  public void initialize() throws InitializeException
  {

    int pcaFeatures = 0;
    if (energyFactors == null)
    {
      ErniePackage.LOGGER.info("Energy factors not configured");
//      throw new InitializeException("energyFactors not configured");
    }
    if (pcaTransform == null)
    {
      ErniePackage.LOGGER.info("Energy PCA transform not configured");
//      throw new InitializeException("pcaTransform not configured");
    }
    else
    {
      pcaFeatures = pcaTransform.rows();
      // These are only needed if pcaTransform is configured
      if (dataMean == null)
      {
        throw new InitializeException("dataMean not configured");
      }
      if (coh == null)
      {
        throw new InitializeException("coh not configured");
      }
    }
    ErniePackage.LOGGER.log(Level.FINE,
            "FeatureExtractorEnergy2: all data matrices initialized");
    this.description = new EnergyFeaturesDescription(pcaFeatures,
            this.ratioTests.size(), this.getHypothesisTitles());
  }

  public FeaturesDescription getGroupDescription()
  {
    return this.description;
  }

  public EnergyFeatures compute(int samples, double factor, 
          double[] sampleRate, 
          double[] backgroundRate)
  {

    int n = sampleRate.length;

    // Allocate the result matrix
    EnergyFeatures out = new EnergyFeatures(this.description);

    // Compute the modified energy factors for background
    double[] backgroundRateEstimate;
    if (energyFactors != null && factor != 1)
    {
      backgroundRateEstimate = new double[9];
      for (int i = 0; i < n; ++i)
      {
        backgroundRateEstimate[i] = (energyFactors[i] * (factor - 1) + 1) * backgroundRate[i];
      }
    }
    else
    {
      backgroundRateEstimate = backgroundRate.clone();
    }

    // Compute the source rate estimate
    double sampleRateEstimate[] = new double[9];
    for (int i = 0; i < n; ++i)
    {
      //Negatives should not exist in source rate estimates
      sampleRateEstimate[i] = Math.max(sampleRate[i] - backgroundRateEstimate[i], 0);
    }

    if (this.pcaTransform != null)
    {
      try // Compute the PCA features
      {
        Matrix normalizedEstimate = MatrixFactory.createColumnVector(sampleRateEstimate);
        double sourceRateEstimate = Math.sqrt(DoubleArray.sumSqr(sampleRateEstimate));
        if (sourceRateEstimate > 0.0)
        {
          MatrixOps.divideAssign(normalizedEstimate, sourceRateEstimate);
        }

        //PCA Coherence
        MatrixOps.subtractAssign(normalizedEstimate, dataMean);
        Matrix tmp = MatrixOps.multiply(pcaTransform, coh);

        //  pcaFeatures=T*(S/|S|);
        Matrix pca = MatrixOps.multiply(tmp, normalizedEstimate);

        // Copy into out
        out.pcaFeatures = pca.copyColumn(0);
      }
      catch (SizeException ex)
      {
        throw new RuntimeException(ex);
      }

    }
    else
    {
      out.pcaFeatures = new double[0];
    }

    double[] sampleCounts = new double[9];
    double[] backgroundCounts = new double[9];
    for (int i = 0; i < n; i++)
    {
      sampleCounts[i] = sampleRate[i] * samples;
      backgroundCounts[i] = backgroundRateEstimate[i] * samples;
    }

    {
      int offset = 0;
      out.ratioFeatures = new double[ratioTests.size()];
      for (RatioTest test : ratioTests)
      {
        out.ratioFeatures[offset++] = test.evaluate(sampleCounts, backgroundRate);
      }
    }

    // Compute the Hypothsis Features
    {
      double k = 0;
      HypothesisResult result = new HypothesisResult();

      int offset = 0;
      for (HypothesisTest test : this.hypothesisTests)
      {
        test.evaluate(result, sampleCounts, backgroundCounts, samples);
        k = Math.max(k, result.k / samples);
        out.hypothesisFeatures[offset] = result.distance;
        offset++;
      }
      out.k = k;
      offset++;

      // Evaulate the background hypothesis
      backgroundHypothesisTest.evaluate(result, sampleCounts, backgroundRate, backgroundRateEstimate, samples);
      out.background = result.distance;
    }

    // Return the result
    return out;
  }

  public EnergyFeatures newFeatures()
  {
    return new EnergyFeatures(this.description);
  }

  public String[] getHypothesisTitles()
  {
    return hypothesisTests.stream().map(w -> w.title).toArray(String[]::new);
  }

//<editor-fold desc="internal classes" defaultstate="collapsed">
  public static class RatioTest implements Serializable
  {
    int window[] = new int[9];

    public double evaluate(double sampleCounts[], double backgroundRateEstimate[])
    {
      double Ml = 0;
      double Mh = 0;
      double Bl = 0;
      double Bh = 0;
      for (int i = 0; i < window.length; ++i)
      {
        if (window[i] == 1)
        {
          Ml += sampleCounts[i];
          Bl += backgroundRateEstimate[i] * 10;
        }
        if (window[i] == -1)
        {
          Mh += sampleCounts[i];
          Bh += backgroundRateEstimate[i] * 10;
        }
      }
      Ml = Math.max(Ml, 1e-5);
      Mh = Math.max(Mh, 1e-5);
      Bl = Math.max(Bl, 1e-5);
      Bh = Math.max(Bh, 1e-5);

      double sigma = Ml + sqr(Bl * Mh / Bh) * (1 / Mh + 1 / Bl + 1 / Bh);
      return (Ml - Mh * (Bl / Bh)) / Math.sqrt(sigma);
    }

  }

  public static class HypothesisResult implements Serializable
  {
    public double k;
    public double distance;
  }

  public HypothesisResult allocateResult()
  {
    return new HypothesisResult();
  }

  public static class HypothesisTest implements Serializable
  {
    public String title = null;
    public Matrix mean = null;
    public Matrix cov = null;

    public void evaluate(
            HypothesisResult result, 
            double[] sampleCounts, 
            double[] backgroundCounts, 
            int samples)
    {
      try
      {
        int n = sampleCounts.length;
        double sampleIntensity = DoubleArray.sum(sampleCounts);

        double source[] = new double[n];

        for (int i = 0; i < n; i++)
        {
          source[i] = Math.max(sampleCounts[i] - backgroundCounts[i], 0);
        }

        // Compute inverse sigma
        // Sigma=diag(max([y1 b1],[],2))+intensity1^2*Cov{j};
        Matrix sigma = MatrixFactory.newMatrix(n, n);
        if (cov != null)
        {
          MatrixOps.addAssign(sigma, cov);
          MatrixOps.multiplyAssign(sigma, sampleIntensity * sampleIntensity);
        }
        double base = 0;
        for (int i = 0; i < n; ++i)
        {
          double b = Math.max(sampleCounts[i], backgroundCounts[i]);
          base+=b;
          sigma.set(i, i, sigma.get(i, i) + b);
        }
        Matrix invSigma = MatrixOps.invert(sigma);

        // Compute k
        double k = 0;
        Matrix err = MatrixFactory.newMatrix(n, 1);
        Matrix r2 = MatrixOps.multiply(invSigma, mean).transpose();
        double r3 = MatrixOps.multiply(r2, source)[0];
        double r4 = MatrixOps.multiply(r2, mean).get(0, 0);

        k = r3 / r4;
        result.k = k / samples;
        double min = 2*Math.sqrt(base);
        if (k < min)
        {
          k = min;
        }

        for (int i = 0; i < n; ++i)
        {
          err.set(i, 0, (sampleCounts[i] - backgroundCounts[i]) - k * mean.get(i, 0));
        }

        Matrix r5 = MatrixOps.multiply(invSigma, err);
        double distance = MatrixOps.multiply(r5.transpose(), err).get(0, 0);

        // Copy back the result
        result.distance = distance / (n - 1);
      }
      catch (SizeException ex)
      {
        // This cannot happen because the covariance matrix has already been checked
        throw new RuntimeException(ex);
      }
    }
  }

  public static class BackgroundHypothesisTest implements Serializable
  {
    public Matrix cov = null;

    /**
     *
     * @param result
     * @param sample is the sample spectrum in rate
     * @param background1 is the background spectrum as a rate
     * @param background2 is the suppressed background spectrum as a rate
     * @param samples is the number of samples added to make the sample spectrum
     *
     */
    public void evaluate(HypothesisResult result,
            double sample[],
            double background1[], double background2[],
            int samples)
    {
      try
      {
        int n = sample.length;
        double sampleIntensity = DoubleArray.sum(sample);
        Matrix bkg = MatrixFactory.newMatrix(n, 2);
        for (int i = 0; i < n; ++i)
        {
          bkg.set(i, 0, background1[i]);
          bkg.set(i, 1, background2[i]);
        }

        // Compute inverse sigma
        // Sigma=diag(max([y1 b1],[],2))+intensity1^2*Cov{j};
        Matrix sigma = MatrixFactory.newMatrix(n, n);
        if (cov != null)
        {

          MatrixOps.addAssign(sigma, cov);
          MatrixOps.multiplyAssign(sigma, sampleIntensity * sampleIntensity);
        }
        for (int i = 0; i < n; ++i)
        {
          sigma.set(i, i, sigma.get(i, i) + sample[i]);
        }
        Matrix invSigma = MatrixOps.invert(sigma);

        // Compute k
        Matrix V1 = MatrixOps.multiply(bkg.transpose(), invSigma);
        Matrix V2 = MatrixOps.multiply(V1, bkg);
        V2.set(1, 1, V1.get(1, 1) + 1e-6); // If bkg1==bkg2 then this is singular
        Matrix V3 = MatrixOps.multiply(V1, MatrixFactory.wrapColumnVector(sample));
        V3 = MatrixOps.divideLeft(V2, V3);

        Matrix err = MatrixFactory.newMatrix(n, 1);
        for (int i = 0; i < n; i++)
        {
          err.set(i, 0, sample[i] - V3.get(0, 0) * background1[i] - V3.get(1, 0) * background2[i]);
        }
        Matrix T1 = MatrixOps.multiply(invSigma, err);
        Matrix T2 = MatrixOps.multiply(err.transpose(), T1);

        result.k = 0;
        result.distance = T2.get(0, 0) / (n - 2); // 9 channels -2 dof
      }
      catch (SizeException ex)
      {
        // This cannot happen because the covariance matrix has already been checked
        throw new RuntimeException(ex);
      }
    }
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