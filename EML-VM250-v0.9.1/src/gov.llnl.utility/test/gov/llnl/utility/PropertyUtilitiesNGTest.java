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

import static org.testng.Assert.*;
import org.testng.annotations.Test;

/**
 *
 * @author pham21
 */
public class PropertyUtilitiesNGTest
{

  public PropertyUtilitiesNGTest()
  {
  }

  /**
   * Test of get method, of class PropertyUtilities.
   */
  @Test(expectedExceptions =
  {
    RuntimeException.class
  })
  public void testGet()
  {
    System.out.println("get");

    assertEquals(PropertyUtilities.get("java.home", "Nakama"), System.getProperty("java.home"));
    assertEquals(PropertyUtilities.get("ONE_PIECE", "Nakama"), "Nakama");
    assertEquals(PropertyUtilities.get("java.home", 1999), Integer.valueOf(1999));
    // Test RuntimeException
    PropertyUtilities.get("ONE_PIECE", null);
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