/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */

package test;

/*
 * Copyright 2016, Lawrence Livermore National Security, LLC. 
 * All rights reserved
 * 
 * Terms and conditions are given in "Notice" file.
 */
/**
 *
 * @author nelson85
 */
public class CopySpeedTest
{
  static void copy(double[] src, int srcOffset, double[] target, int targetOffset, int len)
  {
    while (len >= 8)
    {
      target[targetOffset + 0] = src[srcOffset + 0];
      target[targetOffset + 1] = src[srcOffset + 1];
      target[targetOffset + 2] = src[srcOffset + 2];
      target[targetOffset + 3] = src[srcOffset + 3];
      target[targetOffset + 4] = src[srcOffset + 4];
      target[targetOffset + 5] = src[srcOffset + 5];
      target[targetOffset + 6] = src[srcOffset + 6];
      target[targetOffset + 7] = src[srcOffset + 7];
      len -= 8;
      targetOffset += 8;
      srcOffset += 8;
    }
    while (len > 0)
    {
      target[targetOffset++] = src[srcOffset++];
      --len;
    }
  }

  static void v0(double[] a, double[] b)
  {
    for (int i = 0; i < 10000; i++)
    {
      System.arraycopy(a, 0, b, 0, a.length);
      copy(b, 0, a, 0, a.length);
    }
  }

  static void v1(double[] a, double[] b)
  {
    for (int i = 0; i < 400000; i++)
    {
      copy(a, 0, b, 0, a.length);
      copy(b, 0, a, 0, a.length);
    }
  }

  static void v2(double[] a, double[] b)
  {
    for (int i = 0; i < 400000; i++)
    {
      System.arraycopy(a, 0, b, 0, a.length);
      System.arraycopy(b, 0, a, 0, a.length);
    }
  }

  static public void main(String[] args)
  {
    double[] a = new double[10000];
    double[] b = new double[10000];
    for (int i = 0; i < a.length; ++i)
      a[i] = i;

    for (int i = 0; i < 4; ++i)
    {
      System.out.println(i);
      v0(a, b);
      v2(a, b);
      v1(a, b);
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