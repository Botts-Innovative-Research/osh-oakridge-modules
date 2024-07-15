/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.ernie.spectral;

import gov.llnl.ernie.ErniePackage;
import gov.llnl.math.matrix.Matrix;
import gov.llnl.utility.io.WriterException;
import gov.llnl.utility.xml.bind.ObjectWriter;

/**
 *
 * @author nelson85
 */
public class EnergyExtractor2Writer extends ObjectWriter<EnergyExtractor2>
{
  public EnergyExtractor2Writer()
  {
    super(Options.NONE, "energyExtractor2", ErniePackage.getInstance());
  }

  @Override
  public void attributes(WriterAttributes attributes, EnergyExtractor2 object) throws WriterException
  {
  }

  @Override
  public void contents(EnergyExtractor2 object) throws WriterException
  {
    WriterBuilder builder = newBuilder();
    if (object.ratioTests.size() > 0)
      builder.element("ratioTests").putList(object.ratioTests, new RatioTestsWriter());
    if (object.energyFactors != null)
      builder.element("energyFactors").contents(double[].class).put(object.energyFactors);
    if (object.pcaTransform != null)
    {
      builder.element("pcaTransform").contents(Matrix.class).put(object.pcaTransform);
      builder.element("dataMean").contents(Matrix.class).put(object.dataMean);
      builder.element("coh").contents(Matrix.class).put(object.coh);
    }
    builder.element("hypothesisTests").putList(object.hypothesisTests, new HypothesisTestWriter());
    builder.element("bkgHypothesisCov").contents(Matrix.class).put(object.backgroundHypothesisTest.cov);
  }

//<editor-fold desc="internal" defaultstate="collapsed">
  public class RatioTestsWriter extends ObjectWriter<EnergyExtractor2.RatioTest>
  {
    public RatioTestsWriter()
    {
      super(Options.NONE, "ratioTest", ErniePackage.getInstance());
    }

    @Override
    public void attributes(WriterAttributes attributes, EnergyExtractor2.RatioTest object) throws WriterException
    {
    }

    @Override
    public void contents(EnergyExtractor2.RatioTest object) throws WriterException
    {
      this.addContents(object.window);
    }
  }

  public class HypothesisTestWriter extends ObjectWriter<EnergyExtractor2.HypothesisTest>
  {
    public HypothesisTestWriter()
    {
      super(Options.NONE, "hypothesisTest", ErniePackage.getInstance());
    }

    @Override
    public void attributes(WriterAttributes attributes, EnergyExtractor2.HypothesisTest object) throws WriterException
    {
      attributes.add("title", object.title);
    }

    @Override
    public void contents(EnergyExtractor2.HypothesisTest object) throws WriterException
    {
      WriterBuilder builder = newBuilder();
      builder.element("mean").contents(Matrix.class).put(object.mean);
      builder.element("cov").contents(Matrix.class).put(object.cov);
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