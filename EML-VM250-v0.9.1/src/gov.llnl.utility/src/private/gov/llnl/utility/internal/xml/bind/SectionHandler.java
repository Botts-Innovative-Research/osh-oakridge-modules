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
import gov.llnl.utility.xml.bind.Reader.SectionInterface;
import gov.llnl.utility.xml.bind.ReaderContext;
import gov.llnl.utility.xml.bind.SchemaBuilder;
import java.util.EnumSet;
import org.xml.sax.Attributes;

@Internal
@SuppressWarnings("unchecked")
public class SectionHandler extends ElementHandlerImpl
{
  final SectionInterface section;

  SectionHandler(EnumSet<Options> flags, SectionInterface section)
  {
    super(section.getHandlerKey(), flags, null, null, null);
    this.section = section;
  }

  @Override
  public Object onStart(ReaderContext context, Attributes attr) throws ReaderException
  {
    section.setContext(context);
    // Copy the parentGroup to child
    HandlerContextImpl hc = (HandlerContextImpl) context.getCurrentHandlerContext();
    hc.targetObject = hc.parentObject;

    // Call the start, but the object created will be ignored.
    section.start(attr);
    return hc.targetObject;
  }

  @Override
  public Object onEnd(Object object) throws ReaderException
  {
    section.end();
    return null;
  }

  @Override
  public Object onTextContent(Object obj, String textContent) throws ReaderException
  {
    section.contents(textContent);
    return null;
  }

  @Override
  public ElementHandlerMap getHandlers() throws ReaderException
  {
    return section.getHandlers();
  }

  public Reader getReader()
  {
    return section;
  }

  @Override
  public void createSchemaElement(SchemaBuilder builder, DomBuilder group) throws ReaderException
  {
    group = section.createSchemaElement(builder, getName(), group, false);
    SchemaBuilderUtilities.applyOptions(group, options);
  }

  @Override
  public boolean hasTextContent()
  {
    Reader.Declaration decl = section.getDeclaration();
    return decl.contents() == Reader.Contents.TEXT;
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