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

import java.util.UUID;
import static org.testng.Assert.*;
import org.testng.annotations.Test;

/**
 *
 * @author pham21
 */
public class UUIDUtilitiesNGTest
{
  
  public UUIDUtilitiesNGTest()
  {
  }

  /**
   * Test of createUUID method, of class UUIDUtilities.
   */
  @Test
  public void testCreateUUID()
  {
    System.out.println("createUUID");
    assertEquals(UUIDUtilities.createUUID("ToInfinityAndBeyond"), UUID.fromString("aae59d09-127e-a8e9-2862-ddbf9f2d6cde"));
  }

  /**
   * Test of createLong method, of class UUIDUtilities.
   */
  @Test
  public void testCreateLong()
  {
    System.out.println("createLong");
    assertEquals(UUIDUtilities.createLong("ToInfinityAndBeyond"), -9041186574123678665L);
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