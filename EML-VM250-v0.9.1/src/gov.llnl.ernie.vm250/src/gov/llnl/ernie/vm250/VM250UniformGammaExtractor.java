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

import gov.llnl.ernie.common.UniformGammaExtractor;
import gov.llnl.ernie.data.Record;
import gov.llnl.ernie.vm250.data.VM250RecordInternal;
import gov.llnl.math.RebinUtilities;
import gov.llnl.math.RebinUtilities.RebinException;
import gov.llnl.math.matrix.Matrix;
import gov.llnl.math.matrix.MatrixColumnArray;
import gov.llnl.math.matrix.MatrixFactory;
import gov.llnl.math.matrix.MatrixOps;
import gov.llnl.math.matrix.MatrixViews;
import gov.llnl.utility.xml.bind.Reader;

/**
 *
 * @author mattoon1
 */
@Reader.Declaration(pkg = ErnieVM250Package.class, name = "uniformGammaExtractor", 
        referenceable=true)
public class VM250UniformGammaExtractor implements UniformGammaExtractor
{
  public int TARGET_LENGTH = 128;
  public static final int DEFAULT_FRONT_PAD = 0;
  public static final int DEFAULT_BACK_PAD = 0;

  public Matrix extract(Matrix out, Record record, int frontPad, int backPad)
  {
    
    Matrix sampleData = ExtractUtilities.extractPanels(record);

    //FIXME compare RPM-provided background to presamples? Only have 1 second of presample...
    VM250RecordInternal internalRecord = (VM250RecordInternal)record.getInternal();
    Matrix meanBkg = MatrixFactory.wrapRowVector(internalRecord.getSegmentDescription().getGammaBackground());
    
    for (int i0 = 0; i0 < sampleData.columns(); ++i0)
    {
      for (int i1 = 0; i1 < sampleData.rows(); ++i1)
      {
        sampleData.set(i1, i0, sampleData.get(i1, i0) - meanBkg.get(0, i0));
      }
    }

    int[] limits = new int[] {0, sampleData.rows()};
    
    Matrix vehicle = MatrixViews.selectRowRange(sampleData, limits[0], limits[1]);
    try
    {
      out = RebinUtilities.rescale(vehicle, TARGET_LENGTH);
    }
    catch (RebinException ex)
    {
      throw new RuntimeException(ex);
    }
    MatrixOps.multiplyAssign(out, (double) TARGET_LENGTH / vehicle.rows());

    return out;
  }
  
  @Override
  public Matrix extract(Record record, int frontPad, int backPad)
  {
    return extract(new MatrixColumnArray(), record, frontPad, backPad);
  }
  
  @Override
  public Matrix extract(Record record)
  {
    return extract(new MatrixColumnArray(), record, DEFAULT_FRONT_PAD, DEFAULT_BACK_PAD);
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