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
import gov.llnl.utility.io.WriterException;
import gov.llnl.utility.xml.bind.ObjectWriter;

/**
 *
 * @author nelson85
 */
public class ShieldingModelWriter extends ObjectWriter<ShieldingModel>
{
  public ShieldingModelWriter()
  {
    super(Options.NONE, "shieldingModel", ErniePackage.getInstance());
  }

  @Override
  public void attributes(WriterAttributes attributes, ShieldingModel object) throws WriterException
  {
  }

  @Override
  public void contents(ShieldingModel object) throws WriterException
  {
    WriterBuilder builder = newBuilder();
    builder.element("inputBins").putInteger(object.inputBins);
    builder.element("fluxBins").putInteger(object.fluxBins);
    builder.element("outputBins").putInteger(object.outputBins);
    builder.element("transform").contents(Matrix.class).put(object.transform);
    builder.element("basis").contents(Matrix.class).put(object.basis);

    WriteObject<FluxModel> wo = builder.element("model").writer(new FluxModelWriter());
    for (int i = 0; i < object.fluxModels.size(); ++i)
    {
      wo.put(object.fluxModels.get(i)).attr("id", i);
    }
  }

  class FluxModelWriter extends ObjectWriter<FluxModel>
  {
    public FluxModelWriter()
    {
      super(Options.NONE, "model", ErniePackage.getInstance());
    }

    @Override
    public void attributes(WriterAttributes attributes, FluxModel object) throws WriterException
    {
    }

    @Override
    public void contents(FluxModel object) throws WriterException
    {

      WriterBuilder builder = newBuilder();
      int n = object.getNumArealDensities();
      for (int j = 0; j < n; ++j)
      {
        double[] v = new double[object.spectrumModel.length];
        for (int k = 0; k < object.spectrumModel.length; ++k)
        {
          v[k] = object.spectrumModel[k].getControlY(j);
        }
        builder.element("shielding")
                .putContents(v)
                .attr("ad", String.format("%.1f", object.getArealDensity(j)));
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