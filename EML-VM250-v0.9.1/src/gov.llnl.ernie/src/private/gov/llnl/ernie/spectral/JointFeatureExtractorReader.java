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
import gov.llnl.utility.io.ReaderException;
import gov.llnl.utility.xml.bind.ObjectReader;
import gov.llnl.utility.xml.bind.Reader;
import org.xml.sax.Attributes;

/**
 *
 * @author nelson85
 */
@Reader.Declaration(pkg = ErniePackage.class, name = "jointFeatureExtractor",
        referenceable = true,
        document = true,
        order = Reader.Order.OPTIONS)
public class JointFeatureExtractorReader extends ObjectReader<JointFeatureExtractor>
{

  @Override
  public JointFeatureExtractor start(Attributes attributes) throws ReaderException
  {
    return new JointFeatureExtractor();
  }

  @Override
  public ElementHandlerMap getHandlers() throws ReaderException
  {
    ReaderBuilder<JointFeatureExtractor> builder = newBuilder();
    builder.element("panels").callInteger(JointFeatureExtractor::setPanels);
    builder.element("energyExtractor").contents(EnergyExtractor2.class)
            .call(JointFeatureExtractor::setEnergyExtractor);
    builder.element("fallback").callBoolean((e, p) -> e.fallback = p);
    return builder.getHandlers();
  }

  @Override
  public Class<? extends JointFeatureExtractor> getObjectClass()
  {
    return JointFeatureExtractor.class;
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