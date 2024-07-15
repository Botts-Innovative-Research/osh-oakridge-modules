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
import gov.llnl.utility.io.WriterException;
import gov.llnl.utility.xml.bind.ObjectWriter;

/**
 *
 * @author nelson85
 */
class VehicleRepresentativeWriter extends ObjectWriter<VehicleRepresentative>
{

  public VehicleRepresentativeWriter()
  {
    super(Options.NONE, "representative", ErniePackage.getInstance());
  }

  @Override
  public void attributes(WriterAttributes attributes, VehicleRepresentative object) throws WriterException
  {
    attributes.add("index", object.getId());
  }

  @Override
  public void contents(VehicleRepresentative object) throws WriterException
  {
    WriterBuilder builder = newBuilder();
    if (object.getTemplate() != null)
      builder.element("template").contents(double[].class).put(object.getTemplate());
    if (object.getFeatures() != null)
      builder.element("features").contents(double[].class).put(object.getFeatures());
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