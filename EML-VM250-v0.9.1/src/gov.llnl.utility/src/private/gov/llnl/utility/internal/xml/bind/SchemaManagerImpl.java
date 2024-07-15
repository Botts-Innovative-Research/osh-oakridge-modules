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

import gov.llnl.utility.ClassUtilities;
import gov.llnl.utility.ListUtilities;
import gov.llnl.utility.StringUtilities;
import gov.llnl.utility.UtilityPackage;
import gov.llnl.utility.annotation.Internal;
import gov.llnl.utility.xml.bind.ReaderInfo;
import gov.llnl.utility.xml.bind.ReaderName;
import gov.llnl.utility.xml.bind.WriterInfo;
import gov.llnl.utility.internal.xml.bind.readers.AttributesReader;
import gov.llnl.utility.internal.xml.bind.readers.CharContents;
import gov.llnl.utility.internal.xml.bind.readers.DoubleArrayContents;
import gov.llnl.utility.internal.xml.bind.readers.DoubleContents;
import gov.llnl.utility.internal.xml.bind.readers.DoublesArrayContents;
import gov.llnl.utility.internal.xml.bind.readers.DurationContents;
import gov.llnl.utility.internal.xml.bind.readers.FileContents;
import gov.llnl.utility.internal.xml.bind.readers.FloatArrayContents;
import gov.llnl.utility.internal.xml.bind.readers.FloatContents;
import gov.llnl.utility.internal.xml.bind.readers.FloatsArrayContents;
import gov.llnl.utility.internal.xml.bind.readers.InstantContents;
import gov.llnl.utility.internal.xml.bind.readers.IntegerArrayContents;
import gov.llnl.utility.internal.xml.bind.readers.IntegerContents;
import gov.llnl.utility.internal.xml.bind.readers.LongArrayContents;
import gov.llnl.utility.internal.xml.bind.readers.LongContents;
import gov.llnl.utility.internal.xml.bind.readers.OffsetDateTimeContents;
import gov.llnl.utility.internal.xml.bind.readers.PrimitiveReaderImpl;
import gov.llnl.utility.internal.xml.bind.readers.StringContents;
import gov.llnl.utility.internal.xml.bind.readers.UUIDContents;
import gov.llnl.utility.io.ReaderException;
import gov.llnl.utility.io.WriterException;
import gov.llnl.utility.xml.bind.ObjectReader;
import gov.llnl.utility.xml.bind.ObjectWriter;
import gov.llnl.utility.xml.bind.Reader;
import gov.llnl.utility.xml.bind.SchemaManager;
import gov.llnl.utility.xml.bind.WriterContext;
import gov.llnl.utility.xml.bind.WriterContext.Marshaller;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 * @author nelson85
 */
@Internal
public final class SchemaManagerImpl implements SchemaManager, EntityResolver
{
//<editor-fold desc="singleton">
  private final TreeMap<String, Class<? extends ObjectReader>> readerMap = new TreeMap<>();
  private final TreeMap<String, Class<? extends ObjectWriter>> writerMap = new TreeMap<>();
  private final HashMap<String, String> classMap = new HashMap<>();
  private final HashMap<String, URL> aliasMap = new HashMap<>();
  final ArrayList<ReaderFactory> readerFactories = new ArrayList<>();
  final ArrayList<WriterFactory> writerFactories = new ArrayList<>();

  public SchemaManagerImpl()
  {
    // Register standard content reader classes
    registerReaderFor(String.class, StringContents.class);
    registerReaderFor(Path.class, FileContents.class);
    registerReaderFor(Duration.class, DurationContents.class);
    registerReaderFor(Instant.class, InstantContents.class);
    registerReaderFor(OffsetDateTime.class, OffsetDateTimeContents.class);
    registerReaderFor(AttributeContents.class, AttributeContentsReader.class);
    registerReaderFor(Integer.class, IntegerContents.class);
    registerReaderFor(Character.class, CharContents.class);
    registerReaderFor(Long.class, LongContents.class);
    registerReaderFor(Float.class, FloatContents.class);
    registerReaderFor(Double.class, DoubleContents.class);
    registerReaderFor(int[].class, IntegerArrayContents.class);
    registerReaderFor(long[].class, LongArrayContents.class);
    registerReaderFor(float[].class, FloatArrayContents.class);
    registerReaderFor(float[][].class, FloatsArrayContents.class);
    registerReaderFor(double[].class, DoubleArrayContents.class);
    registerReaderFor(double[][].class, DoublesArrayContents.class);
    registerReaderFor(Attributes.class, AttributesReader.class);
    registerReaderFor(UUID.class, UUIDContents.class);
  }

//</editor-fold>
//<editor-fold desc="utility">
  public URL mangleURI(URI uri)
  {
    // We only pretend to be http addresses
    if (!"http".equals(uri.getScheme()))
      return null;

    // Check alias map
    if (aliasMap.containsKey(uri.toString()))
      return aliasMap.get(uri.toString());

    StringBuilder sb = new StringBuilder();
    if (uri.getHost() != null)
      sb.append(StringUtilities.join(
              ListUtilities.reverse(
                      uri.getHost().split("\\.")), "/"));
    sb.append(uri.getPath());
    return this.getClass().getClassLoader().getResource(sb.toString());
  }
//</editor-fold>
//<editor-fold desc="get readers">

  @Override
  public Class<?> getObjectClass(String namespaceURI, String name)
          throws ClassNotFoundException
  {
    SchemaManagerImpl schemaMgr = (SchemaManagerImpl) SchemaManager.getInstance();
    String className = schemaMgr.lookupClass(namespaceURI, name);
    if (className == null)
      throw new ClassNotFoundException("Unable to find class for " + namespaceURI + " " + name);

    try
    {
      // Handling for primatives
      Class<?> cls;
      if (!className.contains("."))
        cls = ClassUtilities.forNamePrimative(className);
      else
        cls = Class.forName(className);
      return cls;
    }
    catch (NoClassDefFoundError ex)
    {
      throw new ClassNotFoundException("Unable to load class for " + namespaceURI + " " + name + " with " + className, ex);
    }
  }

//</editor-fold>
//<editor-fold desc="entity-resolver">
  @Override
  public InputSource resolveEntity(String publicId, String systemId)
          throws SAXException, IOException
  {
    try
    {
      UtilityPackage.LOGGER.log(Level.FINE, "Resolve entity {0} {1}", new Object[]
      {
        publicId, systemId
      });
      if (publicId != null)
        return null;

      // See if it is a jar resource
      URL url = mangleURI(new URI(systemId));
      if (url == null)
      {
        url = new URL(systemId);
      }

      UtilityPackage.LOGGER.log(Level.FINE, "Resolve to resource {0}", url);
      scanSchema(url);
      URLConnection connection = url.openConnection();
      connection.setUseCaches(false);
      InputStream is = connection.getInputStream();
      InputSource source = new InputSource(is);
      source.setSystemId(systemId);
      return source;
    }
    catch (URISyntaxException ex)
    {
      UtilityPackage.LOGGER.warning("Unable to parse systemId " + systemId);
      return null;
    }
  }

//</editor-fold>
//<editor-fold desc="maps">
  @Override
  public synchronized void alias(URI systemId, URL location)
  {
    this.aliasMap.put(systemId.toString(), location);
  }

  private synchronized void registerClass(String targetURI, String name, String cls)
  {
    classMap.put(targetURI + "#" + name, cls);
  }

  private synchronized String lookupClass(String namespaceURI, String name)
  {
    return classMap.get(namespaceURI + "#" + name);
  }

  /**
   * Get an object writer for class.
   * <p>
   * <ol>
   * <li>Use the context to look for a marshaller.</li>
   * <li>Use the WriterInfo annotation if it exists.</li>
   * <li></li>
   * <li></li>
   * <li></li>
   * <li></li>
   * </ol>
   *
   * @param context
   * @param cls
   * @return
   * @throws WriterException
   */
  @Override
  @SuppressWarnings("unchecked")
  public <T> ObjectWriter<T> findObjectWriter(WriterContext context, Class<T> cls) throws WriterException
  {
    try
    {
      if (context != null)
      {
        Marshaller marshaller = context.getMarshaller(cls);
        if (marshaller != null)
          return new ContentWriters.PrimitiveWriter(marshaller);
      }

      Class<? extends ObjectWriter> writerClass = null;
      WriterInfo annotation = cls.getAnnotation(WriterInfo.class);
      if (annotation != null)
        writerClass = annotation.value();
      else
        writerClass = this.writerMap.get(cls.getName());
      if (writerClass != null)
        return writerClass.getDeclaredConstructor().newInstance();

      // Use a writer factory
      for (WriterFactory rf : writerFactories)
      {
        ObjectWriter writer = rf.getWriter(cls);
        if (writer != null)
        {
          return writer;
        }
      }
      throw new WriterException("No writers defined for " + cls.getCanonicalName());
    }
    catch (WriterException | IllegalAccessException 
            | IllegalArgumentException | InstantiationException 
            | NoSuchMethodException | SecurityException | InvocationTargetException ex)
    {
      throw new WriterException("Unable to create writer for " + cls.getCanonicalName(), ex);
    }
  }

  /**
   * Find the object reader for the class.
   * <p>
   * Rules:
   * <ol>
   * <li>If primitive type, get the standard primitive reader.</li>
   * <li>Use the ReaderDecl annotation if it exists.</li>
   * <li>Use the ReaderInfo annotation if it exists.</li>
   * <li>Use the ReaderName annotation if it exists.</li>
   * <li>Use the reader registered in the SchemaManager.</li>
   * <li>Search for a ReaderFactory that produces a reader for the class.</li>
   * <li>Otherwise, throw a ReaderException.</li>
   * </ol>
   *
   * @param <T>
   * @param cls
   * @return
   * @throws ReaderException
   */
  @Override
  @SuppressWarnings("unchecked")
  public synchronized <T> ObjectReader<T> findObjectReader(Class<T> cls) throws ReaderException
  {
    // Primitives are easy
    if (cls.isPrimitive())
      return new PrimitiveReaderImpl(ClassUtilities.getPrimitive(cls));

    // Use annotations for internally defined classes
    Class<? extends ObjectReader> readerClass = null;
    try
    {
      // Check for annotations

      // If we have a ReaderDecl, this reader was generated automatically
      Reader.Declaration readerDecl = cls.getAnnotation(Reader.Declaration.class);
      if (readerDecl != null)
      {
        String readerName = cls.getName() + "$$Reader";
        readerClass = (Class<? extends ObjectReader>) Class.forName(readerName);
        if (readerClass == null)
        {
          throw new RuntimeException("Unable to find automatic reader " + readerName);
        }
      }
      else
      {
//        return GenericObjectReader.create(cls);
//       If this has a ReaderInfo, then it will point to the contents class
        ReaderInfo readerInfo = cls.getAnnotation(ReaderInfo.class);
        ReaderName readerName = cls.getAnnotation(ReaderName.class);
        if (readerInfo != null)
          readerClass = readerInfo.value();
        else if (readerName != null)
          readerClass = (Class<? extends ObjectReader>) Class.forName(readerName.value());
        else
        {
          // Lookup the class in the map
          readerClass = this.readerMap.get(cls.getName());
        }
      }

      if (readerClass != null)
      {
        // Lookup the construct to create a new reader
        Constructor<? extends ObjectReader> ctor = readerClass.getDeclaredConstructor(new Class[0]);
        ctor.setAccessible(true);
        return ctor.newInstance(new Object[0]);
      }

      // Use a reader factory
      for (ReaderFactory rf : readerFactories)
      {
        ObjectReader reader = rf.getReader(cls);
        if (reader != null)
        {
          return reader;
        }
      }
      
      if (cls.isEnum())
      {
        return new EnumReader(cls);
      }
      throw new ReaderException("No readers defined for " + cls.getCanonicalName());
    }
    catch (ClassNotFoundException | SecurityException | NoSuchMethodException | InstantiationException
            | IllegalAccessException | IllegalArgumentException ex)
    {
      throw new ReaderException("Unable to create  " + readerClass + " for " + cls.getName(), ex);
    }
    catch (InvocationTargetException ex)
    {
      if (ex.getCause() instanceof ReaderException)
        throw (ReaderException) ex.getCause();
      throw new ReaderException("Exception creating reader for " + cls.getName(), ex);
    }
  }

  @Override
  public EntityResolver getEntityResolver()
  {
    return this;
  }

  @Override
  public synchronized void registerReaderFactory(ReaderFactory factory)
  {
    readerFactories.add(factory);
  }

  @Override
  @SuppressWarnings("unchecked")
  public void registerReaderFor(Class cls, Class readerCls)
  {
    readerMap.put(cls.getName(), readerCls);
  }

  @Override
  public void registerWriterFactory(WriterFactory factory)
  {
    writerFactories.add(factory);
  }

  @Override
  public void registerWriterFor(Class cls, Class<? extends ObjectWriter> writerCls)
  {
    writerMap.put(cls.getName(), writerCls);
  }

//</editor-fold>
//<editor-fold desc="xsd-scanner">
  private Set<String> scanned_ = new TreeSet<String>();

  public void scanSchema(URL url)
  {
    // Avoid reading the schema multiple times, which can be costly
    // for large schema.
    synchronized (scanned_)
    {
      if (url==null)
        throw new NullPointerException("URL is null");

      if (scanned_.contains(url.toString()))
        return;
      scanned_.add(url.toString());
    }

    // Scan the schema for java annotations.
    try
    {
      URLConnection connection = url.openConnection();
      connection.setUseCaches(false);
      InputStream is = connection.getInputStream();

      SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
      SAXParser newSAXParser = saxParserFactory.newSAXParser();
      XMLReader reader = newSAXParser.getXMLReader();

      reader.setFeature("http://xml.org/sax/features/namespaces", true);
      reader.setFeature("http://xml.org/sax/features/namespace-prefixes", false); //true);
      reader.setContentHandler(new XsdHandler());

      InputSource inputSource = new InputSource(is);
      inputSource.setSystemId(url.toString());
      reader.parse(inputSource);
    }
    catch (UnknownHostException ex)
    {
      throw new RuntimeException("Unable to locate schema for " + url);
    }
    catch (IOException | SAXException ex)
    {
      throw new RuntimeException(ex);
    }
    catch (ParserConfigurationException ex)
    {
      Logger.getLogger(SchemaManagerImpl.class.getName()).log(Level.SEVERE, null, ex);
    }
  }

  /* Example that neeeds to be handled
      xsi:schemaLocation=
      "http://rtk.llnl.gov http://rtk.llnl.gov/mjx/schema/rtk-mjx.xsd
       http://rdak.llnl.gov http://rdak.llnl.gov/mjx/schema/rdak-mjx.xsd
       http://rnak.llnl.gov http://rnak.llnl.gov/mjx/schema/rnak-mjx.xsd"
       >
   */
  public void processSchemaLocation(String includedSchema)
  {
    if (includedSchema == null)
    {
      UtilityPackage.LOGGER.warning("Null Schema Location");
      return;
    }
    String[] locations = includedSchema.split("\\s+");
    // split by white space
    // handle every other argument
    SchemaManagerImpl schemaMgr = (SchemaManagerImpl) SchemaManager.getInstance();
    for (int i = 1; i < locations.length; i += 2)
    {
      try
      {
        URL url = mangleURI(new URI(locations[i]));
        if (url == null)
        {
          UtilityPackage.LOGGER.warning("Fail in url "+new URI(locations[i]));
          continue;
        }
        schemaMgr.scanSchema(url);
      }
      catch (URISyntaxException ex)
      {
        UtilityPackage.LOGGER.warning("Malformed URI " + locations[i]);
      }
    }
  }

  class XsdHandler extends DefaultHandler
  {
    String targetURI;
    int level = 0;
    String util = UtilityPackage.getInstance().getNamespaceURI();

    @Override
    public void startElement(String uri, String localName, String qualifiedName, Attributes attr)
            throws SAXException
    {
      level++;
      if (level == 1)
        targetURI = attr.getValue("targetNamespace");
      if (!XMLConstants.W3C_XML_SCHEMA_NS_URI.equals(uri))
        return;
      if (localName.equals("import"))
      {
        boolean validate = true;
        String propString = System.getProperty("gov.llnl.utility.xml.validation");
        if (propString != null)
          validate = !propString.toLowerCase().equals("false");

        // No validation means we scan in the schema ourselves bc it won't be done automatically.
        if (!validate)
        {
          try
          {
            SchemaManagerImpl schemaMgr = (SchemaManagerImpl) SchemaManager.getInstance();
            URL url = schemaMgr.mangleURI(new URI(attr.getValue("schemaLocation")));
            schemaMgr.scanSchema(url);
          }
          catch (URISyntaxException ex)
          {
            Logger.getLogger(SchemaManagerImpl.class.getName()).log(Level.SEVERE, null, ex);
          }
        }
      }
      if (!"element".equals(localName))
        return;
      String cls = attr.getValue(util, "class");

      if (cls == null)
        return;
      registerClass(targetURI, attr.getValue("name"), cls);
    }

    @Override
    public void endElement(String uri, String localName, String qualifiedName)
            throws SAXException
    {
      level--;
    }
  }

//</editor-fold>
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