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
import gov.llnl.math.matrix.Matrix;
import gov.llnl.utility.io.ReaderException;
import gov.llnl.utility.xml.bind.ObjectReader;
import gov.llnl.utility.xml.bind.Reader;
import org.xml.sax.Attributes;

/**
 *
 * @author mattoon1
 */
@Reader.Declaration(pkg=ErniePackage.class, name="transformFeatureExtractor", 
        referenceable=true, 
        order=Reader.Order.SEQUENCE)
public class TransformFeatureExtractorReader extends ObjectReader<TransformFeatureExtractor>
{

  @Override
  public TransformFeatureExtractor start(Attributes attributes) throws ReaderException
  {
    return new TransformFeatureExtractor();
  }
  
  @Override
  public Reader.ElementHandlerMap getHandlers() throws ReaderException
  {
    Reader.ReaderBuilder<TransformFeatureExtractor> builder = this.newBuilder();
    builder.section(new Imports()).optional();
    builder.element("fft")
            .contents(Matrix.class)
            .call(TransformFeatureExtractor::setTransformFFT);
    builder.element("pca")
            .contents(Matrix.class)
            .call(TransformFeatureExtractor::setTransformPCA);
    builder.element("uniformGammaExtractor")
            .contents(UniformGammaExtractor.class)
            .call(TransformFeatureExtractor::setUniformGammaExtractor);
    return builder.getHandlers();
  }

  @Override
  public Class<? extends TransformFeatureExtractor> getObjectClass()
  {
    return TransformFeatureExtractor.class;
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