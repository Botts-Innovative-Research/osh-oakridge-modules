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

import gov.llnl.utility.annotation.Internal;
import gov.llnl.utility.io.ReaderException;
import gov.llnl.utility.xml.bind.ObjectReader;
import gov.llnl.utility.xml.bind.Reader;
import gov.llnl.utility.xml.bind.Reader.Options;
import gov.llnl.utility.xml.bind.SchemaManager;
import java.util.EnumSet;
import org.xml.sax.Attributes;

/**
 *
 * @author nelson85
 */
@Internal
public class ImportsAny implements Reader.AnyFactory<Object>
{
  @Override
  public Class<Object> getObjectClass()
  {
    return Object.class;
  }

  @Override
  public ObjectReader getReader(String namespaceURI, String localName, String qualifiedName, Attributes attr) throws ReaderException
  {
    try
    {
      SchemaManager schemaMgr = SchemaManager.getInstance();
      Class<?> cls = schemaMgr.getObjectClass(namespaceURI, localName);
      ObjectReader reader = schemaMgr.findObjectReader(cls);
      Reader.Declaration decl = reader.getDeclaration();
      if (!decl.document())
        throw new ReaderException(namespaceURI + "#" + localName + " is not a document type.");
      return new ImportsReader(reader);
    }
    catch (ClassNotFoundException ex)
    {
      throw new ReaderException(ex);
    }
  }

  @Override
  public EnumSet<Options> getOptions()
  {
    return EnumSet.of(Options.ANY_ALL);
  }

}
//</editor-fold>


/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */