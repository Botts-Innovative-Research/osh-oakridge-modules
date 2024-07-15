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

import gov.llnl.utility.ClassUtilities;
import gov.llnl.utility.PackageResource;
import gov.llnl.utility.Singletons;
import gov.llnl.utility.UtilityPackage;
import gov.llnl.utility.io.PathLocation;
import gov.llnl.utility.io.ReaderException;
import gov.llnl.utility.xml.DomBuilder;
import gov.llnl.utility.xml.DomUtilities;
import gov.llnl.utility.xml.bind.Reader.ElementHandlerMap;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.regex.Pattern;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * SchemaBuilder write a schema for ObjectReader. Multiple elements can be
 * created in the same schema. SchemaBuilder automatically tracks what items are
 * already definedType, so it is not an issue if a Reader is added more than
 * once.
 */
public class SchemaBuilder
{
  public static String getXmlPrefix(Reader reader)
  {
    PackageResource resource = reader.getPackage();
    if (resource == null)
      return "";
    return resource.getSchemaPrefix() + ":";
  }
  private Document document;
  private final TreeMap<String, String> namespaces = new TreeMap<>();
  // static final String XSD_URI = XMLConstants.W3C_XML_SCHEMA_NS_URI;
  private PackageResource defaultSchema;
  private final TreeSet<String> definedType = new TreeSet<>();
  private final TreeMap<String, Reader> definedElementKey = new TreeMap<>();
  private String prefix;
  int error = 0;

  @SuppressWarnings("unchecked")
  static public void main(String[] args) throws IOException, ClassNotFoundException
  {
    Class<PackageResource> schema = (Class<PackageResource>) Class.forName(args[0]);
    PackageResource instance = Singletons.getSingleton(schema);

    SchemaBuilder sb = new SchemaBuilder();
    sb.setTargetNamespace(instance);
    sb.scanForReaders(Paths.get(args[1]), ".class");
    Path file = Paths.get(args[2], args[3]);
    Files.createDirectories(file.getParent());
    try (OutputStream os = new BufferedOutputStream(Files.newOutputStream(file));
            ByteArrayOutputStream baos = new ByteArrayOutputStream())
    {
      DomUtilities.printXml(baos, sb.getDocument());
      String str = baos.toString("UTF-8").replaceAll("\r\n", "\n");
      os.write(str.getBytes("UTF-8"));
    }
    System.out.println("Wrote schema for " + args[0]);
    System.exit(sb.getError());
  }

  /**
   * Create a new SchemaBuilder.
   */
  public SchemaBuilder()
  {
    createDocument();
  }

  /**
   * (Internal) Creates a document.
   */
  private void createDocument()
  {
    try
    {
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      DocumentBuilder db = dbf.newDocumentBuilder();
      DOMImplementation dom = db.getDOMImplementation();
      document = dom.createDocument(XMLConstants.W3C_XML_SCHEMA_NS_URI, "xs:schema", null);
      Element element = document.getDocumentElement();
      element.setAttribute("version", "1.0");
      element.setAttribute("elementFormDefault", "qualified");
    }
    catch (ParserConfigurationException ex)
    {
      throw new RuntimeException(ex);
    }
  }

  /**
   * Get a DomBuilder for the root element.
   *
   * @return a dom builder for the root element of the document.
   */
  public DomBuilder getRoot()
  {
    return new DomBuilder(document.getDocumentElement());
  }

  /**
   * Define a schema for an object reader.
   *
   * @param reader is the reader to add a schema for.
   * @throws ReaderException
   */
  public void addObjectReader(Reader reader)
          throws ReaderException
  {
    // Skip readers marked for no schema.
    if (reader.getClass().getAnnotation(Reader.NoSchema.class) != null)
      return;

    Class objectClass = reader.getObjectClass();
    if (objectClass != null)
    {
      UtilityPackage.LOGGER.log(Level.FINE, "Creating reader schema for {0}", objectClass);
      if (ClassUtilities.isInnerClass(objectClass))
        UtilityPackage.LOGGER.log(Level.SEVERE, "Inner classes may not be type base {0}", objectClass);
    }
    createReaderSchemaType(reader, true);

    Reader.Declaration decl = reader.getDeclaration();
    if (decl.document() || decl.referenceable())
      createReaderSchemaElement(reader, reader.getXmlName());
  }

  @SuppressWarnings("unchecked")
  public void addObjectReaderForClass(Class cls)
          throws ReaderException
  {
    addObjectReader(ObjectReader.create(cls));
  }

  public <T> void alias(Class<T> cls, String name) throws ReaderException
  {
    UtilityPackage.LOGGER.fine("Alias " + name);
    ObjectReader<T> reader = ObjectReader.create(cls);
    createReaderSchemaElement(reader, name);
  }

  /**
   * (internal)
   *
   * @param reader
   * @param name
   * @throws ReaderException
   */
  public void createReaderSchemaElement(Reader reader, String name)
          throws ReaderException
  {

    // Check for conflicts between names
    if (definedElementKey.containsKey(name))
    {
      if (definedElementKey.get(name).getClass() != reader.getClass())
      {
        throw new ReaderException("Schema conflict on element " + name
                + " between " + reader.getClass() + " and "
                + definedElementKey.get(name).getClass());
      }
      return;
    }
    definedElementKey.put(name, reader);

    // Watch for unnamed elements
    if (name.isEmpty())
      throw new ReaderException("Unnamed element " + reader.getClass().getName());

    // Create the schema element so that it can be used for definitions and documents.
    UtilityPackage.LOGGER.fine("Create schema element " + name);
    reader.createSchemaElement(this, name, this.getRoot(), true);
  }

  /**
   * (internal)
   *
   * @param reader
   * @param recursive
   * @throws ReaderException
   */
  public void createReaderSchemaType(Reader reader, boolean recursive)
          throws ReaderException
  {
    // Prevent the object reader form being definedType twice
    if (definedType.contains(getReaderKey(reader)))
      return;
    definedType.add(getReaderKey(reader));
    if (defaultSchema != reader.getPackage())
    {
      UtilityPackage.LOGGER.log(Level.FINER, "Skip {0}", reader.getClass().getName());
      return;
    }

    UtilityPackage.LOGGER.fine("Create schema type " + reader.getXmlName());
    reader.createSchemaType(this);

    if (recursive)
    {
      ElementHandlerMap handlers = reader.getHandlers();
      if (handlers == null)
        return;
      for (Reader.ElementHandler handler : handlers.toList())
      {
        Reader childReader = handler.getReader();
        if (childReader == null)
          continue;

        UtilityPackage.LOGGER.fine("Create schema type " + childReader.getXmlName());
        createReaderSchemaType(childReader, recursive);
      }
    }
  }

  /**
   * Get the DOM hold the schema for all readers stored.
   *
   * @return the document.
   */
  public Document getDocument()
  {
    // Horrible hack to sort the nodes after we have completed building the dom
    Element element = document.getDocumentElement();

    // Remove all the existing elements
    List<Node> contents = new LinkedList<>();
    while (element.getFirstChild() != null)
    {
      contents.add(element.removeChild(element.getFirstChild()));
    }

    // Sort out those elements have a "name"
    Iterator<Node> iter;
    for (iter = contents.iterator(); iter.hasNext();)
    {
      Node next = iter.next();
      if (next.getNodeType() == Element.ELEMENT_NODE && ((Element) next).hasAttribute("name"))
        continue;
      element.appendChild(next);
      iter.remove();
    }

    // Split between element declarations and everything else
    List<Node> types = new LinkedList<>();
    for (iter = contents.iterator(); iter.hasNext();)
    {
      Node next = iter.next();
      if (next.getNodeType() == Element.ELEMENT_NODE && ((Element) next).getTagName().equals("xs:element"))
        continue;
      types.add(next);
      iter.remove();
    }

    // Sort all of them by name
    Collections.sort(types, (Node x, Node y)
            -> ((Element) x).getAttribute("name").compareTo(((Element) y).getAttribute("name")));
    Collections.sort(contents, (Node x, Node y)
            -> ((Element) x).getAttribute("name").compareTo(((Element) y).getAttribute("name")));

    // Insert back in the document
    types.stream().forEach((x) -> element.appendChild(x));
    contents.stream().forEach((x) -> element.appendChild(x));

    return document;
  }

  /**
   * Add an include statement to the schema.
   *
   * @param file is the name of the file to include in the schema.
   */
  public void include(String file)
  {
    getRoot().element("xs:include", true)
            .attr("schemaLocation", file);
  }

  /**
   * Add an import statement to the schema.
   *
   * @param resource defines the file and the prefix to use.
   */
  public void imports(PackageResource resource)
  {
    if (this.namespaces.containsKey(resource.getNamespaceURI()))
      return;
    addNamespace(resource);
    getRoot().element("xs:import", true)
            .attr("namespace", resource.getNamespaceURI())
            .attr("schemaLocation", resource.getSchemaURI());
  }

  /**
   * Adds a namespace to be used for this schema. This is called automatically
   * by {@link #imports} and {@link #setTargetNamespace setTargetNamespace}.
   *
   * @param resource
   */
  public void addNamespace(PackageResource resource)
  {
    if (this.namespaces.containsKey(resource.getNamespaceURI()))
      return;
    namespaces.put(resource.getNamespaceURI(), resource.getSchemaPrefix());
    Element element = document.getDocumentElement();
    element.setAttributeNS(resource.getNamespaceURI(), resource.getSchemaPrefix() + ":version", "1.0");
    for (PackageResource dep : resource.getDependencies())
    {
      imports(dep);
    }

    for (PackageResource dep : resource.getIncludes())
    {
      include(dep.getSchemaURI());
    }
  }

  /**
   * Define the namespace for the resulting schema.
   *
   * @param resource
   */
  public void setTargetNamespace(PackageResource resource)
  {
    this.defaultSchema = resource;
    Element element = document.getDocumentElement();
    this.prefix = "";
    if (resource != null)
    {
      this.prefix = resource.getSchemaPrefix() + ":";
      element.setAttribute("targetNamespace", resource.getNamespaceURI());
    }
    addNamespace(resource);
  }

  private String getReaderKey(Reader reader)
  {
    String key = reader.getSchemaType();
    if (key == null)
      return reader.getClass().getName();
    return key;
  }

  public void scanForReaders(Path dir) throws IOException
  {
    scanForReaders(dir, ".java");
  }

  @SuppressWarnings("unchecked")
  public void scanForReaders(Path dir, String extension) throws IOException
  {

    final PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:**/*" + extension);
    final List<Path> files = new LinkedList<>();
    Files.walkFileTree(dir, new FileVisitor<Path>()
    {
      @Override
      public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException
      {
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
      {
        if (matcher.matches(file))
          files.add(file);
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException
      {
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException
      {
        return FileVisitResult.CONTINUE;
      }
    });
    Collections.sort(files, (Path p1, Path p2) ->
    {
      String s1 = p1.toFile().getAbsolutePath();
      String s2 = p2.toFile().getAbsolutePath();
      return s1.compareTo(s2);
    });
    for (Path file : files)
    {
      String name = "";
      try
      {
        name = dir.relativize(file).toString();
        name = name.substring(0, name.length() - extension.length());
        name = name.replaceAll(Pattern.quote(File.separator), ".");
        UtilityPackage.LOGGER.finest("Looking for class " + name);
        Class<?> cls = Class.forName(name);

        for (Field field : cls.getDeclaredFields())
        {
          Reader.ElementDeclaration annotation = field.getAnnotation(Reader.ElementDeclaration.class);
          if (annotation == null)
            continue;

          try
          {
            createReaderSchemaElement(ObjectReader.create(annotation.type()), annotation.name());
          }
          catch (ReaderException ex)
          {
            throw new RuntimeException(ex);
          }

        }

        Reader.Declaration rd = cls.getAnnotation(Reader.Declaration.class);
        if (rd == null)
          continue;

        try
        {
          UtilityPackage.LOGGER.fine("process " + cls + " " + ObjectReader.class.isAssignableFrom(cls));

          // Skip unnamed readers as they do not produce a schema.
          if (rd.name().equals(Reader.Declaration.NULL))
          {
            continue;
          }

          // Skip inner classes for now
          if (ClassUtilities.isInnerClass(cls))
          {
            continue;
          }

          if (ObjectReader.class.isAssignableFrom(cls))
          {
            ObjectReader reader = (ObjectReader) cls.getDeclaredConstructor().newInstance();
            this.addObjectReader(reader);
          }
          else
          {
            this.addObjectReaderForClass(cls);
          }

        }
        catch (NoSuchMethodException | SecurityException | IllegalArgumentException | InvocationTargetException | InstantiationException | IllegalAccessException ex)
        {
          UtilityPackage.LOGGER.severe("Unable to create instance for document reader " + cls);
          UtilityPackage.LOGGER.log(Level.SEVERE, "Exeception", ex);
          error = -1;
        }
        catch (ReaderException ex)
        {
          ByteArrayOutputStream os = new ByteArrayOutputStream();
          ex.printStackTrace(new PrintStream(os));
          UtilityPackage.LOGGER.warning("Failure in object reader " + cls + "\n"
                  + new String(os.toByteArray()));
          error = -1;
        }
      }
      catch (ClassNotFoundException ex)
      {
        UtilityPackage.LOGGER.warning("Class not found while getting " + name);
      }
    }
  }

  public ReaderContext getReaderContext(Class cls)
  {
    return new SchemaReaderContext(cls);
  }

  public int getError()
  {
    return error;
  }

  static Object notUsed()
  {
    throw new UnsupportedOperationException("Not used.");
  }

//<editor-fold desc="context" defaultstate="collapsed">
  private class SchemaReaderContext implements ReaderContext
  {
    Class cls;
    Object object;

    private SchemaReaderContext(Class cls)
    {
      this.cls = cls;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String name, Class<T> cls) throws ReaderException
    {
      return (T) notUsed();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <Obj> Obj put(String name, Obj object) throws ReaderException
    {
      return (Obj) notUsed();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <Obj> Obj putScoped(String name, Obj object) throws ReaderException
    {
      return (Obj) notUsed();
    }

    @Override
    public String getElementPath()
    {
      return (String) notUsed();
    }

    @Override
    public URL getExternal(String extern) throws ReaderException
    {
      return (URL) notUsed();
    }

    @Override
    public URI getFile()
    {
      return (URI) notUsed();
    }

    @Override
    public PathLocation getLocation()
    {
      return (PathLocation) notUsed();
    }

    @Override
    public HandlerContext getCurrentHandlerContext()
    {
      return new SchemaHandlerContext();
    }

    @Override
    public HandlerContext getLastHandlerContext()
    {
      return (HandlerContext) notUsed();
    }

    @Override
    public void setErrorHandler(ReaderContext.ExceptionHandler handler)
    {
      notUsed();
    }

    @Override
    public void handleException(Throwable ex) throws ReaderException
    {
      notUsed();
    }

    @Override
    public void setPropertyHandler(PropertyMap handler)
    {
      notUsed();
    }

    @Override
    public void addDeferred(Object target, BiConsumer methodName, String refId, Class cls) throws ReaderException
    {
      notUsed();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Iterable<Map.Entry<String, Object>> getReferences()
    {
      return (Iterable<Map.Entry<String, Object>>) notUsed();
    }

    @Override
    public DocumentReader getDocumentReader()
    {
      notUsed();
      return null;
    }

    @Override
    public Object getLastObject()
    {
      return null;
    }

    private class SchemaHandlerContext implements ReaderContext.HandlerContext
    {
      private SchemaHandlerContext()
      {
      }

      @Override
      public void characters(char[] chars, int start, int length)
      {
        notUsed();
      }

      @Override
      public Object getReference(String name) throws ReaderException
      {
        return notUsed();
      }

      @Override
      @SuppressWarnings("unchecked")
      public <T> T putReference(String name, T obj)
      {
        return (T) notUsed();
      }

      @Override
      @SuppressWarnings("unchecked")
      public Object getTargetObject()
      {
        try
        {
          if (object == null)
          {
            Constructor ctor = cls.getDeclaredConstructor((Class[]) null);
            ctor.setAccessible(true);
            object = ctor.newInstance((Object[]) null);
          }
          return object;
        }
        catch (IllegalAccessException | InstantiationException | NoSuchMethodException ex)
        {
          throw new RuntimeException("Unable to create object for schema with type " + cls, ex);
        }
        catch (SecurityException | IllegalArgumentException | InvocationTargetException ex)
        {
          throw new RuntimeException(ex);
        }
      }

      @Override
      public Object getParentObject()
      {
        return notUsed();
      }

      @Override
      public void dumpTrace(PrintStream out)
      {
        notUsed();
      }

      @Override
      public String getNamespaceURI()
      {
        return (String) notUsed();
      }

      @Override
      public String getLocalName()
      {
        return (String) notUsed();
      }
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