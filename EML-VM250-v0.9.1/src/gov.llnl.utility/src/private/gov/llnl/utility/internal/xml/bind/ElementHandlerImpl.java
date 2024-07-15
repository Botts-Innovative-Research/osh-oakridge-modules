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

import gov.llnl.utility.xml.bind.ElementGroup;
import gov.llnl.utility.annotation.Internal;
import gov.llnl.utility.io.ReaderException;
import gov.llnl.utility.xml.DomBuilder;
import gov.llnl.utility.xml.bind.Reader;
import gov.llnl.utility.xml.bind.Reader.ElementHandler;
import gov.llnl.utility.xml.bind.Reader.Options;
import gov.llnl.utility.xml.bind.ReaderContext;
import gov.llnl.utility.xml.bind.ReaderContext.HandlerContext;
import gov.llnl.utility.xml.bind.SchemaBuilder;
import java.util.EnumSet;
import java.util.function.BiConsumer;
import org.xml.sax.Attributes;

/**
 * Element handlers provide the mapping to elements and call the method hooks.
 *
 * @author nelson85
 */
@Internal
public class ElementHandlerImpl<T, T2> implements ElementHandler
{
  private final String key;
  final T target;
  final BiConsumer<T, T2> method;
  EnumSet<Options> options;   // options used are OPTIONAL, REQUIRED, UNBOUNDED, ANY_OTHER, ANY_ALL
  private final Class<T2> resultCls;

  // Hints for validation
  private ElementGroup parentGroup;
  private ElementHandler nextHandler;
  HandlerContextImpl cache = null;

  public ElementHandlerImpl(String key, EnumSet<Options> flags, T target, Class<T2> resultCls, BiConsumer<T, T2> method)
  {
    this.key = key;
    this.target = target;
    this.method = method;
    this.options = flags;
    this.resultCls = resultCls;
  }

  public void setOptions(EnumSet<Options> flags)
  {
    if (flags == null)
      return;
    if (options == null)
      this.options = EnumSet.copyOf(flags);
    else
      this.options.addAll(flags);

    if (flags.contains(Options.REQUIRED))
      this.options.remove(Options.OPTIONAL);
    if (flags.contains(Options.OPTIONAL))
      this.options.remove(Options.REQUIRED);
  }

  /**
   * Action to perform when the start of an element is encounter.
   *
   * @param context is the contextual information parsed from the document.
   * @param attr
   * @return the element which will be the parentGroup for any elements that
   * appear in the block. This is null if there are no elements allowed within
   * the element.
   * @throws ReaderException
   */
  public Object onStart(ReaderContext context, Attributes attr) throws ReaderException
  {
    return null;
  }

  /**
   * Performs all actions required when the element is closed. This may create a
   * new object if it was not created in the start. A null value is ignored.
   *
   * @param object is the object created by this element.
   * @return the object produced or null if no object associated.
   * @throws ReaderException if the object requirements were not met.
   */
  public Object onEnd(Object object) throws ReaderException
  {
    return null;
  }

  @SuppressWarnings("unchecked")
  public void onCall(ReaderContext context, Object parent, Object child) throws ReaderException
  {
    if (method == null)
      return;

    try
    {
      method.accept((T) parent, (T2) child);
    }
    catch (RuntimeException ex)
    {
      ReaderException rex;
      if (ex.getCause() != null)
      {
        // If the cause is already a reader exception, then we just need to unwrap it.
        if (ex.getCause() instanceof ReaderException)
          rex = (ReaderException) ex.getCause();
        else
          // Otherwise wrap the exception.
          rex = new ReaderException(ex.getCause());
      }
      else if (parent == null)
        rex = new ReaderException("Parent object was not constructed by start.", ex);
      else
        rex = new ReaderException(ex);
      throw rex;
    }
  }

  public Object onTextContent(Object obj, String textContent) throws ReaderException
  {
    return null;
  }

  /**
   * Returns the handlers to be used to interpret all elements that appear
   * within this element. If no elements may appear within this element then the
   * getHandlers returns null.
   *
   * @return the handlers or null if no child elements allowed.
   * @throws ReaderException
   */
  public Reader.ElementHandlerMap getHandlers() throws ReaderException
  {
    return null;
  }

  /**
   * @return the key
   */
  @Override
  public String getKey()
  {
    return key;
  }

  @Override
  public String getName()
  {
    String[] contents = key.split("#");
    return contents[0];
  }

  @Override
  public void createSchemaElement(SchemaBuilder builder, DomBuilder type)
          throws ReaderException
  {
  }

  public boolean hasTextContent()
  {
    return false;
  }

  public Object getParent(HandlerContext context)
  {
    if (this.target != null)
      return this.target;
    return context.getTargetObject();
  }

  public boolean mustReference()
  {
    return false;
  }

  @Override
  public Reader getReader()
  {
    return null;
  }

  @Override
  public EnumSet<Options> getOptions()
  {
    return options;
  }

  @Override
  public Class getTargetClass()
  {
    return this.resultCls;
  }

  /**
   * @return the parentGroup
   */
  public ElementGroup getParentGroup()
  {
    return parentGroup;
  }

  /**
   * @return the nextHandler
   */
  public ElementHandler getNextHandler()
  {
    return nextHandler;
  }

  public void setParentGroup(ElementGroup parentGroup)
  {
    this.parentGroup = parentGroup;
  }

  public void setNextHandler(ElementHandler handler)
  {
    this.nextHandler = handler;
  }

  @Override
  public BiConsumer getMethod()
  {
    return method;
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