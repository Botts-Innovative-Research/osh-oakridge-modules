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
import gov.llnl.ernie.analysis.AnalysisException;
import gov.llnl.ernie.analysis.FeatureExtractor;
import gov.llnl.math.Fourier;
import gov.llnl.math.MathExceptions.ResizeException;
import gov.llnl.math.MathExceptions.SizeException;
import gov.llnl.math.matrix.Matrix;
import gov.llnl.math.matrix.MatrixColumnTable;
import gov.llnl.math.matrix.MatrixOps;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import gov.llnl.ernie.data.Record;
import gov.llnl.math.ComplexVector;
import gov.llnl.utility.InitializeException;
import gov.llnl.ernie.analysis.Features;
import gov.llnl.ernie.analysis.FeaturesDescription;
import gov.llnl.math.DoubleArray;
import gov.llnl.utility.xml.bind.ReaderInfo;

/**
 *
 * @author nelson85
 */
@ReaderInfo(TransformFeatureExtractorReader.class)
public class TransformFeatureExtractor implements FeatureExtractor
{
  private Matrix transformPCA = new MatrixColumnTable();
  private Matrix transformFFT = new MatrixColumnTable();

  private UniformGammaExtractor uniformGammaExtractor;

  private static final Logger LOGGER = ErniePackage.LOGGER;
  TransformFeaturesDescription description = null;

  public TransformFeatureExtractor()
  {
  }

  @Override
  public void initialize() throws InitializeException
  {
    if (transformPCA == null)
    {
      throw new InitializeException("transform pca matrix not set");
    }
    if (transformFFT == null)
    {
      throw new InitializeException("transform fft matrix not set");
    }
    this.description = new TransformFeaturesDescription(this.transformPCA.rows(), this.transformFFT.rows());
  }

  @Override
  public FeaturesDescription getDescription()
  {
    return description;
  }

  @Override
  public Features newFeatures()
  {
    return new TransformFeatures(this.description);
  }

  @Override
  public Features compute(Record record) throws AnalysisException
  {

    TransformFeatures result = null;
    try
    {
      Matrix uniformGamma = uniformGammaExtractor.extract(record, 0, 0);
      double[] profile = uniformGamma.flatten();
      DoubleArray.normColumns2(profile);

      // FIXME: #cols != #rows --- throws exception here - Joe
      double[] pcaResult = MatrixOps.multiply(transformPCA, profile);

      // Compute the PCA features
      ComplexVector fft = Fourier.fft(ComplexVector.create(profile, null));
      double[] fftResult = MatrixOps.multiply(transformFFT, Arrays.copyOfRange(fft.getAbs(), 0, 32));

      result = new TransformFeatures(this.description, pcaResult, fftResult);
    }
    catch (SizeException | ResizeException ex)
    {
      LOGGER.log(Level.SEVERE, null, ex);
      throw new AnalysisException("Unable to extract");
    }
    return result;
  }

//<editor-fold desc="loader">
  /**
   * @return the transformPCA
   */
  public Matrix getTransformPCA()
  {
    return transformPCA;
  }

  /**
   * @return the transformFFT
   */
  public Matrix getTransformFFT()
  {
    return transformFFT;
  }

//</editor-fold>
//<editor-fold desc="loader">
  /**
   * (internal) Used to load the pca transform.
   *
   * @param transformPCA the transformPCA to set
   */
  void setTransformPCA(Matrix transformPCA)
  {
    this.transformPCA = transformPCA;
  }

  /**
   * (internal) Used to load the fftRadix2 transform.
   *
   * @param transformFFT the transformFFT to set
   */
  void setTransformFFT(Matrix transformFFT)
  {
    this.transformFFT = transformFFT;
  }

  /**
   * (internal) Used to load the uniform gamma extractor.
   *
   * @param uge the extractor to set
   */
  void setUniformGammaExtractor(UniformGammaExtractor uge)
  {
    this.uniformGammaExtractor = uge;
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