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

import gov.llnl.ernie.ErniePackage;
import gov.llnl.math.IntegerArray;
import gov.llnl.math.matrix.Matrix;
import gov.llnl.math.matrix.MatrixColumnTable;
import gov.llnl.math.matrix.MatrixFactory;
import gov.llnl.math.matrix.MatrixViews;
import java.util.List;
import gov.llnl.ernie.data.Record;
import gov.llnl.ernie.data.SensorMeasurement;
import java.util.logging.Level;

/**
 * This code has been verified against Matlab
 *
 * @author duenoortiz1
 */
public class ExtractUtilities
{

  /**
   * Converts a list of panel data into a Matrix with gross counts for each
   * panel.
   *
   * @param matrix
   * @param panels
   * @param begin
   * @param end
   * @return
   * @throws Exception
   */
  static public Matrix extractPanels(Matrix matrix, List<SensorMeasurement> panels, int begin, int end) throws Exception
  {
    int length = end - begin;
    matrix.resize(length, panels.size());
    for (int i0 = 0; i0 < panels.size(); ++i0)
    {
      SensorMeasurement data = panels.get(i0);
      MatrixViews.selectColumn(matrix, i0).assign(
              MatrixFactory.wrapColumnVector(
                      IntegerArray.promoteToDoubles(data.getCountsRange(begin, end)))
      );
    }
    return matrix;

  }

  static public Matrix extractPanels(Record record)
  {
    try
    {
      int begin = 0;
      int end = record.getGammaMeasurements().get(0).size();
      return extractPanels(new MatrixColumnTable(), record.getGammaMeasurements(), begin, end);
    }
    catch (Exception ex)
    {
      ErniePackage.getInstance().getLogger().log(Level.WARNING, "Error in extractPanels", ex);
      throw new RuntimeException(ex);
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