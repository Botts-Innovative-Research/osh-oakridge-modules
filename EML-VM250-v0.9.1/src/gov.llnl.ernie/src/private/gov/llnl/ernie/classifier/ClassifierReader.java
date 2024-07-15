/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.ernie.classifier;

import gov.llnl.ernie.AnalysisOptions;
import gov.llnl.ernie.ErniePackage;
import gov.llnl.utility.io.ReaderException;
import gov.llnl.utility.xml.bind.ObjectReader;
import gov.llnl.utility.xml.bind.Reader;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.xml.sax.Attributes;

/**
 * Load the CMU classifier from an external source.
 *
 * This uses the search path for xml documents.
 *
 * @author nelson85
 */
@Reader.Declaration(pkg = ErniePackage.class, name = "classifier",
        referenceable = true,
        order = Reader.Order.ALL)
@Reader.Attribute(name = "model", type = String.class, required = true)
@Reader.Attribute(name = "metrics", type = String.class, required = true)
public class ClassifierReader extends ObjectReader<Classifier>
{
  @Override
  public Classifier start(Attributes attributes) throws ReaderException
  {
    try
    {
      // We need to be able to turn off the loading of the classifiers.
      // If disabled we will get a null rather than attempting to load the class.
      Boolean disable = getContext().get(AnalysisOptions.DISABLE_CLASSIFIERS, Boolean.class);
      if (disable != null && disable == true)
        return null;

      // Fetch the attributes
      String model = attributes.getValue("model");
      String metrics = attributes.getValue("metrics");

      // Convert the attributes into files using the DocumentReader.SEARCH_PATHS.
      Path externModel = Paths.get(getContext().getExternal(model).toURI());
      Path externMetrics = Paths.get(getContext().getExternal(metrics).toURI());

      // Load the classifier
      return new Classifier(externModel.toString(), externMetrics.toString(), false);
    }
    catch (URISyntaxException ex)
    {
      throw new ReaderException(ex);
    }
  }

  @Override
  public ElementHandlerMap getHandlers() throws ReaderException
  {
    return null;
  }

  @Override
  public Class<? extends Classifier> getObjectClass()
  {
    return Classifier.class;
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