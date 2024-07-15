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

import gov.llnl.ernie.data.Record;
import gov.llnl.math.matrix.Matrix;
import gov.llnl.utility.xml.bind.ReaderInfo;

/**
 * Interface used to pull a background model which is
 * normalized to the length of the vehicle.
 *
 * @author mattoon1
 */
@ReaderInfo(UniformGammaExtractorReader.class)
public interface UniformGammaExtractor
{
  /**
   *
   * @param record
   * @return
   */
  public Matrix extract(Record record);

  /**
   *
   * @param record
   * @param frontPad
   * @param backPad
   * @return
   */
  public Matrix extract(Record record, int frontPad, int backPad);

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