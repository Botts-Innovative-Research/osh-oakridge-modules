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
@Reader.Declaration(pkg = ErniePackage.class, name = "vehicleClassifier",
        referenceable = true,
        document = true,
        order = Reader.Order.SEQUENCE)
public class VehicleClassifierReader extends ObjectReader<VehicleClassifierImpl>
{

  @Override
  public VehicleClassifierImpl start(Attributes attributes) throws ReaderException
  {
    return new VehicleClassifierImpl();
  }

  @Override
  public ElementHandlerMap getHandlers() throws ReaderException
  {
    ReaderBuilder<VehicleClassifierImpl> builder = newBuilder();
    builder.element("renormalizeTemplates")
            .callBoolean(VehicleClassifierImpl::setRenormalizeTemplates)
            .optional();
    builder.element("extractor")
            .reader(AnyReader.of(VehicleClassifierExtractor.class))
            .call(VehicleClassifierImpl::setFeatureExtractor).optional();
    builder.reader(new VehicleClassReader())
            .call(
                    (VehicleClassifierImpl vc, VehicleClass v) -> vc.classDatabase.add(v)
            ).unbounded();
    return builder.getHandlers();
  } 
  
  @Override
  public Class<? extends VehicleClassifierImpl> getObjectClass()
  {
    return VehicleClassifierImpl.class;
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