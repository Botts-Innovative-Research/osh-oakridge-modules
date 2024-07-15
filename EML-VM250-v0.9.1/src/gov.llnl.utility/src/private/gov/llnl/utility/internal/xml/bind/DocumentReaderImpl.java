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

import gov.llnl.utility.PackageResource;
import gov.llnl.utility.PropertyUtilities;
import gov.llnl.utility.UtilityPackage;
import gov.llnl.utility.annotation.Internal;
import gov.llnl.utility.internal.xml.bind.SaxHandler.SAXExceptionProxy;
import gov.llnl.utility.io.MD5FilterInputStream;
import gov.llnl.utility.io.ReaderException;
import gov.llnl.utility.xml.bind.*;
import gov.llnl.utility.xml.bind.Reader.ElementHandler;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.zip.GZIPInputStream;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

/**
 * Base class for all ObjectReaders that can load a document. DocumentReader can
 * be included or imported.
 *
 * @param <Component> is the type of object produced by this document.
 */
@Internal
@SuppressWarnings("unchecked")
public class DocumentReaderImpl<Component> implements DocumentReader<Component>
{
  ReaderContextImpl readerContext;
  ObjectReader objectReader;
  Class<Component> cls;
  TreeMap<String, Object> properties = new TreeMap<>();
  ReaderContext.ExceptionHandler exceptionHandler;
  private PropertyMap propertyHandler;

  public DocumentReaderImpl(ObjectReader reader)
  {
    if (reader == null)
      throw new NullPointerException();
    this.objectReader = reader;
    setSchema(reader);
  }

  void setSchema(ObjectReader reader)
  {
    if (reader == null)
      return;

    PackageResource pkg = reader.getPackage();
    Reader.Declaration attr = reader.getClass().getAnnotation(Reader.Declaration.class);
    if (!attr.schema().equals(Reader.Declaration.NULL))
    {
      this.properties.put(SCHEMA_SOURCE, attr.schema());
    }
    else if (pkg != null)
    {
      this.properties.put(SCHEMA_SOURCE, pkg.getSchemaURI());
    }
  }

  public DocumentReaderImpl(Class<Component> cls)
  {
    if (cls == null)
      throw new NullPointerException();
    this.objectReader = null;
    this.cls = cls;
    try
    {
      setSchema(ObjectReader.create(cls));
    }
    catch (ReaderException ex)
    {
    }
  }

//<editor-fold desc="readerContext" defaultstate="collapsed">
  @Override
  public ReaderContext getContext()
  {
    return readerContext;
  }

  @Override
  public void clearContext()
  {
    readerContext = null;
  }

  public ReaderContextImpl createContext() throws ReaderException
  {
    readerContext = new ReaderContextImpl();
    if (cls == null)
    {
      String url = this.objectReader.getPackage().getNamespaceURI();
      readerContext.currentHandlerContext = createContextHandler(url);
    }
    else
      readerContext.currentHandlerContext = createContextAnyHandler();

    readerContext.setDocumentReader(this);
    readerContext.setErrorHandler(exceptionHandler);
    readerContext.setPropertyHandler(n -> this.getProperty(n));

    return readerContext;
  }

  /**
   * Create the context handler for the root element of the document.
   *
   * @param document
   * @return
   */
  private HandlerContextImpl createContextHandler(String url)
  {
    HandlerContextImpl context = new HandlerContextImpl();
    context.elementName = "";
    context.currentHandler = new ElementHandlerImpl(null, null, null, null, null);
    context.namespaceURI = url;

    ReaderHandler rootHandler = new ReaderHandler(objectReader.getHandlerKey(),
            EnumSet.of(Reader.Options.NO_REFERENCE),
            null, null, objectReader);
    ReaderBuilderImpl.HandlerList hl = new ReaderBuilderImpl.HandlerList();
    hl.firstHandler = rootHandler;
    context.handlerMap = ElementHandlerMapImpl.newInstance("#", hl);
    return context;
  }

  private HandlerContextImpl createContextAnyHandler()
  {
    HandlerContextImpl context = new HandlerContextImpl();
    context.elementName = "";
    context.currentHandler = new ElementHandlerImpl(null, null, null, null, null);

    ElementHandler rootHandler = new AnyHandlerImpl(
            EnumSet.of(Reader.Options.NO_REFERENCE),
            null, null, null, new AnyContents(cls));
    ReaderBuilderImpl.HandlerList hl = new ReaderBuilderImpl.HandlerList();
    hl.firstHandler = rootHandler;
    context.handlerMap = ElementHandlerMapImpl.newInstance("#", hl);
    return context;
  }

//</editor-fold>
//<editor-fold desc="load" defaultstate="collapsed">
  @Override
  public Component loadStream(InputStream stream) throws ReaderException
  {
    InputSource inputSource = new InputSource();
    inputSource.setByteStream(stream);
    inputSource.setSystemId("stream");
    return (Component) loadSource(inputSource);
  }

  @Override
  public Component loadResource(String resourceName) throws ReaderException, IOException
  {
    URL url = getClass().getClassLoader().getResource(resourceName);
    if (url == null)
      throw new RuntimeException("Unable to locate resource " + resourceName);
    return loadURL(url);
  }

  @Override
  public synchronized Component loadFile(Path file) throws ReaderException, FileNotFoundException, IOException
  {
    ReaderContextImpl newContext = (ReaderContextImpl) createContext();
    newContext.setFile(file.toUri());
    try ( InputStream fs = Files.newInputStream(file))
    {
      InputStream fs2 = fs;
      UtilityPackage.LOGGER.log(Level.FINE, "File {0}", file);
      if (file.getFileName().toString().endsWith(".gz"))
      {
        fs2 = new GZIPInputStream(fs);
      }
      InputSource inputSource = new InputSource();
      inputSource.setByteStream(fs2);
      inputSource.setSystemId(file.toString());
      return (Component) loadSourceInternal(inputSource);
    }
  }

  @Override
  public synchronized Component loadURL(URL url) throws IOException, ReaderException
  {
    try
    {
      ReaderContextImpl newContext = (ReaderContextImpl) createContext();
      newContext.setFile(url.toURI());
      URLConnection connection = url.openConnection();
      connection.setUseCaches(false);
      try ( InputStream is = connection.getInputStream())
      {
        InputStream is2 = is;
        if (url.getFile().endsWith(".gz"))
          is2 = new GZIPInputStream(is);
        InputSource source = new InputSource();
        source.setByteStream(is2);
        source.setSystemId(url.toString());
        return (Component) loadSourceInternal(source);
      }
    }
    catch (URISyntaxException ex)
    {
      throw new ReaderException(ex);
    }
  }

//</editor-fold>
//<editor-fold desc="internal" defaultstate="collapsed">
  @Override
  public synchronized Component loadSource(InputSource inputSource) throws ReaderException
  {
    this.readerContext = (ReaderContextImpl) createContext();
    this.readerContext.setFile(URI.create(inputSource.getSystemId()));
    return loadSourceInternal(inputSource);
  }

  private synchronized Component loadSourceInternal(InputSource inputSource) throws ReaderException
  {
    boolean computeChecksum = false;
    Map<String, String> checksums = null;
    Class contentClass = this.cls;
    if (contentClass == null)
    {
      contentClass = this.objectReader.getObjectClass();
    }

    Class<Hook> hookClass;
    Hook hook = null;
    Hooks annotation = (Hooks) contentClass.getAnnotation(Hooks.class);
    if (annotation != null)
    {
      hookClass = (Class<Hook>) annotation.value();

      try
      {
        hook = hookClass.getConstructor().newInstance();
      }
      catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException ex)
      {
        // Reflection failed not use.
      }
    }

    if (hook != null)
    {
      hook.startDocument(this);
    }

    Object propComputeChecksum = properties.get(DocumentReader.COMPUTE_MD5SUM);
    if (propComputeChecksum != null && (Boolean) propComputeChecksum == true)
    {
      if (!properties.containsKey(DocumentReader.RESULT_MD5SUM))
        properties.put(DocumentReader.RESULT_MD5SUM, new TreeMap<>());
      computeChecksum = true;
      checksums = (Map<String, String>) properties.get(DocumentReader.RESULT_MD5SUM);
      inputSource.setByteStream(new MD5FilterInputStream(inputSource.getByteStream()));
    }

    try
    {
      SAXParserFactory spf = SAXParserFactory.newInstance();

      // Only turn validate off if the system property is set to false
      boolean validate = PropertyUtilities.get("gov.llnl.utility.xml.validation", true);

      spf.setValidating(validate);
      spf.setNamespaceAware(true);
      SAXParser saxParser = spf.newSAXParser();

      // Path for validation
      SchemaManagerImpl schemaMgr = (SchemaManagerImpl) SchemaManager.getInstance();
      if (validate)
      {
        saxParser.setProperty("http://java.sun.com/xml/jaxp/properties/schemaLanguage",
                "http://www.w3.org/2001/XMLSchema");

        // Install the user requested schema
        String schema = this.getProperty(SCHEMA_SOURCE, String.class);
        if (schema != null)
        {
          saxParser.setProperty(SCHEMA_SOURCE, schema);
        }
      }
      else
      {
        if (this.getProperty(SCHEMA_SOURCE) != null)
        {
          try
          {
            URL i = schemaMgr.mangleURI(new URI((String) this.getProperty(SCHEMA_SOURCE)));
            schemaMgr.scanSchema(i);
          }
          catch (URISyntaxException ex)
          {
            throw new ReaderException("Unable to handle SCHEMA_SOURCE", ex);
          }
        }
      }

      XMLReader xmlReader = saxParser.getXMLReader();
      SaxHandler sh = new SaxHandler(readerContext);
      sh.setValidate(validate);
      xmlReader.setErrorHandler(sh);
      xmlReader.setEntityResolver(schemaMgr);

      // Apply an xslt if requested
      if (this.getProperty(XSLT_SOURCE) != null)
      {
        try
        {
          // Parse the xslt into a Path
          Path xsltPath;
          Object xslt = this.getProperty(XSLT_SOURCE);
          if (xslt instanceof String)
          {
            xsltPath = Paths.get((String) xslt);
          }
          else if (xslt instanceof Path)
          {
            xsltPath = (Path) xslt;
          }
          else
          {
            throw new ReaderException("Unknown xslt type");
          }

          // Use the reader to parse the input into a source for the xslt transform
          SAXSource source = new SAXSource(inputSource);

          // Create an XSLT transform using the xslt document
          StreamSource styleSource = new StreamSource(xsltPath.toFile());
          TransformerFactory transformerFactory = TransformerFactory.newInstance();
          Transformer transformer = transformerFactory.newTransformer(styleSource);

          // The result will be pushed to our SAX handler
          ByteArrayOutputStream bs = new ByteArrayOutputStream();
          StreamResult result = new StreamResult(bs);  // Create a SAXResult with our contentHandler
          transformer.transform(source, result);

          // Parse the remaining output
          ByteArrayInputStream bis = new ByteArrayInputStream(bs.toByteArray());
          inputSource.setByteStream(bis);
          xmlReader.setContentHandler(sh);
          xmlReader.parse(inputSource);
        }
        catch (TransformerException ex)
        {
          throw new ReaderException(ex);
        }
      }
      else
      {
        xmlReader.setContentHandler(sh);
        xmlReader.parse(inputSource);
      }
      // If this the final scope then we must resolve all deferred actions.
      if (readerContext.hasDeferred())
      {
        String deferred = readerContext.getDeferredElements();
        throw new ReaderException("unresolved deferred elements " + deferred);
      }

      if (computeChecksum)
      {
        String ns = readerContext.getLastHandlerContext().namespaceURI;
        StringBuilder sb = new StringBuilder();
        if (ns != null)
        {
          sb.append(ns);
          sb.append("#");
        }
        sb.append(readerContext.getLastHandlerContext().elementName);
        checksums.put(sb.toString(), ((MD5FilterInputStream) inputSource.getByteStream()).getChecksum());
      }

      if (hook != null)
      {
        hook.endDocument(this);
      }

      return (Component) readerContext.getLastObject();
    }
    catch (SAXExceptionProxy ex)
    {
      Throwable cause = ex.exception;
      if (cause instanceof ReaderException)
        throw ((ReaderException) cause).addPathLocation(readerContext.getLocation());
      throw new ReaderException("Error in sax parsing " + cause.getMessage() + " <<", cause)
              .addPathLocation(readerContext.getLocation());
    }
    catch (SAXParseException ex)
    {
      throw new ReaderException("Error in sax parsing " + ex.getLineNumber(), ex)
              .addPathLocation(readerContext.getLocation());
    }
    catch (SAXException ex)
    {
      throw new ReaderException("Error in sax parsing", ex)
              .addPathLocation(readerContext.getLocation());
    }
    catch (IOException ex)
    {
      throw new ReaderException("IO Error in sax parsing", ex)
              .addPathLocation(readerContext.getLocation());
    }
    catch (ParserConfigurationException ex)
    {
      throw new RuntimeException(ex);
    }

  }

//</editor-fold>
  @Override
  public ObjectReader getObjectReader()
  {
    return this.objectReader;
  }

  @Override
  public void setErrorHandler(ReaderContext.ExceptionHandler exceptionHandler)
  {
    this.exceptionHandler = exceptionHandler;
  }

  @Override
  public void setProperty(String key, Object value)
  {
    this.properties.put(key, value);
  }

  @Override
  public Object getProperty(String key)
  {
    Map.Entry<String, Object> entry = this.properties.ceilingEntry(key);
    if (entry != null && entry.getKey().equals(key))
      return entry.getValue();
    if (this.propertyHandler == null)
      return null;
    return this.propertyHandler.get(key);
  }

  @Override
  public Map<String, Object> getProperties()
  {
    return Collections.unmodifiableMap(properties);
  }

  @Override
  public void setPropertyHandler(PropertyMap handler)
  {
    this.propertyHandler = handler;
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