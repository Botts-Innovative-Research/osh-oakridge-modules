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
import gov.llnl.utility.xml.DomBuilder;
import gov.llnl.utility.xml.bind.Reader;
import gov.llnl.utility.xml.bind.Reader.Options;
import gov.llnl.utility.xml.bind.ReaderContext;
import gov.llnl.utility.xml.bind.SchemaBuilder;
import java.util.EnumSet;
import java.util.function.BiConsumer;
import org.xml.sax.Attributes;

/**
 *
 * @author nelson85
 */
@Internal
@SuppressWarnings("unchecked")
public class ReferenceHandler<T, T2> extends ElementHandlerImpl
{
  ReferenceHandler(String key, EnumSet<Options> flags, T target, Class<T2> cls, BiConsumer<T, T2> method)
  {
    super(key, flags, target, cls, method);
  }

  @Override
  public Object onStart(ReaderContext context, Attributes attr) throws ReaderException
  {
    return null;
  }

  @Override
  public Reader.ElementHandlerMap getHandlers()
  {
    return null;
  }

  @Override
  public void createSchemaElement(SchemaBuilder builder, DomBuilder type) throws ReaderException
  {
    DomBuilder group = type.element("xs:element")
            .attr("name", getName())
            .attr("type", "util:reference-type");
    SchemaBuilderUtilities.applyOptions(group, options);
  }

  @Override
  public boolean mustReference()
  {
    return true;
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