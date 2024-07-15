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

import gov.llnl.utility.PackageResource;
import gov.llnl.utility.internal.xml.bind.ReaderDeclarationImpl;
import gov.llnl.utility.io.ReaderException;
import org.xml.sax.Attributes;

/**
 * Reader for handling many different types that all derive from a common base
 * class.
 *
 * @param <T>
 */
public abstract class PolymorphicReader<T> extends ObjectReader<T>
{
  T object;

  /**
   * Polymorphic readers require a modified Reader.Declaration as they are a
   * specific generated type.
   *
   * @return
   */
  @Override
  public Reader.Declaration getDeclaration()
  {
    Declaration decl = this.getClass().getAnnotation(Reader.Declaration.class);
    return new ReaderDeclarationImpl(decl)
    {
      @Override
      public Class<? extends PackageResource> pkg()
      {
        return decl.pkg();
      }

      @Override
      public String name()
      {
        return decl.name();
      }

      @Override
      public Order order()
      {
        return Order.CHOICE;
      }

      @Override
      public String schema()
      {
        return Reader.Declaration.NULL;
      }
    };
  }

//  @Override
//  abstract public Class<T> getObjectClass();
  /**
   * Get the list of readers supported by this object.
   *
   * @return
   * @throws ReaderException
   */
  abstract public ObjectReader<? extends T>[] getReaders()
          throws ReaderException;

  /**
   * Utility function for creating a group of readers with the same base type.
   *
   * @param <Obj>
   * @param readers
   * @return
   */
  @SuppressWarnings("unchecked")
  static protected <Obj> ObjectReader<? extends Obj>[]
          group(ObjectReader<? extends Obj>... readers)
  {
    return readers;
  }

  @SuppressWarnings("unchecked")
  static protected <Obj> ObjectReader<? extends Obj>[]
          of(Class<? extends Obj>... obj)
          throws ReaderException
  {
    ObjectReader[] out = new ObjectReader[obj.length];
    for (int i = 0; i < obj.length; i++)
    {
      out[i] = ObjectReader.create(obj[i]);
    }
    return out;
  }

//<editor-fold desc="internal" defaultstate="collapsed">
  @Override
  final public T start(Attributes attributes) throws ReaderException
  {
    return null;
  }

  @Override
  final public T end() throws ReaderException
  {
    return object;
  }

  @Override
  @SuppressWarnings("unchecked")
  final public ElementHandlerMap getHandlers() throws ReaderException
  {
    ReaderBuilder<T> rb = newBuilder();
    rb.using(this).readers((Class<T>)getObjectClass(), getReaders())
            .call((PolymorphicReader<T> p, T o) -> p.object = o);
    return rb.getHandlers();
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