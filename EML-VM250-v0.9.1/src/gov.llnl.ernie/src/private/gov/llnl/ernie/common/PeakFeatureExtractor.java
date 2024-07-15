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
import gov.llnl.ernie.data.Record;
import gov.llnl.ernie.data.SensorBackground;
import gov.llnl.ernie.data.SensorMeasurement;
import gov.llnl.ernie.spectral.EnergyExtractor2;
import gov.llnl.math.DoubleArray;
import gov.llnl.ernie.analysis.FeaturesDescription;
import gov.llnl.ernie.data.Lane;
import gov.llnl.ernie.data.SensorPosition;
import gov.llnl.ernie.math.Utility;
import gov.llnl.math.euclidean.Vector3;
import gov.llnl.utility.xml.bind.Reader;

/**
 *
 * @author nelson85
 */
@Reader.Declaration(pkg = ErniePackage.class, name = "peakFeatureExtractor",
        referenceable = true)
public class PeakFeatureExtractor
{
  private EnergyExtractor2 energyExtractor;
  private FeaturesDescription description;

  @Reader.Element(name = "energyExtractor")
  public void setEnergyExtractor(EnergyExtractor2 ee)
  {
    energyExtractor = ee;
  }

  public void initialize()
  {
    FeaturesDescription contents = null;
    if (energyExtractor != null)
    {
      contents = energyExtractor.getGroupDescription();
    }
    this.description = new PeakFeaturesDescription(contents);
  }

  public FeaturesDescription getGroupDescription()
  {
    return this.description;
  }

  public EnergyExtractor2 getEnergyExtractor()
  {
    return this.energyExtractor;
  }

  public PeakFeatures newFeatures()
  {
    PeakFeatures out = new PeakFeatures(description);
    if (energyExtractor != null)
    {
      out.energyFeatures = energyExtractor.newFeatures();
    }
    return out;
  }

  public PeakFeatures extract(Record record, int peakStartTime, int peakEndTime)
  {
    PeakFeatures result = new PeakFeatures(description);
    if (peakEndTime <= peakStartTime)
    {
      if (this.energyExtractor != null)
      {
        result.energyFeatures = energyExtractor.newFeatures();
      }
      return result;
    }
    Lane lane = record.getLane();

    int nsensors = lane.getGammaSensorProperties().size();
    SensorMeasurement comboMeasurement = 
            record.getCombinedGammaMeasurement(SensorPosition.ALL);
    SensorBackground comboBackground = 
            record.getCombinedGammaBackground(SensorPosition.ALL);
    double dt = comboMeasurement.getSensorProperties().getSamplePeriodSeconds();
    double[] sampleSpectrum = 
            comboMeasurement.getSpectrumRate(peakStartTime, peakEndTime);

    double sum = 0;
    double sum2 = 0;
    double sum3 = 0;
    for (int i = peakStartTime; i < peakEndTime; ++i)
    {

      double d1 = comboMeasurement.getCounts(i);
      double d2 = comboBackground.getCounts(i);
      double d = d1 - d2;
      if (d < 0)
        d = 0;
      sum += d * d;
      sum2 += d1;
      sum3 += d2;
    }

    result.peakStatistics[0] = Math.pow(sum / (peakEndTime - peakStartTime), 0.5) / dt;

    // Peak Position
    double[] panelDelta = new double[nsensors];
    double[] panelBkg = new double[nsensors];
    double[] panelData = new double[nsensors];
    double min = 0;
    for (int i = 0; i < nsensors; i++)
    {
      SensorMeasurement measurement = record.getGammaMeasurements().get(i);
      SensorBackground background = record.getGammaBackgrounds().get(i);
      panelData[i] = measurement.getCountsSum(peakStartTime, peakEndTime);
      panelBkg[i] = background.getCountsSum(peakStartTime, peakEndTime);
      panelDelta[i] = panelData[i] - panelBkg[i];
      if (panelDelta[i] < min)
        min = panelDelta[i];
    }
    if (min < 0)
    {
      DoubleArray.add(panelDelta, -min);
    }

    // Auditing
    result.peakStartTime = peakStartTime;
    result.peakEndTime = peakEndTime;
    result.panelDelta = panelDelta;
    result.panelData = panelData;
    result.panelBkg = panelBkg;
    
    // This was gross counts divided by sqrt(bkg*time)
    result.peakSignificance = (sum2-sum3) / Math.sqrt((sum3 + 1) * (peakEndTime - peakStartTime));

    double sumPanelData = DoubleArray.sum(panelDelta);
    double sumBottom = Utility.sum(panelDelta, lane.getGammaPanels(SensorPosition.BOTTOM));
    double sumTop = Utility.sum(panelDelta, lane.getGammaPanels(SensorPosition.TOP));
    double sumRight = Utility.sum(panelDelta, lane.getGammaPanels(SensorPosition.RIGHT));
    double sumLeft = Utility.sum(panelDelta, lane.getGammaPanels(SensorPosition.LEFT));

    if (sumPanelData > 0)
    {
      double ratio1 = (sumBottom - sumTop) / (sumPanelData + 1);
      double ratio2 = (sumRight - sumLeft) / (sumPanelData + 1);
      Vector3 lp = record.getLane().convertGammaRatios(ratio1, ratio2);
      result.peakStatistics[1] = lp.getY();
      result.peakStatistics[2] = lp.getZ();
      result.ratio1 = ratio1;
      result.ratio2 = ratio2;
    }

    // Peak Energy
    // Compute the energy spectrum for that region
    // Multichannel systems have energy spectrum so we extract more features
    if (this.energyExtractor != null)
    {
      double[] suppressedBackgroundSpectrum = 
              comboBackground.getSpectrumRate(peakStartTime, peakEndTime);
      result.energyFeatures = energyExtractor.compute(peakEndTime - peakStartTime, 1, 
              sampleSpectrum, 
              suppressedBackgroundSpectrum);
      
      // Store the spectrum and background for the display
      result.sampleSpectrum = sampleSpectrum.clone();
      result.backgroundSpectrum = suppressedBackgroundSpectrum.clone();
    }
    

    return result;
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