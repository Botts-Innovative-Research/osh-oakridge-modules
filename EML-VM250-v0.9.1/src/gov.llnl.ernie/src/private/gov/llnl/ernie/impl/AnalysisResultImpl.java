/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.ernie.impl;

import gov.llnl.ernie.Analysis.RecommendedAction;
import gov.llnl.ernie.data.AnalysisResult;
import gov.llnl.ernie.data.AnalysisSource;
import gov.llnl.ernie.data.AnalysisContextualInfo;
import gov.llnl.ernie.data.Record;
import gov.llnl.ernie.data.ScanContextualInfo;
import gov.llnl.ernie.data.VehicleInfo;
import gov.llnl.ernie.data.VendorAnalysis;
import gov.llnl.math.matrix.Matrix;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import gov.llnl.ernie.analysis.Features;
import gov.llnl.math.euclidean.Vector3;

/**
 * Concrete implementation for analysis results.
 *
 * FIXME this class may need to be different for different vendors. We will use
 * a common implementation for now.
 *
 * @author nelson85
 */
public class AnalysisResultImpl implements AnalysisResult
{
  // Plotting
  public Record currentRecord;
  public double position[];  // in m
  public Matrix background;

  double threshold;
  double metrics[]; // FIXME should have separate metrics for each classifier

  // CMU Probability
  private double probabilityRelease;
  private double probabilityInvestigate;

  boolean neutronAlarm = false;
  boolean valid = false;

  boolean verbose = true;
  public AnalysisSource sourceFull;
  boolean fallback;

  ArrayList<AnalysisSource> sources;
  List<Features> features = new ArrayList<>();

  public Vector3 neutronLocation;
  String fault;
  
  // FIXME team ernie, there's no setting nor vairables are ever assigned anywhere
  // it's always null
  private VendorAnalysis vendorAnalysis;
  private ScanContextualInfo scanContextualInfo;
  private AnalysisContextualInfo analysisContextualInfo;

  public AnalysisResultImpl()
  {
    sources = new ArrayList<>();
  }
  
// Needed for ERNIE Recommendation
  /**
   * Gives the last record called with processRecord. For use by the display.
   *
   * @return
   */
  @Override
  public Record getRecord()
  {
    return this.currentRecord;
  }

//  /**
//   * Returns the background computed for the current record. For use by the
//   * display.
//   *
//   * @return
//   */
//  @Override
//  public Matrix getBackground()
//  {
//    return background;
//  }
  @Override
  public boolean getNeutronAlarm()
  {
    return this.neutronAlarm;
  }

  @Override
  public double getProbabilityInvestigate()
  {
    return this.probabilityInvestigate;
  }

  /**
   * Determine the recommended action for ERNIE. Returns true if this is either
   * a neutron alarm or the analysis completed and indicates an alarm, or the
   * fallback indicate the existing portal settings indicate an alarm.
   *
   * @return
   */
  @Override
  public RecommendedAction getRecommendedAction()
  {
    if (neutronAlarm == true)
    {
      return RecommendedAction.INVESTIGATE;
    }
    if (this.probabilityInvestigate > threshold)
    {
      return RecommendedAction.INVESTIGATE;
    }
    return RecommendedAction.RELEASE;
  }

  /**
   * Returns number of sources detected
   *
   * @return
   */
  @Override
  public int getNumberOfSources()
  {
    return this.sources.size();
  }

  /**
   * @param i index of source to return
   * @return
   */
  @Override
  public AnalysisSource getSource(int i)
  {
    return this.sources.get(i);
  }

  /**
   * The Total Source detected
   *
   * @return
   */
  @Override
  public AnalysisSource getFullSource()
  {
    return this.sourceFull;
  }

  /**
   * Returns true if the Ernie algorithm processed the record successfully.
   *
   * @return
   */
  @Override
  public boolean isValid()
  {
    return this.valid;
  }

  /**
   * Get the probability of release. This only has a proper value if Ernie
   * completed analysis successfully. For general use, method
   * 'getRecommendedAction' should be used instead.
   *
   * @return
   */
  @Override
  public double getProbabilityRelease()
  {
    return probabilityRelease;
  }

  /**
   * @return true if fallback was executed on this record being processed; i.e.
   * analysis failed.
   */
  @Override
  public boolean isFallback()
  {
    return fallback;
  }

  public void saveToXML(java.io.File outputFile) throws JAXBException
  {
    JAXBContext context = JAXBContext.newInstance(AnalysisResultImpl.class);

    Marshaller m = context.createMarshaller();
    m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

    m.marshal(this, outputFile);
  }

  public void addFeatures(Features group)
  {
    features.add(group);
  }

  public void clearFeatures()
  {
    features.clear();
  }

  @Override
  public Collection<Features> getFeatures()
  {
    return this.features;
  }

  public <T> T findFeatures(Class<? extends Features> cls)
  {
    for (Features group : this.features)
    {
      if (cls.isAssignableFrom(group.getClass()))
      {
        return (T) group;
      }
    }
    return null;
  }

  public void addSource(AnalysisSource source)
  {
    this.sources.add(source);
  }

  //***** Information for the graphs 
  // vectors are all N=record.getLength()
  // Contains the measured counts per 0.1 interval by panel, by channel
  //   panelData[panels=1..4].gammaData[time 1..N][channel 1..9]
  //   Gross gamma
  //      int[] record.combinedPanelData.gross() => double []
  //   Per panel
  //      int[] record.panelData[i].gross() => double []
  //   Energy by time
  //      record.combinedPanelData.getGammaSpectrum(time=1..N)
  //
  // panel order is bottom driver, bottom passanger, top driver, top passanger
  //public Record currentRecord;
  // Background counts per 0.1 interval
  //   background[time 1..N][4]
  //   Gross 
  //     background background.sumOfColumns()
  //   Per panel
  //     background.get(1..N,panel)
  //public MatrixColumnTable background;
  // Position by 0.1 interval
  //   position[1..N]
  //public double position[];  // in m
  // Where do we get spectra?  spectrum[1..9]
  //  For now we will always use statistica features (later compact/dist)
  //    statisticalFeatures.sampleSpectrum
  //    statisticalFeatures.backgroundSpectrum
  // Where do we get vehicleInfo?
  //   vehicleFeatures.overhang
  //   vehicleFeatures.cabinLength
  //   vehicleFeatures.trailerLength
  //   vehicleFeatures.vehicleLength
  // Where do we get radiation locations
  //   If compact source (from CMU data)
  //     fittingFeatures.compact.features.px
  //     fittingFeatures.compact.features.py
  //     fittingFeatures.compact.features.pz
  //   If distributed source (from CMU data)
  //     fittingFeatures.distribured.features.px[1,2]
  //     fittingFeatures.distribured.features.py
  //     fittingFeatures.distribured.features.pz
  @Override
  public <T extends Features> T getFeatureGroup(Class<T> cls)
  {
    throw new UnsupportedOperationException("Unused but required by interface AnalysisResult.");
  }

  public void setFallback(boolean b)
  {
    this.fallback = b;
  }

  public void setValid(boolean b)
  {
    this.valid = b;
  }

  public void setNeutronAlarm(boolean b)
  {
    this.neutronAlarm = b;
  }

  @Override
  public String getFaultMessage()
  {
    return fault;
  }
  
  public void setFaultMessage(String message)
  {
    fault = message;
  }

  @Override
  public AnalysisContextualInfo getAnalysisContextualInfo()
  {
    return this.analysisContextualInfo;
  }

  @Override
  public ScanContextualInfo getScanContextualInfo()
  {
    return this.scanContextualInfo;
  }

  @Override
  public VendorAnalysis getVendorAnalysis()
  {
    return this.vendorAnalysis;
  }

  @Override
  public VehicleInfo getVehicleInfo()
  {
    throw new UnsupportedOperationException("Unused but required by interface AnalysisResult.");
  }

  /**
   * @param probabilityRelease the probabilityRelease to set
   */
  public void setProbabilityRelease(double probabilityRelease)
  {
    this.probabilityRelease = probabilityRelease;
  }

  /**
   * @param probabilityInvestigate the probabilityInvestigate to set
   */
  public void setProbabilityInvestigate(double probabilityInvestigate)
  {
    this.probabilityInvestigate = probabilityInvestigate;
  }

  public double getThreshold()
  {
    return threshold;
  }
  
  public void setThreshold(double threshold)
  {
    this.threshold = threshold;
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