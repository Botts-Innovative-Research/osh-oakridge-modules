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
package gov.llnl.ernie.vehicle2;

import gov.llnl.ernie.Analysis;
import gov.llnl.ernie.Fault;
import gov.llnl.ernie.analysis.AnalysisPreprocessor;
import gov.llnl.ernie.analysis.VehicleClassifier;
import gov.llnl.ernie.data.Record;
import java.util.List;
import gov.llnl.ernie.data.VehicleClass;
import gov.llnl.ernie.data.VehicleRepresentative;
import gov.llnl.utility.InitializeException;
import gov.llnl.utility.xml.bind.ReaderInfo;
import gov.llnl.utility.xml.bind.WriterInfo;
import java.util.ArrayList;
import gov.llnl.ernie.data.VehicleClassifierExtractor;
import gov.llnl.ernie.data.VehicleInfo;

/**
 *
 * @author nelson85
 */
@ReaderInfo(Vehicle2ClassifierReader.class)
@WriterInfo(Vehicle2ClassifierWriter.class)
public class Vehicle2ClassifierImpl implements VehicleClassifier, AnalysisPreprocessor
{

  private VehicleClassifierExtractor extractor = null; //new RPM8VehicleTraceExtractor();
  private int defaultId;
  private VehicleInfo defaultVehicleInfo;

  static final double ACCEPTABLE = 500;
  List<VehicleClass> classes = new ArrayList<>();
  private Fault badVehicleFault;

  @Override
  public void initialize(Analysis analysis) throws InitializeException
  {
    for(VehicleClass vehicleClass : classes)
    {
      if(vehicleClass.getInfo().getId() == defaultId)
      {
        defaultVehicleInfo = vehicleClass.getInfo();
        break;
      }
    }

    this.badVehicleFault = (Fault) Enum.valueOf(analysis.getFaultClass(), "UNABLE_TO_CLASSIFY_VEHICLE");
  }

  /**
   * Entry point for Analysis preprocessors.
   *
   * Assigned default vehicle info. Assigned vehicle class or null if vehicle
   * can not be determined.
   */
  @Override
  public void compute(Record record)
  {

    record.setVehicleInfo(defaultVehicleInfo);

    if (record.getVehicleMotion() == null)
      return;

    Output output = classifyVehicle(record);
    if (output != null)
    {
      record.setVehicleFeatures(output.getFeatures());
      record.setVehicleClass(output.getVehicleClass());
    }
    else
    {
      record.addFault(badVehicleFault);
    }
  }

  public double bestDistance;
  public VehicleRepresentative bestRepresentative;
  public VehicleClass bestVehicleClass;

  /**
   * Classify a vehicle using the VPS data.
   *
   * @param record
   * @return
   */
  @Override
  public Output classifyVehicle(Record record)
  {
    // Extract the features for use in classifying
    double[] features = extractor.extract(record);

    // System.out.println(Arrays.toString(features)); 
    // Run through all the classes
    this.bestVehicleClass = null;
    this.bestDistance = Double.MAX_VALUE;
    for (VehicleClass vehicleClass : classes)
    {
      for (VehicleRepresentative rep : vehicleClass.getRepresentatives())
      {
        double distance = rep.getDistance(features);
        if (distance < bestDistance)
        {
          bestRepresentative = rep;
          bestVehicleClass = vehicleClass;
          bestDistance = distance;
        }
      }
    }

    if (bestDistance > ACCEPTABLE)
    {
      return null;
    }

    return new OutputImpl(bestVehicleClass, features);
  }

  public void addVehicleClass(VehicleClass vc)
  {
    ((Vehicle2ClassImpl) vc).classifier = this;
    this.classes.add(vc);
  }

  @Override
  public int getNumVehicleClasses()
  {
    return this.classes.size();
  }

  // FIXME KARL team meeting
  @Override
  public VehicleClass getVehicleClass(int id)
  {
    // Throws out-of-bound exception because id doesn't have a 1-to-1 relationship

    return this.classes.get(id - 1);
  }

  public List<VehicleClass> getVehicleClasses()
  {
    return this.classes;
  }

  /**
   * @return the vte
   */
  public VehicleClassifierExtractor getExtractor()
  {
    return extractor;
  }

  /**
   * @param vte the vte to set
   */
  public void setTraceExtractor(VehicleClassifierExtractor vte)
  {
    this.extractor = vte;
  }

//<editor-fold desc="internal">  
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
  //</editor-fold>

  /**
   * @return the defaultId
   */
  public int getDefaultId()
  {
    return defaultId;
  }

  /**
   * @param defaultId the defaultId to set
   */
  public void setDefaultId(int defaultId)
  {
    this.defaultId = defaultId;
  }

  /**
   * @return the defaultVehicleInfo
   */
  public VehicleInfo getDefaultVehicleInfo()
  {
    return defaultVehicleInfo;
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