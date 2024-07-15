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

import gov.llnl.ernie.data.EnergyScale;
import gov.llnl.utility.io.ReaderException;
import gov.llnl.utility.xml.bind.ObjectReader;
import java.util.function.BiConsumer;
import org.xml.sax.Attributes;

/**
 *
 * @author nelson85
 * @param <T>
 */
public abstract class SpectraListReader<T extends SpectraList> extends ObjectReader<T>
{
  private T list;
  private Class<T> listClass;
  private final Class contentsClass;

  public SpectraListReader(T list)
  {
    this.list = list;
    this.listClass = (Class<T>) list.getClass();
    contentsClass = list.getSpectrumClass();
  }

  SpectraListReader(Class<T> listClass)
  {
    try
    {
      this.listClass = listClass;
      contentsClass = listClass.newInstance().getSpectrumClass();
    }
    catch (InstantiationException | IllegalAccessException ex)
    {
      throw new RuntimeException(ex);
    }
  }

  @Override
  public T start(Attributes attributes) throws ReaderException
  {
    if (list != null)
      return list;
    try
    {
      return listClass.newInstance();
    }
    catch (InstantiationException | IllegalAccessException ex)
    {
      throw new ReaderException(ex);
    }
  }

  @Override
  public Class<T> getObjectClass()
  {
    return listClass;
  }

  @Override
  public ElementHandlerMap getHandlers() throws ReaderException
  {
    ReaderBuilder<T> builder = this.newBuilder();
    builder.element("gadrasVersion").contents(String.class).nop();
    builder.element("energyBins").contents(EnergyScale.class).nop();
    BiConsumer<SpectraList, Spectrum> add = SpectraList::add;
    builder.element("spectrum").contents(contentsClass)
            .call(add);
    return builder.getHandlers();
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