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
import gov.llnl.utility.InitializeException;
import gov.llnl.utility.io.ReaderException;
import gov.llnl.utility.xml.bind.AnyReader;
import gov.llnl.utility.xml.bind.ObjectReader;
import gov.llnl.utility.xml.bind.Reader;
import org.xml.sax.Attributes;
import gov.llnl.ernie.data.VehicleClassifierExtractor;

/**
 *
 * @author nelson85
 */
@Reader.Declaration(pkg = ErniePackage.class, name = "vehicleClassifier2",
        referenceable = true,
        document = true,
        order = Reader.Order.SEQUENCE)
@Reader.Attribute(name = "defaultId", type = Integer.class)
public class Vehicle2ClassifierReader extends ObjectReader<Vehicle2ClassifierImpl>
{
  private Vehicle2ClassifierImpl parent;

  @Override
  public Vehicle2ClassifierImpl start(Attributes attributes) throws ReaderException
  {
    this.parent = new Vehicle2ClassifierImpl();
    this.parent.setDefaultId(Integer.parseInt(attributes.getValue("defaultId")));
    return this.parent;
  }

  @Override
  public ElementHandlerMap getHandlers() throws ReaderException
  {
    ReaderBuilder<Vehicle2ClassifierImpl> builder = newBuilder();
    builder.element("extractor")
            .reader(AnyReader.of(VehicleClassifierExtractor.class))
            .call(Vehicle2ClassifierImpl::setTraceExtractor).optional();
    builder.reader(new Vehicle2ClassReader())
            .call(
                    (Vehicle2ClassifierImpl vc, VehicleClass v) -> vc.addVehicleClass(v)
            ).unbounded().optional();
    return builder.getHandlers();
  }  
  
  @Override
  public Class<? extends Vehicle2ClassifierImpl> getObjectClass()
  {
    return Vehicle2ClassifierImpl.class;
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