/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.utility.internal.xml.bind.readers;

import gov.llnl.utility.io.ReaderException;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import static org.testng.Assert.*;
import org.testng.annotations.Test;
import org.xml.sax.helpers.AttributesImpl;

/**
 *
 * @author pham21
 */
public class DurationContentsNGTest
{

  public DurationContentsNGTest()
  {
  }

  /**
   * Test of getObjectClass method, of class DurationContents.
   */
  @Test
  public void testGetObjectClass()
  {
    System.out.println("getObjectClass");
    DurationContents instance = new DurationContents();
    assertEquals(instance.getObjectClass(), Duration.class);
  }

  /**
   * Test of start method, of class DurationContents.
   */
  @Test(expectedExceptions =
  {
    ReaderException.class
  })
  public void testStart() throws Exception
  {
    System.out.println("start");
    DurationContents dc = new DurationContents();
    AttributesImpl attr = new AttributesImpl();
    attr.addAttribute("", "units", "units", "null", null);

    assertNull(dc.start(attr));

    // Set nanos
    attr.setValue(0, "ns");
    assertNull(dc.start(attr));
    assertTrue(dc.contents("1").equals(Duration.of(1L, ChronoUnit.NANOS)));

    // Set micros
    attr.setValue(0, "us");
    assertNull(dc.start(attr));
    assertTrue(dc.contents("1").equals(Duration.of(1L, ChronoUnit.MICROS)));

    // Set millis
    attr.setValue(0, "ms");
    assertNull(dc.start(attr));
    assertTrue(dc.contents("1").equals(Duration.of(1L, ChronoUnit.MILLIS)));

    // Set seconds
    attr.setValue(0, "s");
    assertNull(dc.start(attr));
    assertTrue(dc.contents("1").equals(Duration.of(1L, ChronoUnit.SECONDS)));

    // Test ReaderException
    attr.setValue(0, "superseconds");
    dc.start(attr);
  }

  /**
   * Test of contents method, of class DurationContents.
   */
  @Test(expectedExceptions =
  {
    DateTimeParseException.class
  })
  public void testContents() throws Exception
  {
    System.out.println("contents");
    DurationContents dc = new DurationContents();
    
    // When timeUnit is null
    assertTrue(dc.contents("PT3.141592653S").equals(Duration.of(3141592653L, ChronoUnit.NANOS)));
    
    // When timeUnit is set
    AttributesImpl attr = new AttributesImpl();
    attr.addAttribute("", "units", "units", "string", "ns");
    dc.start(attr);
    Duration duration = dc.contents("3141592653");
    assertEquals(duration.toString(), "PT3.141592653S");
    assertTrue(duration.equals(Duration.of(3141592653L, ChronoUnit.NANOS)));
    
    // Test NumberFormatException
    try
    {
      dc.contents("T2.0");
    }
    catch(NumberFormatException ex)
    {
      // Test passed
    }
    
    // Test DateTimeParseException
    dc = new DurationContents();
    dc.contents("3.14");
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