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

import gov.llnl.utility.UtilityPackage;
import gov.llnl.utility.annotation.Internal;
import gov.llnl.utility.io.ReaderException;
import org.xml.sax.Attributes;

/**
 *
 * @author nelson85
 */
@Reader.Declaration(pkg = UtilityPackage.class,
        name = "any", order = Reader.Order.SEQUENCE,
        referenceable = true)
public class AnyReader<T> extends ObjectReader<T>
{
  Class<T> cls;
  T obj;

  /**
   * Used to build the schema.
   */
  @Internal
  AnyReader()
  {
    this.cls = null;
  }

  private AnyReader(Class<T> cls)
  {
    this.cls = cls;
  }

  static public <T> AnyReader<T> of(Class<T> cls)
  {
    return new AnyReader<>(cls);
  }

  @Override
  public T start(Attributes attributes) throws ReaderException
  {
    obj = null;
    return null;
  }

  @Override
  public T end() throws ReaderException
  {
    return obj;
  }

  @Override
  public ElementHandlerMap getHandlers() throws ReaderException
  {
    ReaderBuilder<T> builder = this.newBuilder();
    builder.using(this)
            .anyElement(cls)
            .call(AnyReader::setObj);
    return builder.getHandlers();
  }

  @Override
  public Class getObjectClass()
  {
    return cls;
  }

  void setObj(T obj)
  {
    this.obj = obj;
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