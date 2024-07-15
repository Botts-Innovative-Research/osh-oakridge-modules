/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.utility.internal.io;

import gov.llnl.utility.UtilityPackage;
import gov.llnl.utility.annotation.Internal;
import gov.llnl.utility.io.DataContentFactory;
import gov.llnl.utility.io.ReaderException;
import gov.llnl.utility.xml.bind.ObjectReader;
import gov.llnl.utility.xml.bind.Reader;
import org.xml.sax.Attributes;

@Internal
@Reader.Declaration(pkg = UtilityPackage.class, name = "dataContextFactory",
        document = true, referenceable = true)
public class DataContentFactoryReader extends ObjectReader<DataContentFactory>
{
  //    public DataContentFactoryReader()
  //    {
  //      super(Order.FREE, Options.DOCUMENT, "dataContentFactory", UtilityPackage.getInstance());
  //    }
  @Override
  public ElementHandlerMap getHandlers() throws ReaderException
  {
    ReaderBuilder<DataContentFactory> builder = this.newBuilder();
    builder.using(this).element("handler")
            .contents(DataContentFactoryImpl.ContentHandler.class)
            .call(DataContentFactoryReader::addHandler);
    return builder.getHandlers();
  }

  @Override
  public Class<DataContentFactory> getObjectClass()
  {
    return DataContentFactory.class;
  }

  @Override
  public DataContentFactory start(Attributes attributes) throws ReaderException
  {
    return new DataContentFactoryImpl();
  }

  void addHandler(DataContentFactoryImpl.ContentHandler ch)
  {
    ((DataContentFactoryImpl) getObject()).addContentHandler(ch);
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