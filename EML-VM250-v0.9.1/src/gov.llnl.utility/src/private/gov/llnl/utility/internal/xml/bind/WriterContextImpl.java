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

import gov.llnl.utility.internal.xml.bind.marshallers.ContentMarshallers;
import gov.llnl.utility.PackageResource;
import gov.llnl.utility.annotation.Internal;
import gov.llnl.utility.io.WriterException;
import gov.llnl.utility.xml.DomBuilder;
import gov.llnl.utility.xml.bind.DocumentWriter;
import gov.llnl.utility.xml.bind.ObjectWriter;
import gov.llnl.utility.xml.bind.ObjectWriter.WriterAttributes;
import gov.llnl.utility.xml.bind.ObjectWriter.WriterAttributesOptions;
import gov.llnl.utility.xml.bind.WriterContext;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Pattern;
import org.w3c.dom.Document;

/**
 *
 * @author nelson85
 */
@Internal
@SuppressWarnings("unchecked")
public class WriterContextImpl implements WriterContext
{
//  Document document;
  DocumentWriter documentWriter;
  HashMap<Integer, ReferenceEntry> references = new HashMap<>();
  HashSet<PackageResource> schema = new HashSet<>();
  private ContextEntry currentContext = null;
  private ContextEntry lastContext;
  private final WriteAttributesImpl attr = new WriteAttributesImpl();
  private final Marshallers marshallers = new Marshallers();
  private final MarshallerOptionsImpl properties = new MarshallerOptionsImpl();

  public WriterContextImpl(DocumentWriter documentWriter, ObjectWriter objectWriter) throws WriterException
  {
    this.documentWriter = documentWriter;
    this.marshallers.addAll(ContentMarshallers.getDefault());
    this.schema.add(objectWriter.getPackage());
  }
  
  @Override
  public DomBuilder element()
  {
    return current().element;
  }

  public void setMarshaller(Marshaller marshall)
  {
    this.marshallers.add(marshall);
  }

  @Override
  public Marshaller getMarshaller(Class cls) throws WriterException
  {
    return marshallers.get(cls);
  }

  @Override
  public MarshallerOptions getMarshallerOptions()
  {
    return this.properties;
  }

//<editor-fold desc="properties">
  @Override
  public void setProperty(String key, Object value) throws UnsupportedOperationException
  {
    Pattern pattern = Pattern.compile("[A-z0-9._]+#.*");
    if (pattern.matcher(key).matches())
    {
      try
      {
        String[] parts = key.split("#", 2);
        Class cls = Class.forName(parts[0]);

        Marshaller marshaller = marshallers.get(cls);
        if (marshaller == null)
          throw new UnsupportedOperationException("No marshaller for " + key);
        if (!marshaller.hasProperty(key))
          throw new UnsupportedOperationException("Marshaller does not support " + key);
      }
      catch (ClassNotFoundException ex)
      {
//        throw new UnsupportedOperationException("Can't find class for property " + key);
      }
    }
    this.properties.put(key, value);
  }

  @Override
  public Object getProperty(String key)
  {
    return properties.get(key);
  }

  @Override
  public <T> T getProperty(String key, Class<T> cls, T defaultValue)
  {
    return properties.get(key, cls, defaultValue);
  }
//</editor-fold>

  @Override
  public <Type> void addContents(Type object) throws WriterException
  {
    if (object == null)
      return;
    if (object instanceof String)
      currentContext.element.text((String) object);
    else
    {
      Marshaller marshaller = marshallers.get(object.getClass());
      if (marshaller == null)
        throw new WriterException("Unable to find marshaller for " + object.getClass().getCanonicalName());
      currentContext.element.text(marshaller.marshall(object, properties));
    }
  }

  public String register(Object obj, String prefix, String key)
  {
    ReferenceEntry ref;

    // The object is already registered if we have the same hashcode
    int hash = obj.hashCode();

    // Compute an automatic reference id if not supplied.
    if (key == null)
      key = String.format("%s.%08x", prefix, hash);

    // and object types are the same
    references.put(hash, ref = new ReferenceEntry(key, obj));
    return ref.key;
  }

  public String get(Object obj)
  {
    int i = obj.hashCode();
    ReferenceEntry ref = references.get(i);
    if (ref == null || ref.object == null)
      return null;
    if (ref.object.getClass() == obj.getClass())
      return ref.key;
    return null;
  }

  @Override
  public <Type> DomBuilder write(
          Document document,
          ObjectWriter<Type> writer,
          String elementName,
          Type object,
          boolean objectRoot)
          throws WriterException
  {
    if (document != null)
    {
      currentContext = new ContextEntry(
              new DomBuilder(document.getDocumentElement()),
              writer, "##root", writer.getPackage());
    }
    else
    {
      if (currentContext == null)
        throw new RuntimeException("writer called on without a document");
    }

    // Hold the context
    writer.setContext(this);
    int options = writer.getOptions();

    // Document element is special
    DomBuilder element;

    PackageResource pkg = this.documentWriter.getObjectWriter().getPackage();

    // Create the element
    if (elementName == null)
    {
      elementName = writer.getElementName();
      pkg = writer.getPackage();
    }
    
    // Create a new element
    if (objectRoot || (options & ObjectWriter.Options.COMMENT) == ObjectWriter.Options.COMMENT)
    {
      element = currentContext.element;
    }
    else
    {
//      System.out.println(this.currentContext.pkg.getSchemaPrefix()+ " " + pkg.getSchemaPrefix() + " " + elementName);
      element = newElement(this.currentContext.pkg, elementName);
    }
    
    // Pack the opject
    ContextEntry entry = pushContext(element, object, writer.getElementName(), writer.getPackage());
    if ((options & ObjectWriter.Options.REFERENCEABLE) == ObjectWriter.Options.REFERENCEABLE)
    {
      String ref = this.get(object);
      if (ref != null)
      {
        // If the reference was already defined, we don't need to save
        // an identical object.  Instead, mark it as a reference to
        // a previously defined object.
        entry.referenceTo(ref);
        popContext();
        return element;
      }
    }
    writer.attributes(attr, object);
    attr.flush();
    writer.contents(object);
    if ((options & ObjectWriter.Options.REFERENCEABLE) == ObjectWriter.Options.REFERENCEABLE)
    {
      // If it is referenceable then it needs to be registered
      entry.setId(null);
    }
    popContext();
    return element;
  }

  <Type> void writeContent(String elementName, Type value) throws WriterException
  {
    PackageResource pkg = current().getPackage();
    DomBuilder element = newElement(pkg, elementName);
    pushContext(element, null, elementName, pkg);
    addContents(value);
    popContext();
  }

  @Override
  public WriterBuilderImpl newBuilder(ObjectWriter writer)
  {
    return new WriterBuilderImpl(writer);
  }

//<editor-fold desc="context stack">
  ContextEntry pushContext(DomBuilder element, Object object, String elementName, PackageResource pkg)
  {
    ContextEntry entry = new ContextEntry(element, object, elementName, pkg);
    entry.previous = currentContext;
    currentContext = entry;
    return entry;
  }

  void popContext()
  {
    this.lastContext = currentContext;
    this.currentContext = currentContext.previous;
  }

  ContextEntry current()
  {
    return currentContext;
  }

  ContextEntry last()
  {
    return this.lastContext;
  }

//</editor-fold>
//<editor-fold desc="internal">
  DomBuilder newElement(PackageResource pkg, String elementName) throws WriterException
  {
    if (elementName == null)
      throw new WriterException("null element name");

    DomBuilder parent = currentContext.element;

    PackageResource rootPackage = this.documentWriter.getObjectWriter().getPackage();
    if (pkg == rootPackage || pkg == null)
      return parent.element(elementName);
    else
    {
      this.schema.add(pkg);
      return parent.elementNS(pkg, elementName);
    }
  }

//</editor-fold>
//<editor-fold desc="inner classes">
  private class WriteAttributesImpl implements WriterAttributes, WriterAttributesOptions
  {
    String key = null;
    Object content = null;
    Marshaller marshaller = null;
    MarshallerOptions options = properties;

    @Override
    public void add(String key, String value)
    {
      currentContext.element.attr(key, value);
    }

    @Override
    public WriterAttributesOptions add(String key, Object content) throws WriterException
    {
      flush();
      if (content == null)
      {
        currentContext.element.attr(key, "");
        return this;
      }
      marshaller = marshallers.get(content.getClass());
      if (marshaller == null)
        throw new WriterException("Cannot find marshaller for " + content.getClass());
      this.key = key;
      this.content = content;
      return this;
    }

    @Override
    public ObjectWriter.WriterAttributesOptions set(final String key, final Object obj)
    {
      if (!marshaller.hasProperty(key))
        throw new UnsupportedOperationException("Marshaller does not support " + key);
      this.options = new MarshallerOptionsProxy(this.options, key, obj);
      return this;
    }

    public void flush()
    {
      if (key == null)
        return;
      if (marshaller == null)
      {
        throw new NullPointerException("null marshaller");
      }
      currentContext.element.attr(key, marshaller.marshall(content, options));
      key = null;
      content = null;
      options = properties;
    }
  }

  public class MarshallerOptionsImpl extends TreeMap<String, Object> implements MarshallerOptions
  {
    private String cacheKey;
    private Object cacheValue;

    @Override
    public Object put(String value, Object object)
    {
      clearCache();
      return super.put(value, object);
    }

    @Override
    public <Type> Type get(String key, Class<Type> cls, Type defaultValue)
    {
      if (key.equals(cacheKey))
        return cls.cast(cacheValue);
      Object value = get(key);
      if (value == null)
      {
        if (containsKey(key) == false)
          return defaultValue;
        return null;
      }
      if (cls.isInstance(value))
        return cache(key, cls.cast(value));
      throw new ClassCastException("Property is not correct type " + key + " " + cls);
    }

    private <Type> Type cache(String key, Type value)
    {
      this.cacheKey = key;
      this.cacheValue = value;
      return value;
    }

    private void clearCache()
    {
      this.cacheKey = null;
    }
  }

  public class Marshallers extends TreeMap<Integer, Marshaller>
  {
    public void addAll(List<Marshaller> marshall)
    {
      for (Marshaller m : marshall)
      {
        setMarshaller(m);
      }
    }

    public void add(Marshaller marshall)
    {
      int hash = System.identityHashCode(marshall.getObjectClass());
      this.put(hash, marshall);
    }

    public Marshaller get(Class cls)
    {
      int hash = System.identityHashCode(cls);
      return this.get(hash);
    }
  }

  @Internal
  static class ReferenceEntry
  {
    Object object;
    String key;

    ReferenceEntry(String key, Object obj)
    {
      this.key = key;
      this.object = obj;
    }
  }

  @Internal
  class ContextEntry
  {
    private DomBuilder element;
    private final Object object;
    private final String elementName;
    private final PackageResource pkg;
    boolean defined = true;
    ContextEntry previous = null;

    ContextEntry(DomBuilder element, Object object, String elementName, PackageResource pkg)
    {
      this.element = element;
      this.object = object;
      this.elementName = elementName;
      this.pkg = pkg;
    }

    private void referenceTo(String ref)
    {
      defined = false;
      element.attr("ref_id", ref);
    }

    void setId(String id)
    {
      if (!defined)
        return;
      String ref = register(object, elementName, id);
      element.attr("id", ref);
    }

    public Object getObject()
    {
      return object;
    }

    public PackageResource getPackage()
    {
      return pkg;
    }

    public DomBuilder getElement()
    {
      return this.element;
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