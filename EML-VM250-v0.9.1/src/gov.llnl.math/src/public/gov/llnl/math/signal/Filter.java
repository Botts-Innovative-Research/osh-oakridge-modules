/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.math.signal;

/**
 * Interface for functions that act as filters.
 *
 * @author nelson85
 */
public interface Filter
{
  double[] apply(double[] in);

  double[] apply(IterateDoubles iter);

  public interface IterateDoubles
  {
    boolean hasNext();

    double next();

    int size();
  }

  public class RollingBuffer
  {
    double[] memory;
    int index;

    public RollingBuffer(int size)
    {
      memory = new double[size];
    }

    public void add(double value)
    {
      index--;
      if (index < 0)
        index += memory.length;
      memory[index] = value;
    }

    public double get(int i)
    {
      int n = (index + i) % memory.length;
      return memory[n];
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