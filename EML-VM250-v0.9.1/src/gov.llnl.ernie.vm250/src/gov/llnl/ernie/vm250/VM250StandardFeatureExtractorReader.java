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

import gov.llnl.utility.io.ReaderException;
import gov.llnl.utility.xml.bind.ObjectReader;
import gov.llnl.utility.xml.bind.Reader;
import org.xml.sax.Attributes;

/**
 *
 * @author nelson85
 */
@Reader.Declaration(pkg = ErnieVM250Package.class, name = "standardFeatureExtractor",
        referenceable=true, 
        document = true, 
        order = Reader.Order.SEQUENCE)
public class VM250StandardFeatureExtractorReader extends ObjectReader<VM250StandardFeatureExtractor>
{

  @Override
  public VM250StandardFeatureExtractor start(Attributes attributes) throws ReaderException
  {
    return new VM250StandardFeatureExtractor();
  }

  @Override
  public VM250StandardFeatureExtractor end() throws ReaderException
  {
    getObject().initialize();
    return null;
  }

  @Override
  public ElementHandlerMap getHandlers() throws ReaderException
  {
    ReaderBuilder<VM250StandardFeatureExtractor> builder = this.newBuilder();
    builder.element("panels").callInteger(VM250StandardFeatureExtractor::setPanels);
    return builder.getHandlers();
  }

  @Override
  public Class<? extends VM250StandardFeatureExtractor> getObjectClass()
  {
    return VM250StandardFeatureExtractor.class;
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