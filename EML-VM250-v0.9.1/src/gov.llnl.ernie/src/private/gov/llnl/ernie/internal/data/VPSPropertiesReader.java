/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.ernie.internal.data;

import gov.llnl.ernie.ErniePackage;
import gov.llnl.ernie.data.VPSProperties;
import gov.llnl.utility.io.ReaderException;
import gov.llnl.utility.xml.bind.ObjectReader;
import gov.llnl.utility.xml.bind.Reader;
import org.xml.sax.Attributes;

/**
 *
 * @author pham21
 */
@Reader.Declaration(pkg=ErniePackage.class, name="vpsProperites",
        referenceable=true, cls=VPSProperties.class)
public class VPSPropertiesReader  extends ObjectReader<VPSProperties>
{
   @Override
  public VPSProperties start(Attributes attributes) throws ReaderException
  {
    return null;
  }

  @Override
  public VPSProperties end() throws ReaderException
  {  
    if(getObject() == null)
    {
      throw new ReaderException("element was not defined");
    }
    // Assuming the object is completely loaded we return it.
    return null;
  }
  
  @Override
  public ElementHandlerMap getHandlers() throws ReaderException
  {
    return null;
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