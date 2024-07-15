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
import gov.llnl.utility.xml.bind.SchemaBuilder;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import gov.llnl.utility.xml.bind.Reader;
import gov.llnl.utility.xml.bind.Reader.AnyHandler;
import gov.llnl.utility.xml.bind.Reader.ElementHandler;

/**
 *
 * @author nelson85
 */
@Internal
public class ElementHandlerMapImpl implements Reader.ElementHandlerMap
{
  protected final String namespaceURI;
  final ElementHandler first;
  protected final ElementHandler[] handlers;
  final boolean hasAny;
  public final StackTraceElement trace;

  private ElementHandlerMapImpl(String namespaceURI, ElementHandler first,
          ElementHandler[] handlers, boolean hasAny,
          StackTraceElement trace)
  {
    this.namespaceURI = namespaceURI;
    this.first = first;
    this.handlers = handlers;
    this.hasAny = hasAny;
    this.trace = trace;
    if (!namespaceURI.startsWith("#"))
      throw new RuntimeException("namespace issue");
  }

  static ElementHandlerMapImpl newInstance(String namespaceURI, ReaderBuilderImpl.HandlerList handlerList)
  {
    ElementHandler rootHandler = handlerList.firstHandler;
    boolean hasAny = false;
    // Count the elements 
    int count = 0;
    ElementHandler iter = rootHandler;
    while (iter != null)
    {
      if (iter instanceof AnyHandler)
        hasAny = true;
      count++;
      iter = iter.getNextHandler();
    }

    // Pack into array
    ElementHandler[] handlers = new ElementHandler[count];
    int i = 0;
    iter = rootHandler;
    while (iter != null)
    {
      // Sanity check
      if (iter.getKey() == null)
        throw new NullPointerException("Null pointer getting key " + iter + " " + iter.getMethod());
      int u1 = iter.getKey().indexOf('#', 1);
      int u2 = iter.getKey().indexOf('#', u1 + 1);
      if (u2 != -1)
        throw new RuntimeException("bad key " + iter.getKey());

      // Pack into array      
      handlers[i++] = iter;
      iter = iter.getNextHandler();
    }

    // sort array
    Arrays.sort(handlers, new ByKeys());
    return new ElementHandlerMapImpl(namespaceURI, rootHandler, handlers, hasAny, handlerList.trace);
  }

  @Override
  public StackTraceElement getTrace()
  {
    return trace;
  }

  @Override
  public List<ElementHandler> toList()
  {
    return Arrays.asList(handlers);
  }

//  @Override
//  public ElementHandler first()
//  {
//    return this.first;
//  }
  @Override
  public AnyHandler getAnyHandler(ElementHandler last)
  {
    if (!hasAny)
      return null;

    ElementHandler iter = last;
    while (iter != null)
    {
      if (iter instanceof AnyHandler)
        return (AnyHandler) iter;
      iter = iter.getNextHandler();
    }

    iter = this.first;
    while (iter != null && iter != last)
    {
      while (iter != null)
      {
        if (iter instanceof AnyHandler)
          return (AnyHandler) iter;
        iter = iter.getNextHandler();
      }
    }
    return null;
  }

  static class ByKeys implements Comparator<ElementHandler>
  {
    @Override
    public int compare(ElementHandler o1, ElementHandler o2)
    {
      return o1.getKey().compareTo(o2.getKey());
    }
  }

  private static final Comparator KEY_FINDER = new Comparator<Object>()
  {
    @Override
    public int compare(Object o1, Object o2)
    {
      return ((ElementHandler) o1).getKey().compareTo((String) o2);
    }
  };

  @Override
  public ElementHandler get(String namespaceURI, String localName)
  {
    String handlerName;
    if (namespaceURI == null)
      handlerName = localName + this.namespaceURI;
    else
      handlerName = localName + "#" + namespaceURI;
    @SuppressWarnings("unchecked")
    int index = Arrays.binarySearch(handlers, handlerName, KEY_FINDER);
    if (index < 0)
      return null;
    return handlers[index];
  }

  @Override
  public void dump(PrintStream ps)
  {
    if (handlers == null)
      return;
    for (ElementHandler handler : handlers)
    {
      ps.println("handler=" + handler.getKey());
    }
  }

//  @Override
//  public String getDefaultNamespaceURI()
//  {
//    return namespaceURI;
//  }
  public boolean isEmpty()
  {
    return handlers.length == 0;
  }

  @Override
  public void createSchemaType(SchemaBuilder builder, DomBuilder type)
          throws ReaderException
  {
    if (first == null)
      return;
    BuildSchema bs = new BuildSchema(builder, type, first);
    bs.process();
  }

  private class BuildSchema
  {
    SchemaBuilder builder;
    DomBuilder root;
    ElementHandler current;
    ElementGroup top;

    BuildSchema(SchemaBuilder builder, DomBuilder root, ElementHandler handler)
    {
      if (handler == null)
        throw new RuntimeException("null");
      this.builder = builder;
      this.root = root;
      this.current = handler;
      top = current.getParentGroup();
      while (top.getParent() != null)
      {
        top = top.getParent();
      }
    }

    void process() throws ReaderException
    {
      DomBuilder dom = top.createSchemaGroup(root);
      while (current != null)
      {
        if (current.getParentGroup() != top)
        {
          process2(dom);
        }
        else
        {
          //         System.out.println("process "+current.getKey());
          current.createSchemaElement(builder, dom);
          current = current.getNextHandler();
        }
      }
    }

    private void process2(DomBuilder domParent) throws ReaderException
    {
      ElementGroup group = current.getParentGroup();
      DomBuilder dom = group.createSchemaGroup(domParent);
      while (current != null)
      {
        if (current.getParentGroup() != group)
          break;
        current.createSchemaElement(builder, dom);
        current = current.getNextHandler();
      }
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