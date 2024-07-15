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

import gov.llnl.utility.UtilityPackage;
import gov.llnl.utility.annotation.Internal;
import gov.llnl.utility.io.WriterException;
import gov.llnl.utility.xml.bind.ObjectWriter;
import gov.llnl.utility.xml.bind.WriterContext.Marshaller;

/**
 *
 * @author nelson85
 */
@Internal
public class ContentWriters
{
  static public class PrimitiveWriter<Obj> extends ObjectWriter<Obj>
  {
    Marshaller marshaller;

    public PrimitiveWriter(Marshaller marshaller)
    {
      super(Options.NONE, "none", UtilityPackage.getInstance());
      this.marshaller = marshaller;
    }

    @Override
    public void attributes(WriterAttributes attributes, Obj object) throws WriterException
    {
    }

    @Override
    @SuppressWarnings("unchecked")
    public void contents(Obj object) throws WriterException
    {
      this.addContents(marshaller.marshall(object, getContext().getMarshallerOptions()));
    }

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