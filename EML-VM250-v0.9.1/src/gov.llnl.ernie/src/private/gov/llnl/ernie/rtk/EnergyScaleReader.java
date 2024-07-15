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
import gov.llnl.ernie.ErniePackage;
import gov.llnl.utility.annotation.Internal;
import gov.llnl.utility.io.ReaderException;
import gov.llnl.utility.xml.bind.ObjectReader;
import gov.llnl.utility.xml.bind.Reader;
import org.xml.sax.Attributes;

/**
 * Polymorphic contents.
 *
 * @author nelson85
 */
@Reader.Declaration(pkg = ErniePackage.class, name = "energyScale", document = true,
        order = Reader.Order.CHOICE, referenceable = true)
@Reader.Attribute(name = "use_db", type = String.class)
@Reader.Attribute(name = "type", type = String.class)
@Reader.Attribute(name = "external", type = String.class)
@Internal
public class EnergyScaleReader extends ObjectReader<EnergyScale>
{
  boolean loaded = false;
  EnergyScale obj;
  private boolean use_db;

  @Override
  public EnergyScale start(Attributes attributes) throws ReaderException
  {
    String value;
    loaded = false;
    use_db = false;

    // Needed for old RNAK identify.  Remove once attribute is moved or reworked.
    value = attributes.getValue("use_db");
    if (value != null)
    {
      use_db = Boolean.parseBoolean(value);
      //EnergyScale obj = new EnergyBinsImpl(new double[0]);
      //obj.setAttribute("use_database", Boolean.parseBoolean(value));
      //return obj;
    }

    value = attributes.getValue("external");
    if (value != null)
    {
      throw new UnsupportedOperationException("External bin reader not currently supported.  Should use import.");
    }
    return null;
  }

  @Override
  public EnergyScale end() throws ReaderException
  {
    if (use_db == true)
    {
      EnergyScale obj = new EnergyBinsImpl(new double[0]);
      obj.setAttribute("use_database", use_db);
      return obj;
    }
    return obj;
  }

  @Override
  public Class<EnergyScale> getObjectClass()
  {
    return EnergyScale.class;
  }

  @Override
  public ElementHandlerMap getHandlers() throws ReaderException
  {
    ReaderBuilder<EnergyScaleReader> builder = this.newBuilder().using(this);
//    builder.element("pairs")
//            .reader(new EnergyPairsScaleReader())
//            .call(EnergyScaleReader::assign);
    builder.element("sqrt")
            .reader(new BinsSqrt())
            .call(EnergyScaleReader::assign);
    builder.element("linear")
            .reader(new BinsLinear())
            .call(EnergyScaleReader::assign);
    builder.element("values")
            .call(EnergyScaleReader::assignValues, double[].class);
    return builder.getHandlers();
  }

  void assign(EnergyScale scale)
  {
    obj = scale;
  }

  void assignValues(double[] pairs)
  {
    obj = EnergyScaleFactory.newScale(pairs);
  }

  @Reader.Declaration(pkg = ErniePackage.class, name = "linear", order = Reader.Order.ALL)
  @Reader.Attribute(name = "begin", type = Double.class)
  @Reader.Attribute(name = "end", type = Double.class)
  @Reader.Attribute(name = "steps", type = Integer.class)
  static public class BinsLinear extends ObjectReader<EnergyScale>
  {
    @Override
    public EnergyScale start(Attributes attributes) throws ReaderException
    {
      double begin = Double.parseDouble(attributes.getValue("begin").trim());
      double end = Double.parseDouble(attributes.getValue("end").trim());
      int steps = Integer.parseInt(attributes.getValue("steps").trim());
      return EnergyScaleFactory.newLinearScale(begin, end, steps);
    }

    @Override
    public ElementHandlerMap getHandlers() throws ReaderException
    {
      return null;
    }

    @Override
    public Class<EnergyScale> getObjectClass()
    {
      return EnergyScale.class;
    }
  }

  @Reader.Declaration(pkg = ErniePackage.class, name = "sqrt", order = Reader.Order.ALL)
  @Reader.Attribute(name = "begin", type = Double.class)
  @Reader.Attribute(name = "end", type = Double.class)
  @Reader.Attribute(name = "steps", type = Integer.class)
  static public class BinsSqrt extends ObjectReader<EnergyScale>
  {
    @Override
    public EnergyScale start(Attributes attributes) throws ReaderException
    {
      double begin = Double.parseDouble(attributes.getValue("begin").trim());
      double end = Double.parseDouble(attributes.getValue("end").trim());
      int steps = Integer.parseInt(attributes.getValue("steps").trim());
      return EnergyScaleFactory.newSqrtScale(begin, end, steps);
    }

    @Override
    public ElementHandlerMap getHandlers() throws ReaderException
    {
      return null;
    }

    @Override
    public Class<EnergyScale> getObjectClass()
    {
      return EnergyScale.class;
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