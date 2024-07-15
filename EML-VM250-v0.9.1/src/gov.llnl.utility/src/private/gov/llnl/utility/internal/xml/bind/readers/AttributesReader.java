/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.utility.internal.xml.bind.readers;

import gov.llnl.utility.UtilityPackage;
import gov.llnl.utility.internal.xml.bind.SchemaBuilderUtilities;
import gov.llnl.utility.io.ReaderException;
import gov.llnl.utility.xml.DomBuilder;
import gov.llnl.utility.xml.bind.ObjectReader;
import gov.llnl.utility.xml.bind.Reader;
import gov.llnl.utility.xml.bind.Reader.Order;
import gov.llnl.utility.xml.bind.SchemaBuilder;
import org.xml.sax.Attributes;

/**
 *
 * @author nelson85
 */
@Reader.Declaration(
        pkg = UtilityPackage.class,
        name = "attributes",
        order = Order.ALL)
@Reader.AnyAttribute(processContents = Reader.ProcessContents.Skip)
@Reader.TextContents(base = "util:empty-attr")
public class AttributesReader extends ObjectReader<Attributes>
{

  @Override
  public ElementHandlerMap getHandlers() throws ReaderException
  {
    return null;
  }

  @Override
  public Class<Attributes> getObjectClass()
  {
    return Attributes.class;
  }

  @Override
  public Attributes start(Attributes attributes) throws ReaderException
  {
    return attributes;
  }

  @Override
  public void createSchemaType(SchemaBuilder builder) throws ReaderException
  {
  }

  @Override
  public DomBuilder createSchemaElement(SchemaBuilder builder, String name, DomBuilder group, boolean topLevel)
  {
    return SchemaBuilderUtilities.createSchemaElementSimple(this, builder, name, group, topLevel);
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