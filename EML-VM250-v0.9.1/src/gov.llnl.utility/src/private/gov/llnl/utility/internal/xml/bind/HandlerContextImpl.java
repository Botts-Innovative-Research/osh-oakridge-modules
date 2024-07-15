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
import gov.llnl.utility.xml.bind.ReaderContext;
import java.io.PrintStream;
import java.util.TreeMap;
import org.xml.sax.Attributes;
import gov.llnl.utility.xml.bind.Reader;
import gov.llnl.utility.xml.bind.Reader.ElementHandler;

/**
 *
 * @author nelson85
 */
@Internal
public class HandlerContextImpl implements ReaderContext.HandlerContext
{
  HandlerContextImpl previousContext;

  // State information
  ElementHandler currentHandler;
  ElementHandler lastHandler;
  Reader.ElementHandlerMap handlerMap;
  public Object parentObject;
  public Object targetObject;
  String referenceName;
  boolean isReference = false;

  // Element contents
  public String namespaceURI;
  public String elementName;
  public Attributes attributes;
  public StringBuilder textContent;

  // Scoped referenced
  public TreeMap<String, Object> references;

  @Override
  public String getNamespaceURI()
  {
    return namespaceURI;
  }

  @Override
  public String getLocalName()
  {
    return elementName;
  }

  @Override
  public void characters(char[] chars, int start, int length)
  {
    if (textContent == null)
      return;
    textContent.append(chars, start, length);
  }

  public void dump(PrintStream ps)
  {
    ps.println("previousContext=" + previousContext);
    ps.println("handler=" + currentHandler);
    ps.println("handlerMap=" + handlerMap);
    ps.println("parentObject=" + parentObject);
    ps.println("targetObject=" + targetObject);
    ps.println("reference=" + referenceName);
    ps.println("textContent=" + textContent);
    ps.println("elementName=" + elementName);
  }

//  /**
//   * Search the context to find the parent object in the previous context. The
//   * currentHandler can override the parent.
//   *
//   * @param handler
//   */
//  public void setParentObject(Object obj)
//  {
//    parentObject = obj;
//  }
  @Override
  public Object getReference(String name)
  {
    HandlerContextImpl hc = this;
    while (hc != null)
    {
      if (hc.references != null)
      {
        Object obj = hc.references.get(name);
        if (obj != null)
          return obj;
      }
      hc = hc.previousContext;
    }
    return null;
  }

  @Override
  public <T> T putReference(String name, T obj)
  {
    if (references == null)
      references = new TreeMap<>();
    references.put(name, obj);
    return obj;
  }

  @Override
  public Object getTargetObject()
  {
    return this.targetObject;
  }

  @Override
  public Object getParentObject()
  {
    return this.parentObject;
  }

  /**
   * @return the previousContext
   */
  public HandlerContextImpl getPreviousContext()
  {
    return previousContext;
  }

  /**
   * @return the currentHandler
   */
  public ElementHandler getCurrentHandler()
  {
    return currentHandler;
  }

  /**
   * @return the lastHandler
   */
  public ElementHandler getLastHandler()
  {
    return lastHandler;
  }

  /**
   * @return the handlerMap
   */
  public Reader.ElementHandlerMap getHandlerMap()
  {
    return handlerMap;
  }

  @Override
  public void dumpTrace(PrintStream out)
  {
    HandlerContextImpl current = this;
    while (current != null)
    {
      out.println("HandlerContext:");
      out.println("  parent=" + current.parentObject);
      out.println("  target=" + current.targetObject);
      out.println("  handler=" + current.currentHandler);
      current = current.previousContext;
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