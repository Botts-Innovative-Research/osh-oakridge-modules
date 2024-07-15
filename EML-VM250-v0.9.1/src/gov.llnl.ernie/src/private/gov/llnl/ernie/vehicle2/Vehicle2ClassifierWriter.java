/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.ernie.vehicle2;

import gov.llnl.ernie.ErniePackage;
import gov.llnl.ernie.data.VehicleClass;
import gov.llnl.utility.io.WriterException;
import gov.llnl.utility.xml.bind.ObjectWriter;

/**
 *
 * @author nelson85
 */
public class Vehicle2ClassifierWriter extends ObjectWriter<Vehicle2ClassifierImpl>
{
  public Vehicle2ClassifierWriter()
  {
    super(Options.NONE, "vehicleClassifier2", ErniePackage.getInstance());
  }
  
  @Override
  public void attributes(WriterAttributes attributes, Vehicle2ClassifierImpl object) throws WriterException
  {
  }
  
  @Override
  public void contents(Vehicle2ClassifierImpl object) throws WriterException
  {
    WriterBuilder builder = newBuilder();
    builder.element("extractor").writer(new ExtractorWriter()).put(object.getExtractor());
    WriteObject<VehicleClass> wo = builder.writer(new Vehicle2ClassWriter());
    for (VehicleClass cls : object.classes)
    {
      wo.put(cls);
    }
  }
  
  private static class ExtractorWriter extends ObjectWriter<Object>
  {
    public ExtractorWriter()
    {
      super(Options.NONE, "extractor", ErniePackage.getInstance());
    }
    
    @Override
    public void attributes(WriterAttributes attributes, Object object) throws WriterException
    {
    }
    
    @Override
    public void contents(Object object) throws WriterException
    {
      WriterBuilder builder = newBuilder();
      builder.contents(object.getClass()).put(object);
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