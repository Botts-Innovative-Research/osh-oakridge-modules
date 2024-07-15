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

import gov.llnl.ernie.io.LaneDatabase;
import gov.llnl.ernie.utility.PropertiesReader;
import gov.llnl.utility.io.ReaderException;
import gov.llnl.utility.xml.bind.ObjectReader;
import gov.llnl.utility.xml.bind.Reader;
import org.xml.sax.Attributes;

/**
 *
 * @author mattoon1
 */
@Reader.Declaration(pkg = ErnieVM250Package.class, name = "recordDatabase",
        referenceable=true, 
        document = true, 
        order = Reader.Order.OPTIONS)
public class VM250RecordDatabaseReader extends ObjectReader<VM250RecordDatabase>
{
  @Override
  public VM250RecordDatabase start(Attributes attributes) throws ReaderException
  {
    return new VM250RecordDatabase();
  }

  @Override
  public VM250RecordDatabase end() throws ReaderException
  {
    getObject().initialize();
    return null;
  }

  @Override
  public ElementHandlerMap getHandlers() throws ReaderException
  {
    ReaderBuilder<VM250RecordDatabase> builder = newBuilder();

    builder.section(new Imports()).optional();
    builder.element("queries")
            .reader(new PropertiesReader())
            .call(VM250RecordDatabase::setProperties);
    builder.element("lanes").contents(LaneDatabase.class).
            call(VM250RecordDatabase::setLaneDatabase);
    return builder.getHandlers();
  }

  @Override
  public Class<? extends VM250RecordDatabase> getObjectClass()
  {
    return VM250RecordDatabase.class;
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