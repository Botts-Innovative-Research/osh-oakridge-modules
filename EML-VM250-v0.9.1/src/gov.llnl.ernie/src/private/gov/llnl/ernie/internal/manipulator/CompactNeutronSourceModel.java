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

import gov.llnl.math.MathExceptions.SizeException;
import gov.llnl.math.matrix.Matrix;
import gov.llnl.math.matrix.MatrixFactory;
import gov.llnl.math.matrix.MatrixOps;
import gov.llnl.math.matrix.MatrixViews;
import gov.llnl.math.matrix.io.MatlabReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.DataFormatException;

/**
 *
 * @author mattoon1
 */
public class CompactNeutronSourceModel
{

  double[] xArray, yArray, zArray, energyBounds;
  Matrix vData;
  double xMax, yMax, zMin, zMax;
  int ebins;

  public CompactNeutronSourceModel(String filename) throws MatlabReader.MatlabReaderException, IOException, DataFormatException
  {
    MatlabReader mr = new MatlabReader();

    File file = new File(filename);
    mr.load(file);

    xArray = ((Matrix) mr.get("xArray")).flatten();
    yArray = ((Matrix) mr.get("yArray")).flatten();
    zArray = ((Matrix) mr.get("zArray")).flatten();
    energyBounds = ((Matrix) mr.get("energyBounds")).flatten();
    vData = (Matrix) mr.get("data");

    xMax = xArray[xArray.length - 1];
    yMax = yArray[yArray.length - 1];
    zMin = zArray[0];
    zMax = zArray[zArray.length - 1];

    ebins = energyBounds.length - 1;
  }

  public boolean check()
  {
    // ensure data sizes match up:
    return (vData.rows() * vData.columns() == xArray.length * yArray.length * zArray.length * ebins * 4);
  }

  public int[] injectPointSource(double x, double y, double z, double[] sourceSpectrum)
  {
    /*
     * Return number of counts recorded in each panel from a source with spectrum sourceSpectrum
     * at specified location.
     */
    List<double[]> detectionProbabilities = getDetectionProbabilities(x, y, z);
    int[] counts = new int[4];

    for (int i = 0; i < 4; i++)
    {
      double sum = 0;
      for (int j = 0; j < 4; j++)
      {
        sum += detectionProbabilities.get(i)[j] * sourceSpectrum[j];
      }
      counts[i] = (int) Math.round(sum);
    }
    return counts;
  }

  public List<double[]> getDetectionProbabilities(double x, double y, double z)
  {
    try
    {
      /*
       * Finds the probability that a neutron emitted at the given location will be detected by
       * each panel. Returns a list of four double[] corresponding to the different panels.
       * The contents of each array correspond to probabilities wrt energy.
       *
       * If the requested x,y,z location is outside the known area, use the nearest value (on the boundary)
       */
      x = Math.abs(x);
      if (x > xMax)
      {
        // FIXME: warn about being outside known region?
        x = xMax;
      }

      boolean mirror = (y < 0);
      y = Math.abs(y);
      if (y > yMax)
      {
        y = yMax;
      }

      if (z < zMin || z > zMax)
      {
        z = Math.min(z, zMax);
        z = Math.max(z, zMin);
      }

      interpResults xI, yI, zI;
      xI = interpolationHelper(xArray, x);
      yI = interpolationHelper(yArray, y);
      zI = interpolationHelper(zArray, z);

      double f1, f2, f3, f4, f5, f6, f7, f8;
      Matrix r1, r2, r3, r4, r5, r6, r7, r8;

      f1 = xI.dhi * yI.dhi * zI.dlo;
      f2 = xI.dhi * yI.dlo * zI.dlo;
      f3 = xI.dlo * yI.dhi * zI.dlo;
      f4 = xI.dlo * yI.dlo * zI.dlo;
      f5 = xI.dhi * yI.dhi * zI.dhi;
      f6 = xI.dhi * yI.dlo * zI.dhi;
      f7 = xI.dlo * yI.dhi * zI.dhi;
      f8 = xI.dlo * yI.dlo * zI.dhi;

      //return arrays:
      int start;
      start = computeIndex(xI.ilo, yI.ilo, zI.ihi);
      r1 = MatrixViews.selectRowRange(vData, start, start + ebins).copyOf();
      start = computeIndex(xI.ilo, yI.ihi, zI.ihi);
      r2 = MatrixViews.selectRowRange(vData, start, start + ebins).copyOf();
      start = computeIndex(xI.ihi, yI.ilo, zI.ihi);
      r3 = MatrixViews.selectRowRange(vData, start, start + ebins).copyOf();
      start = computeIndex(xI.ihi, yI.ihi, zI.ihi);
      r4 = MatrixViews.selectRowRange(vData, start, start + ebins).copyOf();
      start = computeIndex(xI.ilo, yI.ilo, zI.ilo);
      r5 = MatrixViews.selectRowRange(vData, start, start + ebins).copyOf();
      start = computeIndex(xI.ilo, yI.ihi, zI.ilo);
      r6 = MatrixViews.selectRowRange(vData, start, start + ebins).copyOf();
      start = computeIndex(xI.ihi, yI.ilo, zI.ilo);
      r7 = MatrixViews.selectRowRange(vData, start, start + ebins).copyOf();
      start = computeIndex(xI.ihi, yI.ihi, zI.ilo);
      r8 = MatrixViews.selectRowRange(vData, start, start + ebins).copyOf();

      Matrix result = MatrixFactory.newMatrix(r1.rows(), r1.columns());
      try
      {
        MatrixOps.addAssignScaled(result, r1, f1);
        MatrixOps.addAssignScaled(result, r2, f2);
        MatrixOps.addAssignScaled(result, r3, f3);
        MatrixOps.addAssignScaled(result, r4, f4);
        MatrixOps.addAssignScaled(result, r5, f5);
        MatrixOps.addAssignScaled(result, r6, f6);
        MatrixOps.addAssignScaled(result, r7, f7);
        MatrixOps.addAssignScaled(result, r8, f8);
        MatrixOps.divideAssign(result, xI.width * yI.width * zI.width);
      }
      catch (SizeException ex)
      {
        System.out.println("Encountered array size mismatch!");
      }

      List<double[]> result2 = new ArrayList<>();
      if (mirror)
      {
        result2.add(result.copyColumnTo(new double[result.rows()], 0, 2));
        result2.add(result.copyColumnTo(new double[result.rows()], 0, 3));
        result2.add(result.copyColumnTo(new double[result.rows()], 0, 0));
        result2.add(result.copyColumnTo(new double[result.rows()], 0, 1));
      }
      else
      {
        result2.add(result.copyColumnTo(new double[result.rows()], 0, 0));
        result2.add(result.copyColumnTo(new double[result.rows()], 0, 1));
        result2.add(result.copyColumnTo(new double[result.rows()], 0, 2));
        result2.add(result.copyColumnTo(new double[result.rows()], 0, 3));
      }

      return result2;
    }
    catch (SizeException ex)
    {
      throw new RuntimeException(ex);
    }
  }

  public int computeIndex(int x, int y, int z)
  {
    /*
     * vData was originally a 5-dimensional array that was resized down to 2-d.
     * From x,y,z get the corresponding start index in the flattened array.
     * Note: z is fastest-varying, followed by y, followed by x. Ignore panel number:
     * DoubleMatrix.selectR( rowStart, rowEnd ) takes care of that for us.
     */
    return ebins * (z + y * zArray.length + x * zArray.length * yArray.length);
  }

  private class interpResults
  {
    int ilo, ihi;
    double dlo, dhi, width;

    public interpResults(int i1, int i2, double d1, double d2, double d3)
    {
      ilo = i1;
      ihi = i2;
      dlo = d1;
      dhi = d2;
      width = d3;
    }
  }

  private interpResults interpolationHelper(double[] array, double value)
  {
    int idx;
    for (idx = 1; idx < array.length; idx++)
    { // find location in the array
      if (value <= array[idx])
      {
        break;
      }
    }
    interpResults IR = new interpResults(idx - 1, idx, value - array[idx - 1], array[idx] - value, array[idx] - array[idx - 1]);
    return IR;
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