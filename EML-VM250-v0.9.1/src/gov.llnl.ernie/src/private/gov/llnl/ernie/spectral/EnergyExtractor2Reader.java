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
import gov.llnl.ernie.spectral.EnergyExtractor2.HypothesisTest;
import gov.llnl.ernie.spectral.EnergyExtractor2.RatioTest;
import gov.llnl.math.matrix.Matrix;
import gov.llnl.utility.ArrayEncoding;
import gov.llnl.utility.InitializeException;
import gov.llnl.utility.io.ReaderException;
import gov.llnl.utility.xml.bind.ObjectReader;
import gov.llnl.utility.xml.bind.ObjectStringReader;
import gov.llnl.utility.xml.bind.Reader;
import java.text.ParseException;
import java.util.List;
import org.xml.sax.Attributes;

/**
 *
 * @author nelson85
 */
@Reader.Declaration(pkg = ErniePackage.class, name = "energyExtractor2",
        referenceable = true,
        document = true,
        order = Reader.Order.ALL)
public class EnergyExtractor2Reader extends ObjectReader<EnergyExtractor2>
{

  @Override
  public EnergyExtractor2 start(Attributes attributes) throws ReaderException
  {
    return new EnergyExtractor2();
  }

  @Override
  public EnergyExtractor2 end() throws ReaderException
  {
    try
    {
      getObject().initialize();
      return null;
    }
    catch (InitializeException ex)
    {
      throw new ReaderException(ex);
    }
  }

  @Override
  public ElementHandlerMap getHandlers() throws ReaderException
  {
    ReaderBuilder<EnergyExtractor2> builder = newBuilder();
    builder.element("ratioTests").list(new RatioTestReader())
            .call((EnergyExtractor2 ee, List<RatioTest> m)
                    -> ee.ratioTests.addAll(m)).optional();

    builder.element("energyFactors").contents(double[].class)
            .call((EnergyExtractor2 ee, double[] m) -> ee.energyFactors = m).optional();
    builder.element("pcaTransform").contents(Matrix.class)
            .call((EnergyExtractor2 ee, Matrix m) -> ee.pcaTransform = m).optional();
    builder.element("dataMean").contents(Matrix.class)
            .call((EnergyExtractor2 ee, Matrix m) -> ee.dataMean = m).optional();
    builder.element("coh").contents(Matrix.class)
            .call((EnergyExtractor2 ee, Matrix m) -> ee.coh = m).optional();

    builder.element("hypothesisTests").list(new HypothesisTestReader())
            .call((EnergyExtractor2 ee, List<HypothesisTest> m)
                    -> ee.hypothesisTests.addAll(m));

    builder.element("bkgHypothesisCov").contents(Matrix.class)
            .call((EnergyExtractor2 ee, Matrix m) -> ee.backgroundHypothesisTest.cov = m);
    return builder.getHandlers();
  }

  @Override
  public Class<? extends EnergyExtractor2> getObjectClass()
  {
    return EnergyExtractor2.class;
  }

//<editor-fold desc="internal" defaultstate="collapsed">
  @Reader.Declaration(pkg = ErniePackage.class, name = "ratioTest",
          contents = Reader.Contents.TEXT)
  public class RatioTestReader extends ObjectStringReader<EnergyExtractor2.RatioTest>
  {
    @Override
    public EnergyExtractor2.RatioTest contents(String textContents) throws ReaderException
    {
      try
      {
        RatioTest rt = new RatioTest();
        rt.window = ArrayEncoding.decodeIntegers(textContents);
        return rt;
      }
      catch (ParseException ex)
      {
        throw new ReaderException(ex);
      }
    }

    @Override
    public Class<? extends EnergyExtractor2.RatioTest> getObjectClass()
    {
      return RatioTest.class;
    }
  }

  @Reader.Declaration(pkg = ErniePackage.class, name = "hypothesisTest",
          order = Reader.Order.ALL)
  @Reader.Attribute(name = "title", type = String.class)
  public class HypothesisTestReader extends ObjectReader<EnergyExtractor2.HypothesisTest>
  {

    @Override
    public EnergyExtractor2.HypothesisTest start(Attributes attributes) throws ReaderException
    {
      HypothesisTest hypothesis = new HypothesisTest();
      hypothesis.title = attributes.getValue("title");
      return hypothesis;
    }

    @Override
    public ElementHandlerMap getHandlers() throws ReaderException
    {
      ReaderBuilder<EnergyExtractor2.HypothesisTest> builder = newBuilder();
      builder.element("mean").contents(Matrix.class).call((HypothesisTest obj, Matrix m) -> obj.mean = m);
      builder.element("cov").contents(Matrix.class).call((HypothesisTest obj, Matrix m) -> obj.cov = m);
      return builder.getHandlers();
    }

    @Override
    public Class<? extends EnergyExtractor2.HypothesisTest> getObjectClass()
    {
      return HypothesisTest.class;
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