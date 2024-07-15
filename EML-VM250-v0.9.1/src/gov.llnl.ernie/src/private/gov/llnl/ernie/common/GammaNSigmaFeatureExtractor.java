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
import gov.llnl.ernie.data.Record;
import gov.llnl.ernie.data.SensorMeasurement;
import gov.llnl.ernie.analysis.Features;
import gov.llnl.ernie.analysis.FeaturesDescription;
import gov.llnl.utility.xml.bind.Reader;

/**
 * Duplicate the NSigma alarm algorithm used for VM250 (TSA) portals. Three
 * features are extracted: - for sum of all four panels, - for sum of left two
 * panels, - for sum of right two panels
 *
 * For each, the feature is the max(k-sigma), where k-sigma is computed for a
 * 1-second rolling average as (count - gamma_background) /
 * sqrt(gamma_background)
 *
 * @author mattoon1
 */
@Reader.Declaration(pkg = ErniePackage.class, name = "gammaNSigmaFeatureExtractor")
public class GammaNSigmaFeatureExtractor implements FeatureExtractor
{
  final static FeaturesDescription DESCRIPTION = new GammaNSigmaFeaturesDescription();

  @Override
  public void initialize()
  {
    // NOT USED
  }

  @Override
  public FeaturesDescription getDescription()
  {
    return DESCRIPTION;
  }

  public GammaNSigmaFeatures compute(Record record) throws AnalysisException
  {
    GammaNSigmaFeatures result = new GammaNSigmaFeatures();

    if (record.getLane().getGammaSensorProperties().size() != 4)
    {
      throw new RuntimeException("NSigmaFeatureExtractor requires 4 panels");
    }

    int N = record.getGammaMeasurements().get(0).size();
    int rw = 5;

    result.master = new double[N - rw];
    result.slave = new double[N - rw];
    result.allPanels = new double[N - rw];
    result.bgRatio = new double[N - rw];

    // for VM250 we are using the panel's estimated background rate rather than re-computing:
    double masterGB = record.getGammaBackgrounds().get(0).getExpectedCountRate() + record.getGammaBackgrounds().get(1).getExpectedCountRate();
    double slaveGB = record.getGammaBackgrounds().get(2).getExpectedCountRate() + record.getGammaBackgrounds().get(3).getExpectedCountRate();
    double allGB = masterGB + slaveGB;

    double sqrtMaster = Math.sqrt(masterGB);
    double sqrtSlave = Math.sqrt(slaveGB);
    double sqrtAll = Math.sqrt(allGB);

    SensorMeasurement measurement0 = record.getGammaMeasurements().get(0);
    SensorMeasurement measurement1 = record.getGammaMeasurements().get(1);
    SensorMeasurement measurement2 = record.getGammaMeasurements().get(2);
    SensorMeasurement measurement3 = record.getGammaMeasurements().get(3);

    // initialize rolling filters:
    int all, master = 0, slave = 0;
    for (int i = 0; i < rw; i++)
    {
      master += measurement0.getCounts(i);
      master += measurement1.getCounts(i);
      slave += measurement2.getCounts(i);
      slave += measurement3.getCounts(i);
    }
    all = master + slave;

    int i;
    for (i = 0; i < N - rw; ++i)
    {
      result.master[i] = computeNSigma(master, masterGB, sqrtMaster);
      result.slave[i] = computeNSigma(slave, slaveGB, sqrtSlave);
      result.allPanels[i] = computeNSigma(all, allGB, sqrtAll);
      result.bgRatio[i] = all / allGB;

      // update rolling windows:
      int dmaster = measurement0.getCounts(i + rw) - measurement0.getCounts(i)
              + measurement1.getCounts(i + rw) - measurement1.getCounts(i);
      int dslave = measurement2.getCounts(i + rw) - measurement2.getCounts(i)
              + measurement3.getCounts(i + rw) - measurement3.getCounts(i);

      master += dmaster;
      slave += dslave;
      all += dmaster + dslave;
    }

    result.maxAllPanels = result.allPanels[DoubleArray.findIndexOfMaximum(result.allPanels)];
    result.maxMaster = result.master[DoubleArray.findIndexOfMaximum(result.master)];
    result.maxSlave = result.slave[DoubleArray.findIndexOfMaximum(result.slave)];

    // Skip 1st sample to avoid potentially computing zeros due to using same
    // presamples that were used to create background estimate.
    // FIXME should skip presamples for NSigma features too, but leaving those alone for v0.9.1
    int skipSamples = 1;
    result.maxBgRatio = result.bgRatio[DoubleArray.findIndexOfMaximumRange(
            result.bgRatio, skipSamples, N-rw)];
    return result;
  }

  /*
  * For specified panels, compute NSigma as a function of time + max NSigma
   */
  static double computeNSigma(int counts, double BG, double sqrtBg)
  {
    return (counts - BG) / sqrtBg;
  }

  private static double sqr(double d)
  {
    return d * d;
  }

  @Override
  public Features newFeatures()
  {
    return new GammaNSigmaFeatures();
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