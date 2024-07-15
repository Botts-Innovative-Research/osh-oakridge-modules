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
import gov.llnl.utility.xml.bind.Reader;
import gov.llnl.utility.xml.bind.Reader.Order;
import java.lang.annotation.Annotation;

/**
 * Front end for creating a dynamic reader declaration.
 *
 * Note: The defaults should always match the defaults in the class definition.
 *
 * @author nelson85
 */
public abstract class ReaderDeclarationImpl implements Reader.Declaration
{
  final Reader.Declaration base;

  public ReaderDeclarationImpl()
  {
    base = null;
  }

  public ReaderDeclarationImpl(Reader.Declaration base)
  {
    this.base = base;
  }

  public Class<? extends PackageResource> pkg()
  {
    if (base != null)
      return base.pkg();
    return null;
  }

  public String name()
  {
    if (base != null)
      return base.name();
    return null;
  }

  @Override
  public Reader.Order order()
  {
    if (base != null)
      return base.order();
    return Order.FREE;
  }

  @Override
  public boolean referenceable()
  {
    if (base != null)
      return base.referenceable();
    return false;
  }

  @Override
  public boolean contentRequired()
  {
    if (base != null)
      return base.contentRequired();
    return false;
  }

  @Override
  public Reader.Contents contents()
  {
    if (base != null)
      return base.contents();
    return Reader.Contents.ELEMENTS;
  }

  @Override
  public boolean copyable()
  {
    if (base != null)
      return base.copyable();
    return false;
  }

  @Override
  public String typeName()
  {
    if (base != null)
      return base.typeName();
    return null;
  }

  @Override
  public boolean document()
  {
    if (base != null)
      return base.document();
    return false;
  }

  @Override
  public boolean autoAttributes()
  {
    if (base != null)
      return base.autoAttributes();
    return false;
  }

  @Override
  public Class impl()
  {
    if (base != null)
      return base.impl();
    return null;
  }

  @Override
  public Class cls()
  {
    if (base != null)
      return base.cls();
    return null;
  }

  @Override
  public Class<? extends Annotation> annotationType()
  {
    return Reader.Declaration.class;
  }

  public String schema()
  {
    if (base != null)
      return base.schema();
    return Reader.Declaration.NULL;
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