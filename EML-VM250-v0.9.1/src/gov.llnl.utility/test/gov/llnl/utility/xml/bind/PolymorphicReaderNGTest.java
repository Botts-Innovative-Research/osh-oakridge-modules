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
import gov.llnl.utility.TestSupport.TestPolyReader;
import gov.llnl.utility.TestSupport.TestReader;
import gov.llnl.utility.internal.xml.bind.readers.DoubleContents;
import gov.llnl.utility.internal.xml.bind.readers.IntegerContents;
import gov.llnl.utility.internal.xml.bind.readers.LongContents;
import gov.llnl.utility.internal.xml.bind.readers.PrimitiveReaderImpl;
import gov.llnl.utility.io.ReaderException;
import static org.testng.Assert.*;
import org.testng.annotations.Test;
import org.xml.sax.Attributes;

/**
 *
 * @author pham21
 */
public class PolymorphicReaderNGTest
{
  
  public PolymorphicReaderNGTest()
  {
  }

  /**
   * Test of getDeclaration method, of class PolymorphicReader.
   */
  @Test
  public void testGetDeclaration()
  {
    System.out.println("getDeclaration");
    PolymorphicReader instance = new PolymorphicReaderImpl();
    Reader.Declaration result = instance.getDeclaration();
    assertEquals(result.pkg().getName(), "gov.llnl.utility.TestSupport$TestPackage");
    assertEquals(result.name(), "PolymorphicReaderImpl");
    assertEquals(result.order(), Reader.Order.CHOICE);
  }

  /**
   * Test of getReaders method, of class PolymorphicReader.
   */
  @Test
  public void testGetReaders() throws Exception
  {
    System.out.println("getReaders");
    PolymorphicReader instance = new PolymorphicReaderImpl();
    ObjectReader[] readers = instance.getReaders();
    assertNotNull(readers);
    assertTrue(readers.length == 1);
    assertEquals(readers[0].getClass(), LongContents.class);
  }

  /**
   * Test of group method, of class PolymorphicReader.
   */
  @Test
  public void testGroup()
  {
    System.out.println("group");
    ObjectReader[] result = PolymorphicReader.group(
            new PolymorphicReaderImpl(),
            TestReader.of(String.class),
            new TestPolyReader()
    );
    assertEquals(result.length, 3);
    assertEquals(result[0].getClass(), PolymorphicReaderImpl.class);
    assertEquals(result[1].getClass(), TestReader.class);
    assertEquals(result[2].getClass(), TestPolyReader.class);
  }

  /**
   * Test of of method, of class PolymorphicReader.
   */
  @Test
  public void testOf() throws Exception
  {
    System.out.println("of");
    ObjectReader[] result = PolymorphicReader.of(Integer.class, Double.class, Long.class);
    assertTrue(result.length == 3);
    assertEquals(result[0].getClass(), IntegerContents.class);
    assertEquals(result[1].getClass(), DoubleContents.class);
    assertEquals(result[2].getClass(), LongContents.class);
    assertEquals(result[0].getObjectClass(), Integer.class);
    assertEquals(result[1].getObjectClass(), Double.class);
    assertEquals(result[2].getObjectClass(), Long.class);
  }

  /**
   * Test of start method, of class PolymorphicReader.
   */
  @Test
  public void testStart() throws Exception
  {
    System.out.println("start");
    PolymorphicReader instance = new PolymorphicReaderImpl();    
    assertNull(instance.start(null));
  }

  /**
   * Test of end method, of class PolymorphicReader.
   */
  @Test
  public void testEnd() throws Exception
  {
    System.out.println("end");
    PolymorphicReader instance = new PolymorphicReaderImpl();
    assertNull(instance.end());
    
    IntPolyReaderImpl ipri = new IntPolyReaderImpl();
    Integer obj = Integer.valueOf("0");
    ipri.object = obj;
    assertSame(ipri.end(), obj);
  }

  /**
   * Test of getHandlers method, of class PolymorphicReader.
   */
  @Test
  public void testGetHandlers() throws Exception
  {
    System.out.println("getHandlers");
    PolymorphicReader instance = new PolymorphicReaderImpl();
    assertNotNull(instance.getHandlers());
  }

  @Reader.Declaration(pkg = TestSupport.TestPackage.class, 
          name = "PolymorphicReaderImpl", cls = Long.class)  
  public class PolymorphicReaderImpl extends PolymorphicReader
  {
    public ObjectReader[] getReaders() throws ReaderException
    {
      return new ObjectReader[]{ new LongContents() };
    }
  }
  
  public class IntPolyReaderImpl<Integer> extends PolymorphicReader<Integer>
  {
    public ObjectReader[] getReaders() throws ReaderException
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