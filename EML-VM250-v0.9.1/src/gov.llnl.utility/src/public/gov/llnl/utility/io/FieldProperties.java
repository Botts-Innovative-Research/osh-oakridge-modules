/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.utility.io;

import gov.llnl.utility.ClassUtilities;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;

/**
 *
 * @author nelson85
 */
public class FieldProperties implements PropertyInterface
{
  Object parent;

  @Retention(RetentionPolicy.RUNTIME)
  public @interface Property
  {
    String name() default "";

    boolean set() default true;

    boolean get() default true;
  }

  public FieldProperties()
  {
    this.parent = this;
  }

  public FieldProperties(Object parent)
  {
    this.parent = parent;
  }

  private Field findProperty(String key)
  {
    Field[] fields = parent.getClass().getDeclaredFields();
    for (Field f : fields)
    {
      Property annotation = f.getAnnotation(Property.class);
      if (annotation == null)
        continue;
      if (annotation.name().equals("") && f.getName().equals(key))
        return f;
      if (annotation.name().equals(key))
        return f;
    }
    return null;
  }

  public void setProperty(String key, Object property) throws PropertyException
  {
    Field field = findProperty(key);
    if (field == null)
      throw new PropertyException("Unable to find property " + key);

    if (field.getType().isPrimitive())
    {
      if (property == null)
        throw new PropertyException("Primitives cannot be assigned to null");
      ClassUtilities.Primitive prim = ClassUtilities.getPrimitive(field.getType());
      property = prim.cast(property);
    }
    else
    {
      if (!field.getType().isAssignableFrom(property.getClass()))
        throw new PropertyException(
                String.format("Property %s is incorrect type, expected %s, got %s",
                        key, field.getType(), property.getClass()));
    }

    try
    {
      Property annotation = field.getAnnotation(Property.class);
      if (annotation == null || annotation.set() == false)
        throw new PropertyException(String.format("Property %s is not writable", key));
      field.setAccessible(true);
      field.set(parent, property);
    }
    catch (SecurityException | IllegalArgumentException | IllegalAccessException ex)
    {
      throw new PropertyException("Unable to set property " + key);
    }
  }

  public Object getProperty(String key) throws PropertyException
  {
    try
    {
      Field field = parent.getClass().getDeclaredField(key);
      Property annotation = field.getAnnotation(Property.class);
      if (annotation == null || annotation.get() == false)
        throw new PropertyException(String.format("Property %s is not readable", key));
      field.setAccessible(true);
      return field.get(parent);
    }
    catch (NoSuchFieldException | SecurityException ex)
    {
      throw new PropertyException("Unable to find property " + key);
    }
    catch (IllegalArgumentException | IllegalAccessException ex)
    {
      throw new PropertyException("Unable to get property " + key);
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