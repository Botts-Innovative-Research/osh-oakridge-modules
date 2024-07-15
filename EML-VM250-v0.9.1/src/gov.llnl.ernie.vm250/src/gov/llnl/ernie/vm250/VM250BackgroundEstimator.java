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

import gov.llnl.ernie.Analysis;
import gov.llnl.ernie.vm250.data.VM250RecordInternal;
import gov.llnl.ernie.vm250.data.VM250Record;
import gov.llnl.ernie.analysis.AnalysisPreprocessor;
import gov.llnl.ernie.analysis.BackgroundEstimator;
import gov.llnl.ernie.data.SensorPosition;
import gov.llnl.ernie.data.Record;
import gov.llnl.ernie.data.SensorMeasurement;
import gov.llnl.math.DoubleArray;
import gov.llnl.math.MathExceptions;
import gov.llnl.math.RebinUtilities;
import gov.llnl.math.matrix.Matrix;
import gov.llnl.math.matrix.MatrixColumnTable;
import gov.llnl.math.matrix.MatrixFactory;
import gov.llnl.math.matrix.MatrixOps;
import gov.llnl.math.matrix.MatrixViews;
import gov.llnl.utility.InitializeException;
import gov.llnl.utility.xml.bind.Reader;
import java.util.List;

/**
 *
 * @author nelson85
 */
@Reader.Declaration(pkg = ErnieVM250Package.class, name = "backgroundEstimator",
        referenceable = true)
public class VM250BackgroundEstimator implements AnalysisPreprocessor, BackgroundEstimator
{

  private static final long serialVersionUID
          = gov.llnl.utility.UUIDUtilities.createLong("VM250PreprocessorBackground-v1");

  public static final int DEFAULT_FRONT_PAD = 20;
  public static final int DEFAULT_BACK_PAD = 20;

  @Override
  public void initialize(Analysis par0) throws InitializeException
  {
  }

  @Override
  public void compute(Record record)
  {
    VM250RecordInternal recordInternal = (VM250RecordInternal) record.getInternal();
    VM250Record recordTyped = (VM250Record) record;
    // Must have vehicle class to proceed
    if (recordTyped.getVehicleClass() == null)
    {
      return;
    }

    recordTyped.gammaBackgroundSpectrum = MatrixFactory.wrapColumns(new double[][]
    {
      extractBackgroundSpectrum(recordInternal, 0),
      extractBackgroundSpectrum(recordInternal, 1),
      extractBackgroundSpectrum(recordInternal, 2),
      extractBackgroundSpectrum(recordInternal, 3),
    });
    recordTyped.gammaBackgroundPanelRate = this.extractPanelRates(recordTyped);
    recordTyped.gammaBackgroundGross = this.computeBackground(recordTyped, 0); // FIXME magic number

    // If we don't have  background we cannot proceed
    if (recordTyped.gammaBackgroundGross == null)
    {
      record.addFault(VM250Record.Fault.BAD_BACKGROUND);
    }
  }

  /**
   * Compute the gross counts as a function of time for each panel.
   *
   * @param record
   * @return a matrix with size gamma samples by panels.
   */
  @Override
  public MatrixColumnTable computeDefaultBackground(Record record)
  {
    try
    {
      SensorMeasurement cgm = record.getCombinedGammaMeasurement(SensorPosition.ALL);
      List<SensorMeasurement> gm = record.getGammaMeasurements();
      int lengthTotal = cgm.size();
      MatrixColumnTable background = new MatrixColumnTable(lengthTotal, gm.size());
      double meanBkg[] = extractPanelRates(record);
      for (int i = 0; i < background.columns(); ++i)
      {
        MatrixOps.addAssign(MatrixViews.selectColumn(background, i), meanBkg[i]);
      }
      return background;
    }
    catch (MathExceptions.SizeException ex)
    {
      throw new RuntimeException(ex);
    }
  }

  /**
   * Estimate background suppression for a record using specified background
   * model
   *
   * @param record
   * @param backgroundModelIndex (0=average, 1=high, 2=low)
   * @return
   */
  @Override
  public MatrixColumnTable computeBackground(Record record, int backgroundModelIndex)
  {
    try
    {
      List<SensorMeasurement> gm = record.getGammaMeasurements();
      SensorMeasurement cgm = record.getCombinedGammaMeasurement(SensorPosition.ALL);

      int lengthTotal = cgm.size();

      //This code matches that in extract uniform gamma
      // FIXME consider making a common routine for this
      int start = record.getVPSMeasurement().getGammaOccupancyStart();
      int end = record.getVPSMeasurement().getGammaOccupancyEnd();
      int length = end - start;
      int beginningBkg = (int) (start
              - Math.round(DEFAULT_FRONT_PAD * (length) / (double) TARGET_LENGTH));

      int endingBkg = (int) (end
              + Math.round(DEFAULT_BACK_PAD * (length) / (double) TARGET_LENGTH));

      // in counts per sample
      double meanBkg[] = DoubleArray.multiplyAssign(extractPanelRates(record),
              record.getLane().getGammaSensorProperties().get(0).getSamplePeriodSeconds());

      MatrixColumnTable sampleData = new MatrixColumnTable(lengthTotal, gm.size());
      int vehicleLength = endingBkg - beginningBkg + 1;

      // Special logic for truncated vehicles
      int i1 = 0;
      int i2 = vehicleLength;
      if (beginningBkg < 1)
      {
        i1 = -beginningBkg + 1;
        beginningBkg = 1;
      }
      if (endingBkg > lengthTotal)
      {
        i2 = i2 - (endingBkg - lengthTotal);
        endingBkg = lengthTotal;
      }

      // Rebin vehicle to a common length 
      Matrix tmp = new MatrixColumnTable();
      Matrix backgroundTemplate = record.getVehicleClass().getBackgroundModels().get(backgroundModelIndex).toMatrix();

      RebinUtilities.execute(
              new RebinUtilities.MatrixOutputWrapper(tmp),
              new RebinUtilities.MatrixInputWrapper(backgroundTemplate),
              RebinUtilities.StepBinEdges.createLinear(0, 1, TARGET_LENGTH),
              RebinUtilities.StepBinEdges.createLinear(
                      ((double) i1) / vehicleLength,
                      ((double) i2) / vehicleLength,
                      (i2 - i1)));
      MatrixOps.multiplyAssign(tmp, (double) vehicleLength / (double) TARGET_LENGTH);
      MatrixViews.selectRowRange(sampleData, beginningBkg - 1, endingBkg).assign(tmp);
      MatrixOps.addAssign(sampleData, 1.0);
      MatrixOps.multiplyAssignColumns(sampleData, MatrixFactory.wrapRowVector(meanBkg));
      return sampleData;
    }
    catch (MathExceptions.SizeException | RebinUtilities.RebinException ex)
    {
      throw new RuntimeException(ex);
    }
  }

  // FIXME For some reason there are two different background estimators with very
  // different logic.
  /**
   * Extract the expected rate in each panel.
   *
   * @param record
   * @return
   */
  public double[] extractPanelRates(Record record)
  {
    int nPanels = record.getGammaMeasurements().size();
    double meanBkg1[] = new double[nPanels];
    for (int i = 0; i < nPanels; ++i)
    {
      meanBkg1[i] = ((VM250RecordInternal) record.getInternal())
              .getSegmentDescription().getGammaBackground()[i];
    }

    return meanBkg1;
  }

  static public double[] extractBackgroundSpectrum(VM250RecordInternal record, int panel)
  {
    double spectrum[] =
    {
      record.getSegmentDescription().getGammaBackground()[panel]
    };
    return spectrum;
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