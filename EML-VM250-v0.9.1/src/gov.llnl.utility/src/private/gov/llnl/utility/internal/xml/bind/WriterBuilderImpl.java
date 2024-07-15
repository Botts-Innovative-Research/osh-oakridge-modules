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
import gov.llnl.utility.io.WriterException;
import gov.llnl.utility.xml.DomBuilder;
import gov.llnl.utility.xml.bind.ObjectWriter;
import gov.llnl.utility.xml.bind.SchemaManager;
import gov.llnl.utility.xml.bind.WriterContext;

/**
 *
 * @author nelson85
 */
@Internal
@SuppressWarnings("unchecked")
public class WriterBuilderImpl implements
        ObjectWriter.WriterBuilder,
        ObjectWriter.WriteObject,
        ObjectWriter.WriteOptions,
        ObjectWriter.WriterContents
{
  ObjectWriter parentWriter;
  String elementName = null;
  Producer producer = null;

  WriterBuilderImpl(ObjectWriter writer)
  {
    this.parentWriter = writer;
  }

  @Override
  public WriterBuilderImpl element(String name)
  {
    elementName = name;
    producer = null;
    return this;
  }

  @Override
  public <Type> WriterBuilderImpl writer(ObjectWriter<Type> writer)
  {
    producer = new WriterProducer(elementName, writer);
    elementName = null;
    return this;
  }

  @Override
  public <Type> WriterBuilderImpl contents(Class<? extends Type> cls) throws WriterException
  {
    return writer(SchemaManager.getInstance().findObjectWriter(getContext(), cls));
  }

  @Override
  public void comment(String str) throws WriterException
  {
    getContext().current().getElement().comment(str);
  }

  @Override
  public WriterBuilderImpl put(Object object) throws WriterException
  {
    producer.handle(object);
    return this;
  }

  @Override
  public void section(ObjectWriter.Section writer) throws WriterException
  {
    if (elementName == null)
      elementName = writer.getElementName();
    producer = new WriterProducer(elementName, writer);
    producer.handle(getContext().current().getObject());
    elementName = null;
  }

  @Override
  public WriterBuilderImpl id(String id) throws WriterException
  {
    getContext().last().setId(id);
    return this;
  }

  @Override
  public <T> WriterBuilderImpl attr(String key, T value) throws WriterException
  {
    if (value == null)
      return this;
    WriterContext.Marshaller marshaller = getContext().getMarshaller(value.getClass());
    if (marshaller == null)
      throw new WriterException("No marshaller for " + value.getClass().getName());
    getContext().last().getElement().attr(key,
            marshaller.marshall(value, getContext().getMarshallerOptions()));
    return this;
  }

//<editor-fold desc="internal">  
  private WriterContextImpl getContext() throws WriterException
  {
    return (WriterContextImpl) this.parentWriter.getContext();
  }

  @Override
  public <Type> WriterBuilderImpl putContents(Type value) throws WriterException
  {
    getContext().writeContent(elementName, value);
    elementName = null;
    return this;
  }

  @Override
  public <Type> WriterBuilderImpl putList(Iterable<Type> value) throws WriterException
  {
    if (elementName == null)
      throw new WriterException("list element not named");
    WriterContextImpl context = getContext();
    PackageResource pkg = this.parentWriter.getPackage();
    DomBuilder element = context.newElement(pkg, elementName);
    context.pushContext(element, null, elementName, pkg);
    Class cls = null;
    ObjectWriter writer = null;
    for (Type v : value)
    {
      if (v == null)
        throw new WriterException("null element");
      if (cls != v.getClass())
      {
        cls = v.getClass();
        writer = ObjectWriter.create(cls);
        producer = new WriterProducer(writer.getElementName(), writer);
      }
      producer.handle(v);
    }
    elementName = null;
    context.popContext();
    return this;
  }

  @Override
  public <Type> WriterBuilderImpl putList(Iterable<Type> value, ObjectWriter<Type> writer) throws WriterException
  {
    if (elementName == null)
      throw new WriterException("list element not named");
    WriterContextImpl context = getContext();
    PackageResource pkg = this.parentWriter.getPackage();
    DomBuilder element = context.newElement(pkg, elementName);
    context.pushContext(element, null, elementName, pkg);
    producer = new WriterProducer(writer.getElementName(), writer);
    elementName = null;
    for (Type v : value)
    {
      producer.handle(v);
    }
    context.popContext();
    return this;
  }

  @Override
  public void reference(String id) throws WriterException
  {
    getContext().current().getElement().element(elementName).attr("ref_id", id);
    elementName = null;
  }

  @Override
  public WriterBuilderImpl put()
          throws WriterException
  {
    return putContents(null);
  }

  @Override
  public WriterBuilderImpl putString(String value)
          throws WriterException
  {
    return putContents(value);
  }

  @Override
  public WriterBuilderImpl putInteger(int value)
          throws WriterException
  {
    return putContents(value);
  }

  @Override
  public WriterBuilderImpl putLong(long value)
          throws WriterException
  {
    return putContents(value);
  }

  @Override
  public WriterBuilderImpl putDouble(double value)
          throws WriterException
  {
    return putContents(value);
  }

  @Override
  public WriterBuilderImpl putDouble(double value, String format)
          throws WriterException
  {
    return putContents(String.format(format, value));
  }

  @Override
  public WriterBuilderImpl putBoolean(boolean value)
          throws WriterException
  {
    return putContents(value);
  }

  @Override
  public WriterBuilderImpl putFlag(boolean value)
          throws WriterException
  {
    if (value == true)
      putContents(null);
    elementName = null;
    return this;
  }

  interface Producer
  {
    void handle(Object object) throws WriterException;
  }

  class WriterProducer implements Producer
  {
    String elementName;
    ObjectWriter writer;

    WriterProducer(String elementName, ObjectWriter writer)
    {
      this.elementName = elementName;
      this.writer = writer;
    }

    @Override
    public void handle(Object object) throws WriterException
    {
      getContext().write(null, writer, elementName, object, false);
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