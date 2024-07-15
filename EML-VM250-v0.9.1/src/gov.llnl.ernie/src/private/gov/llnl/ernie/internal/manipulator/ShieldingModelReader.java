/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.ernie.internal.manipulator;

import gov.llnl.ernie.ErniePackage;
import gov.llnl.ernie.internal.manipulator.ShieldingModel.FluxModel;
import gov.llnl.math.matrix.Matrix;
import gov.llnl.math.matrix.MatrixColumnArray;
import gov.llnl.utility.ArrayEncoding;
import gov.llnl.utility.io.ReaderException;
import gov.llnl.utility.xml.bind.ObjectReader;
import gov.llnl.utility.xml.bind.Reader;
import java.text.ParseException;
import org.xml.sax.Attributes;

/**
 *
 * @author nelson85
 */
@Reader.Declaration(pkg = ErniePackage.class, name = "shieldingModel",
        document = true, order = Reader.Order.SEQUENCE)
public class ShieldingModelReader extends ObjectReader<ShieldingModel>
{

  @Override
  public ShieldingModel start(Attributes attributes) throws ReaderException
  {
    return new ShieldingModel();
  }

  @Override
  public ElementHandlerMap getHandlers() throws ReaderException
  {
    ReaderBuilder<ShieldingModel> builder = newBuilder();
    builder.element("inputBins").callInteger((ShieldingModel object, Integer i) -> object.inputBins = i);
    builder.element("fluxBins").callInteger((ShieldingModel object, Integer i) -> object.fluxBins = i);
    builder.element("outputBins").callInteger((ShieldingModel object, Integer i) -> object.outputBins = i);
    builder.element("transform")
            .contents(Matrix.class)
            .call((ShieldingModel object, Matrix i) -> object.transform = new MatrixColumnArray(i));
    builder.element("basis")
            .contents(Matrix.class)
            .call((ShieldingModel object, Matrix i) -> object.basis = new MatrixColumnArray(i));
    builder.element("model").reader(new FluxModelReader())
            .call((ShieldingModel object, FluxModel i) -> object.fluxModels.add(i)).unbounded();
    return builder.getHandlers();
  }

  @Override
  public Class<? extends ShieldingModel> getObjectClass()
  {
    return ShieldingModel.class;
  }
//<editor-fold desc="internal" defaultstate="collapsed">

  @Reader.Declaration(pkg = ErniePackage.class, name = "shieldingFluxModel",
          document = true, order = Reader.Order.SEQUENCE)
  @Reader.Attribute(name = "id", type = String.class)
  public static class FluxModelReader extends ObjectReader<FluxModel>
  {
    ShieldingModel.PrototypeModel prototype;

    @Override
    public FluxModel start(Attributes attributes) throws ReaderException
    {
      prototype = new ShieldingModel.PrototypeModel();
      return null;
    }

    @Override
    public FluxModel end() throws ReaderException
    {
      return prototype.convert();
    }

    @Override
    public ElementHandlerMap getHandlers() throws ReaderException
    {
      ReaderBuilder<FluxModel> builder = newBuilder();
      builder.using(this)
              .element("shielding")
              .call(FluxModelReader::addShielding)
              .unbounded();
      return builder.getHandlers();
    }

    void addShielding(Attributes attributes, String contents)
    {
      try
      {
        double ad = Double.parseDouble(attributes.getValue("ad"));
        double[] content = ArrayEncoding.decodeDoubles(contents);
        prototype.addShielding(ad, content);
      }
      catch (ParseException ex)
      {
        throw new RuntimeException(ex);
      }
    }

    @Override
    public Class<? extends FluxModel> getObjectClass()
    {
      return FluxModel.class;
    }

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