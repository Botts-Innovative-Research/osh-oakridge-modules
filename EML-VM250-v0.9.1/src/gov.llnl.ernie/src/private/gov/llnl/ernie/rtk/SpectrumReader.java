/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gov.llnl.ernie.rtk;

import gov.llnl.ernie.data.EnergyScale;
import gov.llnl.utility.ArrayEncoding;
import gov.llnl.utility.io.ReaderException;
import gov.llnl.utility.xml.bind.ObjectReader;
import gov.llnl.utility.xml.bind.ReaderContext;
import gov.llnl.utility.xml.bind.SchemaManager;
import java.io.Serializable;
import java.text.ParseException;
import java.util.EnumSet;
import org.xml.sax.Attributes;

/**
 *
 * @author nelson85
 */
public abstract class SpectrumReader<T extends SpectrumBase> extends ObjectReader<T>
{
  Class<T> spectrumClass;

  public SpectrumReader(Class<T> cls)
  {
    this.spectrumClass = cls;
  }

  @Override
  public T start(Attributes attributes) throws ReaderException
  {
    try
    {
      return this.spectrumClass.newInstance();
    }
    catch (InstantiationException | IllegalAccessException ex)
    {
      throw new ReaderException(ex);
    }
  }

  @Override
  public Class<T> getObjectClass()
  {
    return this.spectrumClass;
  }

  @Override
  public ElementHandlerMap getHandlers() throws ReaderException
  {
    ReaderBuilder<T> builder = this.newBuilder();
    builder.using(this)
            .element("gammaCounts")
            .call(SpectrumReader::assign);
    builder.element("title").callString(T::setTitle);
    builder.element("realTime").callDouble(T::setRealTime);
    builder.element("liveTime").callDouble(T::setLiveTime);
    builder.element("gammaEnergyBins").call(T::setEnergyScale, EnergyScale.class);
    builder.section(new AttributesSection());
    return builder.getHandlers();
  }

  void assign(Attributes attr, String contents) throws ReaderException
  {
    try
    {
      Class cls = getObject().getCountClass();
      if (cls.equals(int[].class))
        getObject().assignData(ArrayEncoding.decodeIntegers(contents));
      else if (cls.equals(double[].class))
        getObject().assignData(ArrayEncoding.decodeDoubles(contents));
      else
        throw new ReaderException("Unable to handle " + cls);
    }
    catch (ParseException ex)
    {
      throw new ReaderException(ex);
    }
  }

  class AttributesSection extends Section
  {
    public AttributesSection()
    {
      super(Order.FREE, "attributes");
    }

    @Override
    public ElementHandlerMap getHandlers() throws ReaderException
    {
      ReaderBuilder<T> builder = this.newBuilder();
      builder.using(this)
              .anyElement(new SpectrumAttribute())
              .call(AttributesSection::setAttribute).optional();
      return builder.getHandlers();
    }

    void setAttribute(Object object)
    {
      ReaderContext.HandlerContext hc = getContext().getLastHandlerContext();
      String namespace = hc.getNamespaceURI();
      String localname = hc.getLocalName();
      if (namespace == null)
        namespace = "";
      getObject().setAttribute(String.format("%s#%s", namespace, localname), (Serializable) object);
    }
  }

  class SpectrumAttribute implements AnyFactory<Object>
  {
    @Override
    public Class<Object> getObjectClass()
    {
      return Object.class;
    }

    @Override
    public ObjectReader getReader(
            String namespaceURI, String name, String qualifiedName,
            Attributes attr)
            throws ReaderException
    {
      try
      {
        SchemaManager schemaMgr = SchemaManager.getInstance();
        Class<?> cls = schemaMgr.getObjectClass(namespaceURI, name);
        return schemaMgr.findObjectReader(cls);
      }
      catch (ClassNotFoundException ex)
      {
        throw new RuntimeException(ex);
      }
    }

    @Override
    public EnumSet<Options> getOptions()
    {
      return EnumSet.of(Options.ANY_ALL, Options.ANY_SKIP);
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