/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.math.internal.matrix;

import gov.llnl.math.MathPackage;
import gov.llnl.math.matrix.Matrix;
import gov.llnl.utility.ArrayEncoding;
import gov.llnl.utility.annotation.Internal;
import gov.llnl.utility.io.WriterException;
import gov.llnl.utility.xml.bind.ObjectWriter;

/**
 * Writer for storing a Matrix in xml document.
 *
 * @author nelson85
 */
@Internal
public class MatrixWriter extends ObjectWriter<Matrix>
{
  public MatrixWriter()
  {
    super(Options.NONE, "matrix", MathPackage.getInstance());
  }

  @Override
  public void attributes(WriterAttributes attributes, Matrix object) throws WriterException
  {
    attributes.add("major", "column");
    attributes.add("rows", object.rows());
    attributes.add("columns", object.columns());
  }

  @Override
  public void contents(Matrix object) throws WriterException
  {
    // FIXME, we should have special handling for sparse matrix here.
    this.addContents(ArrayEncoding.encodeDoubles(object.flatten()));
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