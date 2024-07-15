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
import gov.llnl.ernie.data.SensorPosition;
import gov.llnl.ernie.data.SensorMeasurement;
import gov.llnl.ernie.vm250.data.VM250Record;
import gov.llnl.ernie.vm250.data.VM250Record.Fault;
import gov.llnl.ernie.vm250.data.VM250RecordInternal;
import gov.llnl.utility.xml.bind.Reader;
import java.util.EnumSet;

/**
 *
 * @author nelson85
 */
@Reader.Declaration(pkg = ErnieVM250Package.class, name = "qualityCheck",
        referenceable = true)
public class VM250QualityCheck implements AnalysisPreprocessor
{
  /**
   * Examine a record checking for common problems with the VPS that would
   * prohibit further analysis.
   *
   * This algorithm will attempt to correct a number of these issues where
   * possible such as removing a vehicle in the presamples.
   *
   * @param record
   */
  @Override
  public void compute(Record record)
  {
    VM250Record recordImpl = (VM250Record) record;
    VM250RecordInternal recordInternal = (VM250RecordInternal) record.getInternal();

    if (record.getVehicleMotion() != null)
    {
      return;
    }
    
    SensorMeasurement combo = record.getCombinedGammaMeasurement(SensorPosition.ALL);
    if (combo.size() == 0)
    {
      record.addFault(Fault.BAD_GAMMAS);
    }
    if (combo.size() < 5)
    {
      record.addFault(Fault.BAD_LENGTH);
    }
    for (int idx = 0; idx < recordInternal.getNPanels(); idx++)
    {
      if (recordInternal.getSegmentDescription().getGammaBackground()[idx] == -1)
      {
        record.addFault(Fault.MISSING_GAMMA_BACKGROUND);
      }
    }

    recordImpl.setMotion(null);
    recordImpl.vehicleClass = null;

    if (record.getInternal() == null
            || recordImpl.getInternal().isGammaHighBackground()
            || recordImpl.getInternal().isGammaLowBackground()
            || recordImpl.getInternal().isNeutronHighBackground()
            || record.getLane() == null
            || record.getVPSMeasurement().getGammaOccupancyStart() == 0
            || (record.getVPSMeasurement().getGammaOccupancyEnd()
               <= record.getVPSMeasurement().getGammaOccupancyStart())
            || (record.getCombinedGammaMeasurement(SensorPosition.ALL).size()
               - record.getVPSMeasurement().getGammaOccupancyEnd() <= 0))
    {
      record.addFault(Fault.BAD_DATA);
    }
  }

  @Override
  public void initialize(Analysis par0)
  {
    // not used
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