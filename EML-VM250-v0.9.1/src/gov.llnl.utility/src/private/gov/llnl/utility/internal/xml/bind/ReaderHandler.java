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
import gov.llnl.utility.xml.bind.Reader.ElementHandlerMap;
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
public class ReaderHandler<T, T2> extends ElementHandlerImpl<T, T2>
{
  final Reader reader;
  final Reader.Declaration decl;

  @SuppressWarnings("unchecked")
  ReaderHandler(String key, EnumSet<Options> flags, T target, BiConsumer<T, T2> method, Reader reader)
  {
    super(key, flags, target, reader.getObjectClass(), method);
    this.reader = reader;
    this.decl = reader.getDeclaration();
  }

  @Override
  @SuppressWarnings("unchecked")
  public Object onStart(ReaderContext context, Attributes attr) throws ReaderException
  {
    reader.setContext(context);
    Object obj = reader.start(attr);
    if (decl.autoAttributes())
    {
      if (obj == null)
        throw new ReaderException("Auto attributes applied to null object");

      AttributeHandlers handlers = new AttributeHandlers(obj.getClass());
      handlers.applyAttributes(context, obj, attr);
    }
    return obj;
  }

  @Override
  public Object onEnd(Object object) throws ReaderException
  {
    Object out = reader.end();
    reader.setContext(null);
    return out;
  }

  @Override
  public Object onTextContent(Object obj, String textContent) throws ReaderException
  {
    return reader.contents(textContent);
  }

  @Override
  public ElementHandlerMap getHandlers() throws ReaderException
  {
    return reader.getHandlers();
  }

  @Override
  public Reader getReader()
  {
    return reader;
  }

  @Override
  public void createSchemaElement(SchemaBuilder builder, DomBuilder group) throws ReaderException
  {
    group = reader.createSchemaElement(builder, getName(), group, false);
    SchemaBuilderUtilities.applyOptions(group, options);
  }

  @Override
  public boolean hasTextContent()
  {
    Reader.Contents contents = decl.contents();
    return (contents == Reader.Contents.TEXT || contents == Reader.Contents.MIXED);
//    return Options.Check.captureTextContents(reader.getOptions());
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