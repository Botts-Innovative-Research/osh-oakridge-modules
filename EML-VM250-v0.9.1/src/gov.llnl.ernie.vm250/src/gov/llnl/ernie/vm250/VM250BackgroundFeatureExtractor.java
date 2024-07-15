/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gov.llnl.ernie.vm250;

import gov.llnl.ernie.analysis.AnalysisException;
import gov.llnl.ernie.analysis.FeatureExtractor;
import gov.llnl.ernie.analysis.Features;
import gov.llnl.ernie.analysis.FeaturesDescription;
import gov.llnl.ernie.common.BackgroundFeatures;
import gov.llnl.ernie.common.BackgroundFeaturesDescription;
import gov.llnl.ernie.data.Record;
import gov.llnl.ernie.vm250.data.VM250RecordInternal;
import gov.llnl.math.DoubleArray;
import gov.llnl.utility.xml.bind.Reader;

/**
 *
 * @author nelson85
 */
@Reader.Declaration(pkg = ErnieVM250Package.class, name = "backgroundFeatureExtractor")
public class VM250BackgroundFeatureExtractor implements FeatureExtractor
{

  private BackgroundFeaturesDescription description;
  int panels = 4;

//<editor-fold desc="settings" defaultstate="collapsed">
  /**
   * @return the panels
   */
  public int getPanels()
  {
    return panels;
  }

  /**
   * @param panels the panels to set
   */
  @Reader.Element(name = "panels", type = double.class, required = false)
  public void setPanels(int panels)
  {
    this.panels = panels;
  }
//</editor-fold>

  @Override
  public void initialize()
  {
    this.description = new BackgroundFeaturesDescription(panels);
  }

  @Override
  public FeaturesDescription getDescription()
  {
    return description;
  }

  @Override
  public Features newFeatures()
  {
    BackgroundFeatures out = new BackgroundFeatures(description);
    return out;
  }

  @Override
  public BackgroundFeatures compute(Record record) throws AnalysisException
  {
    BackgroundFeatures result = new BackgroundFeatures(description);
    compute(result, record);
    return result;
  }

//<editor-fold desc="internal" defaultstate="collapsed">
  public void compute(BackgroundFeatures result, Record record) throws AnalysisException
  {
    VM250RecordInternal internal = (VM250RecordInternal) record.getInternal();
    int lengthBkg = 5;
    if (panels != record.getGammaMeasurements().size())
    {
      throw new AnalysisException("Panel size mismatch");
    }

    result.delta = new double[panels];
    result.panel = new double[panels];

    if (internal.getLength() <= lengthBkg)
    {
      return;
    }

    for (int i = 0; i < panels; ++i)
    {
      result.panel[i] = record.getGammaMeasurements().get(i).getCountRate(0, lengthBkg);
      result.delta[i] = result.panel[i] - internal.getSegmentDescription().getGammaBackground()[i];
      // FIXME the delta is supposed to be the difference between the short and long
      // background estimates for the panel.  Not sure how this applies to the VM250. --KEN
//      if (lengthBkg > 0)
//      {
//        double pre = record.getGammaMeasurements().get(i).getCountRate(0, lengthBkg);
//        result.delta[i] = pre - result.panel[i];
//      }
    }
    result.panelAvg = DoubleArray.mean(result.panel);
    result.deltaAvg = DoubleArray.mean(result.delta);

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