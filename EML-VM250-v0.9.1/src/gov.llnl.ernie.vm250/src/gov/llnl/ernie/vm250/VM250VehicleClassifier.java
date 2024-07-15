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
import gov.llnl.ernie.analysis.AnalysisPreprocessor;
import gov.llnl.ernie.analysis.VehicleClassifier;
import gov.llnl.ernie.data.Record;
import gov.llnl.ernie.data.VehicleClass;
import gov.llnl.ernie.vehicle.VehicleClassifierImpl;
import gov.llnl.ernie.vm250.data.VM250Record;
import gov.llnl.utility.xml.bind.ReaderInfo;
import gov.llnl.utility.xml.bind.WriterInfo;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author nelson85
 */
@ReaderInfo(VM250VehicleClassifierReader.class)
@WriterInfo(VM250VehicleClassifierWriter.class)
public class VM250VehicleClassifier extends VehicleClassifierImpl implements VehicleClassifier, AnalysisPreprocessor
{

  public List<VM250VehicleClass> classDatabase = new LinkedList<>();

  @Override
  public double getCorrelation(Record record, VehicleClass cls)
  {
    throw new UnsupportedOperationException("Not supported for VM250.");
  }

  @Override
  public void compute(Record record) 
  {
    VM250Record recordTyped = (VM250Record) record;    
    try
    {
      recordTyped.setVehicleType(this.classify(record));
    }
    catch (AnalysisException ex)
    {
      record.addFault(VM250Record.Fault.UNABLE_TO_CLASSIFY_VEHICLE);
    }
  }

  public VehicleClass classify(Record record) throws AnalysisException
  {
    double vl = record.getVehicleMotion().getVehicleLength();
    for (VM250VehicleClass vc : classDatabase)
    {
      if (vl >= vc.getTemplate()[0] && vl < vc.getTemplate()[1])
      {
        return vc;
      }
    }
    throw new AnalysisException("Unable to identify vehicle class");
  }

  @Override
  public VehicleClass getVehicleClass(int i)
  {
    return this.classDatabase.get(i - 1); // adjust for 1-based index in vehicleClassDatabase
  }

  @Override
  public int getNumVehicleClasses()
  {
    return classDatabase.size();
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