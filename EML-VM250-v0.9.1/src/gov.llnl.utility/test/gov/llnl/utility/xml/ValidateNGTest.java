/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.utility.xml;

import gov.llnl.utility.PackageResource;
import java.io.InputStream;
import java.nio.file.Path;
import static org.testng.Assert.*;
import org.testng.annotations.Test;

/**
 *
 * @author pham21
 */
public class ValidateNGTest
{
  
  public ValidateNGTest()
  {
  }

  /**
   * Test of getPackageResource method, of class Validate.
   */
  @Test
  public void testGetPackageResource() throws Exception
  {
    System.out.println("getPackageResource");
    Class cls = null;
    Validate instance = new Validate();
    PackageResource expResult = null;
    PackageResource result = instance.getPackageResource(cls);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of validate method, of class Validate.
   */
  @Test
  public void testValidate_PackageResource_Path() throws Exception
  {
    System.out.println("validate");
    PackageResource obj = null;
    Path file = null;
    Validate instance = new Validate();
    boolean expResult = false;
    boolean result = instance.validate(obj, file);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of validate method, of class Validate.
   */
  @Test
  public void testValidate_3args()
  {
    System.out.println("validate");
    PackageResource resource = null;
    InputStream inputStream = null;
    String systemId = "";
    Validate instance = new Validate();
    boolean expResult = false;
    boolean result = instance.validate(resource, inputStream, systemId);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
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