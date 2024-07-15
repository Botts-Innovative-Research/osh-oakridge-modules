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

import gov.llnl.ernie.data.VehicleClass;
import gov.llnl.ernie.ErniePackage;
import gov.llnl.ernie.vehicle.VehicleClassWriter;
import gov.llnl.utility.io.WriterException;
import gov.llnl.utility.xml.bind.ObjectWriter;

/**
 *
 * @author nelson85
 */
public class VM250VehicleClassifierWriter extends ObjectWriter<VM250VehicleClassifier>
{
  public VM250VehicleClassifierWriter()
  {
    super(Options.NONE, "vehicleClassifier", ErnieVM250Package.getInstance());
  }

  @Override
  public void attributes(WriterAttributes attributes, VM250VehicleClassifier object) throws WriterException
  {
  }

  @Override
  public void contents(VM250VehicleClassifier object) throws WriterException
  {
    WriterBuilder builder = newBuilder();
    WriteObject<VehicleClass> wo = builder.writer(new VehicleClassWriter());
    for (VehicleClass cls : object.classDatabase)
    {
      wo.put(cls);
    }
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