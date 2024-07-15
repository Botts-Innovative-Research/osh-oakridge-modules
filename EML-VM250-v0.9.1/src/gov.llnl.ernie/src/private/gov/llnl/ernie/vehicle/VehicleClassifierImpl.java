/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.ernie.vehicle;

import gov.llnl.ernie.Analysis;
import gov.llnl.ernie.Fault;
import gov.llnl.ernie.analysis.AnalysisException;
import gov.llnl.ernie.analysis.AnalysisPreprocessor;
import gov.llnl.ernie.analysis.VehicleClassifier;
import gov.llnl.ernie.data.Record;
import gov.llnl.ernie.data.VehicleClass;
import gov.llnl.ernie.data.VehicleRepresentative;
import gov.llnl.utility.InitializeException;
import gov.llnl.utility.xml.bind.ReaderInfo;
import gov.llnl.utility.xml.bind.WriterInfo;
import java.util.LinkedList;
import java.util.List;
import gov.llnl.ernie.data.VehicleClassifierExtractor;

/**
 *
 * @author nelson85
 */
@ReaderInfo(VehicleClassifierReader.class)
@WriterInfo(VehicleClassifierWriter.class)
public class VehicleClassifierImpl implements VehicleClassifier, AnalysisPreprocessor
{
  public VehicleClassifierExtractor extractor;
  public List<VehicleClass> classDatabase = new LinkedList<>();
  public double threshold = 0.8;
  private boolean renormalizeTemplates = true;
  private Fault badVehicleFault;

  @Override
  public void initialize(Analysis analysis) throws InitializeException
  {
    if (renormalizeTemplates)
    {
      classDatabase.forEach((VC) ->
      {
        ((VehicleClassImpl) VC).renormalizeTemplate();
      });
    }
    this.badVehicleFault = (Fault) Enum.valueOf(analysis.getFaultClass(), "UNABLE_TO_CLASSIFY_VEHICLE");
  }

  @Override
  public void compute(Record record)
  {
    Output output = null;
    try
    {
      output = this.classifyVehicle(record);
    }
    catch (AnalysisException ex)
    {
      // Ignore exception taken care of in the conditoinal statement below
    }

    if (output != null)
    {
      record.setVehicleClass(output.getVehicleClass());
    }
    else
    {
      record.addFault(badVehicleFault);
    }
  }

  @Override
  public Output classifyVehicle(Record record) throws AnalysisException
  {
    double[] trace = this.extractor.extract(record);
    VehicleClass vc = classify(trace);
    return new OutputImpl(vc, trace);
  }

  class OutputImpl implements Output
  {
    final private double[] features;
    final private VehicleClass vehicleClass;

    OutputImpl(VehicleClass cls, double[] features)
    {
      this.vehicleClass = cls;
      this.features = features;
    }

    @Override
    public VehicleClass getVehicleClass()
    {
      return this.vehicleClass;
    }

    @Override
    public VehicleRepresentative getVehicleRepresentative()
    {
      return null;
    }

    @Override
    public double[] getFeatures()
    {
      return this.features;
    }

  }

  // FIXME this assumes that template is centered with values between 0 and 2
  // but this is not the case for normalized records.
  public double getCorrelation(Record record, VehicleClass cls)
  {
    double[] trace = this.extractor.extract(record);

    return getCorrelation(trace, cls);
  }

  public static double getCorrelation(double[] trace, VehicleClass cls)
  {
    double maxCorr = -Double.MAX_VALUE;
    for (VehicleRepresentative rep : cls.getRepresentatives())
    {
      double[] template = rep.getFeatures();
      double m1 = 0;
      double m2 = 0;
      double m3 = 0;
      for (int i = 0; i < trace.length; ++i)
      {
        m1 += trace[i] * template[i];
        m2 += trace[i] * trace[i];
        m3 += template[i] * template[i];
      }
      double corr = m1 / Math.sqrt(m2 * m3);
      if (corr > maxCorr)
        maxCorr = corr;
    }
    return maxCorr;
  }

  /**
   * Classified from extracted vehicle trace.
   *
   * Used by proj-ernie4/py/rpm8/buildBackgroundModel.py
   *
   * @param trace
   * @return
   */
  public VehicleClass classify(double trace[])
  {
    if (trace == null)
    {
      return null;
    }

    double maxCorrelation = 0;
    VehicleClass best = null;
    for (VehicleClass entry : classDatabase)
    {
      double correlation = getCorrelation(trace, entry);
      if (correlation > maxCorrelation)
      {
        maxCorrelation = correlation;
        best = entry;
      }
    }

    // If the scan does not fit the pattern we need to fallback
    if (maxCorrelation < threshold)
    {
      return null;
    }
    return best;
  }

  @Override
  public VehicleClass getVehicleClass(int i)
  {
    return this.classDatabase.get(i - 1); // adjust for 1-based index in vehicleClassDatabase
  }

  public void add(VehicleClass vc)
  {
    this.classDatabase.add(vc);
  }

  @Override
  public int getNumVehicleClasses()
  {
    return classDatabase.size();
  }

  public void setFeatureExtractor(VehicleClassifierExtractor extractor)
  {
    this.extractor = extractor;
  }

  public VehicleClassifierExtractor getFeatureExtractor()
  {
    return this.extractor;
  }

  public void setRenormalizeTemplates(boolean renormalizeTemplates)
  {
    this.renormalizeTemplates = renormalizeTemplates;
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