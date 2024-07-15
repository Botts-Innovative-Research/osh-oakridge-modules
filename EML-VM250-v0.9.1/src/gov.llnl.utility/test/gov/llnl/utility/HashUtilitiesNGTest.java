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

import static org.testng.Assert.assertEquals;
import org.testng.annotations.Test;

/**
 *
 * @author nelson85
 */
public class HashUtilitiesNGTest
{

  public HashUtilitiesNGTest()
  {
  }

  /**
   * Test of hash method, of class HashUtilities.
   */
  @Test
  public void testHash()
  {
    class TestPattern
    {
      int[] in;
      long out;

      private TestPattern(int[] in, long out)
      {
        this.in = in;
        this.out = out;
      }
    }

    TestPattern[] patterns = new TestPattern[]
    {
      new TestPattern(null, 0),
      new TestPattern(new int[]
      {
      }, -3162216497309240828l),
      new TestPattern(new int[]
      {
        0
      }, -1021191746955413710l),
      new TestPattern(new int[]
      {
        1
      }, -1061438811504499547l),
      new TestPattern(new int[]
      {
        1, 0
      }, 2306653601870186394l)
    };

    for (TestPattern pattern : patterns)
    {
      long result = HashUtilities.hash(pattern.in);
      assertEquals(result, pattern.out);
    }
  }

  /**
   * Test of byteArrayToHexString method, of class HashUtilities.
   */
  @Test
  public void testByteArrayToHexString()
  {
    byte[] b = new byte[]
    {
      (byte) 0x1, (byte) 0x2, (byte) 0x3, (byte) 0xaa, (byte) 0xbb, (byte) 0xcc
    };
    assertEquals(HashUtilities.byteArrayToHexString(b), "010203aabbcc");
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