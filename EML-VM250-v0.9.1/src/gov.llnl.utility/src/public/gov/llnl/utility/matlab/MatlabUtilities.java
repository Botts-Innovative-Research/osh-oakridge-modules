/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.utility.matlab;

import gov.llnl.utility.annotation.Matlab;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

/**
 * Set of utilities that are needed to support Matlab. Certain operations in
 * Matlab are hard to achieve because of conflicts in the Matlab syntax.
 *
 * @author nelson85
 */
public class MatlabUtilities
{
  /**
   * Utility to convert a list of vectors into an array for Matlab. This is
   * useful for quickly aggregating vectors of results for plotting. Each entry
   * will constitute a row in the output.
   *
   * <p>
   * Matlab example:
   * <pre>
   * {@code
   * % Import required packages
   * import gov.llnl.utility.matlab.*;
   * import java.util.*;
   *
   * % Pack data into a result list
   * results=LinkedList();
   * for i=1:100
   *   results.add([i 2*i 4*i]);
   * end
   *
   * % Convert to Matlab structure
   * results=MatlabUtilities.toArray(results);
   * plot(results);
   * }
   * </pre>
   *
   *
   * @param in is a list containing double vectors with the same length.
   * @return an array suitable to convert to Matlab structures.
   */
  @Matlab
  public static <T> Object toArray(Collection<T> in)
  {
    if (in.isEmpty())
      return null;

    T front = in.iterator().next();
    Class<?> cls = front.getClass();
    if (front instanceof double[])
      return ((List<double[]>) in).toArray(new double[0][]);

    // Automatic unbox
    if (front instanceof Double)
    {
      double[] out = new double[in.size()];
      int i = 0;
      for (Double d : (List<Double>) in)
      {
        out[i++] = d;
      }
      return out;
    }

    return in.toArray((T[]) Array.newInstance(cls, 0));
  }

  @Matlab
  public static <T> Object toArray(Collection<T> in, String methodName)
  {
    try
    {
      if (in.isEmpty())
        return null;

      T front = in.iterator().next();
      Class<?> cls = front.getClass();

      Method method = cls.getMethod(methodName, (Class<?>[]) null);
      Class<?> returnType = method.getReturnType();

      Object[] out = (Object[]) Array.newInstance(returnType, in.size());
      int i = 0;
      for (T t : in)
      {
        out[i++] = method.invoke(t);
      }

      return out;
    }
    catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex)
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