/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.ernie.rtk;

import gov.llnl.ernie.ErniePackage;
import gov.llnl.utility.Expandable;
import gov.llnl.utility.io.WriterException;
import gov.llnl.utility.xml.bind.ObjectWriter;
import java.io.Serializable;
import java.util.function.Predicate;

/**
 *
 * @author nelson85
 * @param <T>
 */
public class SpectrumWriter<T extends Spectrum> extends ObjectWriter<T>
{
  public SpectrumWriter()
  {
    super(Options.NONE, "spectrum", ErniePackage.getInstance());
  }

  @Override
  public void attributes(WriterAttributes attributes, T object) throws WriterException
  {
  }

  @Override
  public void contents(T object) throws WriterException
  {
    Predicate<String> exclude = this.getContext()
            .getProperty(SpectrumAttributes.WRITER_EXCLUDE, Predicate.class, null);
    WriterBuilder wb = newBuilder();
    if (!object.getAttributes().isEmpty())
      wb.element("attributes").writer(new AttributesWriter()).put(object);
    if (exclude == null || !exclude.test("realTime"))
      wb.element("realTime").putDouble(object.getRealTime());
    if (exclude == null || !exclude.test("liveTime"))
      wb.element("liveTime").putDouble(object.getLiveTime());
    if (object.getEnergyScale() != null)
      wb.element("gammaEnergyBins").writer(new EnergyScaleWriter()).put(object.getEnergyScale());
    if (object instanceof SpectrumBase)
    {
      wb.element("gammaCounts").putContents(((SpectrumBase) object).toArray());
    }
    else
    {
      wb.element("gammaCounts").putContents(object.toDoubles());
    }
  }

  private static class AttributesWriter extends ObjectWriter<Expandable>
  {
    public AttributesWriter()
    {
      super(Options.NONE, "attributes", ErniePackage.getInstance());
    }

    @Override
    public void attributes(WriterAttributes attributes, Expandable object) throws WriterException
    {
    }

    @Override
    public void contents(Expandable object) throws WriterException
    {
      Predicate<String> exclude = this.getContext()
              .getProperty(SpectrumAttributes.WRITER_EXCLUDE, Predicate.class, null);
      WriterBuilder wb = newBuilder();
      for (String keys : object.getAttributes().keySet())
      {
        if (exclude!=null && exclude.test(keys))
          continue;
        String[] tokens = keys.split("#");
        if (tokens.length < 2)
          continue;
        String localName = tokens[1];
        Serializable value = object.getAttribute(keys);
        wb.element(localName).putContents(value);
      }
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