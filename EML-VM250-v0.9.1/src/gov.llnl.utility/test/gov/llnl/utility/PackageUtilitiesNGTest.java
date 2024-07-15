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

import java.net.URL;
import java.util.jar.Manifest;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNotNull;
import org.testng.annotations.Test;

/**
 *
 * @author nelson85
 */
public class PackageUtilitiesNGTest
{
  PackageUtilities pu;

  public PackageUtilitiesNGTest()
  {
    pu = new PackageUtilities();
  }

  /**
   * Test of getClassURL method, of class PackageUtilities.
   */
  @Test
  public void testGetClassURL()
  {
    System.out.println("getClassURL");
    Class<?> klass = pu.getClass();
    URL expResult = pu.getClass().getClassLoader()
            .getResource("gov/llnl/utility/PackageUtilities.class");
    URL result = PackageUtilities.getClassURL(klass);
    assertEquals(result, expResult);
  }

  /**
   * Test of getJarURL method, of class PackageUtilities.
   */
  @Test
  public void testGetJarURL()
  {
    System.out.println("getJarURL");
    Class<?> klass = pu.getClass();
    URL result = PackageUtilities.getJarURL(klass);
    assertNotNull(result);
    assertEquals(result.getClass(), URL.class);
  }

  /**
   * Test of getJarMd5Checksum method, of class PackageUtilities.
   */
  @Test
  public void testGetJarMd5Checksum() throws Exception
  {
    System.out.println("getJarMd5Checksum");
    Class<?> klass = pu.getClass();
    String result = PackageUtilities.getJarMd5Checksum(klass);
    assertNotNull(result);
    assertNotEquals(result, "none");
    assertEquals(result.length(), 32);
  }

  /**
   * Test of getManifestURL method, of class PackageUtilities.
   */
  @Test
  public void testGetManifestURL()
  {
    System.out.println("getManifestURL");
    Class<?> klass = pu.getClass();
    URL expResult = klass.getClassLoader().getResource("META-INF/MANIFEST.MF");
    URL result = PackageUtilities.getManifestURL(klass);
    assertEquals(result, expResult);
  }

  /**
   * Test of getManifest method, of class PackageUtilities.
   */
  @Test
  public void testGetManifest()
  {
    System.out.println("getManifest");
    Class<?> cl = pu.getClass();
    Manifest result = PackageUtilities.getManifest(cl);
    assertNotNull(result);
    assertEquals(result.getClass(), Manifest.class);
    assertNotNull(result.getMainAttributes().entrySet());
//    for (Map.Entry<Object, Object> s : result.getMainAttributes().entrySet())
//    {
//      System.out.println(s.getKey() + " " + s.getValue());
//    }
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