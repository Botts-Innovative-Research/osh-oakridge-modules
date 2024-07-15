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

import gov.llnl.utility.Copyable;
import gov.llnl.utility.annotation.Internal;
import gov.llnl.utility.io.ReaderException;
import gov.llnl.utility.xml.DomBuilder;
import gov.llnl.utility.xml.bind.Reader.ElementHandler;
import gov.llnl.utility.xml.bind.ReaderContext;
import gov.llnl.utility.xml.bind.SchemaBuilder;
import org.xml.sax.Attributes;

/**
 *
 * @author nelson85
 */
@Internal
@SuppressWarnings("unchecked")
public class DeferredHandler extends ElementHandlerImpl
{
  ReaderContextImpl context;
  Object previous;
  Object parent;
  ElementHandlerImpl base;
  boolean copy;

  public DeferredHandler(ReaderContextImpl context, ElementHandler base,
          Object previous, Object parent, boolean copy)
  {
    super("##deferred", null, null, null, null);
    this.base = (ElementHandlerImpl) base;
    this.context = context;
    this.previous = previous;
    this.parent = parent;
    this.copy = copy;
  }

  @Override
  public Object onStart(ReaderContext context, Attributes attr) throws ReaderException
  {
    parent = context.getCurrentHandlerContext().getParentObject();
    return null;
  }

  @Override
  public Object onEnd(Object object) throws ReaderException
  {
    return null;
  }

  void executeDeferred(Object child) throws ReaderException
  {
    if (copy)
      if (child instanceof Copyable)
      {
        child = ((Copyable) child).copyOf();
      }
      else
        throw new ReaderException("Unable to copy object.");
    // Previous context is required for sections to work properly
    context.pushTemporaryContext(null, previous);
    base.onCall(context, parent, child);
    context.popTemporaryContext();
  }

  @Override
  public void createSchemaElement(SchemaBuilder builder, DomBuilder type) throws ReaderException
  {
    // not used
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