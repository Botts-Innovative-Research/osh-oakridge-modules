/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.utility;

import java.io.Serializable;
import java.util.Map;

/**
 * Interface for data types that support keyed data.
 *
 * @author nelson85
 */
public interface Expandable
{

  /**
   * Access the attributes map. This should support operations to clear(),
   * get(), and remove().
   *
   * @return
   */
  Map<String, Serializable> getAttributes();

  /**
   * Get the attribute.
   *
   * @param name of the attribute
   * @return the attribute with the name, or null if the object is not set.
   */
  Serializable getAttribute(String name);

  /**
   * Set the attribute.
   *
   * @param <T>
   * @param name
   * @param value
   */
  <T extends Serializable> void setAttribute(String name, T value);

//<editor-fold desc="default">
  /**
   * Get the attribute with class specified. If the attribute exists and has the
   * correct type, then the attribute is returned. Otherwise, it will return a
   * null.
   *
   * @param <T>
   * @param name of the attribute
   * @param cls of the attribute
   * @return the attribute with the name, or null if the object is not set.
   * @throws ClassCastException if the type of the attribute is incorrect.
   */
  default <T> T getAttribute(String name, Class<T> cls) throws ClassCastException
  {
    return getAttribute(name, cls, null);
  }
  
  
  
  /**
   * Get the attribute with class specified. If the attribute exists and has the
   * correct type, then the attribute is returned. Otherwise, it will return a
   * null.
   *
   * @param <T> 
   * @return the attribute with the name, or null if the object is not set.
   * @throws ClassCastException if the type of the attribute is incorrect.
   */
  
  default <T> T getAttribute(Attribute<T> key) throws ClassCastException
  {
    Map<String, Serializable> map = this.getAttributes();
    if (map == null)
      return key.getDefaultValue();

    // Step 1 try to get the value
    Object value = map.get(key.getName());
    Class<T> cls = key.getAttributeClass();
    // Step 2 we found it so all is good
    if (value != null && cls.isInstance(value))
    {
      return cls.cast(value);
    }

    // Step 3 if the type is wrong this is an error
    if (value != null)
      throw new ClassCastException("incorrect attributed type " + cls + "!=" + value.getClass());

    return key.getDefaultValue();
  }

  /**
   * Get the attribute with class specified. If the attribute exists and has the
   * correct type, then the attribute is returned. If it is the wrong type then
   * it will throw an ClassCastException. Otherwise, it will return the given
   * default value.
   *
   * @param <T>
   * @param name of the attribute
   * @param cls of the attribute
   * @param defaultValue
   * @return the attribute with the name, or null if the object is not set.
   * @throws ClassCastException if the type of the attribute is incorrect.
   */
  default <T> T getAttribute(String name, Class<T> cls, T defaultValue) throws ClassCastException
  {
    Map<String, Serializable> map = this.getAttributes();
    if (map == null)
      return defaultValue;

    // Step 1 try to get the value
    Object value = map.get(name);

    // Step 2 we found it so all is good
    if (value != null && cls.isInstance(value))
    {
      return cls.cast(value);
    }

    // Step 3 if the type is wrong this is an error
    if (value != null)
      throw new ClassCastException("incorrect attributed type " + cls + "!=" + value.getClass());

    return defaultValue;
  }

  /**
   * Check if an attribute is set.
   *
   * @param key
   * @return true if the attribute exists.
   */
  default boolean hasAttribute(String key)
  {
    Map<String, Serializable> map = this.getAttributes();
    if (map == null)
      return false;
    return map.containsKey(key);
  }

  /**
   * Remove an attribute.
   *
   * @param key
   */
  default void removeAttribute(String key)
  {
    Map<String, Serializable> map = this.getAttributes();
    if (map == null)
      return;
    getAttributes().remove(key);
  }
//</editor-fold>

  /**
   * Copy the attributes from one object to another.
   *
   * @param ai
   */
  default void copyAttributes(Expandable ai)
  {
    Map<String, Serializable> attr = ai.getAttributes();
    if (attr == null)
      return;
    getAttributes().putAll(ai.getAttributes());
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