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

/**
 * Utility functions for converting to unsigned number in Java. Java lacks the
 * ability to handle unsigned numbers. This is a problem for file formats that
 * store their data as unsigned. The correct solution is to promote the number
 * to the next largest size. These utility functions do that promotion.
 */
public class UnsignedUtilities
{
  /**
   * Convert an unsigned short into a integer.
   */
  static public int getUnsignedShort(short s)
  {
    int out = s;
    if (out < 0)
      out += 65536;
    return out;
  }

  /**
   * Convert an unsigned integer into a long.
   */
  static public long getUnsignedInt(int i)
  {
    long out = i;
    if (out < 0)
      out += 4294967296l;
    return out;
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