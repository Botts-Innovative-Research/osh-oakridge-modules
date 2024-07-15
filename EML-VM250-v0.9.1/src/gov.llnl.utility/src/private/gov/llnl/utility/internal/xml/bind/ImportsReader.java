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
import gov.llnl.utility.annotation.Internal;
import gov.llnl.utility.io.ReaderException;
import gov.llnl.utility.xml.DomBuilder;
import gov.llnl.utility.xml.bind.ObjectReader;
import gov.llnl.utility.xml.bind.Reader;
import gov.llnl.utility.xml.bind.ReaderContext;
import gov.llnl.utility.xml.bind.SchemaBuilder;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.regex.Pattern;
import org.xml.sax.Attributes;

/**
 * Reads an object from an external source.
 *
 * @author nelson85
 */
@Internal
final public class ImportsReader extends ObjectReader<Object>
{
  final ObjectReader objectReader;
  final DocumentReaderImpl documentReader;
  ReaderContext localContext;

  public ImportsReader(ObjectReader reader)
  {
    this.objectReader = reader;
    //  super(Order.FREE, documentReader.getXmlName(), documentReader.getPackage());
    this.documentReader = new DocumentReaderImpl(reader);
  }

  /**
   * This is a dynamically created reader and thus we create the annotation on
   * the fly.
   *
   * @return
   */
  public Reader.Declaration getDeclaration()
  {
    return new ReaderDeclarationImpl()
    {
      @Override
      public Class<? extends PackageResource> pkg()
      {
        return objectReader.getDeclaration().pkg();
      }

      @Override
      public String name()
      {
        return objectReader.getDeclaration().name();
      }

      @Override
      public Order order()
      {
        return Order.FREE;
      }

      @Override
      public String schema()
      {
        return Reader.Declaration.NULL;
      }
    };
  }

  @Override
  @SuppressWarnings("unchecked")
  public Object start(Attributes attributes) throws ReaderException
  {
    URL url;
    String uri = attributes.getValue("extern");
    if (uri == null)
      throw new ReaderException("extern is required for import on " + this.getXmlName());
    url = getContext().getExternal(uri);
    try
    {
      synchronized (documentReader)
      {
        // Copy any properties from the parent documentReader
        Map<String, Object> props = getContext().getDocumentReader().getProperties();
        // Load the file
        for (Map.Entry<String, Object> p : props.entrySet())
        {
          documentReader.setProperty(p.getKey(), p.getValue());
        }

        Object out = documentReader.loadURL(url);

        // Grab the context to extract references from
        this.localContext = documentReader.getContext();

        this.documentReader.clearContext();
        return out;
      }
    }
    catch (IOException ex)
    {
      throw new ReaderException("Error loading resource " + url.toString(), ex);
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public Class<Object> getObjectClass()
  {
    return documentReader.getObjectReader().getObjectClass();
  }

  @Override
  public ElementHandlerMap getHandlers() throws ReaderException
  {
    ReaderBuilder<Object> builder = newBuilder();
    builder.reader(new ReferenceReader()).nop();
    return builder.getHandlers();
  }

  @Override
  public void createSchemaType(SchemaBuilder builder) throws ReaderException
  {
  }

  @Override
  public DomBuilder createSchemaElement(SchemaBuilder builder, String name, DomBuilder type, boolean options) throws ReaderException
  {
    return type;
  }

  /**
   * This element can appear inside of a extern to bring references into scope.
   */
  final class ReferenceReader extends ObjectReader<Object>
  {
//    public ReferenceReader()
//    {
//      super(Order.ALL, Options.NONE, "reference", ImportsReader.this.getPackage());
//    }

    /**
     * This is a dynamically created reader and thus we create the annotation on
     * the fly.
     *
     * @return
     */
    public Reader.Declaration getDeclaration()
    {
      return new ReaderDeclarationImpl()
      {
        @Override
        public Class<? extends PackageResource> pkg()
        {
          return objectReader.getDeclaration().pkg();
        }

        @Override
        public String name()
        {
          return "reference";
        }

        @Override
        public Order order()
        {
          return Order.ALL;
        }
      };
    }

    @Override
    public Object start(Attributes attributes) throws ReaderException
    {
      String prefix = attributes.getValue("prefix");
      if (prefix == null)
        prefix = "";
      else
        prefix += ".";

      // import a specific reference, may be renamed
      String from = attributes.getValue("import");
      if (from != null)
      {
        Object object = localContext.get(from, Object.class);
        if (object == null)
          throw new ReaderException("Unable to find imported reference " + from);
        String to = attributes.getValue("id");
        if (to == null)
          getContext().put(prefix + from, object);
        return object;
      }

      // Copy over all references that fit a particular pattern
      String filter = attributes.getValue("filter");
      if (filter != null)
      {
        if (attributes.getValue("id") != null)
          throw new ReaderException("id is not supported on filter imports");
        Pattern pattern = Pattern.compile(filter);
        for (Map.Entry<String, Object> entry : localContext.getReferences())
        {
          if (pattern.matcher(entry.getKey()).matches())
          {
            getContext().put(prefix + entry.getKey(), entry.getValue());
          }
        }
        return null;
      }
      throw new ReaderException("Incorrect reference");
    }

    @Override
    public Class<Object> getObjectClass()
    {
      return null;
    }

    @Override
    public ElementHandlerMap getHandlers() throws ReaderException
    {
      return null;
    }

    @Override
    public void createSchemaType(SchemaBuilder builder) throws ReaderException
    {
    }

    @Override
    public DomBuilder createSchemaElement(SchemaBuilder builder, String name, DomBuilder type, boolean options)
            throws ReaderException
    {
      return type;
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