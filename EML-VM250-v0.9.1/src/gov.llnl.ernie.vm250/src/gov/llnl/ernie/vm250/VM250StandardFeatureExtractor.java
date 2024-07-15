/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.ernie.vm250;

import gov.llnl.ernie.analysis.AnalysisException;
import gov.llnl.ernie.data.Record;
import gov.llnl.utility.xml.bind.ReaderInfo;
import gov.llnl.ernie.data.SensorBackground;
import gov.llnl.ernie.data.SensorMeasurement;
import gov.llnl.utility.annotation.Debug;
import gov.llnl.ernie.analysis.Features;
import gov.llnl.ernie.analysis.FeaturesDescription;
import gov.llnl.ernie.analysis.StandardFeatureExtractor;
import gov.llnl.ernie.data.SensorPosition;

/**
 *
 * @author nelson85
 */
@ReaderInfo(VM250StandardFeatureExtractorReader.class)
public class VM250StandardFeatureExtractor implements StandardFeatureExtractor
{
  @Debug public int panels = 4;
  @Debug public VM250StandardFeaturesDescription description;

  @Override
  public void initialize()
  {
    description = new VM250StandardFeaturesDescription(panels);
  }

  @Override
  public FeaturesDescription getDescription()
  {
    return description;
  }

  @Override
  public VM250StandardFeatures compute(Record record) throws AnalysisException
  {
    int nsensors = record.getLane().getGammaSensorProperties().size();
    VM250StandardFeatures result = new VM250StandardFeatures(description);

    double GC[] = result.grossCountMetric;

    int N = record.getGammaMeasurements().get(0).size();
    int rw = 5;
    int buffer = 20;  // Search seconds before and after vehicle passes
    int N1 = Math.max(record.getVPSMeasurement().getGammaOccupancyStart() - buffer + rw / 2 - 1, rw);
    result.N1 = N1;
    int N2 = Math.min(record.getVPSMeasurement().getGammaOccupancyEnd() + rw / 2 + buffer, N);

    if (N1 > N)
    {
      throw new AnalysisException("Panel data is too short");
    }

    for (int i = 0; i < nsensors; ++i)
    {
      SensorBackground sr = record.getGammaBackgrounds().get(i);
      SensorMeasurement pd = record.getGammaMeasurements().get(i);
      GC[i] = computeGrossMetric(sr, pd, rw, N1, N2);
    }
    GC[nsensors] = computeGrossMetric(record.getCombinedGammaBackground(SensorPosition.ALL),
            record.getCombinedGammaMeasurement(SensorPosition.ALL), rw, N1, N2);

    return result;
  }

  @Override
  public Features newFeatures()
  {
    return new VM250StandardFeatures(description);
  }

  void setPanels(int panels)
  {
    this.panels = panels;
  }

//<editor-fold desc="internal" defaultstate="collapsed">

  // Verified
  private static double computeGrossMetric(SensorBackground sr, SensorMeasurement pd,
          int rw, int N1, int N2)
  {
    double out = -1000;
    double bkg = sr.getExpectedCountRate() * rw * pd.getSensorProperties().getSamplePeriodSeconds();
    double sbkg = Math.sqrt(bkg);

    // implement rolling filter initialization
    int sum = 0;
    for (int j = N1 - rw; j < N1; j++)
    {
      sum += pd.getCounts(j);
    }

    for (int j = N1; j < N2; ++j)
    {
      // Update the rolling window
      sum += pd.getCounts(j);
      sum -= pd.getCounts(j - rw);

      // Compute the decision metric
      double decisionMetric = (sum - bkg) / sbkg;

      // Update values
      if (out < decisionMetric)
      {
        out = decisionMetric;
      }
    }
    return out;
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