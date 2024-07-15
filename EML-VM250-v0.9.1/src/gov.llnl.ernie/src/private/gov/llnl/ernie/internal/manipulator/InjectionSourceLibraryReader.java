/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.ernie.internal.manipulator;

import gov.llnl.ernie.manipulator.InjectionSourceLibrary;
import gov.llnl.ernie.ErniePackage;
import gov.llnl.utility.io.ReaderException;
import gov.llnl.utility.xml.bind.ObjectReader;
import gov.llnl.utility.xml.bind.Reader;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import org.xml.sax.Attributes;

/**
 *
 * @author nelson85
 */
@Reader.Declaration(pkg = ErniePackage.class, name = "sourceLibrary",
        document = true, contents = Reader.Contents.NONE)
@Reader.Attribute(name = "path", required = true)
public class InjectionSourceLibraryReader extends ObjectReader<InjectionSourceLibrary>
{
  @Override
  public InjectionSourceLibrary start(Attributes attributes) throws ReaderException
  {
    try
    {
      InjectionSourceLibraryImpl out = new InjectionSourceLibraryImpl();
      String path = attributes.getValue("path");
      out.setPath(Paths.get(getContext().getExternal(path).toURI()));
      return out;
    }
    catch (URISyntaxException ex)
    {
      throw new ReaderException(ex);
    }
  }

  @Override
  public Class getObjectClass()
  {
    return InjectionSourceLibrary.class;
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