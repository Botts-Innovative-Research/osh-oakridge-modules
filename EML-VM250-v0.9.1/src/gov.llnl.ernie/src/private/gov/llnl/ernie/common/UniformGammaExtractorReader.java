/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.ernie.common;

import gov.llnl.ernie.ErniePackage;
import gov.llnl.utility.io.ReaderException;
import gov.llnl.utility.xml.bind.ObjectReader;
import gov.llnl.utility.xml.bind.Reader;
import org.xml.sax.Attributes;

@Reader.Declaration(pkg = ErniePackage.class, name = "uniformGammaExtractor", referenceable = true, order = Reader.Order.SEQUENCE)
public class UniformGammaExtractorReader extends ObjectReader<UniformGammaExtractor>
{
  UniformGammaExtractor out;

  @Override
  public UniformGammaExtractor start(Attributes attributes) throws ReaderException
  {
    out = null;
    return null;
  }

  @Override
  public ElementHandlerMap getHandlers() throws ReaderException
  {
    ReaderBuilder<UniformGammaExtractor> builder = this.newBuilder();
    builder.using(this).anyElement(UniformGammaExtractor.class).call((p, v) -> p.out = v);
    return builder.getHandlers();
  }

  @Override
  public UniformGammaExtractor end() throws ReaderException
  {
    return out;
  }

  @Override
  public Class<UniformGammaExtractor> getObjectClass()
  {
    return UniformGammaExtractor.class;
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