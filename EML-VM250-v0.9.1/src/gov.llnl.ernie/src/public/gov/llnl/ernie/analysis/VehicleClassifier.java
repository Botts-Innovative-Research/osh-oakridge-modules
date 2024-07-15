/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.ernie.analysis;

import gov.llnl.ernie.data.Record;
import gov.llnl.ernie.data.VehicleClass;
import gov.llnl.ernie.data.VehicleRepresentative;
import gov.llnl.ernie.vehicle.VehicleClassifierReader;
import gov.llnl.ernie.vehicle.VehicleClassifierWriter;
import gov.llnl.utility.xml.bind.ReaderInfo;
import gov.llnl.utility.xml.bind.WriterInfo;

/**
 * Algorithm used to decide the vehicle typed.
 *
 * This algorithm is run during the preprocessor stage after the vehicle motion
 * has been extracted.
 *
 * Each portal vendor supplies different types of information about the vehicle.
 * Thus we will have a specialization for vendor portal.
 *
 * @author nelson85
 */
@ReaderInfo(VehicleClassifierReader.class)
@WriterInfo(VehicleClassifierWriter.class)
public interface VehicleClassifier
{

  /**
   * Classify a vehicle by type.
   *
   * Extracts a set of features from the
   * (Renamed to break Python scripts)
   * 
   * @param record is the record to be classified.
   * @return a vehicle class from the vehicle database.
   * @throws AnalysisException if the vehicle could not be classified.
   */
  Output classifyVehicle(Record record) throws AnalysisException;

  /**
   * Get the number of vehicle classes in the classifier.
   *
   * @return
   */
  int getNumVehicleClasses();

  /** Access a particular vehicle class by id.
   *
   * @param id is the index of the vehicle class.
   * @return
   */
  VehicleClass getVehicleClass(int id);
  
  public interface Output
  {
    VehicleClass getVehicleClass();
    VehicleRepresentative getVehicleRepresentative();
    double[] getFeatures();
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