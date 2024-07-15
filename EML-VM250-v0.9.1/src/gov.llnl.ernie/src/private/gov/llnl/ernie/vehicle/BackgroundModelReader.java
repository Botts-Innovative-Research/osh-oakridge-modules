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
import gov.llnl.math.matrix.MatrixColumnTable;
import gov.llnl.utility.io.ReaderException;
import gov.llnl.utility.xml.bind.ObjectReader;
import gov.llnl.utility.xml.bind.Reader;
import org.xml.sax.Attributes;

@Reader.Declaration(pkg = ErniePackage.class, name = "backgroundModel", order = Reader.Order.OPTIONS)
public class BackgroundModelReader extends ObjectReader<VehicleClass.BackgroundModel> {
  @Override
  public VehicleClass.BackgroundModel start(Attributes attributes) throws ReaderException
  {
    return new BackgroundModelImpl();
  }

  @Override
  public ElementHandlerMap getHandlers() throws ReaderException
  {
    ReaderBuilder<BackgroundModelImpl> builder = newBuilder(BackgroundModelImpl.class);
    builder.element("matrix").contents(Matrix.class).call((BackgroundModelImpl vc, Matrix m) -> vc.matrix = new MatrixColumnTable(m));
    return builder.getHandlers();
  }

  @Override
  public Class<? extends VehicleClass.BackgroundModel> getObjectClass()
  {
    return BackgroundModelImpl.class;
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