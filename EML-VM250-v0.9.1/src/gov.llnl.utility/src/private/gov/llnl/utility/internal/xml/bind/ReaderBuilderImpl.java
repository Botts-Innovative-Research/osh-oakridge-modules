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
import gov.llnl.utility.UtilityPackage;
import gov.llnl.utility.annotation.Internal;
import gov.llnl.utility.internal.xml.bind.readers.BooleanContents;
import gov.llnl.utility.internal.xml.bind.readers.DoubleContents;
import gov.llnl.utility.internal.xml.bind.readers.FlagReader;
import gov.llnl.utility.internal.xml.bind.readers.IntegerContents;
import gov.llnl.utility.internal.xml.bind.readers.LongContents;
import gov.llnl.utility.internal.xml.bind.readers.StringContents;
import gov.llnl.utility.io.ReaderException;
import gov.llnl.utility.xml.bind.AnyReader;
import gov.llnl.utility.xml.bind.ObjectReader;
import gov.llnl.utility.xml.bind.Reader;
import gov.llnl.utility.xml.bind.Reader.ElementHandler;
import gov.llnl.utility.xml.bind.Reader.ElementHandlerMap;
import gov.llnl.utility.xml.bind.Reader.Options;
import gov.llnl.utility.xml.bind.Reader.Order;
import gov.llnl.utility.xml.bind.Reader.ReaderBuilder;
import gov.llnl.utility.xml.bind.Reader.ReaderBuilderCall;
import gov.llnl.utility.xml.bind.Reader.ReaderBuilderContents;
import gov.llnl.utility.xml.bind.Reader.ReaderBuilderOptions;
import gov.llnl.utility.xml.bind.Reader.SectionInterface;
import java.util.EnumSet;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.xml.sax.Attributes;
import gov.llnl.utility.xml.bind.Reader.AnyFactory;

/**
 *
 * @author nelson85
 */
@Internal
@SuppressWarnings("unchecked")
public class ReaderBuilderImpl<T, T2> implements ReaderBuilder<T>,
        ReaderBuilderContents<T>, ReaderBuilderCall<T, T2>, ReaderBuilderOptions
{
  protected final String uriName;
  protected final Reader parentReader;

  // Auto-chain
  protected ElementGroupImpl parentGroup;
  final HandlerList handlerList;

  // Global Settings
  protected Object target;
  protected Class baseClass;

  // Transient settings
  protected ElementHandlerImpl lastHandler = null;
  protected Class resultClass = null;
  protected String elementName = null;
  protected Producer producer;

  public ReaderBuilderImpl(Reader reader)
  {
    this.parentReader = reader;
    PackageResource schema = reader.getPackage();
    String namespaceURI = "";
    if (schema != null)
      namespaceURI = schema.getNamespaceURI();
    this.uriName = "#" + namespaceURI;
    Reader.Declaration decl = reader.getDeclaration();

    EnumSet<Options> options = null;
    if (decl.referenceable())
      options = EnumSet.of(Options.OPTIONAL);
    else if (decl.contentRequired())
      options = EnumSet.of(Options.REQUIRED);

    this.parentGroup = ElementGroupImpl.newInstance(null, decl.order(), options); //reader.getOrder(), reader.getOptions());
    this.baseClass = reader.getObjectClass();
    this.handlerList = new HandlerList();

    // Auditing
    // FIXME this call is proving too slow for this routine.
    // Thus we should need to rework this section.
//    this.handlerList.trace = (new Throwable()).getStackTrace()[2];
    this.handlerList.trace = null;
  }

  protected ReaderBuilderImpl(ReaderBuilderImpl rb)
  {
    this.uriName = rb.uriName;
    this.parentReader = rb.parentReader;

    // Global Settings
    this.target = rb.target;
    this.baseClass = rb.baseClass;

    // Transient settings
    this.resultClass = rb.resultClass;
    this.elementName = rb.elementName;
    this.handlerList = rb.handlerList;
    this.parentGroup = rb.parentGroup;
  }

  @Override
  public ElementHandlerMap getHandlers() throws ReaderException
  {
    if (this.producer != null)
      throw new ReaderException("Incomplete reader on getHandlers " + producer);
    return ElementHandlerMapImpl.newInstance(uriName, handlerList);
  }

  @Override
  public ReaderBuilderImpl element(String name)
  {
    this.lastHandler = null;
    this.elementName = name + this.uriName;
    return this;
  }

  @Override
  public ReaderBuilderImpl contents(Class readerClass) throws ReaderException
  {
    return reader(ObjectReader.create(readerClass));
  }

  @Override
  public ReaderBuilderImpl any(Class readerClass) throws ReaderException
  {
    return reader(AnyReader.of(readerClass));
  }

  @Override
  public <Obj> ReaderBuilderImpl reader(Reader<Obj> reader)
          throws ReaderException
  {
    this.lastHandler = null;
    if (this.producer != null)
      throw new ReaderException("element contents redefined,\n    previous="
              + this.producer.toString() + "\n    new=" + reader.toString());
    if (reader == null)
      throw new ReaderException("reader is null");
    if (this.producer != null)
      // FIXME Unreachable and repeated producer null check. Need to select one and remove the other one.
      throw new ReaderException("element contents redefined " + reader.getXmlName());
    this.resultClass = reader.getObjectClass();
    this.producer = new ReaderHandlerProducer(reader);
    return this;
  }

  @Override
  public <Obj> ReaderBuilderCall readers(Class<Obj> cls, ObjectReader<? extends Obj>... readers) throws ReaderException
  {
    this.lastHandler = null;
    if (this.producer != null)
      throw new ReaderException("element contents redefined");
    if (readers == null)
      throw new ReaderException("reader is null");

    this.resultClass = cls;
    this.producer = new ReadersProducer(readers);
    return this;
  }

//<editor-fold desc="subs">
  @Override
  public <T2> ReaderBuilder<T2> using(T2 o)
  {
    ReaderBuilderImpl out = new ReaderBuilderImpl(this);
    out.target = o;
    out.baseClass = o.getClass();
    return out;
  }

  @Override
  public ReaderBuilderImpl choice(Options... options)
  {
    EnumSet<Options> optionSet = null;
    if (options != null && options.length > 0)
      optionSet = EnumSet.of(options[0], options);
    ReaderBuilderImpl out = new ReaderBuilderImpl(this);
    out.parentGroup = new ElementGroupImpl.ChoiceGroup(parentGroup, optionSet);
    return out;
  }

  @Override
  public ReaderBuilderImpl choice()
  {
    ReaderBuilderImpl out = new ReaderBuilderImpl(this);
    out.parentGroup = new ElementGroupImpl.ChoiceGroup(parentGroup, null);
    return out;
  }

//</editor-fold>
//<editor-fold desc="contents">
  @Override
  public ReaderBuilderImpl anyElement(Class cls) throws ReaderException
  {
    return this.anyElement(new AnyContents(cls));
  }

  @Override
  public ReaderBuilderImpl anyElement(Class cls, Options... options) throws ReaderException
  {
    return this.anyElement(new AnyContents(cls, options));
  }

  @Override
  public ReaderBuilderImpl anyElement(AnyFactory any) throws ReaderException
  {
    // TODO verify if this restriction is really necessary
    Reader.Declaration decl = parentReader.getDeclaration();
    if (decl.order() == Order.ALL
            || decl.order() == Order.CHOICE)
      throw new ReaderException("any not accepted in all or choice order.");
    if (producer != null)
      throw new ReaderException("element contents redefined");
    this.resultClass = any.getObjectClass();
    this.producer = new AnyHandlerProducer(any);
    return this;
  }

  @Override
  public ReaderBuilderImpl reference(Class resultClass) throws ReaderException
  {
    if (producer != null)
      throw new ReaderException("element contents redefined");
    this.resultClass = resultClass;
    this.producer = new ReferenceHandlerProducer();
    return this;
  }

  @Override
  public ReaderBuilderOptions section(SectionInterface section) throws ReaderException
  {
    if (this.producer != null)
      throw new ReaderException("element contents redefined");
    if (section == null)
      throw new ReaderException("section is null");
    this.resultClass = section.getObjectClass();
    this.producer = new SectionHandlerProducer(section);
    executeCall(null);
    define();
    return this;
  }
//</editor-fold>
//<editor-fold desc="setOptions">

  @Override
  public void setOptions(EnumSet<Options> flags)
  {
    if (lastHandler == null)
      throw new RuntimeException("null handler");

    // Options apply to all the elements in the last group.
    //  FIXME it is not entirely clear what this is used for
    //  currently.   It may be part of groups of elements, but
    //  further testing is required.
    ElementHandler iter = lastHandler;
    while (iter != null)
    {
      ((ElementHandlerImpl) iter).setOptions(flags);
      iter = iter.getNextHandler();
    }
  }

  @Override
  public EnumSet<Options> getOptions()
  {
    return lastHandler.getOptions();
  }

  @Override
  public ReaderBuilderImpl deferrable()
  {
    setOptions(EnumSet.of(Options.DEFERRABLE));
    return this;
  }

  @Override
  public ReaderBuilderImpl optional()
  {
    setOptions(EnumSet.of(Options.OPTIONAL));
    return this;
  }

  @Override
  public ReaderBuilderImpl required()
  {
    setOptions(EnumSet.of(Options.REQUIRED));
    return this;
  }

  @Override
  public ReaderBuilderImpl unbounded()
  {
    setOptions(EnumSet.of(Options.UNBOUNDED));
    return this;
  }

  @Override
  public ReaderBuilderImpl define()
  {
    setOptions(EnumSet.of(Options.NO_REFERENCE));
    return this;
  }

//  @Override
//  public ReaderBuilderImpl nocache()
//  {
//    if (this.parentReader instanceof ObjectReader)
//      ((ObjectReader) this.parentReader).setNoCache();
//    else
//      throw new RuntimeException("nocache is only valid on object readers");
//    return this;
//  }
  @Override
  public ReaderBuilderImpl noid()
  {
    setOptions(EnumSet.of(Options.NO_ID));
    return this;
  }

//</editor-fold>
//<editor-fold desc="call">
  protected <T2> ReaderBuilderImpl executeCall(BiConsumer<T, T2> method) throws ReaderException
  {
    this.lastHandler = producer.newInstance(this, elementName,
            this.parentGroup.getElementOptions(),
            target, resultClass, method);
    producer = null;
    elementName = null;
    resultClass = null;
    return this;
  }

  @Override
  public ReaderBuilderImpl call(BiConsumer<T, T2> method) throws ReaderException
  {
    return executeCall(method);
  }

  @Override
  public <T3> ReaderBuilderImpl call(BiConsumer<T, T3> method, Class<T3> resultClass) throws ReaderException
  {
    if (this.resultClass == null)
      this.contents(resultClass);
    else
    {
      if (!resultClass.isAssignableFrom(this.resultClass))
        throw new ReaderException("Can't assign " + this.resultClass + " to " + resultClass + " for method " + method);
      this.resultClass = resultClass;
    }

    ReaderBuilderImpl<T, T3> impl = (ReaderBuilderImpl<T, T3>) this;
    return impl.call(method);
  }

  @Override
  public ReaderBuilderImpl call(Consumer<T> method) throws ReaderException
  {
    // FIXME this seems a bit like a kludge.  I can add a full path for this case,
    reader(new NopReader());
    BiConsumer<T, Integer> proxy = (T t, Integer i) -> method.accept(t);
    return call(proxy, Integer.class);
  }

  @Override
  public ReaderBuilderImpl call(final Reader.AttributesStringConsumer method) throws ReaderException
  {
    BiConsumer<T, AttributeContents> proxy = (T t, AttributeContents c) ->
    {
      try
      {
        method.call(t, c.getAttributes(), c.getContents());
      }
      catch (ReaderException ex)
      {
        throw new RuntimeException(ex);
      }
    };
    return call(proxy, AttributeContents.class);
  }

  @Override
  public ReaderBuilderImpl callString(BiConsumer<T, String> methodName) throws ReaderException
  {
    return this.reader(new StringContents()).call(methodName);
  }

  @Override
  public ReaderBuilderImpl callDouble(BiConsumer<T, Double> methodName) throws ReaderException
  {
    return this.reader(new DoubleContents()).call(methodName);
  }

  @Override
  public ReaderBuilderImpl callInteger(BiConsumer<T, Integer> methodName) throws ReaderException
  {
    return this.reader(new IntegerContents()).call(methodName);
  }

  @Override
  public ReaderBuilderImpl callLong(BiConsumer<T, Long> methodName) throws ReaderException
  {
    return this.reader(new LongContents()).call(methodName);
  }

  @Override
  public ReaderBuilderImpl callBoolean(BiConsumer<T, Boolean> methodName) throws ReaderException
  {
    return this.reader(new BooleanContents()).call(methodName);
  }

  @Override
  public ReaderBuilderImpl nop() throws ReaderException
  {
    return executeCall(null);
  }
//</editor-fold>
//<editor-fold desc="producer">

  String getKey(String value)
  {
    return value + this.uriName;
  }

  @Override
  public ReaderBuilderCall<T, Boolean> flag() throws ReaderException
  {
    return this.reader(new FlagReader());
  }

  @Override
  public <T2> ReaderBuilderCall<T, List<T2>> list(ObjectReader<T2> reader) throws ReaderException
  {
    return this.reader(new ObjectListReader(reader, this.elementName, this.parentReader.getPackage()));
  }

  protected interface Producer<T, T2>
  {
    ElementHandlerImpl newInstance(ReaderBuilderImpl builder,
            String elementName, EnumSet<Options> flags, T target, Class<T2> resultClass, BiConsumer<T, T2> method)
            throws ReaderException;
  }

  private class ReaderHandlerProducer<T, T2> implements Producer<T, T2>
  {
    Reader<T2> reader;

    ReaderHandlerProducer(Reader<T2> reader)
    {
      this.reader = reader;
    }

    @Override
    public ElementHandlerImpl newInstance(ReaderBuilderImpl builder, String key,
            EnumSet<Options> flags, T target, Class<T2> resultCls, BiConsumer<T, T2> method) throws ReaderException
    {
      if (key == null)
      {
        if (reader.getPackage()!=parentReader.getPackage())
          throw new ReaderException("Schema mismatch on element. element name must be specifier in handler.");
        key = reader.getHandlerKey();
      }
      if (key == null)
        throw new ReaderException("No element name for " + reader.getClass());
      ElementHandlerImpl out = new ReaderHandler(key, flags, target, method, reader);
      builder.add(out);
      return out;
    }

    @Override
    public String toString()
    {
      return "reader for " + reader.getObjectClass().toString();
    }
  }

  static private class ReadersProducer<T, T2> implements Producer<T, T2>
  {
    ObjectReader[] obj;

    ReadersProducer(ObjectReader[] obj)
    {
      this.obj = obj;
    }

    @Override
    public ElementHandlerImpl newInstance(ReaderBuilderImpl builder, String elementName, EnumSet<Options> flags, T target, Class<T2> resultClass, BiConsumer<T, T2> method)
    {
      ElementHandlerImpl out = null;
      for (ObjectReader reader : obj)
      {
        EnumSet<Options> nextFlags = flags;
//        nextFlags |= (reader.getOptions() & Options.Internal.INHERIT_MASK);
        ElementHandlerImpl next = new ReaderHandler(reader.getHandlerKey(), nextFlags, target, method, reader);
        if (next.getKey() == null)
          throw new RuntimeException("Missing key for " + reader);
        builder.add(next);
        if (out == null)
          out = next;
      }
      return out;
    }
  }

  static private class SectionHandlerProducer<T, T2> implements Producer<T, T2>
  {
    SectionInterface obj;

    SectionHandlerProducer(SectionInterface obj)
    {
      this.obj = obj;
    }

    @Override
    public ElementHandlerImpl newInstance(
            ReaderBuilderImpl builder,
            String elementName, EnumSet<Options> flags,
            T target, Class<T2> resultClass, BiConsumer<T, T2> method)
    {
      SectionHandler handler = new SectionHandler(flags, obj);
      builder.add(handler);
      return handler;
    }

    @Override
    public String toString()
    {
      return "section of " + obj.getObjectClass().getClass();
    }
  }

  static private class ReferenceHandlerProducer<T, T2> implements Producer<T, T2>
  {
    ReferenceHandlerProducer()
    {
    }

    @Override
    public ElementHandlerImpl newInstance(
            ReaderBuilderImpl builder,
            String elementName, EnumSet<Options> flags,
            T target, Class<T2> resultClass, BiConsumer<T, T2> method)
    {
      ElementHandlerImpl out = new ReferenceHandler<>(elementName,
              flags, target, resultClass, method);
      builder.add(out);
      return out;
    }

    @Override
    public String toString()
    {
      return "reference";
    }
  }

  static private class AnyHandlerProducer<T, T2> implements Producer<T, T2>
  {
    AnyFactory obj;

    AnyHandlerProducer(AnyFactory obj)
    {
      this.obj = obj;
    }

    @Override
    public ElementHandlerImpl newInstance(ReaderBuilderImpl builder, String elementName,
            EnumSet<Options> flags, T target, Class<T2> resultClass, BiConsumer<T, T2> method)
    {
      ElementHandlerImpl out = new AnyHandlerImpl(flags, target, resultClass, method, obj);
      builder.add(out);
      return out;
    }

    @Override
    public String toString()
    {
      return "any " + obj.getClass().toString();
    }
  }

//</editor-fold>
//<editor-fold desc="list">
  @Internal
  public static class HandlerList
  {
    public ElementHandler firstHandler;
    public ElementHandler lastHandler;
    public StackTraceElement trace;

    void add(ElementHandler handler)
    {
      if (this.firstHandler == null)
      {
        this.firstHandler = handler;
        this.lastHandler = handler;
      }
      else
      {
        ((ElementHandlerImpl) lastHandler).setNextHandler(handler);
        this.lastHandler = handler;
      }
    }
  };

  void add(ElementHandler handler)
  {
    ((ElementHandlerImpl) handler).setParentGroup(parentGroup);
    handlerList.add(handler);
  }

  @Reader.Declaration(pkg = UtilityPackage.class, name = "nop")
  private class NopReader extends ObjectReader<Integer>
  {

    @Override
    public Integer start(Attributes attributes) throws ReaderException
    {
      return null;
    }

    @Override
    public Reader.ElementHandlerMap getHandlers() throws ReaderException
    {
      return null;
    }

    @Override
    public Class<? extends Integer> getObjectClass()
    {
      return Integer.class;
    }

    public String getSchemaType()
    {
      return "";
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