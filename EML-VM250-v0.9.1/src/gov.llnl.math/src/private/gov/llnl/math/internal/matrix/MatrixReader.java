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
import gov.llnl.math.matrix.MatrixColumnArray;
import gov.llnl.utility.ArrayEncoding;
import gov.llnl.utility.annotation.Internal;
import gov.llnl.utility.io.ReaderException;
import gov.llnl.utility.xml.bind.ObjectReader;
import gov.llnl.utility.xml.bind.ObjectStringReader;
import gov.llnl.utility.xml.bind.Reader;
import java.text.ParseException;
import org.xml.sax.Attributes;

/**
 * Reader for Matrix.
 *
 * This currently produces array type matrices and does not store sparse or
 * diagonal matrix in a compressed form. If a different memory layout is
 * required it should be cast when the load is complete.
 *
 * TODO, it should be possible to load a specific memory layout by using a
 * requested type. But this will require a delegant pattern that copies the
 * memory into the desired shape.
 *
 * @author nelson85
 */
@Internal

@Reader.Declaration(pkg = MathPackage.class, name = "matrix",
        cls = Matrix.class,
        document = true, referenceable = true,
        contents = Reader.Contents.TEXT)
@Reader.Attribute(name = "major", type = String.class) // declare the order that elements appear in the file
@Reader.Attribute(name = "rows", type = Integer.class) // declare how many rows the data has
@Reader.Attribute(name = "columns", type = Integer.class) // declare how many columns the data has
public class MatrixReader extends ObjectReader<Matrix>
{
  String major = "row";
  int rows = 1;
  int columns = 1;
  double[] data;

  @Override
  public Matrix start(Attributes attributes) throws ReaderException
  {
    rows = 1;
    columns = 1;
    major = "row";

    if (attributes.getValue("rows") != null)
      rows = Integer.parseInt(attributes.getValue("rows"));
    if (attributes.getValue("columns") != null)
      columns = Integer.parseInt(attributes.getValue("columns"));
    if (attributes.getValue("major") != null)
      major = attributes.getValue("major");
    return null;
  }

  @Override
  public Matrix end() throws ReaderException
  {
    if (data.length != rows * columns)
      throw new ReaderException("size mismatch");
    if ("column".equals(major))
      return new MatrixColumnArray(data, rows, columns);
    if ("row".equals(major))
      return new MatrixColumnArray(data, rows, columns);
    throw new ReaderException("Unknown order");
  }

  @Override
  public Matrix contents(String textContents) throws ReaderException
  {
    try
    {
      data = ArrayEncoding.decodeDoubles(textContents);
      return null;
    }
    catch (ParseException ex)
    {
      throw new ReaderException("Error decoding double[]\n " + textContents, ex);
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