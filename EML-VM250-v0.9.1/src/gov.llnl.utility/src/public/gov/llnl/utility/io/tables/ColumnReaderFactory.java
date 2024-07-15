/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.utility.io.tables;

import gov.llnl.utility.io.tables.ColumnReader.Field;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 *
 * @author seilhan3
 */
public class ColumnReaderFactory implements TablesReaderFactory<ColumnReader>
{
  private boolean hasHeader = false;
  private int skipRowCount = 0;
  private Pattern commentPattern = null;
  ArrayList<Field> fields = new ArrayList<>();

  @Override
  public void setHasHeader(boolean b)
  {
    this.hasHeader = b;
  }

  @Override
  public void setSkipRowCount(int count)
  {
    this.skipRowCount = count;
  }

  @Override
  public void setCommentPattern(String pattern)
  {
    this.commentPattern = Pattern.compile(pattern);
  }

  @Override
  public ColumnReader openFile(Path path) throws IOException
  {
    return openStream(Files.newInputStream(path));
  }

  @Override
  public ColumnReader openStream(InputStream is) throws IOException
  {
    // turn io stream into buffered reader
    BufferedReader br = new BufferedReader(new InputStreamReader(is));

    // Skip comment lines at start
    if (this.skipRowCount != 0)
    {
      for (int i = 0; i < this.skipRowCount; ++i)
      {
        br.readLine();
      }
    }

    // Deal with header row
    if (this.hasHeader)
    {
      // Copy the header names in if they are not already set
      String header = br.readLine();
      for (Field field : this.fields)
      {
        if (field.key != null)
          continue;
        field.key = header.substring(field.beginIndex, field.endIndex).trim();
      }
    }

    // Fine the first valid line
    String line = br.readLine();
    if (commentPattern != null)
    {
      while (line != null && commentPattern.matcher(line).lookingAt() == true)
        line = br.readLine();
    }

    // pack column reader with fields
    // setup reader map
    ColumnReader cr = new ColumnReader();
    cr.fields = this.fields.toArray(new Field[0]);
    cr.br = br;
    cr.line = line;
    cr.commentPattern = this.commentPattern;
    for (Field field : fields)
    {
      if (field.key != null)
        cr.headerMap.put(field.key, field);
    }
    clear();

    return cr;
  }

  /**
   *
   * @param begin
   * @param end
   */
  public Field addField(int begin, int end)
  {
    Field out = new Field(begin, end);
    fields.add(out);
    return out;
  }

  private void clear()
  {
    this.commentPattern = null;
    this.fields.clear();
    this.skipRowCount = 0;
    this.hasHeader = false;
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