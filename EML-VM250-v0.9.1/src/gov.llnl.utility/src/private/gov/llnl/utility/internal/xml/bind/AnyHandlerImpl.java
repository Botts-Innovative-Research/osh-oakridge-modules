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
import gov.llnl.utility.xml.bind.ObjectReader;
import gov.llnl.utility.xml.bind.Reader;
import gov.llnl.utility.xml.bind.ReaderContext;
import gov.llnl.utility.xml.bind.SchemaBuilder;
import java.util.function.BiConsumer;
import org.xml.sax.Attributes;
import gov.llnl.utility.xml.bind.Reader.AnyHandler;
import gov.llnl.utility.xml.bind.Reader.Options;
import java.util.EnumSet;

/**
 *
 * @author nelson85
 */
@Internal
@SuppressWarnings("unchecked")
public class AnyHandlerImpl extends ElementHandlerImpl implements AnyHandler
{
  Reader.AnyFactory any;

  public AnyHandlerImpl(EnumSet<Options> flags, Object target, Class cls, BiConsumer method, Reader.AnyFactory any)
  {
    super("##any", merge(flags, any.getOptions()), target, cls, method);
    this.any = any;
  }

  public ReaderHandler getHandler(String namespaceURI, String localName, String qualifiedName, Attributes attr)
          throws ReaderException
  {
    ObjectReader reader = any.getReader(namespaceURI, localName, qualifiedName, attr);
    if (reader == null)
      throw new ReaderException("Unable to handle element " + localName + " " + any);
    return new ReaderHandler("##any", options, target, method, reader);
  }

  @Override
  public Object onStart(ReaderContext context, Attributes attr) throws ReaderException
  {
    throw new ReaderException("not supported");
  }

  @Override
  public Object onEnd(Object object) throws ReaderException
  {
    throw new ReaderException("not supported");
  }

  @Override
  public void createSchemaElement(SchemaBuilder builder, DomBuilder group) throws ReaderException
  {
    group = group.element("xs:any");
    SchemaBuilderUtilities.applyOptions(group, options);
  }

  private static EnumSet<Options> merge(EnumSet<Options> f1, EnumSet<Options> f2)
  {
    if (f1 == null)
      return f2;
    if (f2 == null)
      return f1;
    f1 = EnumSet.copyOf(f1);
    f1.addAll(f2);
    return f1;
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