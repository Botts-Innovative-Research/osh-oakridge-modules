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

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import static org.testng.Assert.*;
import org.testng.annotations.Test;

/**
 *
 * @author pham21
 */
public class StringUtilitiesNGTest
{

  public StringUtilitiesNGTest()
  {
  }

  /**
   * Test of join method, of class StringUtilities.
   */
  @Test
  public void testJoin_Iterable_String()
  {
    System.out.println("join");
    Iterable<String> strings = new ArrayList<>()
    {
      {
        add("c");
        add("d");
        add("e");
        add("f");
        add("g");
        add("a");
        add("b");
      }
    }; 
    assertEquals(StringUtilities.join(strings, ""), "cdefgab");
  }

  /**
   * Test of join method, of class StringUtilities.
   */
  @Test
  public void testJoin_StringArr_String()
  {
    System.out.println("join");
    String[] strings = new String[]{ "c", "d", "e", "f", "g", "a", "b" };
    assertEquals(StringUtilities.join(strings, ", "), "c, d, e, f, g, a, b");
  }

  /**
   * Test of getFixedString method, of class StringUtilities.
   */
  @Test
  public void testGetFixedString() throws UnsupportedEncodingException
  {
    System.out.println("getFixedString");
    assertEquals(StringUtilities.getFixedString(ByteBuffer.wrap("Hello".getBytes("UTF8")), 5), "Hello");
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