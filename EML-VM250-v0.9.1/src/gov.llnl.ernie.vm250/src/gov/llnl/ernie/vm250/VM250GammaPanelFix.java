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
import gov.llnl.ernie.analysis.AnalysisException;
import gov.llnl.ernie.data.Record;
import gov.llnl.ernie.analysis.AnalysisPreprocessor;
import gov.llnl.ernie.data.SensorMeasurement;
import gov.llnl.ernie.vm250.data.VM250Record;
import gov.llnl.ernie.vm250.data.VM250Record.Fault;
import gov.llnl.ernie.vm250.data.VM250RecordInternal;
import gov.llnl.ernie.vm250.data.VM250RecordInternal.SegmentDescription;
import gov.llnl.utility.xml.bind.Reader;
import java.time.Instant;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.logging.Level;

/**
 *
 * @author mattoon1
 */
@Reader.Declaration(pkg = ErnieVM250Package.class, name = "gammaPanelFix",
        referenceable = true)
public class VM250GammaPanelFix implements AnalysisPreprocessor
{
  /**
   * Examine a record checking for data missing from one or more gamma panels
   *
   * This algorithm will correct up to two missing gamma panels (fills in with
   * rescaled information from other panels). Three or more missing panels means
   * the record is bad.
   *
   * @param record
   */

  private double TTBR = 1.0; // top-to-bottom ratio

  @Override
  public void compute(Record record)
  {
    VM250Record recordImpl = (VM250Record) record;
    VM250RecordInternal recordInternal = (VM250RecordInternal) record.getInternal();

    int nPanels = recordInternal.getNPanels();
    if (nPanels != 4)
    {
      record.addFault(Fault.INCORRECT_NUM_PANELS);
      return;
    }

    // first determine which (if any) panels are bad:
    Boolean[] badPanels = new Boolean[nPanels];
    for (int idx = 0; idx < nPanels; idx++)
    {
      badPanels[idx] = false;
      SensorMeasurement gammas = record.getGammaMeasurements().get(idx);
      int[] gammaData = gammas.getCountsRange(0, gammas.size());

      boolean zero = false;
      for (int jdx = 0; jdx < gammaData.length; jdx++)
      {
        if (gammaData[jdx] == 0)
        {
          if (zero) // two zeros in a row
          {
            badPanels[idx] = true;
            break;
          }
          else
          {
            zero = true;
          }
        }
        else
        {
          zero = false;
        }
      }
    }

    long badCount = Arrays.stream(badPanels).filter(c -> c == true).count();
    if (badCount == 0) // data is fine
    {
      return;
    }
    else if (badCount >= 3)
    {
      record.addFault(Fault.BAD_GAMMAS);
      ErnieVM250Package.LOGGER.log(Level.WARNING,
              "Too many gamma panels missing data");
      return;
    }

    // now determine which panel(s) are bad and how to replace them
    // 0 = bottom right, 1 = bottom left, 2 = top right, 3 = top left
    SegmentDescription description = recordInternal.getSegmentDescription();
    double[] background = description.getGammaBackground();

    if (badCount == 1)
    {
      int badPanel = -1;
      for (int idx = 0; idx < badPanels.length; idx++)
      {
        if (badPanels[idx])
        {
          badPanel = idx;
        }
      }

      int[][] badPanelGammaData = recordInternal.getPanelData()[badPanel].gammaData;

      if (badPanel == 0)
      {
        background[0] = (background[1] + background[2] / TTBR) / 2;
        replaceOnePanel(record.getGammaMeasurements().get(1), record.getGammaMeasurements().get(2), 1. / TTBR, 1, badPanelGammaData);
      }
      else if (badPanel == 1)
      {
        background[1] = (background[0] + background[3] / TTBR) / 2;
        replaceOnePanel(record.getGammaMeasurements().get(0), record.getGammaMeasurements().get(3), 1. / TTBR, 1, badPanelGammaData);
      }
      else if (badPanel == 2)
      {
        background[2] = (background[3] + background[0] * TTBR) / 2;
        replaceOnePanel(record.getGammaMeasurements().get(3), record.getGammaMeasurements().get(0), TTBR, 1, badPanelGammaData);
      }
      else
      { // badPanel == 3
        background[3] = (background[2] + background[1] * TTBR) / 2;
        replaceOnePanel(record.getGammaMeasurements().get(2), record.getGammaMeasurements().get(1), TTBR, 1, badPanelGammaData);
      }
    }
    else // 2 bad panels
    {
      if (badPanels[0] && badPanels[1]) // both bottom
      {
        background[0] = background[1] = (background[2] + background[3]) / TTBR / 2;
        replaceOnePanel(record.getGammaMeasurements().get(2), record.getGammaMeasurements().get(3),
                1, 1 / TTBR, recordInternal.getPanelData()[0].gammaData);
        replaceOnePanel(record.getGammaMeasurements().get(2), record.getGammaMeasurements().get(3),
                1, 1 / TTBR, recordInternal.getPanelData()[1].gammaData);
      }
      else if (badPanels[2] && badPanels[3]) // both top
      {
        background[2] = background[3] = (background[1] + background[2]) * TTBR / 2;
        replaceOnePanel(record.getGammaMeasurements().get(0), record.getGammaMeasurements().get(1),
                1, TTBR, recordInternal.getPanelData()[2].gammaData);
        replaceOnePanel(record.getGammaMeasurements().get(0), record.getGammaMeasurements().get(1),
                1, TTBR, recordInternal.getPanelData()[3].gammaData);
      }
      else if (badPanels[0] && badPanels[2]) // right side
      {
        background[0] = (background[1] + background[3] / TTBR) / 2;
        replaceOnePanel(record.getGammaMeasurements().get(1), record.getGammaMeasurements().get(3),
                1 / TTBR, 1, recordInternal.getPanelData()[0].gammaData);

        background[2] = (background[3] + background[1] * TTBR) / 2;
        replaceOnePanel(record.getGammaMeasurements().get(3), record.getGammaMeasurements().get(1),
                TTBR, 1, recordInternal.getPanelData()[2].gammaData);
      }
      else if (badPanels[1] && badPanels[3]) // left side
      {
        background[1] = (background[0] + background[2] / TTBR) / 2;
        replaceOnePanel(record.getGammaMeasurements().get(0), record.getGammaMeasurements().get(2),
                1 / TTBR, 1, recordInternal.getPanelData()[1].gammaData);

        background[3] = (background[2] + background[0] * TTBR) / 2;
        replaceOnePanel(record.getGammaMeasurements().get(2), record.getGammaMeasurements().get(0),
                TTBR, 1, recordInternal.getPanelData()[3].gammaData);
      }
      else
      {
        record.addFault(Fault.BAD_GAMMAS);
        ErnieVM250Package.LOGGER.log(Level.WARNING,
                "Can't recover from diagonal bad gamma panels (BR + TL or BL + TR)");
        return;
      }
    }

    // gamma panel data was replaced in-place, now also replace gamma background:
    int[] gammaBackground = new int[background.length];
    for (int idx = 0; idx < background.length; idx++)
    {
      gammaBackground[idx] = (int) Math.round(background[idx]);
    }
    recordInternal.setSegmentDescription(description.getDataSourceId(), description.getRpmId(),
            description.getRpmDateTime(), description.isContinuation(), gammaBackground,
            description.getNeutronBackground(), description.isAnyGammaAlarm(),
            description.isAnyNeutronAlarm(), description.isRealOccupancy());

    record.addFault(Fault.MISSING_GAMMA_PANELS);
  }

  @Override
  public void initialize(Analysis par0)
  {
    // not used
  }

  /**
   * @param TTBR the Top-to-bottom ratio to set
   */
  @Reader.Attribute(name = "ttbr", type = double.class, required = false)
  public void setTTBR(double TTBR)
  {
    this.TTBR = TTBR;
  }

  /*
  * helper function to replace single missing panel.
  * New data filled in as (panelA + panelB * coef1)*coef2/2
   */
  void replaceOnePanel(SensorMeasurement panelA, SensorMeasurement panelB,
          double coef1, double coef2, int[][] destination)
  {
    int[] gammasA = panelA.getCountsRange(0, panelA.size());
    int[] gammasB = panelB.getCountsRange(0, panelB.size());
    for (int idx = 0; idx < destination.length; idx++)
    {
      destination[idx][0] = (int) Math.round((gammasA[idx] + gammasB[idx] * coef1) * coef2 / 2);
    }
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