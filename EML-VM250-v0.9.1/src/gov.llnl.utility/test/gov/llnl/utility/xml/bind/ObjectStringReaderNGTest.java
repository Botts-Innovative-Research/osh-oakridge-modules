/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.utility.xml.bind;

import gov.llnl.utility.TestSupport;
import gov.llnl.utility.io.ReaderException;
import static org.testng.Assert.*;
import org.testng.annotations.Test;
import org.xml.sax.Attributes;

/**
 *
 * @author pham21
 */
public class ObjectStringReaderNGTest
{
  
  public ObjectStringReaderNGTest()
  {
  }

  /**
   * Test of start method, of class ObjectStringReader.
   */
  @Test
  public void testStart() throws Exception
  {
    System.out.println("start");
    ObjectStringReader instance = new ObjectStringReaderImpl();
    assertNull(instance.start(null));    
  }

  /**
   * Test of contents method, of class ObjectStringReader.
   */
  @Test
  public void testContents() throws Exception
  {
    System.out.println("contents");
    ObjectStringReader instance = new ObjectStringReaderImpl();
    assertNull(instance.contents(null));
  }

  /**
   * Test of end method, of class ObjectStringReader.
   */
  @Test
  public void testEnd() throws Exception
  {
    System.out.println("end");
    ObjectStringReader instance = new ObjectStringReaderImpl();
    assertNull(instance.end());
  }

  /**
   * Test of getHandlers method, of class ObjectStringReader.
   */
  @Test
  public void testGetHandlers()
  {
    System.out.println("getHandlers");
    ObjectStringReader instance = new ObjectStringReaderImpl();
    assertNull(instance.getHandlers());
  }

  /**
   * Test of createSchemaType method, of class ObjectStringReader.
   */
  @Test
  public void testCreateSchemaType() throws Exception
  {
    System.out.println("createSchemaType");
    ObjectStringReader instance = new ObjectStringReaderImpl();
    instance.createSchemaType(new SchemaBuilder());
  }

  @Reader.Declaration(pkg = TestSupport.TestPackage.class, name = "ObjectStringReaderImpl")
  public class ObjectStringReaderImpl extends ObjectStringReader
  {
    @Override
    public Object contents(String textContents) throws ReaderException
    {
      return null;
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