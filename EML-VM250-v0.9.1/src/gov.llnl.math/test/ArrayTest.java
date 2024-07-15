/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author nelson85
 */
public class ArrayTest
{
  /**
   * Shrink the size of a list to a specified size.
   * List type must support subList with clear.
   *
   * @param <T> is a type derived from list.
   * @param list is the list to be shrunk.
   * @param size is the desired size.
   * @return the reduced size list.
   * @throws IllegalArgumentException if the list is is smaller than the
   * requested size.
   */
  static public <T extends List<?>> T shrinkTo(T list, int size)
  {
    list.subList(size, list.size()).clear();
    return list;
  }

  static public void main(String[] args)
  {
    ArrayList<Integer> out = new ArrayList<>();

    out.add(1);
    out.add(2);
    out.add(3);
    out.add(4);
    out.add(5);
    System.out.println(out.size());

    shrinkTo(out, 5);
    //out.subList(3, 5).clear();
    System.out.println(out.size());

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