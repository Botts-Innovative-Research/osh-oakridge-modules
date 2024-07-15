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
import gov.llnl.utility.internal.xml.bind.ImportsAny;
import gov.llnl.utility.internal.xml.bind.ReaderBuilderImpl;
import gov.llnl.utility.internal.xml.bind.ReaderDeclarationImpl;
import gov.llnl.utility.internal.xml.bind.SchemaBuilderUtilities;
import gov.llnl.utility.internal.xml.bind.SectionImpl;
import gov.llnl.utility.io.ReaderException;
import gov.llnl.utility.xml.DomBuilder;
import org.xml.sax.Attributes;

/**
 * Base class to use for all Reader classes that require element contents.
 * <p>
 * Example:
 * <pre>
 * {code
 * Reader.Declaration(pkg=MySchema.class, name="foo", order=Reader.Order.ALL)
 * public class FooReader extends ObjectReader&lt;Foo&gt;
 * {
 *
 *   public Object start(Attributes attributes) throws ReaderException
 *   {
 *     return new Foo();
 *   }
 *
 *   public ElementHandlerMap getHandlers() throws ReaderException
 *   {
 *     ReaderBuilder&lt;Foo&gt; builder=this.newBuilder();
 *     builder.element("bar").reader(new BarReader()).call("setBar");
 *     return builder.getHandlers();
 *   }
 *
 *   public Class&lt;Foo&gt; getObjectClass()
 *   {
 *     return Foo.class;
 *   }
 * }
 * }</pre>
 *
 * @param <Component> is the class for objects produced by this Reader.
 */
public abstract class ObjectReader<Component>
        implements Reader<Component>
{
  private ReaderContext context;

  /**
   * Create the object reader for this class.Searches in order: Primitives,
   * annotations, and contents map.
   *
   * @param <T>
   * @param cls
   * @return does not return null.
   * @throws ReaderException if the contents cannot be found.
   */
  public static <T> ObjectReader<T> create(Class<T> cls) throws ReaderException
  {
    return SchemaManager.getInstance().findObjectReader(cls);
  }

  /**
   * Event called at the start of an element. {@code start} will produce the
   * loaded object if it can be created. The created object will be used as the
   * parent for all handled methods. Elements that contain child elements should
   * always return an object on {@code start}.
   * <p>
   * Readers that use attributes should set the options {@code Options.ANY_ATTR}
   * or {@code Options.ATTR_DEF} in the constructor.
   *
   * @param attributes Holds the attributes to use in creating this object
   * @return the object produced by this element, or null if the object is to be
   * produced at a later stage
   * @throws ReaderException on any failure in the start method. This includes
   * incorrect or missing attributes.
   */
  @Override
  public Component start(Attributes attributes) throws ReaderException
  {
    return null;
  }

  /** Utility to help with unpacking attributes.
   *
   * FIXME this may appear elsewhere in the toolkit, but I could not find it.
   *
   * @param <T>
   * @param attr
   * @param name
   * @param cls
   * @param defaultValue
   * @return
   */
  @SuppressWarnings("unchecked")
  protected static <T> T getAttribute(Attributes attr, String name, Class<T> cls, T defaultValue)
  {
    String s = attr.getValue(name);
    if (s == null)
      return defaultValue;
    return (T) ClassUtilities.newValueOf(cls).valueOf(s);
  }
  
  @SuppressWarnings("unchecked")
  protected static <T> T getAttribute(Attributes attr, String name, Class<T> cls) throws ReaderException
  {
    String s = attr.getValue(name);
    if (s == null)
      throw new ReaderException("Required attribute " + name + " was not found.");
     return (T) ClassUtilities.newValueOf(cls).valueOf(s);
  }

  @Override
  public ElementHandlerMap getHandlers()
          throws ReaderException
  {
    return null;
  }

  /**
   * Event called at the end of the element to process the text contents.
   *
   * @param textContents is the text held by this element
   * @return the object to be produced for this element, will override object if
   * not null
   * @throws ReaderException
   */
  @Override
  public Component contents(String textContents) throws ReaderException
  {
    return null;
  }

  /**
   * Event called when the element end is encountered. For most elements, there
   * is no need to take action when the end is encountered. Deferred actions may
   * not have been complete until after {@code end}. References to this object
   * are set after {@code end}.
   *
   * @return the object for this element or null if the object is already
   * defined
   * @throws ReaderException on any failure while processing this element
   */
  @Override
  public Component end() throws ReaderException
  {
    return null;
  }

  @Override
  final public String getHandlerKey()
  {
    String name = getXmlName();
    if (name == null)
      return null;
    PackageResource resource = this.getPackage();
    if (resource != null)
      return name + "#" + resource.getNamespaceURI();
    return name + "#";
  }

  @Override
  public String getSchemaType()
  {
    // If the user specifies a typename we should use it.
    // This is in the case that the typename got moved or there is some conflict.
    Declaration decl = getDeclaration();
    if (!decl.typeName().equals(Declaration.NULL))
      return decl.typeName();

    // Otherwise use automatic naming
    String className = this.getClass().getName();
    String out = className.replaceAll("[A-z0-9]*\\.", "").replaceAll("[$]+", "-") + "-type";
    return out;
  }

  /**
   * Gets the current object being read. Valid in the getHandlers section.
   *
   * @return the current object or null if the object has not yet been
   * constructed.
   */
  @SuppressWarnings("unchecked")
  protected Component getObject()
  {
    return (Component) getContext().getCurrentHandlerContext().getTargetObject();
  }

  @Override
  public ReaderContext getContext()
  {
    return this.context;
  }

  /**
   * (Internal) Set the current context while loading a reader.
   *
   * This is called internally during the loading process. It should never be
   * called directly.
   *
   * @param context
   */
  @Override
  final public void setContext(ReaderContext context)
  {
    if (this.context != null && this.context != context && context != null)
      throw new RuntimeException("reentrant issue ");
    this.context = context;
  }

  /**
   * Creates a builder to make a ElementHandlerMap.
   *
   * @return a new builder for this object.
   */
  @SuppressWarnings("unchecked")
  final public ReaderBuilder<Component> newBuilder()
  {
    return new ReaderBuilderImpl(this);
  }

  @SuppressWarnings("unchecked")
  final public <T extends Component> ReaderBuilder<T> newBuilder(Class<T> aClass)
  {
    return new ReaderBuilderImpl(this);
  }
//<editor-fold desc="sections" defaultstate="collapsed">

  /**
   * Defines a section with element contents. Sections allow a portion of the
   * readers setOptions to be contained from others. Sections can never be
   * references and do not create a new object.
   *
   * <pre>
   * {@code
   * class FooReader extends ObjectReader<Foo>
   * {
   *    ...
   *    public ElementHandlerMap getHandlers() throws ReaderException
   *    {
   *      ReaderBuilder<Foo> builder = this.newBuilder();
   *      builder.section(new BarSection());
   *      return builder.create();
   *    }
   *
   *    class BarSection extends Section
   *    {
   *      ...
   *     }
   *   }
   * }</pre>
   *
   */
  public abstract class Section extends SectionImpl<Component>
  {
    final Order order;
    final String name;

    public Section(Order order, String name)
    {
      this.order = order;
      this.name = name;
    }

    @Override
    public Reader.Declaration getDeclaration()
    {
      return new ReaderDeclarationImpl()
      {
        @Override
        public Class<? extends PackageResource> pkg()
        {
          return ObjectReader.this.getDeclaration().pkg();
        }

        @Override
        public String name()
        {
          return Section.this.name;
        }

        @Override
        public Order order()
        {
          return Section.this.order;
        }

        @Override
        public String schema()
        {
          return Reader.Declaration.NULL;
        }
      };
    }

    /**
     * {@inheritDoc}
     * <p>
     * {@code start} is optional for {@code Section}s.
     */
    @Override
    public Component start(Attributes attributes) throws ReaderException
    {
      return null;
    }

    /**
     * {@inheritDoc}
     * <p>
     * {@code Section}s may not have text contents.
     */
    @Override
    final public Component contents(String textContents)
            throws ReaderException
    {
      return null;
    }

    /**
     * {@inheritDoc}
     * <p>
     * {@code end} is optional for Section.
     */
    @Override
    public Component end() throws ReaderException
    {
      return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    final public Component getObject()
    {
      return (Component) getContext().getCurrentHandlerContext().getTargetObject();
    }

    @Override
    final public Class getObjectClass()
    {
      return ObjectReader.this.getObjectClass();
    }

    @SuppressWarnings("unchecked")
    final public ReaderBuilder<Component> newBuilder()
    {
      return new ReaderBuilderImpl(this);
    }

    @SuppressWarnings("unchecked")
    final public <T extends Component> ReaderBuilder<T> newBuilder(Class<T> cls)
    {
      return new ReaderBuilderImpl(this);
    }

    @Override
    public void createSchemaType(SchemaBuilder builder) throws ReaderException
    {
      SchemaBuilderUtilities.createSchemaTypeDefault(this, builder);
    }

    @Override
    public DomBuilder createSchemaElement(SchemaBuilder builder, String name, DomBuilder type, boolean options)
            throws ReaderException
    {
      return SchemaBuilderUtilities.createSchemaElementDefault(this, builder, name, type, options); // | Options.Attr.NO_ID);
    }
  }

  /**
   * Defines a section with only text contents.
   *
   * @see ObjectReader.Section
   */
  public abstract class StringSection extends SectionImpl<Component>
  {
//    public StringSection(int options, String name)
//    {
//      super(Order.ALL, options | Options.TEXT, name, ObjectReader.this.getPackage());
//    }

    @Override
    @SuppressWarnings("unchecked")
    final public Component getObject()
    {
      return (Component) getContext().getCurrentHandlerContext().getTargetObject();
    }

    /**
     * {@inheritDoc}
     * <p>
     * start() is optional for classed derived from StringSection.
     * <p>
     */
    @Override
    public Component start(Attributes attributes) throws ReaderException
    {
      return null;
    }

    /**
     * {@inheritDoc}
     * <p>
     * All classes derived from StringSection must implement contents().
     */
    @Override
    public abstract Component contents(String textContents)
            throws ReaderException;

    /**
     * {@inheritDoc}
     * <p>
     * {@code end} is optional for StringSection.
     */
    @Override
    public Component end()
    {
      return null;
    }

    /**
     * {@inheritDoc}
     * <p>
     * StringSection is for objects that do not contain child elements. All
     * classes derived from StringSection may not implement getHandlers().
     */
    @Override
    final public ElementHandlerMap getHandlers()
    {
      return null;
    }

    @Override
    final public Class getObjectClass()
    {
      return ObjectReader.this.getObjectClass();
    }
  }
//</editor-fold>
//<editor-fold desc="imports">

  /**
   * Use this to import resources into the document so that they can be
   * referenced later. No schema is applied to the contents of this section.
   *
   * <pre>
   * {@code
   * public ElementHandlerMap getHandlers() throws ReaderException
   * {
   *   ReaderBuilder builder = this.newBuilder();
   *   builder.section(new Imports()).optional();
   *   ..
   * }
   * }
   * </pre>
   */
  public class Imports extends Section
  {
    public Imports()
    {
      super(Order.FREE, "imports");
    }

    @Override
    @SuppressWarnings("unchecked")
    public ElementHandlerMap getHandlers() throws ReaderException
    {
      ReaderBuilder builder = newBuilder();
      builder.anyElement(new ImportsAny()).nop().define();
      return builder.getHandlers();
    }

    @Override
    public void createSchemaType(SchemaBuilder builder) throws ReaderException
    {
      // Create a type definition
      DomBuilder type = builder.getRoot()
              .element("xs:complexType")
              .attr("name", this.getSchemaType());
      DomBuilder group = type.element("xs:choice")
              .attr("minOccurs", "0")
              .attr("maxOccurs", "unbounded");
      group.element("xs:any").attr("processContents", "skip");
    }

  }

  /**
   * Use this to define resources into the document so that they can be
   * referenced later.
   *
   * <pre>
   * {@code
   * public ElementHandlerMap getHandlers() throws ReaderException
   * {
   *   ReaderBuilder builder = this.newBuilder();
   *   builder.section(new Defines()).optional();
   *   ..
   * }
   * }
   * </pre>
   */
  public class Defines extends Section
  {
    public Defines()
    {
      super(Order.FREE, "defines");
    }

    @Override
    @SuppressWarnings("unchecked")
    public ElementHandlerMap getHandlers() throws ReaderException
    {
      ReaderBuilder builder = this.newBuilder();
      builder.anyElement(Object.class).nop().optional();
      return builder.getHandlers();
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