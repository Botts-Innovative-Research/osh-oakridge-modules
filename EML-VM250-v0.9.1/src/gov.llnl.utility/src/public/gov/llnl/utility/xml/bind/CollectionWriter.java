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

import gov.llnl.utility.io.WriterException;
import java.util.Collection;

/**
 * Convenience class for writing out lists of items.
 *
 * @author nelson85
 */
public class CollectionWriter<Entry> extends ObjectWriter<Collection<Entry>>
{
  private ObjectWriter<Entry> writer;

  public CollectionWriter(ObjectWriter<Entry> writer)
  {
    super(Options.NONE, "collection", writer.getPackage());
    this.writer = writer;
  }

  public static <Type> CollectionWriter<Type> from(Class<Type> cls) throws WriterException
  {
    return new CollectionWriter<Type>(ObjectWriter.create(cls));
  }

  @Override
  public void attributes(WriterAttributes attributes, Collection object) throws WriterException
  {
  }

  @Override
  public void contents(Collection<Entry> object) throws WriterException
  {
    WriterBuilder wb = newBuilder();
    WriteObject<Entry> wo = wb.writer(writer);
    for (Entry obj : object)
    {
      wo.put(obj);
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