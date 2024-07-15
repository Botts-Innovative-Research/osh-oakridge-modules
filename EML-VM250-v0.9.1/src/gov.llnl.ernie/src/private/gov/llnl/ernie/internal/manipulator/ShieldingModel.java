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

import gov.llnl.math.DoubleArray;
import gov.llnl.math.MathExceptions.DomainException;
import gov.llnl.math.MathExceptions.SizeException;
import gov.llnl.math.algebra.Nnlsq;
import gov.llnl.math.algebra.NnlsqFactory;
import gov.llnl.math.matrix.Matrix;
import gov.llnl.math.matrix.MatrixColumnArray;
import gov.llnl.math.matrix.MatrixFactory;
import gov.llnl.math.matrix.MatrixOps;
import gov.llnl.math.matrix.MatrixViews;
import gov.llnl.math.spline.CubicHermiteSpline;
import gov.llnl.math.spline.CubicHermiteSplineFactory;
import gov.llnl.utility.xml.bind.ReaderInfo;
import gov.llnl.utility.xml.bind.WriterInfo;
import gov.llnl.utility.io.ReaderException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import static java.util.Collections.sort;
import java.util.Comparator;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Code for computing the effects of shielding on spectral shape.
 *
 * This code is used exclusively for record manipulation. It needs to be
 * modernized to support RTK spectral classes rather than using the raw arrays.
 *
 * @author nelson85
 */
@ReaderInfo(ShieldingModelReader.class)
@WriterInfo(ShieldingModelWriter.class)
public class ShieldingModel
{

  public ArrayList<FluxModel> fluxModels = new ArrayList<>();
  public MatrixColumnArray transform; // this will be a fluxModels x inputBins matrix
  public MatrixColumnArray basis;
  public int outputBins;
  public int inputBins;
  public int fluxBins;
  public double lambda = 0.01;

  public void setNumOutputBins(int i)
  {
    outputBins = i;
  }

  public void setNumInputBins(int i)
  {
    inputBins = i;
  }

  /**
   * Used by MATLAB to populate the initial transform from the basis set of
   * unshielded sources. Q=inv(basis'*basis+lambda*eye(9))*basis';
   *
   * @param matrix
   */
  public void setTransform(MatrixColumnArray matrix) throws SizeException
  {
    if (matrix.rows() != fluxBins)
    {
      throw new SizeException("Flux bins incorrect");
    }
    if (matrix.columns() != inputBins)
    {
      throw new SizeException("Input bins incorrect");
    }
    this.transform = matrix;
  }

  public void setBasis(MatrixColumnArray matrix) throws SizeException
  {
    if (matrix.rows() != inputBins)
    {
      throw new SizeException("Flux bins incorrect");
    }
    if (matrix.columns() != fluxBins)
    {
      throw new SizeException("Input bins incorrect");
    }
    this.basis = matrix;
  }

  /**
   * Convert the input spectrum into an estimate flux. The values can be
   * negative for this calculation.
   *
   * @param inputSpectrum
   * @return
   * @throws SizeException
   */
  public double[] estimateFlux(double[] inputSpectrum) throws SizeException
  {
    if (inputSpectrum.length != this.inputBins)
    {
      throw new SizeException("Incorrect input size " + inputSpectrum.length);
    }
    double[] flux;
    NnlsqFactory ws = new NnlsqFactory();
    Nnlsq wsImpl = ws.createSolver();
    Nnlsq.Input wsInput = null;
    Nnlsq.Output wsOutput;
    if (basis != null)
    {
      wsInput = ws.createInput(basis, inputSpectrum, null);
      wsImpl.solve(wsInput);
      wsOutput = wsImpl.getSolution();
      flux = wsOutput.toCoefficients();
    }
    else if (transform != null)
    {
      flux = MatrixOps.multiply(transform, inputSpectrum);
    }
    else
    {
      throw new RuntimeException("conversion method missing");
    }

    // Check for excessive error on injection
    double[] est = this.computeFromFlux(flux, 0);
    double err = 0;
    for (int i = 0; i < est.length; ++i)
    {
      err += (inputSpectrum[i] - est[i]) * (inputSpectrum[i] - est[i]) / (inputSpectrum[i] + lambda);
    }

    // If we can't hit the spectrum physically, use an alternative method
    if (err > 5)
    {
      Matrix b = MatrixOps.multiply(basis.transpose(), basis);
      MatrixOps.addAssign(MatrixViews.diagonal(b), 0.01);
      double[] c = MatrixOps.multiply(basis.transpose(), inputSpectrum);
      flux = MatrixOps.divideLeft(b, MatrixFactory.wrapColumnVector(c)).flatten();
    }

    return flux;
  }

  /**
   * Use the model to compute the effect of shielding on a spectrum. The number
   * of output channels will be outputBins. The size of the input must be input
   * bins.
   *
   * @param inputSpectrum
   * @param ad is the areal density in g/cm^2
   * @return the resulting spectrum
   * @throws SizeException
   */
  public double[] compute(double[] inputSpectrum, double ad) throws SizeException
  {
    // Short cut for irregular range
    if (ad <= 0)
    {
      return inputSpectrum.clone();
    }
    double[] flux = estimateFlux(inputSpectrum);
    return computeFromFlux(flux, ad);
  }

  /**
   * Use the model to compute the effect of shielding on a spectrum. The number
   * of output channels will be outputBins. The size of the input must be input
   * bins.
   *
   * @param flux
   * @param ad is the areal density in g/cm^2
   * @return the resulting spectrum
   * @throws SizeException
   */
  public double[] computeFromFlux(double[] flux, double ad) throws SizeException
  {
    double[] out = new double[outputBins];
    for (int i = 0; i < flux.length; ++i)
    {
      double[] shieldedChannel = fluxModels.get(i).getSpectrum(ad);
      DoubleArray.addAssignScaled(out, 0, shieldedChannel, 0, out.length, flux[i]);
    }
    for (int i = 0; i < out.length; ++i)
    {
      out[i] = Math.max(out[i], 0);
    }
    return out;
  }

  /**
   * Read a model from a file.
   *
   * @param file
   * @throws IOException if the file can not be read.
   */
  public void load(File file) throws IOException, ReaderException
  {
    load(Files.newInputStream(file.toPath()));
  }

  public void load(InputStream is) throws IOException, ReaderException
  {

    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder dBuilder;
    Document doc;
    try
    {
      dBuilder = dbFactory.newDocumentBuilder();
      doc = dBuilder.parse(is);
    }
    catch (ParserConfigurationException | SAXException ex)
    {
      throw new ReaderException(ex);
    }

    NodeList nl = doc.getDocumentElement().getChildNodes();

    for (int i = 0; i < nl.getLength(); ++i)
    {
      Node iter = nl.item(i);
      if (iter.getNodeType() != Node.ELEMENT_NODE)
      {
        continue;
      }

      Element element = (Element) iter;
      // These three tags should always appear first in the model.
      if (element.getNodeName().equals("inputBins"))
      {
        this.inputBins = Integer.parseInt(element.getTextContent());
        continue;
      }
      if (element.getNodeName().equals("fluxBins"))
      {
        this.resizeFluxModels(Integer.parseInt(element.getTextContent()));
        continue;
      }
      if (element.getNodeName().equals("outputBins"))
      {
        this.outputBins = Integer.parseInt(element.getTextContent());
        continue;
      }

      // This is the next block that defines how to transform the input into
      // flux.
      if (element.getNodeName().equals("basis"))
      {
        int rows = Integer.parseInt(element.getAttribute("rows"));
        int columns = Integer.parseInt(element.getAttribute("columns"));
        double content[] = DoubleArray.fromString(element.getTextContent());
        if (rows != this.inputBins || columns != this.fluxBins)
        {
          throw new ReaderException("Transform size incorrect");
        }
        try
        {
          this.basis = new MatrixColumnArray(content, rows, columns);
        }
        catch (SizeException ex)
        {
          throw new ReaderException(ex);
        }
        continue;
      }

      // This is the next block that defines how to transform the input into
      // flux.
      if (element.getNodeName().equals("transform"))
      {
        int rows = Integer.parseInt(element.getAttribute("rows"));
        int columns = Integer.parseInt(element.getAttribute("columns"));
        double content[] = DoubleArray.fromString(element.getTextContent());
        if (rows != this.fluxBins || columns != this.inputBins)
        {
          throw new ReaderException("Transform size incorrect");
        }
        try
        {
          this.transform = new MatrixColumnArray(content, rows, columns);
        }
        catch (SizeException ex)
        {
          throw new ReaderException(ex);
        }
        continue;
      }

      // We then have one model for each flux
      if (element.getNodeName().equals("model"))
      {
        parseFluxModel(element);
        continue;
      }
      throw new ReaderException("Unexpected tag " + element.getNodeName());
    }

    // Okay everything should be loaded and the correct size.
    // Verify all of the models have been loaded.
    for (FluxModel l : this.fluxModels)
    {
      if (l == null)
      {
        throw new ReaderException("Undefined flux model");
      }
    }
  }

  /**
   * Helper for MATLAB used to create the shielding matrix.
   *
   * @return
   */
  public static PrototypeModel allocatePrototype()
  {
    return new PrototypeModel();
  }

  /**
   * Internal function used to load a flux model.
   *
   * @param root
   * @throws gov.llnl.ernie.simulation.ShieldingModel.LoadException
   */
  private void parseFluxModel(Element root) throws ReaderException
  {
    int channel = Integer.parseInt(root.getAttribute("id"));
    PrototypeModel prototype = new PrototypeModel();
    NodeList nl = root.getChildNodes();
    for (int i = 0; i < nl.getLength(); ++i)
    {
      Node iter = nl.item(i);
      if (iter.getNodeType() != Node.ELEMENT_NODE)
      {
        continue;
      }

      Element element = (Element) iter;
      if (element.getNodeName().equals("shielding"))
      {
        double ad = Double.parseDouble(element.getAttribute("ad"));
        double[] content = DoubleArray.fromString(element.getTextContent());
        prototype.addShielding(ad, content);
        continue;
      }
      throw new ReaderException("Unexpected tag " + element.getNodeName());
    }
    if (prototype.getNumOutputBins() != this.outputBins)
    {
      throw new ReaderException("Output size mismatch");
    }

    this.fluxModels.set(channel, prototype.convert());
  }

  /**
   * Used by the loader and MATLAB to populate the flux models.
   *
   * @param m
   */
  public void resizeFluxModels(int m)
  {
    this.fluxBins = m;
    this.fluxModels.clear();
    for (int i = 0; i < this.fluxBins; ++i)
    {
      this.fluxModels.add(null);
    }
  }

  /**
   * Save the model to a file.
   *
   * @param file is the file to store the data in.
   * @throws FileNotFoundException if the file cannot be written to.
   */
  public void save(File file) throws FileNotFoundException
  {
//    PrintStream ps = null;
//    ps = new PrintStream(file);
//    try
//    {
//      ps.println("<shieldingModel>");
//      ps.println("  <inputBins>" + this.inputBins + "</inputBins>");
//      ps.println("  <fluxBins>" + this.fluxBins + "</fluxBins>");
//      ps.println("  <outputBins>" + this.outputBins + "</outputBins>");
//      ps.println("  <transform rows=\"" + this.transform.rows()
//              + "\" columns=\"" + this.transform.columns() + "\">");
//      ps.println(DoubleArray.toString(this.transform.toArray()));
//      ps.println("  </transform>");
//
//      ps.println("  <basis rows=\"" + this.basis.rows()
//              + "\" columns=\"" + this.basis.columns() + "\">");
//      ps.println(DoubleArray.toString(this.basis.toArray()));
//      ps.println("  </basis>");
//
//      for (int i = 0; i < this.fluxModels.size(); ++i)
//      {
//        FluxModel model = this.fluxModels.get(i);
//        ps.println("  <model id=\"" + i + "\">");
//        int n = model.getNumArealDensities();
//        for (int j = 0; j < n; ++j)
//        {
//          ps.print("    <shielding ad=\"" + model.getArealDensity(j) + "\">");
//          for (int k = 0; k < this.outputBins; ++k)
//          {
////            ps.print(model.spectrumModel[k].getKnotValue(j) + " ");
//            ps.print(model.spectrumModel[k].getControlY(j) + " ");
//          }
//          ps.println("</shielding>");
//        }
//        ps.println("</model>");
//      }
//      ps.println("</shieldingModel>");
//    }
//    finally
//    {
//      // The file should be closed even if there was an exception during
//      // writing.
//      ps.close();
//    }
  }

//<editor-fold desc="inner classes" defaultstate="collapsed">
  /**
   * Each channel has an expected influence on the output.
   */
  static public class FluxModel
  {
    // This is the internal representation for this spectrum.
//    CubicSpline[] spectrumModel;
    public CubicHermiteSpline[] spectrumModel;

    /**
     * Computes the spectrum associated with this areal density by interpolating
     * with cubic splines.
     *
     * @param ad is the areal density (g/cm^2) to compute.
     * @return the spectrum for this areal density.
     */
    public double[] getSpectrum(double ad)
    {
      try
      {
        double[] out = new double[spectrumModel.length];
        for (int i = 0; i < spectrumModel.length; ++i)
        {
          out[i] = spectrumModel[i].applyAsDouble(ad);
        }
        return out;
      }
      catch (DomainException ex)
      {
        throw new RuntimeException(); // can not occur as the Spline is clamped
      }
    }

    /**
     * Called to allocate memory when converting from prototypes.
     *
     * @param size is the number of output channels.
     */
    public void resize(int size)
    {
//      spectrumModel = new CubicSpline[size];
      spectrumModel = new CubicHermiteSpline[size];
    }

    /**
     * Get the number of interpolation points. Used to save the model to a file.
     *
     * @return
     */
    public int getNumArealDensities()
    {
//      return this.spectrumModel[0].size();
      return (this.spectrumModel[0].getControl()).size();
    }

    /**
     * Get the areal density for the interpolation points. As all of the models
     * have the same spline points, this just uses the first spectral channels
     * output.
     *
     * @param j
     * @return
     */
    public double getArealDensity(int j)
    {
//      return this.spectrumModel[0].getKnotPoint(j);
      return ((this.spectrumModel[0].getControl()).get(j)).x;
    }

  }

  /**
   * The prototypes hold a set of output spectrum for each areal density.
   */
  public static class PrototypeSpectrum
  {
    double ad;
    double[] spectrum;

    private PrototypeSpectrum(double ad, double[] spectrum)
    {
      this.ad = ad;
      this.spectrum = spectrum;
    }
  }

  /**
   * Helper class used by the loader and MATLAB to populate the model.
   */
  public static class PrototypeModel
  {

    ArrayList<PrototypeSpectrum> prototypes = new ArrayList<>();

    public void addShielding(double ad, double[] spectrum)
    {
      prototypes.add(new PrototypeSpectrum(ad, spectrum));
    }

    public int getNumOutputBins()
    {
      return this.prototypes.get(0).spectrum.length;
    }

    /**
     * Convert the prototype models into cubic splines for interpolation.
     *
     * @return the spline model
     */
    public FluxModel convert()
    {
      // Create an output
      FluxModel output = new FluxModel();

      // Sort by AD
      sort(prototypes, new Comparator<PrototypeSpectrum>()
      {
        @Override
        public int compare(PrototypeSpectrum t, PrototypeSpectrum t1)
        {
          if (t.ad < t1.ad)
          {
            return -1;
          }
          if (t.ad > t1.ad)
          {
            return 1;
          }
          return 0;
        }
      });

      int n = this.prototypes.size();
      int m = this.prototypes.get(0).spectrum.length;
      double x[] = new double[n];
      double y[] = new double[n];
      output.resize(n);

      // Populate the interpolation points for the domain
      for (int i = 0; i < n; ++i)
      {
        x[i] = this.prototypes.get(i).ad;
      }

      // For each output channel
      for (int j = 0; j < m; ++j)
      {
        // Copy the data from prototypes into points to fit with spline
        for (int i = 0; i < n; ++i)
        {
          y[i] = this.prototypes.get(i).spectrum[j];
        }

        // Create a cubic spline that fits the points.
//        output.spectrumModel[j] = CubicSpline.create(x, y);
//        output.spectrumModel[j] = CubicSpline.create(x, y);
        output.spectrumModel[j] = CubicHermiteSplineFactory.createNatural(x, y);
        output.spectrumModel[j] = CubicHermiteSplineFactory.createNatural(x, y);

        // Defined how the end points should be interpreted
//        output.spectrumModel[j].setEndBehavior(CubicSpline.CLAMP);
      }
      return output;
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