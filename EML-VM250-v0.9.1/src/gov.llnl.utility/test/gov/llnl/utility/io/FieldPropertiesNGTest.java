/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.utility.io;

import gov.llnl.utility.io.FieldProperties.Property;
import static org.testng.Assert.assertEquals;
import org.testng.annotations.Test;

/**
 *
 * @author nelson85
 */
public class FieldPropertiesNGTest
{

  public FieldPropertiesNGTest()
  {
  }

  public class PropertyTest
  {
    FieldProperties proxy = new FieldProperties(this);

    @Property
    int value;

    @Property
    String name;
  }

  /**
   * Test of setProperty method, of class FieldProperties.
   */
  @Test
  public void testSetProperty() throws Exception
  {
    PropertyTest pt = new PropertyTest();
    pt.proxy.setProperty("name", "hello");
    pt.proxy.setProperty("value", 25);
    assertEquals(pt.name, "hello");
    assertEquals(pt.value, 25);
  }

  /**
   * Test of getProperty method, of class FieldProperties.
   */
  @Test
  public void testGetProperty() throws Exception
  {
    PropertyTest pt = new PropertyTest();
    pt.name = "hello";
    pt.value = 25;
    assertEquals(pt.proxy.getProperty("name"), "hello");
    assertEquals(pt.proxy.getProperty("value"), 25);
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