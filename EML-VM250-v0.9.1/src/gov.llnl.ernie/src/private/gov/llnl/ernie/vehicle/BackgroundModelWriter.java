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
import gov.llnl.math.matrix.Matrix;
import gov.llnl.utility.io.WriterException;
import gov.llnl.utility.xml.bind.ObjectWriter;

/**
 *
 * @author nelson85
 */
public class BackgroundModelWriter extends ObjectWriter<VehicleClass.BackgroundModel> {

  public BackgroundModelWriter()
  {
    super(Options.NONE, "backgroundModel", ErniePackage.getInstance());
  }

  @Override
  public void attributes(WriterAttributes attributes, VehicleClass.BackgroundModel object) throws WriterException
  {
  }

  @Override
  public void contents(VehicleClass.BackgroundModel object) throws WriterException
  {
    WriterBuilder builder = newBuilder();
    builder.element("matrix").contents(Matrix.class).put(object.toMatrix());
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