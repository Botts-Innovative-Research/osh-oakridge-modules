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

import gov.llnl.ernie.analysis.AnalysisPreprocessor;
import gov.llnl.ernie.analysis.FeatureExtractor;
import gov.llnl.ernie.classifier.ClassifierReader;
import gov.llnl.utility.InitializeException;
import gov.llnl.utility.io.ReaderException;
import gov.llnl.utility.xml.bind.AnyReader;
import gov.llnl.utility.xml.bind.ObjectReader;
import gov.llnl.utility.xml.bind.Reader;
import org.xml.sax.Attributes;

/**
 *
 * @author nelson85
 */
@Reader.Declaration(pkg = ErnieVM250Package.class, name = "analysis",
        referenceable = true,
        document = true,
        order = Reader.Order.SEQUENCE)
public class VM250AnalysisReader extends ObjectReader<VM250Analysis>
{
  boolean verbose = false;

  @Override
  public VM250Analysis start(Attributes attributes) throws ReaderException
  {
    return new VM250Analysis();
  }

  @Override
  public VM250Analysis end() throws ReaderException
  {
    try
    {
      getObject().initialize();
    }
    catch (InitializeException ex)
    {
      throw new ReaderException(ex);
    }

    // Assuming the object is completely loaded we return it.
    return null;
  }

  @Override
  public ElementHandlerMap getHandlers() throws ReaderException
  {
    ReaderBuilder<VM250Analysis> builder = this.newBuilder();
    builder.using(this).element("verbose").flag().call(VM250AnalysisReader::setVerbose);
    builder.section(new Imports()).optional();
    builder.section(new Defines()).optional();
    builder.section(new Parameters());
    builder.element("preprocessors")
            .list(AnyReader.of(AnalysisPreprocessor.class))
            .call(VM250Analysis::setPreprocessors);
    builder.element("featureExtractors")
            .list(AnyReader.of(FeatureExtractor.class))
            .call(VM250Analysis::setFeatureExtractors);

    // Classifiers are specific fields in the analysis.  We will
    // organize them into a section.
    builder.section(new ClassifiersSection());

    return builder.getHandlers();
  }

  @Override
  public Class<? extends VM250Analysis> getObjectClass()
  {
    return VM250Analysis.class;
  }

//<editor-fold desc="internal" defaultstate="collapsed">
  class Parameters extends Section
  {
    public Parameters()
    {
      super(Order.OPTIONS, "parameters");
    }

    @Override
    public ElementHandlerMap getHandlers() throws ReaderException
    {
      ReaderBuilder<VM250Analysis> builder = this.newBuilder();
      builder.element("threshold")
              .callDouble(VM250Analysis::setThreshold);
      builder.element("nSigmaThreshold")
              .callDouble(VM250Analysis::setNSigmaThreshold).optional();
      builder.element("backgroundRatioThreshold")
              .callDouble(VM250Analysis::setBackgroundRatioThreshold).optional();
      return builder.getHandlers();
    }

  }
  
  class ClassifiersSection extends Section
  {
    public ClassifiersSection()
    {
      super(Order.OPTIONS, "classifiers");
    }

    @Override
    public ElementHandlerMap getHandlers() throws ReaderException
    {
      ReaderBuilder<VM250Analysis> builder = newBuilder();
      builder.element("alarmClassifier")
              .reader(new ClassifierReader())
              .call(VM250Analysis::setClassifier);
      return builder.getHandlers();
    }
  }


  void setVerbose(boolean verbose)
  {
    this.verbose = verbose;
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