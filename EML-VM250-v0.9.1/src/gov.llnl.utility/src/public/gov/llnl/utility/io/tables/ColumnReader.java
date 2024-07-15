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

import gov.llnl.utility.ClassUtilities.ValueOf;
import static gov.llnl.utility.ClassUtilities.newValueOf;
import gov.llnl.utility.io.tables.ColumnReader.ColumnRecord;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 *
 * @author seilhan3
 */
public class ColumnReader implements TablesReader<ColumnRecord>
{
  Field[] fields;
  TreeMap<String, Field> headerMap = new TreeMap<>();
  BufferedReader br;
  Pattern commentPattern;
  String line;

  @Override
  public void close() throws IOException
  {
    br.close();
  }

//<editor-fold desc="field" defaultstate="collapsed"> 
  static public class Field implements TablesReader.Field
  {
    String key;
    int beginIndex;
    int endIndex;
    Class type = String.class;
    ValueOf vo = newValueOf(String.class);

    Field(int begin, int end)
    {
      this.beginIndex = begin;
      this.endIndex = end;
    }

    public Field type(Class type)
            throws IllegalArgumentException
    {
      // Order maters for this operation
      this.vo = getValueOf(type);
      this.type = type;
      return this;
    }

    public Field key(String key)
    {
      this.key = key;
      return this;
    }

    private <Type> ValueOf getValueOf(Class<Type> type)
            throws IllegalArgumentException
    {
      if (type == this.type)
        return this.vo;
      ValueOf vo = newValueOf(type);
      if (vo == null)
        throw new IllegalArgumentException("No convertor for " + type.getSimpleName());
      return vo;
    }

    @Override
    public String getKey()
    {
      return this.key;
    }

    @Override
    public Class getType()
    {
      return this.type;
    }
  }

  public Field[] getFields()
  {
    return fields;
  }

  @Override
  public Field getField(int fieldNumber)
          throws IndexOutOfBoundsException
  {
    if (fieldNumber >= fields.length)
      throw new IndexOutOfBoundsException("Field out of bounds");
    return fields[fieldNumber];
  }

  @Override
  public Field findField(String headerKey)
  {
    return this.headerMap.get(headerKey);
  }

//</editor-fold>
//<editor-fold desc="iterator" defaultstate="collapsed">
  /**
   * Get next record from reader
   *
   * @return record or null in end of stream
   * @throws NoSuchElementException if there are no more elements or there was a
   * read error on fetching the next element.
   */
  @Override
  public ColumnRecord next() throws NoSuchElementException
  {
    // If there is no line, then we need to throw.  
    if (line == null)
      throw new NoSuchElementException();
    ColumnRecord out = new ColumnRecord(line);
    try
    {

      while (true)
      {
        line = br.readLine();

        // End of file
        if (line == null)
          break;

        // Skip comments
        if (commentPattern.matcher(line).lookingAt() == false)
          return out;
      }
    }
    catch (IOException ex)
    {
      throw (NoSuchElementException) new NoSuchElementException().initCause(ex);
    }
    return out;
  }

  @Override
  public boolean hasNext()
  {
    return line != null;
  }

  @Override
  public void remove()
  {
    throw new UnsupportedOperationException("Not supported.");
  }

//</editor-fold>
//<editor-fold desc="record" defaultstate="collapsed">
  public class ColumnRecord implements TablesReader.Record
  {
    String line;

    private ColumnRecord(String line)
    {
      this.line = line;
    }

    private String getData(Field field)
    {
      int start = field.beginIndex;
      int end = field.endIndex;
      return line.substring(start, end);
    }

    @Override
    public Object get(int fieldNumber)
    {
      if (fieldNumber >= fields.length)
        throw new IndexOutOfBoundsException("Field out of bounds");
      Field field = fields[fieldNumber];
      return field.vo.valueOf(getData(field));
    }

    @Override
    public Object get(String headerKey)
    {
      Field field = headerMap.get(headerKey);
      if (field == null)
        return null;
      return field.vo.valueOf(getData(field));
    }

    @Override
    public <Type> Type get(int fieldNumber, Class<Type> type)
            throws IllegalArgumentException
    {
      if (fieldNumber >= fields.length)
        throw new IndexOutOfBoundsException("Field out of bounds");

      Field field = fields[fieldNumber];
      ValueOf vo = field.getValueOf(type);
      return (Type) vo.valueOf(getData(field));
    }

    @Override
    public <Type> Type get(String headerKey, Class<Type> type) throws IllegalArgumentException
    {
      // Find the field
      Field field = headerMap.get(headerKey);
      if (field == null)
        return null;
      ValueOf vo = field.getValueOf(type);
      return (Type) vo.valueOf(getData(field));
    }

    @Override
    public Object get(TablesReader.Field field)
    {
      if (field == null || !(field instanceof Field))
        return null;
      Field cfield = (Field) field;
      return cfield.vo.valueOf(getData(cfield));
    }

    @Override
    public <Type> Type get(TablesReader.Field field, Class<Type> type) throws IllegalArgumentException
    {
      if (field == null || !(field instanceof Field))
        return null;
      Field cfield = (Field) field;
      ValueOf vo = cfield.getValueOf(type);
      return (Type) vo.valueOf(getData(cfield));
    }
  }
//</editor-fold>
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