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

import java.io.Closeable;
import java.util.Iterator;

/**
 * TablesReader is the base class for reading tabular data from text files.
 *
 * @param <RecordType>
 */
public interface TablesReader<RecordType extends TablesReader.Record> extends Iterator<RecordType>, Closeable
{

  /**
   * Get the field by index.
   *
   * @param index
   * @return the requested field.
   * @throws IndexOutOfBoundsException if the index exceeds the number of fields
   * in the reader.
   */
  public Field getField(int index) throws IndexOutOfBoundsException;

  /**
   * Find an field with a specified key. Keys are either defined manually or can
   * be automatically established by the reader.
   *
   * @param key is the header key to search for.
   * @return the field or null if the field is not found.
   */
  public Field findField(String key);

  /**
   * Fields describe the content to be extracted by the table.
   */
  public interface Field
  {
    /**
     * Get the key for this field.
     *
     * @return the field key or null if not defined.
     */
    String getKey();

    /**
     * Get the type which will be used to parse this field.
     *
     * @return the type or String.class if not defined.
     */
    Class getType();
  }

  /**
   * Records are extracted with each call to next on the TableReader.
   * Implementations vary between Readers.
   */
  public static interface Record
  {
    /**
     * Get value of column number
     *
     * @param fieldNumber
     * @return the value in the field with the type defined by the field
     * default, may be null if the field is empty.
     * @throws IndexOutOfBoundsException if the requested field exceeds the
     * number of available fields.
     */
    Object get(int fieldNumber);

    /**
     * Get value of column with specified header key
     *
     * @param headerKey is the header key to retrieve.
     * @return the value in the field with the type defined by the field
     * default, may be null if the field is empty or the header key is not
     * defined.
     */
    Object get(String headerKey);

    /**
     * Get value of column with specified header key
     *
     * @param field is the field to be extracted.
     * @return the value in the field with the type defined by the field
     * default, may be null if the field is empty or field specified is not
     * found.
     */
    Object get(Field field);

    /**
     * Get value of column with specified type
     *
     * @param <Type>
     * @param fieldNumber
     * @param type is the type to be produced.
     * @return the value with the specified type or null if the field is
     * incorrect or null.
     * @throws IndexOutOfBoundsException if the requested field exceeds the
     * number of available fields.
     * @throws IllegalArgumentException if there is no converter for this type.
     */
    <Type> Type get(int fieldNumber, Class<Type> type)
            throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Get value of column with specified header key and return type
     *
     * @param <Type>
     * @param headerKey is the header key to retrieve.
     * @param type is the type to be produced.
     * @return the value with the specified type or null if the field is
     * incorrect or null.
     * @throws IllegalArgumentException if there is no converter for this type.
     */
    <Type> Type get(String headerKey, Class<Type> type)
            throws IllegalArgumentException;

    /**
     * Get value of column with specified header key and return type
     *
     * @param <Type>
     * @param field is the field to be fetched.
     * @param type is the type to be produced.
     * @return the value with the specified type or null if the field is
     * incorrect or null.
     * @throws IllegalArgumentException if there is no converter for this type.
     */
    <Type> Type get(Field field, Class<Type> type) throws IllegalArgumentException;
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