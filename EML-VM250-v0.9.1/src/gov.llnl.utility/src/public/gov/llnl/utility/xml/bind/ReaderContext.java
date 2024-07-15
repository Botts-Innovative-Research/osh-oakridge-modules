/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.utility.xml.bind;

import gov.llnl.utility.annotation.Internal;
import gov.llnl.utility.io.PathLocation;
import gov.llnl.utility.io.ReaderException;
import java.io.PrintStream;
import java.net.URI;
import java.net.URL;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * State of the ObjectReader while processing.
 *
 * This holds attributes and all references.
 */
public interface ReaderContext
{
  /**
   * @return the documentReader
   */
  DocumentReader getDocumentReader();

  /**
   * Get the last object constructed.
   *
   * @return the last object constructed.
   */
  Object getLastObject();

  /**
   * Get an object reference associated with this context.
   *
   * @param <T>
   * @param name is the name of the element.
   * @param cls is the class for requested reference.
   * @return the references object or null if not found.
   * @throws ReaderException if the element reference is not the correct type.
   */
  <T> T get(String name, Class<T> cls) throws ReaderException;

  Iterable<Map.Entry<String, Object>> getReferences();

  /**
   * Define a global object reference.
   *
   * @param <Obj>
   * @param name is the name of the reference
   * @param object is the object to reference or null to remove the reference.
   * @return the object.
   * @throws ReaderException if the definition results in an exception in a
   * deferred action.
   */
  <Obj> Obj put(String name, Obj object) throws ReaderException;

  /**
   * Define a scoped object reference.
   *
   * @param <Obj>
   * @param name is the name of the reference
   * @param object is the object to reference or null to remove the reference.
   * @return the object.
   * @throws ReaderException if the definition results in an exception in a
   * deferred action.
   */
  <Obj> Obj putScoped(String name, Obj object) throws ReaderException;

  /**
   * Get the current element path for this context. Used primarily to print
   * exception debugging.
   *
   * @return a string describing the location in the document.
   */
  String getElementPath();

  /**
   * Fetch an external referenced object relative to this context.
   *
   * Uses the DocumentReader.SEARCH_PATHS property to locate any resources that
   * are not found in the same directory as the document being processed.
   *
   * @param extern is the URL name of the entity to locate.
   * @return the URL of the named object.
   * @throws ReaderException if the resource cannot be located or the external
   * resource is malformed.
   *
   */
  URL getExternal(String extern) throws ReaderException;

  /**
   * Gets the current file being loaded.
   *
   * @return the URI for the file being processed.
   */
  URI getFile();

  /**
   * Returns the current document location. PathLocation includes the file, line
   * number, and element path.
   *
   * @return the path location in the current document.
   */
  PathLocation getLocation();

  /**
   * Gets the location in the current processing of the sax file. This includes
   * the attributes and element name.
   *
   * @return the current context handler.
   */
  HandlerContext getCurrentHandlerContext();

  /**
   * Gets the location in the last handler processing of the sax file. This
   * includes the attributes and element name. This function is used to view the
   * attributes when processing a directive.
   *
   * @return the context for the last handler.
   */
  HandlerContext getLastHandlerContext();

  public interface ExceptionHandler
  {
    void handle(ReaderContext context, Throwable ex) throws ReaderException;
  }

  /**
   * A method for handling all checked exceptions that c
   *
   * @param handler
   */
  void setErrorHandler(ExceptionHandler handler);

  void handleException(Throwable ex) throws ReaderException;

  void setPropertyHandler(PropertyMap handler);

  /** Defers an action until a reference is defined.
   * 
   * @param <T>
   * @param <T2>
   * @param target
   * @param method
   * @param refId
   * @param cls
   * @throws ReaderException 
   */
  <T, T2> void addDeferred(T target, BiConsumer<T, T2> method, String refId, Class<T2> cls) throws ReaderException;

  /**
   *
   */
  @Internal
  public static interface HandlerContext
  {
    void characters(char[] chars, int start, int length);

    Object getTargetObject();

    Object getParentObject();

    Object getReference(String name) throws ReaderException;

    <T> T putReference(String name, T obj);

    void dumpTrace(PrintStream out);

    String getNamespaceURI();

    String getLocalName();

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