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

import gov.llnl.utility.annotation.Debug;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.LinkedList;

/**
 * Extract a table from a TablesReader. This supports converting to primitive
 * types for Matlab plotting.
 *
 * @author nelson85
 * @param <Type> is the type of the TablesReader we are processing.
 */
public class TableExtractor<Type extends TablesReader<? extends TablesReader.Record>>
{
  Type tr;
  ArrayList<TablesReader.Field> fields = new ArrayList<>();
  @Debug public Object[][] data;

  /**
   * Construct a TableExtractor for a TablesReader.
   *
   * @param <Reader>
   * @param reader
   * @return a new table extractor.
   */
  static public <Reader extends TablesReader<? extends TablesReader.Record>>
          TableExtractor<Reader> create(Reader reader)
  {
    return new TableExtractor(reader);
  }

  private TableExtractor(Type tr)
  {
    this.tr = tr;
  }

  /**
   * Select a set of columns to extract.
   *
   * @param col is an array of columns we want to fetch.
   */
  public void selectFields(int[] col)
  {
    data = null;
    fields.clear();
    for (int i : col)
    {
      TablesReader.Field field = tr.getField(i);
      fields.add(field);
    }
  }

  /**
   * Select a set of columns by the header key.
   *
   * @param names
   */
  public void select(String... names)
  {
    data = null;
    fields.clear();
    for (String name : names)
    {
      TablesReader.Field field = tr.findField(name);
      if (field == null)
        throw new RuntimeException("Unable to find field with key " + name);
      fields.add(field);
    }
  }

  /**
   * Fetch all of the data in the table.
   */
  public void fetchAll()
  {
    fetch(-1);
  }

  /**
   * Fetch a block of data from the TablesReader. The number fetched may be less
   * than requested.
   *
   * @param max is the maximum rows to fetch in this block.
   */
  public void fetch(int max)
  {
    LinkedList<Object[]> out = new LinkedList<>();
    if (max < 0)
      max = Integer.MAX_VALUE;
    int fetched = 0;
    for (int i = 0; i < max; ++i)
    {
      if (!tr.hasNext())
        break;
      TablesReader.Record rec = tr.next();
      Object[] values = new Object[fields.size()];
      int j = 0;
      for (TablesReader.Field field : fields)
      {
        values[j++] = rec.get(field);
      }
      out.add(values);
      fetched++;
    }

    data = new Object[fetched][];
    int k = 0;
    for (Object[] row : out)
    {
      data[k++] = row;
    }
  }

  /**
   * Get the number of rows in the extractor.
   *
   * @return the number of rows in the extractor.
   */
  public int rows()
  {
    return data.length;
  }

  /**
   * Get the number of columns in the extractor.
   *
   * @return the number of columns.
   */
  public int columns()
  {
    return fields.size();
  }

  /**
   * Fetch a row. Rows are of mixed type.
   *
   * @param r
   * @return an array of objects on that row.
   */
  public Object[] getRow(int r)
  {
    return data[r];
  }

  /**
   * Get the contents of the column. Will preform all necessary conversions to
   * make it the native type.
   *
   * @param c is the column number to fetch.
   * @return an array with the specified data type for a column.
   */
  public Object getColumn(int c) throws IndexOutOfBoundsException
  {
    if (data == null)
      throw new RuntimeException("No data has been fetched");
    if (c >= fields.size())
      throw new IndexOutOfBoundsException("Requested field ");

    Class cls = fields.get(c).getType();
    int n = data.length;

    // Special cases for specific primitive types
    if (cls == Double.TYPE)
    {
      double[] out = new double[rows()];
      for (int j = 0; j < n; j++)
      {
        out[j] = (double) data[j][c];
      }
      return out;
    }

    if (cls == Integer.TYPE)
    {
      int[] out = new int[rows()];
      for (int j = 0; j < n; j++)
      {
        out[j] = (int) data[j][c];
      }
      return out;
    }

    if (cls == Long.TYPE)
    {
      long[] out = new long[rows()];
      for (int j = 0; j < n; j++)
      {
        out[j] = (long) data[j][c];
      }
      return out;
    }

    // Fallback objects using reflection Array
    Object out = Array.newInstance(cls, data.length);
    for (int j = 0; j < n; j++)
    {
      Array.set(out, j, cls.cast(data[j][c]));
    }
    return out;
  }

  /**
   * Get the underlying data from the last call to fetch.
   *
   * @return a two dimensional array of objects.
   */
  public Object[][] getData()
  {
    return data;
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