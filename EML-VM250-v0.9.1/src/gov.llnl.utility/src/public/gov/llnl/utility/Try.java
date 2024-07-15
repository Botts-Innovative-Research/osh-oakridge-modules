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

import java.util.function.BiConsumer;

/**
 *
 * @author nelson85
 */
public class Try
{
  @FunctionalInterface
  public interface ExceptionalBiConsumer<T, T2>
  {
    void accept(T t1, T2 t2) throws Exception;
  }

  public static <T, T2> BiConsumer<T, T2> promote(ExceptionalBiConsumer<T, T2> functor)
  {
    return (T t, T2 u) ->
    {
      try
      {
        functor.accept(t, u);
      }
      catch (Exception ex)
      {
        throw new RuntimeException(ex);
      }
    };
  }

//  void foo(int i) throws FileNotFoundException
//  {
//  }
//
//  static public void main(String[] args)
//  {
//    ExceptionalBiConsumer<Try, Integer> i0 = Try::foo;
//    BiConsumer<Try, Integer> i1= Try.promote(Try::foo);
//  }
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