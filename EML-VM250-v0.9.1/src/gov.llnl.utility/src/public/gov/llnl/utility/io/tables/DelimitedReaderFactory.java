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

import gov.llnl.utility.PathUtilities;
import gov.llnl.utility.io.tables.DelimitedReader.Field;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

/**
 *
 * @author nelson85
 */
public class DelimitedReaderFactory implements TablesReaderFactory<DelimitedReader>
{
  private boolean automaticFields = false;
  private boolean hasHeader = false;
  private int skipRowCount = 0;
  private Pattern commentPattern = null;
  private int columns = 0;
  private ArrayList<Field> fields = new ArrayList<>();
  private DelimitedReader.DelimitedLineSplitter splitter = null;

  /**
   * Use Headers as field names
   *
   * @param b
   */
  public void setAutomaticFields(boolean b)
  {
    this.automaticFields = b;
    if (this.automaticFields == true)
    {
      this.hasHeader = true;
    }
  }

  /**
   * define the delimiter between fields, can be multi character
   *
   * @param delimiter
   */
  public void setDelimiter(String delimiter)
  {
    this.splitter = new DelimitedReader.DefaultLineSplitter(delimiter);
  }

  public void setExcelSplitter()
  {
    this.splitter = new DelimitedReader.ExcelLineSplitter();
  }

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
  public DelimitedReader openFile(Path path) throws FileNotFoundException, IOException
  {
    if (PathUtilities.isGzip(path))
    {
      InputStream is = Files.newInputStream(path);
      GZIPInputStream gis = new GZIPInputStream(is);
      return openStream(gis);
    }
    return openStream(Files.newInputStream(path));
  }

  @Override
  public DelimitedReader openStream(InputStream is) throws IOException
  {
    // Turn io stream into buffered reader
    BufferedReader br = new BufferedReader(new InputStreamReader(is));

    // Set up the reader
    DelimitedReader dr = new DelimitedReader();
    dr.splitter = this.splitter;  // we need this to parse the header
    dr.br = br;
    dr.commentPattern = this.commentPattern;  // this will be needed to find the valid line

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

      if (this.automaticFields)
      {
        int del = splitter.countDelimiters(header);
        this.columns = del + 1;
        dr.columns = columns;
        int[] marker = splitter.getMarkers(header, columns);
        fields.clear();
        for (int i = 0; i < columns; ++i)
        {
          this.addField(i).key(header.substring(marker[2 * i], marker[2 * i + 1]).trim());
        }

      }
      else
      {
        int[] marker = splitter.getMarkers(header, columns);
        for (int i = 0; i < columns; ++i)
        {
          Field field = fields.get(i);
          if (field.key == null)
            field.key = header.substring(marker[2 * i], marker[2 * i + 1]).trim();
        }
      }
    }
    dr.line = dr.getNextLine();

    // pack column reader with fields
    // setup reader map
    dr.fields = this.fields.toArray(new Field[0]);
    for (Field field : fields)
    {
      if (field.key != null)
        dr.headerMap.put(field.key, field);
    }
    clear();
    if (!this.hasHeader)
    {
      this.columns = dr.headerMap.values().stream().map(field -> field.column).max(Comparator.naturalOrder()).get() + 1;
      dr.columns = this.columns;
    }
    return dr;
  }

  /**
   * crate field
   *
   * @param column
   * @return newly created field
   */
  public Field addField(int column)
  {
    this.columns = Math.max(columns, column);
    Field out = new Field(column);
    fields.add(out);
    return out;
  }

  private void clear()
  {
    this.columns = 0;
    this.commentPattern = null;
    this.splitter = null;
    this.fields.clear();
    this.hasHeader = false;
    this.skipRowCount = 0;
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