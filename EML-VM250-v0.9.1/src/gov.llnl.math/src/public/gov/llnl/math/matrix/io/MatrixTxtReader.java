/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.math.matrix.io;

import gov.llnl.math.MathExceptions.ResizeException;
import gov.llnl.math.matrix.Matrix;
import gov.llnl.math.matrix.MatrixColumnArray;
import gov.llnl.utility.annotation.Matlab;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * FIXME this code is currently dead.  We do not use direct text for 
 * storing matrices at this time.
 * 
 * @author nelson85
 */
public class MatrixTxtReader
{
  static void logException(Exception ex)
  {
    Logger.getLogger(MatrixTxtReader.class.getName()).log(Level.SEVERE, null, ex);
  }

  // Test code for matlab
  @Matlab static public Matrix load(String filename) throws IOException
  {
    Matrix matrix = new MatrixColumnArray();
    load(matrix, filename);
    return matrix;
  }

  static public void load(Matrix matrix, String filename) throws IOException
  {
    try (FileReader file = new FileReader(filename))
    {
      LineNumberReader br = new LineNumberReader(file);
      load(matrix, br);
    }
    catch (ResizeException ex)
    {
      logException(ex);
    }
  }

  public static void load(Matrix matrix, LineNumberReader br) throws IOException, ResizeException
  {
    // Define a pattern for numbers
    Pattern pattern = Pattern.compile("[-+]?\\d*\\.?\\d+([eE][-+]?\\d+)?");
    String line;
    line = br.readLine();
    int rows, cols;
    {
      Matcher matcher = pattern.matcher(line);
      matcher.find();
      rows = Integer.parseInt(matcher.group());
      matcher.find();
      cols = Integer.parseInt(matcher.group());
    }
    matrix.resize(rows, cols);

    int i = 0;
    int j = 0;
    int k = 0;
    while ((line = br.readLine()) != null)
    {
      Matcher matcher = pattern.matcher(line);
      while (matcher.find())
      {
        matrix.set(i, j, Double.parseDouble(matcher.group()));
        i++;
        k++;
        if (i >= rows * cols)
          break;
        if (i >= rows)
        {
          i = 0;
          j++;
        }
      }
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