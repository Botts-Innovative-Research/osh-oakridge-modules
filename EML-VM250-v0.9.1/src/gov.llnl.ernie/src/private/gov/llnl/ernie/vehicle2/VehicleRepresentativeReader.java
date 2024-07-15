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
import gov.llnl.ernie.data.VehicleRepresentative;
import gov.llnl.utility.io.ReaderException;
import gov.llnl.utility.xml.bind.ObjectReader;
import gov.llnl.utility.xml.bind.Reader;
import org.xml.sax.Attributes;

@Reader.Declaration(pkg = ErniePackage.class, 
        name = "representative", 
        order = Reader.Order.OPTIONS,
        cls = VehicleRepresentative.class)
@Reader.Attribute(name = "index", type = Integer.class)
public class VehicleRepresentativeReader extends ObjectReader<VehicleRepresentative>
{
  @Override
  public VehicleRepresentative start(Attributes attributes) throws ReaderException
  {
    VehicleRepresentativeImpl rep = new VehicleRepresentativeImpl();
    rep.setId(Integer.parseInt(attributes.getValue("index")));
    return rep;
  }

  @Override
  public ElementHandlerMap getHandlers() throws ReaderException
  {
    ReaderBuilder<VehicleRepresentativeImpl> builder = newBuilder(VehicleRepresentativeImpl.class);
    builder.element("template")
            .contents(double[].class)
            .call((VehicleRepresentativeImpl vc, double[] m) -> vc.template = m);
    builder.element("features")
            .contents(double[].class)
            .call((VehicleRepresentativeImpl vc, double[] m) -> vc.features = m);
    return builder.getHandlers();
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