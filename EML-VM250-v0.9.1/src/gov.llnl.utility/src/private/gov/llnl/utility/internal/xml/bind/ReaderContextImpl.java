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
import gov.llnl.utility.URIUtilities;
import gov.llnl.utility.annotation.Debug;
import gov.llnl.utility.annotation.Internal;
import gov.llnl.utility.annotation.Matlab;
import gov.llnl.utility.io.PathLocation;
import gov.llnl.utility.io.ReaderException;
import gov.llnl.utility.xml.bind.DocumentReader;
import gov.llnl.utility.xml.bind.Reader;
import gov.llnl.utility.xml.bind.Reader.AnyHandler;
import gov.llnl.utility.xml.bind.Reader.ElementHandler;
import gov.llnl.utility.xml.bind.Reader.Options;
import gov.llnl.utility.xml.bind.ReaderContext;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import gov.llnl.utility.xml.bind.PropertyMap;

/**
 *
 * @author nelson85
 */
@Internal
public class ReaderContextImpl implements ReaderContext
{
  // Parameters
  @Debug public boolean useCache = true;

  // Information on the current document being loaded
  private URI currentFile = null;
  private URI currentPath = null;
  private Locator locator;

  // State variables
  private final TreeMap<String, Object> documentReferences = new TreeMap<>();

  private HandlerContextImpl lastHandlerContext;
  private Object lastObject = null;
  HandlerContextImpl currentHandlerContext = null;
  private Deferred deferred = new Deferred();
  private ExceptionHandler exceptionHandler = null;
  private boolean contextAccessed = false;
  private DocumentReader documentReader;
  PropertyMap propertyHandler = null;

  public ReaderContextImpl()
  {
    this.currentHandlerContext = null;

    // Clear the origin information 
    this.currentFile = null;
    this.currentPath = null;

    // Variable from the previous document need to be cleared
    this.documentReferences.clear();

    // All document state information is stale
    this.lastObject = null;
    this.lastHandlerContext = null;

    // Deferred actions is stale
    this.deferred.clear();
  }

//<editor-fold desc="location-handling" defaultstate="collapsed">
  @Override
  public PathLocation getLocation()
  {
    String element = getElementPath();
    return new PathLocation(currentFile, locator != null ? locator.getLineNumber() : -1, element);
  }

  @Override
  public String getElementPath()
  {
    StringBuilder sb = new StringBuilder();
    HandlerContextImpl context = this.currentHandlerContext;
    while (context != null)
    {
      sb.insert(0, context.elementName);
      sb.insert(0, '/');
      context = context.getPreviousContext();
    }
    return sb.toString();
  }

  public void setFile(URI file)
  {
    if (file == null)
      throw new NullPointerException();
    this.currentFile = file;
    this.currentPath = URIUtilities.resolve(file, ".");
  }

  @Override
  public URI getFile()
  {
    return currentFile;
  }

  /**
   * Set the SAX locator for error reporting.
   */
  void setLocator(Locator locator)
  {
    this.locator = locator;
  }
//</editor-fold>
//<editor-fold desc="temporary-context" defaultstate="collapsed">

  /**
   * Push a temporary context for a deferred action
   *
   * @param parent
   * @param child
   */
  void pushTemporaryContext(Object parent, Object child)
  {
    HandlerContextImpl context = new HandlerContextImpl();
    context.parentObject = parent;
    context.targetObject = child;
    context.elementName = "#deferred";
    context.previousContext = this.currentHandlerContext;
    this.currentHandlerContext = context;
  }

  /**
   * Pop a temporary context for a deferred action.
   */
  void popTemporaryContext()
  {
    this.currentHandlerContext = this.currentHandlerContext.getPreviousContext();
  }
//</editor-fold>
//<editor-fold desc="deferred" defaultstate="collapsed">  

  class Deferred
  {
    TreeMap<String, LinkedList<DeferredHandler>> map = new TreeMap<>();

    public void add(String refId, DeferredHandler handler)
    {
      LinkedList<DeferredHandler> obj = map.get(refId);
      if (obj == null)
      {
        obj = new LinkedList<>();
        map.put(refId, obj);
      }
      obj.add(handler);
    }

    public LinkedList<DeferredHandler> get(String ref)
    {
      return map.get(ref);
    }

    public void clear(String ref)
    {
      map.remove(ref);
    }

    private void clear()
    {
      map.clear();
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T, T2> void addDeferred(T target, BiConsumer<T, T2> method, String refId, Class<T2> cls) throws ReaderException
  {
    if (refId == null)
      return;
    ReferenceHandler rh = new ReferenceHandler(null, null, target, cls, method);
    DeferredHandler dh = new DeferredHandler(this, rh, target, target, false);
    this.addDeferred(refId, dh);
  }

  void addDeferred(String refId, DeferredHandler handler) throws ReaderException
  {
    Object f = documentReferences.get(refId);
    if (f != null)
    {
      // Force the lastHandlerContext to be null for this call
      HandlerContextImpl prev = this.lastHandlerContext;
      this.lastHandlerContext = null;
      handler.executeDeferred(f);
      this.lastHandlerContext = prev;
    }
    else
      deferred.add(refId, handler);
  }

  public boolean hasDeferred()
  {
    return !deferred.map.isEmpty();
  }

  /**
   * Produce a list of all deferred objects that are pending.
   *
   * @return a string with the name of all the deferred elements.
   */
  public String getDeferredElements()
  {
    StringBuilder sb = new StringBuilder();
    for (String entry : this.deferred.map.keySet())
    {
      sb.append(entry).append(" ");
    }
    return sb.toString();
  }

//</editor-fold>
//<editor-fold desc="external" defaultstate="collapsed">
  @Override
  public URL getExternal(String file) throws ReaderException
  {
    try
    {
      if (file == null || file.isEmpty())
        throw new ReaderException("Null external resource file specified.");

      URI f = URIUtilities.resolve(currentPath, file);

      // Verify that the result exists
      if (URIUtilities.exists(f))
        // Try to convert the uri to a url.
        return f.toURL();

      // Search the path for file
      Path[] paths = (Path[]) this.getDocumentReader().getProperty(DocumentReader.SEARCH_PATHS);
      if (paths == null)
        throw new ReaderException("Unable to locate external reference " + file);
      for (Path path : paths)
      {
        Path out = path.resolve(file).toAbsolutePath().normalize();
        if (Files.exists(out))
          return out.toUri().toURL();
      }
      throw new ReaderException("Unable to locate external reference " + file);
    }
    catch (MalformedURLException ex)
    {
      throw new ReaderException("Error locating external reference " + file, ex);
    }
  }
//</editor-fold>
//<editor-fold desc="object-referencing" defaultstate="collapsed">

  @Override
  public <Obj> Obj put(String name, Obj object) throws ReaderException
  {
    if (object == null)
    {
      documentReferences.remove(name);
      return null;
    }
    documentReferences.put(name, object);

    // Execute any deferred actions that were pending
    LinkedList<DeferredHandler> actions = deferred.get(name);
    if (actions != null)
    {
      for (DeferredHandler a : actions)
      {
        a.executeDeferred(object);
      }
      deferred.clear(name);
    }
    return object;
  }

  @Override
  public <Obj> Obj putScoped(String name, Obj object)
  {
    return this.currentHandlerContext.putReference(name, object);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T get(String name, Class<T> kls) throws ReaderException
  {
    Object o = this.currentHandlerContext.getReference(name);
    if (o == null)
      o = documentReferences.get(name);
    if (o == null && propertyHandler != null)
      o = propertyHandler.get(name);
    if (o == null)
      return null;

    if (kls.isInstance(o))
      return (T) o;
    else
      throw new ReaderException("Reference type mismatch " + o.getClass() + " is not a " + kls);
  }

  @Override
  public List<Map.Entry<String, Object>> getReferences()
  {
    List<Map.Entry<String, Object>> out = new LinkedList<>();
    for (Map.Entry<String, Object> entry : documentReferences.entrySet())
    {
      out.add(entry);
    }
    return out;
  }

  /**
   * Get an object referenceName without typecast. This is for MATLAB debugging
   *
   * @param name is the name of the reference to find.
   * @return the object with the requested name or null if not found.
   */
  @Matlab
  public Object get(String name)
  {
    try
    {
      return get(name, Object.class);
    }
    catch (ReaderException ex)
    {
      return null;
    }
  }

  @Override
  public void setPropertyHandler(PropertyMap handler)
  {
    this.propertyHandler = handler;
  }

//</editor-fold>
//<editor-fold desc="currentHandler-contexts">
  @Override
  public HandlerContextImpl getCurrentHandlerContext()
  {
    contextAccessed = true;
    return currentHandlerContext;
  }

  @Override
  public HandlerContextImpl getLastHandlerContext()
  {
    return lastHandlerContext;
  }

  /**
   * Start of element delegated from SaxHandler.
   *
   * @param namespaceURI
   * @param localName
   * @param qualifiedName
   * @param attr
   * @return
   * @throws ReaderException
   */
  @SuppressWarnings("unchecked")
  HandlerContextImpl startElement(String namespaceURI, String localName,
          String qualifiedName, Attributes attr)
          throws ReaderException
  {
    Reader.ElementHandlerMap handlers = null;
    try
    {
      HandlerContextImpl previous = currentHandlerContext;

      // Get the nextHandler currentHandler
      if (previous == null)
        throw new ReaderException("internal error");

      if (previous.getHandlerMap() == null)
        throw new ReaderException(previous.elementName + " does not support children");
      handlers = previous.getHandlerMap();

      // Get the currentHandler for this element
      if (namespaceURI.isEmpty())
        namespaceURI = previous.namespaceURI;
      ElementHandlerImpl handler = (ElementHandlerImpl) handlers.get(namespaceURI, localName);

      // Handle the case where we have "any"
      if (handler == null)
      {
        AnyHandler any = handlers.getAnyHandler(previous.getLastHandler());
        if (any != null)
        {
          handler = (ElementHandlerImpl) any.getHandler(namespaceURI, localName, qualifiedName, attr);
          previous.lastHandler = any;
        }
      }
      else
        previous.lastHandler = handler;

      if (handler == null)
      {
        System.out.println("Dump handlers:");
        handlers.dump(System.out);
        throw new ReaderException(localName + " is not valid child for " + previous.elementName);
      }

      Object parentObject = handler.getParent(previous);

      // Handler documentReferences
      HandlerContextImpl referenceContext = this.handleReferences(handler, attr, parentObject, previous);
      if (referenceContext != null)
      {
        referenceContext.elementName = localName;
        currentHandlerContext = referenceContext;
        handleId(handler, attr, referenceContext);
        return referenceContext;
      }

      // Otherwise, proceed to set up the currentHandler context
      HandlerContextImpl context = handler.cache;
      if (context == null)
        context = new HandlerContextImpl();

      context.attributes = attr;
      context.namespaceURI = namespaceURI;
      context.elementName = localName;
      context.previousContext = previous;
      context.parentObject = parentObject;
      context.targetObject = null;
      context.currentHandler = handler;
      context.referenceName = null;
      currentHandlerContext = context;

      // Set up to capture text
      if (handler.hasTextContent())
        context.textContent = new StringBuilder();

      // Set up referencing
      handleId(handler, attr, context);

      // Call the currentHandler on start
      Object target = handler.onStart(this, attr);
      if (target != null)
        context.targetObject = target;

      // After we have called start create the currentHandler map.
      // We do this now to ensure the currentHandler has access
      // to getReference() if needed.  Be sure to set
      // Options.Internal.NO_CACHE if we need a fresh currentHandler map.
      this.contextAccessed = false;
      EnumSet<Options> options = handler.getOptions();
      if (context.getHandlerMap() == null
              || (options != null && options.contains(Options.NO_CACHE)))
        context.handlerMap = handler.getHandlers();

      // Automatically set the nocache flag if the handler accessed the context
      if (contextAccessed)
      {
        if (handler.options == null)
          handler.options = EnumSet.of(Options.NO_CACHE);
        else
          handler.options.add(Options.NO_CACHE);
      }

      // Cache it for speed.
      handler.cache = context;
      return context;
    }
    catch (ReaderException ex)
    {
      if (handlers == null)
        throw ex;

      throw ex.setReferencePoint(handlers.getTrace());
    }
    catch (RuntimeException ex)
    {
      if (handlers == null)
        throw new ReaderException(ex);
      throw new ReaderException(ex).setReferencePoint(handlers.getTrace());
    }
  }

  /**
   * End of element delegated from SaxHandler.
   *
   * @return
   * @throws ReaderException
   */
  HandlerContextImpl endElement() throws ReaderException
  {
    HandlerContextImpl context = this.currentHandlerContext;
    try
    {
      ElementHandlerImpl handler = (ElementHandlerImpl) context.getCurrentHandler();

      if (!context.isReference)
      {
        // Handle contents
        if (handler.hasTextContent())
        {
          String textContents = context.textContent.toString();
          if (context.textContent == null)
            throw new ReaderException("Text contents not found");
          Object target = handler.onTextContent(context.targetObject, textContents);
          if (target != null)
            context.targetObject = target;
        }

        // Finish the object and call post actions
        Object target = handler.onEnd(context.targetObject);
        if (target != null)
          context.targetObject = target;
      }

      // Pop the stack
      this.currentHandlerContext = currentHandlerContext.getPreviousContext();
      this.lastHandlerContext = context;

      handler.onCall(this, context.parentObject, context.targetObject);

      // Reference on element close
      if (context.referenceName != null)
        this.put(context.referenceName, context.targetObject);

      this.setLastObject(context.targetObject);
      return this.currentHandlerContext;
    }
    catch (ReaderException ex)
    {
      if (currentHandlerContext == null
              || currentHandlerContext.getPreviousContext() == null
              || currentHandlerContext.getPreviousContext().handlerMap == null)
        throw ex;

      // If we are within a handling routine, mark the location of the document 
      // to be printed out.
      Reader.ElementHandlerMap handlers = this.currentHandlerContext.handlerMap;
      if (handlers != null)
        throw ex.setReferencePoint(handlers.getTrace());
      else
        throw ex;
    }
  }

  private void handleId(ElementHandler handler, Attributes attr, HandlerContextImpl context)
  {
    // FIXME this is likely wrong
    EnumSet<Options> options = handler.getOptions();
    if (options == null || !options.contains(Options.NO_ID))
    {
      String id = attr.getValue("id");
      if (id != null)
        context.referenceName = id;
    }
  }

  @SuppressWarnings("unchecked")
  private HandlerContextImpl handleReferences(ElementHandler handler,
          Attributes attr, Object parentObject, HandlerContextImpl previous)
          throws ReaderException
  {
    boolean copy = false;

    // Handlers that don't support id can't have documentReferences
    EnumSet<Options> options = handler.getOptions();
    if (options != null && options.contains(Options.NO_ID))
      return null;

    // Check for referenceName attribute
    String refId = attr.getValue("ref_id");
    String copyId = attr.getValue("copy_of");

    if (refId != null && copyId != null)
      throw new ReaderException("copy_of and ref_id are mutually exclusive.");

    // Check for requirements
    if (handler.mustReference() && refId == null)
      throw new ReaderException("element must be a reference");

    if (copyId != null)
    {
      refId = copyId;
      copy = true;
    }
    // If there is no referenceName attribute, then no need to continue
    if (refId == null)
      return null;

    if (options != null && options.contains(Options.NO_REFERENCE))
      throw new ReaderException("element must not be a reference");

    Object targetObject = null;
    // handle the referenceName
    // deferred currentHandler will grab parentGroup from the context in onStart
    if (parentObject == null)
      throw new ReaderException("Parent was not set when deferring action.");

    // If the referenceName must already be resolved, execute immediately
    if (options == null || !options.contains(Options.DEFERRABLE))
    {
      targetObject = this.get(refId, handler.getTargetClass());
      if (targetObject == null)
        throw new ReaderException("Unable to find reference for " + refId);
      if (copy == true && targetObject instanceof Copyable)
        targetObject = ((Copyable) targetObject).copyOf();
    }
    else
    {
      DeferredHandler deferredHandler
              = new DeferredHandler(this, handler, previous.targetObject, parentObject, copy);
      this.addDeferred(refId, deferredHandler);
      handler = deferredHandler;
    }

    // Set up a new context
    HandlerContextImpl context = new HandlerContextImpl();
    context.attributes = attr;
    context.previousContext = previous;
    context.currentHandler = handler;
    context.handlerMap = null;
    context.parentObject = parentObject;
    context.targetObject = targetObject;
    context.isReference = true;
    return context;
  }
//</editor-fold>

  /**
   * @param lastObject the lastObject to set
   */
  public void setLastObject(Object lastObject)
  {
    this.lastObject = lastObject;
  }

  /**
   * Get the last object constructed.
   *
   * @return the last object constructed.
   */
  @Override
  public Object getLastObject()
  {
    return lastObject;
  }

  @Override
  public void setErrorHandler(ExceptionHandler handler)
  {
    this.exceptionHandler = handler;
  }

  @Override
  public void handleException(Throwable ex) throws ReaderException
  {
    if (this.exceptionHandler == null)
      throw new ReaderException("Unhandled exception", ex);
    exceptionHandler.handle(this, ex);
  }

  /**
   * @return the documentReader
   */
  public DocumentReader getDocumentReader()
  {
    return documentReader;
  }

  /**
   * @param documentReader the documentReader to set
   */
  public void setDocumentReader(DocumentReader documentReader)
  {
    this.documentReader = documentReader;
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