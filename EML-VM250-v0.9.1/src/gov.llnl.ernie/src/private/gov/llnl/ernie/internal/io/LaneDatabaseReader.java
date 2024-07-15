/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.ernie.internal.io;

import gov.llnl.ernie.ErniePackage;
import gov.llnl.ernie.io.LaneDatabase;
import gov.llnl.utility.io.ReaderException;
import gov.llnl.utility.xml.bind.ObjectReader;
import gov.llnl.utility.xml.bind.Reader;
import org.xml.sax.Attributes;

/**
 *
 * @author nelson85
 */
@Reader.Declaration(pkg = ErniePackage.class, name = "laneDatabase",
        order = Reader.Order.FREE, referenceable = true, document = true)
public class LaneDatabaseReader extends ObjectReader<LaneDatabase>
{

  @Override
  public LaneDatabase start(Attributes attributes) throws ReaderException
  {
    return new LaneDatabaseImpl();
  }

  @Override
  public ElementHandlerMap getHandlers() throws ReaderException
  {
    ReaderBuilder<LaneDatabaseImpl> builder = newBuilder(LaneDatabaseImpl.class);
    builder.section(new Imports()).optional();
    builder.section(new Defines()).optional();
    builder.element("lane").contents(LaneDefinition.class).call(LaneDatabaseImpl::add);
    return builder.getHandlers();
  }

  @Override
  public Class<? extends LaneDatabase> getObjectClass()
  {
    return LaneDatabase.class;
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