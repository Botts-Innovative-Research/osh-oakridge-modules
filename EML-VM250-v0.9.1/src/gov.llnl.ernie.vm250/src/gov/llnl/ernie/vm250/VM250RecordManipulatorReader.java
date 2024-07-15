/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.ernie.vm250;

import gov.llnl.ernie.internal.manipulator.AnisotropicModel;
import gov.llnl.ernie.manipulator.InjectionSourceLibrary;
import gov.llnl.ernie.internal.manipulator.ShieldingModel;
import gov.llnl.utility.ArrayEncoding;
import gov.llnl.utility.io.ReaderException;
import gov.llnl.utility.xml.bind.ObjectReader;
import gov.llnl.utility.xml.bind.ObjectStringReader;
import gov.llnl.utility.xml.bind.Reader;
import java.text.ParseException;
import org.xml.sax.Attributes;

/**
 *
 * @author nelson85
 */
@Reader.Declaration(pkg = ErnieVM250Package.class, name = "recordManipulator",
        referenceable=true, 
        document = true, 
        order = Reader.Order.SEQUENCE)
public class VM250RecordManipulatorReader extends ObjectReader<VM250RecordManipulator>
{

  @Override
  public VM250RecordManipulator start(Attributes attributes) throws ReaderException
  {
    return new VM250RecordManipulator();
  }

  @Override
  public VM250RecordManipulator end() throws ReaderException
  {
    getObject().initialize();
    return null;
  }

  @Override
  public ElementHandlerMap getHandlers() throws ReaderException
  {
    ReaderBuilder<VM250RecordManipulator> builder = newBuilder();
    builder.section(new Imports()).optional();
    builder.element("applyShielding")
            .callBoolean((VM250RecordManipulator obj, Boolean b) -> obj.applyShielding = b)
            .optional();
    builder.element("nominalLaneWidth")
            .callDouble(VM250RecordManipulator::setNominalLaneWidth)
            .optional();
    builder.element("passengerAreaModel")
            .reader(new AnisotropicModelReader())
            .call((VM250RecordManipulator obj, AnisotropicModel.Parameters b) -> obj.passengerAreaModel = b)
            .optional();
    builder.element("cargoModel")
            .reader(new AnisotropicModelReader())
            .call((VM250RecordManipulator obj, AnisotropicModel.Parameters b) -> obj.cargoModel.add(b))
            .unbounded();
    builder.element("shieldingModel")
            .reference(ShieldingModel.class)
            .call(VM250RecordManipulator::setShieldingModel);
    builder.element("sourceLibrary")
            .contents(InjectionSourceLibrary.class)
            .call(VM250RecordManipulator::setInjectionSourceLibrary);
    return builder.getHandlers();
  }

  @Override
  public Class<? extends VM250RecordManipulator> getObjectClass()
  {
    return VM250RecordManipulator.class;
  }

  @Reader.Declaration(pkg=ErnieVM250Package.class, name="anisotropicModel", 
          contents=Reader.Contents.TEXT)
  public class AnisotropicModelReader extends ObjectStringReader<AnisotropicModel.Parameters>
  {

    @Override
    public AnisotropicModel.Parameters contents(String textContents) throws ReaderException
    {
      try
      {
        double[] d = ArrayEncoding.decodeDoubles(textContents);
        if (d.length != 5)
        {
          throw new ReaderException("Bad anisotropic model");
        }
        return new AnisotropicModel.Parameters(d[0], d[1], d[2], d[3], d[4]);
      }
      catch (ParseException ex)
      {
        throw new ReaderException("Anisotropic model, bad double contents", ex);
      }
    }

    @Override
    public Class<? extends AnisotropicModel.Parameters> getObjectClass()
    {
      return AnisotropicModel.Parameters.class;
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