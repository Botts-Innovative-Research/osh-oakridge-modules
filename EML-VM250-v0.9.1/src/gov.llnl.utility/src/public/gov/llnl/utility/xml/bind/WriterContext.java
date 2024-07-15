/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.utility.xml.bind;

import gov.llnl.utility.io.WriterException;
import gov.llnl.utility.xml.DomBuilder;
import gov.llnl.utility.xml.bind.ObjectWriter.WriterBuilder;
import org.w3c.dom.Document;

/**
 *
 * @author nelson85
 */
public interface WriterContext
{
  WriterBuilder newBuilder(ObjectWriter writer);

  Marshaller getMarshaller(Class cls) throws WriterException;

  MarshallerOptions getMarshallerOptions();
  
  DomBuilder element();

  <Type> DomBuilder write(
          Document document,
          ObjectWriter<Type> writer,
          String elementName,
          Type object,
          boolean objectRoot)
          throws WriterException;

  <Type> void addContents(Type object) throws WriterException;

//<editor-fold desc="properties" defaultstate="collapsed">
  void setProperty(String key, Object value)
          throws UnsupportedOperationException;

  Object getProperty(String key);

  <T> T getProperty(String key, Class<T> cls, T defaultValue);
//</editor-fold>
//<editor-fold desc="marshallers" defaultstate="collapsed">  

  /**
   * Marshaller options are a key based type store.
   */
  public interface MarshallerOptions
  {
    /**
     *
     * @param <Type>
     * @param key
     * @param cls
     * @param defaultValue
     * @return
     */
    <Type> Type get(String key, Class<Type> cls, Type defaultValue) throws ClassCastException;
  }

  /**
   * Marshallers are used to convert object to strings.
   *
   * @param <Type>
   */
  public interface Marshaller<Type>
  {
    Class<Type> getObjectClass();

    String marshall(Type o, MarshallerOptions options);

    boolean hasProperty(String part);
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