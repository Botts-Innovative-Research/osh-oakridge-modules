/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.ernie.utility;

import gov.llnl.ernie.ErniePackage;
import gov.llnl.utility.io.ReaderException;
import gov.llnl.utility.xml.bind.ObjectReader;
import gov.llnl.utility.xml.bind.Reader;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;
import org.xml.sax.Attributes;

/**
 * ObjectReader for loading an external property file.
 *
 * @author nelson85
 */
@Reader.Declaration(pkg = ErniePackage.class, name = "properties",
        order = Reader.Order.FREE)
@Reader.Attribute(name = "extern", type = String.class)
public class PropertiesReader extends ObjectReader<Properties>
{
  Properties out;

  @Override
  public Properties start(Attributes attributes) throws ReaderException
  {
    out = new Properties();
    String extern = attributes.getValue("extern");
    if (extern != null)
    {
      try
      {
        URL url = getContext().getExternal(extern);
        out.load(url.openConnection().getInputStream());
      }
      catch (IOException ex)
      {
        throw new ReaderException(ex);
      }
    }

    return out;
  }

  @Override
  public ElementHandlerMap getHandlers() throws ReaderException
  {
    ReaderBuilder<Properties> builder = newBuilder();
    builder.element("set").reader(new KeyValueReader()).nop().optional();
    return builder.getHandlers();
  }

  @Override
  public Class<Properties> getObjectClass()
  {
    // FIXME java 10 will not like this syntax.
    return Properties.class;
  }

//<editor-fold desc="internal" defaultstate="collapsed">
  @Reader.Declaration(pkg = ErniePackage.class, name = "keyValue", 
          order = Reader.Order.ALL)
  @Reader.Attribute(name = "name", type = String.class, required = true)
  @Reader.Attribute(name = "value", type = String.class, required = true)
  public class KeyValueReader extends ObjectReader<Object>
  {
  
    @Override
    public Object start(Attributes attributes) throws ReaderException
    {
      out.put(attributes.getValue("name"), attributes.getValue("value"));
      return null;
    }

    @Override
    public ElementHandlerMap getHandlers() throws ReaderException
    {
      return null;
    }

    @Override
    public Class<? extends Object> getObjectClass()
    {
      return Object.class;
    }
  }
//</editor-fold>
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