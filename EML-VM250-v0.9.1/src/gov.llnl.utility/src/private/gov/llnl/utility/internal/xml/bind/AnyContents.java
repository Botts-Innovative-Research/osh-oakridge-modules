/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.utility.internal.xml.bind;

import gov.llnl.utility.io.ReaderException;
import gov.llnl.utility.xml.bind.ObjectReader;
import gov.llnl.utility.xml.bind.Reader;
import gov.llnl.utility.xml.bind.SchemaManager;
import java.util.EnumSet;
import org.xml.sax.Attributes;

/**
 *
 * @author nelson85
 */
@SuppressWarnings("unchecked")
public class AnyContents<Type> implements Reader.AnyFactory<Type>
{
  EnumSet<Reader.Options> options;
  Class type;

  public AnyContents(Class<Type> type)
  {
    this.type = type;
    this.options = null;
  }

  public AnyContents(Class<Type> type, Reader.Options... options)
  {
    this.type = type;
    if (options != null && options.length > 0)
      this.options = EnumSet.of(options[0], options);
  }

  @Override
  public Class getObjectClass()
  {
    return type;
  }

  @Override
  public ObjectReader getReader(String namespaceURI, String localName, String qualifiedName, Attributes attr) throws ReaderException
  {
    try
    {
      SchemaManager schemaMgr = SchemaManager.getInstance();
      Class<?> cls = schemaMgr.getObjectClass(namespaceURI, localName);
      ObjectReader ori = schemaMgr.findObjectReader(cls);
      if (!getObjectClass().isAssignableFrom(ori.getObjectClass()))
        throw new ReaderException(ori.getObjectClass() + " is not castable to " + getObjectClass());
      return ori;
    }
    catch (ClassNotFoundException ex)
    {
      throw new RuntimeException(ex);
    }
  }

  @Override
  public EnumSet<Reader.Options> getOptions()
  {
    return options;
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