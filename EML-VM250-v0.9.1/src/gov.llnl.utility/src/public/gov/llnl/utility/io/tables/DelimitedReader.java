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
import gov.llnl.utility.Tokenizer;
import gov.llnl.utility.annotation.Debug;
import gov.llnl.utility.annotation.Matlab;
import gov.llnl.utility.io.tables.DelimitedReader.DelimitedRecord;
import java.io.BufferedReader;
import java.io.IOException;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 *
 * @author seilhan3
 */
public class DelimitedReader implements TablesReader<DelimitedRecord>
{
  BufferedReader br;
  Pattern commentPattern;
  String line;
  Field[] fields;
  TreeMap<String, Field> headerMap = new TreeMap<>();
  int columns;
  DelimitedLineSplitter splitter;

  @Override
  public void close() throws IOException
  {
    br.close();
  }

//<editor-fold desc="field" defaultstate="collapsed">
  static public class Field implements TablesReader.Field
  {
    int column;
    String key;
    Class type = String.class;
    ValueOf vo = newValueOf(String.class);

    Field(int column)
    {
      this.column = column;
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
      return key;
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
  @Override
  public boolean hasNext()
  {
    return line != null;
  }

  @Override
  public DelimitedRecord next()
  {
    // If there is no line, then we need to throw.  
    if (line == null)
      throw new NoSuchElementException();

    // Parse the line to extract the columns to create markers for parsing
    int[] marker = splitter.getMarkers(line, columns);
    DelimitedRecord out = new DelimitedRecord(line, marker);
    try
    {
      line = this.getNextLine();
    }
    catch (IOException ex)
    {
      throw (NoSuchElementException) new NoSuchElementException().initCause(ex);
    }
    return out;
  }

  @Override
  public void remove()
  {
    throw new UnsupportedOperationException("Not supported.");
  }

//</editor-fold>
//<editor-fold desc="record" defaultstate="collapsed">
  public class DelimitedRecord implements TablesReader.Record
  {
    @Debug public String line;
    @Debug public int[] marker;

    private DelimitedRecord(String line, int[] marker)
    {
      this.line = line;
      this.marker = marker;
    }

    public int getSize()
    {
      if (marker == null)
        return 0;
      return marker.length / 2;
    }

    private String getData(Field field)
    {
      int column = field.column;
      int start = marker[2 * column];
      int end = marker[2 * column + 1];
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
    public <Type> Type get(int fieldNumber, Class<Type> type) throws IllegalArgumentException
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
    public <Type> Type get(TablesReader.Field field, Class<Type> type)
            throws IllegalArgumentException
    {
      if (field == null || !(field instanceof Field))
        return null;
      Field cfield = (Field) field;
      ValueOf vo = cfield.getValueOf(type);
      return (Type) vo.valueOf(getData(cfield));
    }

    public Instant get(Instant start, Class<Instant> aClass)
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
  }

//</editor-fold>
//<editor-fold desc="internal" defaultstate="collapsed">  
  String getNextLine() throws IOException
  {
    String line;
    // Search for the next valid line
    while ((line = br.readLine()) != null)
    {
      // Skip comments
      if (commentPattern == null
              || commentPattern.matcher(line).lookingAt() == false)
        return line;
    }
    return null;
  }

//</editor-fold>
  public interface DelimitedLineSplitter
  {
    int[] getMarkers(String line, int columns);

    int countDelimiters(String line);
  }

  public static class DefaultLineSplitter implements DelimitedLineSplitter
  {
    String delimiter;

    public DefaultLineSplitter(String delimiter)
    {
      this.delimiter = delimiter;
    }

    @Override
    public int[] getMarkers(String line, int columns)
    {

      // Parse the line to extract the columns to create markers for parsing
      int[] marker = new int[columns * 2];
      int start = 0;
      for (int i = 0; i < columns; ++i)
      {
        int next = (short) line.indexOf(delimiter, start);
        marker[2 * i] = start;
        if (next == -1)
        {
          next = line.length();
          start = next;
        }
        else
        {
          start = next + delimiter.length();
        }
        marker[2 * i + 1] = next;
      }
      return marker;
    }

    // Used to decide the number of columns when automatically reading the header.
    @Override
    public int countDelimiters(String line)
    {
      // Parse the line to extract the columns to create markers for parsing
      int count = 0;
      int start = 0;
      while (true)
      {
        int next = line.indexOf(delimiter, start);
        if (next == -1)
          return count;
        else
          start = next + delimiter.length();
        count++;
      }
    }
  }

  public static class ExcelLineSplitter implements DelimitedLineSplitter
  {
    String delimiter;
    Tokenizer tokenizer = Tokenizer.create(
            ",", //0
            "\"\"", //1
            "\"\\[",//2
            "\\]\"", //3
            "\\[", //4
            "\\]", //5
            "\"", //6
            "[^,\\[\\]\"]+"); //7

    @Override
    public int[] getMarkers(String line, int columns)
    {
      Tokenizer.TokenMatcher matcher = tokenizer.matcher(line);

      int[] marker = new int[columns * 2];
      int level = 0;
      int close = 0;
      int start = 0;
      int end = 0;
      int last = 0;
      int counter = 0;
      //,[121.8,244.7,344.3,778.9,964.1,1408]
      for (Tokenizer.Token token : matcher)
      {
        int id = token.id();
        String g = token.group();
        if ((id == 2 || id == 4 || id == 6) && last == 0)
        {  // starting a quoted string
          if (id == 2)
            close = 3;
          else if (id == 4)
            close = 5;
          else if (id == 6)
            close = 6;
          start = token.end();
          level++;
        }
        else if (level == 0 && id == 0)
        {
          // at end of a field
          marker[counter++] = start;
          marker[counter++] = end;
          if (counter == columns * 2)
            return marker;
        }
        else if (level == 1 && id == close)
        {
          level--;
          end = token.start();
          close = -1;
        }
        else if (level == 0 && last == 0)
        {
          // unquoted string
          start = token.start();
          end = token.end();
        }
        else
        {
          end = token.end();
        }
        last = id;
      }
      if (last != 0)
      {
        marker[counter++] = start;
        marker[counter++] = end;
      }
      return marker;
    }

    // Used to decide the number of columns when automatically reading the header.
    @Override
    public int countDelimiters(String line)
    {
      Tokenizer.TokenMatcher matcher = tokenizer.matcher(line);

      // Parse the line to extract the columns to create markers for parsing
      int count = 0;
      int last = 0;
      int close = 0;
      int level = 0;
      for (Tokenizer.Token token : matcher)
      {
        int id = token.id();
        if ((id == 2 || id == 4 || id == 6) && last == 0)
        {  // starting a quoted string
          if (id == 2)
            close = 3;
          else if (id == 4)
            close = 5;
          else if (id == 6)
            close = 6;
          level++;
        }
        else if (level == 0 && id == 0)
        {
          // at end of a field
          count++;
        }
        else if (level == 1 && id == close)
        {
          level--;
          close = -1;
        }
        last = id;
      }
      return ++count;
    }
  }

  @Matlab public static ExcelLineSplitter makeExcelSplitter()
  {
    return new ExcelLineSplitter();
  }

  static public void main(String[] args)
  {

    ExcelLineSplitter splitter = new ExcelLineSplitter();
    String testPattern = "\"[245.007431261599,441.86844905819]\",foo,\"foo1\",[bar],[bar1,bar2],\"[foo2]\",\"[foo3,foo4]\",235";
    System.out.println(testPattern);
    int[] matches = splitter.getMarkers(testPattern, 8);
    for (int i = 0; i < matches.length; i += 2)
    {
      System.out.println(i / 2 + " " + testPattern.substring(matches[i], matches[i + 1]));
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