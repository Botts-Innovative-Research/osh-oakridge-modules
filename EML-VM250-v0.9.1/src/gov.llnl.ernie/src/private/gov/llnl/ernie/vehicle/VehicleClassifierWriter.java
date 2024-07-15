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

import gov.llnl.ernie.ErniePackage;
import gov.llnl.ernie.data.VehicleClass;
import gov.llnl.utility.io.WriterException;
import gov.llnl.utility.xml.bind.ObjectWriter;

/**
 *
 * @author nelson85
 */
public class VehicleClassifierWriter extends ObjectWriter<VehicleClassifierImpl>
{
  public VehicleClassifierWriter()
  {
    super(Options.NONE, "vehicleClassifier", ErniePackage.getInstance());
  }

  @Override
  public void attributes(WriterAttributes attributes, VehicleClassifierImpl object) throws WriterException
  {
  }

  @Override
  public void contents(VehicleClassifierImpl object) throws WriterException
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