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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Utility class to declare singletons.
 *
 * @author nelson85
 */
final public class Singletons
{
  /**
   * All classes that implement Singleton must implement a static method
   * {@code getInstance}.
   */
  public interface Singleton
  {
  }

  public interface SingletonRequired
  {
  }

  @SuppressWarnings("unchecked")
  static public <T> T getSingleton(Class<T> cls)
  {
    try
    {
      Method method = cls.getDeclaredMethod("getInstance");
      if (method == null)
        throw new RuntimeException("No instance method for " + cls.getCanonicalName());
      return (T) method.invoke(null);
    }
    catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex)
    {
      throw new RuntimeException("Unable to get instance for " + cls.getCanonicalName(), ex);
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