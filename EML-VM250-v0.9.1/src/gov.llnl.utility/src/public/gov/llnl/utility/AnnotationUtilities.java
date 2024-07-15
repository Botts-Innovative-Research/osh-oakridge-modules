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

import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 *
 * @author nelson85
 */
public class AnnotationUtilities
{
  @SuppressWarnings("unchecked")
  public static <T extends Annotation> Collection<T> getRepeatingAnnotation(Class cls, Class<T> annotation)
  {
    Repeatable repeatable = annotation.getAnnotation(Repeatable.class);
    Annotation list = null;
    if (repeatable != null)
    {
      list = cls.getAnnotation(repeatable.value());
    }

    if (list == null)
    {
      T single = (T) cls.getAnnotation(annotation);
      if (single == null)
        return Collections.emptyList();
      return Arrays.asList(single);
    }

    try
    {
      Method method = repeatable.value().getMethod("value");
      return Arrays.asList((T[]) method.invoke(list));
    }
    catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException ex)
    {
      throw new RuntimeException(ex);
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