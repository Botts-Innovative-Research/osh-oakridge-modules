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
import gov.llnl.utility.xml.bind.AnyReader;
import gov.llnl.utility.xml.bind.ObjectReader;
import gov.llnl.utility.xml.bind.PolymorphicReader;
import gov.llnl.utility.xml.bind.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import org.xml.sax.Attributes;

/**
 * FIXME this should be hidden.
 *
 * @author nelson85
 */
@Internal
public class ObjectListReader<T> extends ObjectReader<List<T>>
{
  private final ObjectReader reader;
  private final String name;
  private final PackageResource schema;

  public ObjectListReader(ObjectReader reader, String name, PackageResource schema)
  {
    this.name = name;
    this.schema = schema;
    this.reader = reader;
  }

  public Reader.Declaration getDeclaration()
  {
    return new ReaderDeclarationImpl()
    {
      @Override
      public Class<? extends PackageResource> pkg()
      {
        return schema.getClass();
      }

      @Override
      public String name()
      {
        return name;
      }
    };
  }

  @Override
  public List<T> start(Attributes attributes) throws ReaderException
  {
    return new ArrayList<>();
  }

  @Override
  @SuppressWarnings("unchecked")
  public ElementHandlerMap getHandlers() throws ReaderException
  {
    ReaderBuilder<List<T>> builder = this.newBuilder();

    ReaderBuilderCall source;
    if (reader instanceof PolymorphicReader)
    {
      PolymorphicReader poly = (PolymorphicReader) reader;
      source = builder.readers(poly.getObjectClass(), poly.getReaders());
    }
    else if (reader instanceof AnyReader)
    {
      source = builder.anyElement(reader.getObjectClass());
    }
    else
    {
      source = builder.reader(reader);
    }

    source.call(new BiConsumer<List<T>, T>()
    {
      @Override
      public void accept(List<T> l, T t)
      {
        l.add(t);
      }
    }).optional();
    return builder.getHandlers();
  }

  @Override
  @SuppressWarnings("unchecked")
  public Class<? extends List<T>> getObjectClass()
  {
    return (Class<List<T>>) (Class) List.class;
  }

  public String getSchemaType()
  {
    return "List-" + reader.getSchemaType();
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