/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.utility.internal.xml.bind;

import gov.llnl.utility.PackageResource;
import gov.llnl.utility.UtilityPackage;
import gov.llnl.utility.xml.bind.Reader;
import static org.testng.Assert.*;
import org.testng.annotations.Test;

/**
 *
 * @author pham21
 */
public class ReaderDeclarationImplNGTest
{

  public ReaderDeclarationImplNGTest()
  {
  }

  /**
   * Test of pkg method, of class ReaderDeclarationImpl.
   */
  @Test
  public void testPkg()
  {
    System.out.println("pkg");
    ReaderDeclarationImpl instance = new TestReaderDeclarationImpl();
    assertNull(instance.pkg());

    TestDeclarationBase tdb = new TestDeclarationBase();
    tdb.packageResoruce = UtilityPackage.getInstance();
    instance = new TestReaderDeclarationImpl(tdb);
    assertEquals(instance.pkg(), tdb.packageResoruce.getClass());
  }

  /**
   * Test of name method, of class ReaderDeclarationImpl.
   */
  @Test
  public void testName()
  {
    System.out.println("name");
    ReaderDeclarationImpl instance = new TestReaderDeclarationImpl();
    assertNull(instance.name());

    TestDeclarationBase tdb = new TestDeclarationBase();
    tdb.name = "name";
    instance = new TestReaderDeclarationImpl(tdb);
    assertEquals(instance.name(), tdb.name);
  }

  /**
   * Test of order method, of class ReaderDeclarationImpl.
   */
  @Test
  public void testOrder()
  {
    System.out.println("order");
    ReaderDeclarationImpl instance = new TestReaderDeclarationImpl();
    assertEquals(instance.order(), Reader.Order.FREE);

    TestDeclarationBase tdb = new TestDeclarationBase();
    tdb.order = Reader.Order.ALL;
    instance = new TestReaderDeclarationImpl(tdb);
    assertEquals(instance.order(), tdb.order);
  }

  /**
   * Test of referenceable method, of class ReaderDeclarationImpl.
   */
  @Test
  public void testReferenceable()
  {
    System.out.println("referenceable");
    ReaderDeclarationImpl instance = new TestReaderDeclarationImpl();
    assertFalse(instance.referenceable());

    TestDeclarationBase tdb = new TestDeclarationBase();
    instance = new TestReaderDeclarationImpl(tdb);
    assertTrue(instance.referenceable());
  }

  /**
   * Test of contentRequired method, of class ReaderDeclarationImpl.
   */
  @Test
  public void testContentRequired()
  {
    System.out.println("contentRequired");
    ReaderDeclarationImpl instance = new TestReaderDeclarationImpl();
    assertFalse(instance.contentRequired());

    TestDeclarationBase tdb = new TestDeclarationBase();
    instance = new TestReaderDeclarationImpl(tdb);
    assertTrue(instance.contentRequired());
  }

  /**
   * Test of contents method, of class ReaderDeclarationImpl.
   */
  @Test
  public void testContents()
  {
    System.out.println("contents");
    ReaderDeclarationImpl instance = new TestReaderDeclarationImpl();
    assertEquals(instance.contents(), Reader.Contents.ELEMENTS);

    TestDeclarationBase tdb = new TestDeclarationBase();
    instance = new TestReaderDeclarationImpl(tdb);
    assertEquals(instance.contents(), Reader.Contents.MIXED);
  }

  /**
   * Test of copyable method, of class ReaderDeclarationImpl.
   */
  @Test
  public void testCopyable()
  {
    System.out.println("copyable");
    ReaderDeclarationImpl instance = new TestReaderDeclarationImpl();
    assertFalse(instance.copyable());

    TestDeclarationBase tdb = new TestDeclarationBase();
    instance = new TestReaderDeclarationImpl(tdb);
    assertTrue(instance.copyable());
  }

  /**
   * Test of typeName method, of class ReaderDeclarationImpl.
   */
  @Test
  public void testTypeName()
  {
    System.out.println("typeName");
    ReaderDeclarationImpl instance = new TestReaderDeclarationImpl();
    assertNull(instance.typeName());

    TestDeclarationBase tdb = new TestDeclarationBase();
    instance = new TestReaderDeclarationImpl(tdb);
    assertEquals(instance.typeName(), "typename");
  }

  /**
   * Test of document method, of class ReaderDeclarationImpl.
   */
  @Test
  public void testDocument()
  {
    System.out.println("document");
    ReaderDeclarationImpl instance = new TestReaderDeclarationImpl();
    assertFalse(instance.document());

    TestDeclarationBase tdb = new TestDeclarationBase();
    instance = new TestReaderDeclarationImpl(tdb);
    assertTrue(instance.document());
  }

  /**
   * Test of autoAttributes method, of class ReaderDeclarationImpl.
   */
  @Test
  public void testAutoAttributes()
  {
    System.out.println("autoAttributes");
    ReaderDeclarationImpl instance = new TestReaderDeclarationImpl();
    assertFalse(instance.autoAttributes());

    TestDeclarationBase tdb = new TestDeclarationBase();
    instance = new TestReaderDeclarationImpl(tdb);
    assertTrue(instance.autoAttributes());
  }

  /**
   * Test of impl method, of class ReaderDeclarationImpl.
   */
  @Test
  public void testImpl()
  {
    System.out.println("impl");
    ReaderDeclarationImpl instance = new TestReaderDeclarationImpl();
    assertNull(instance.impl());

    TestDeclarationBase tdb = new TestDeclarationBase();
    instance = new TestReaderDeclarationImpl(tdb);
    assertEquals(instance.impl(), TestDeclarationBase.class);
  }

  /**
   * Test of cls method, of class ReaderDeclarationImpl.
   */
  @Test
  public void testCls()
  {
    System.out.println("cls");
    ReaderDeclarationImpl instance = new TestReaderDeclarationImpl();
    assertNull(instance.cls());

    TestDeclarationBase tdb = new TestDeclarationBase();
    instance = new TestReaderDeclarationImpl(tdb);
    assertEquals(instance.cls(), TestDeclarationBase.class);
  }

  /**
   * Test of annotationType method, of class ReaderDeclarationImpl.
   */
  @Test
  public void testAnnotationType()
  {
    System.out.println("annotationType");
    ReaderDeclarationImpl instance = new TestReaderDeclarationImpl();
    assertEquals(instance.annotationType(), Reader.Declaration.class);
  }

  class TestReaderDeclarationImpl extends ReaderDeclarationImpl
  {
    public TestReaderDeclarationImpl()
    {
      super();
    }

    public TestReaderDeclarationImpl(Reader.Declaration base)
    {
      super(base);

    }
  }

  class TestDeclarationBase extends ReaderDeclarationImpl
  {
    public PackageResource packageResoruce;
    public String name;
    public Reader.Order order;

    @Override
    public Class<? extends PackageResource> pkg()
    {
      return packageResoruce.getClass();
    }

    @Override
    public String name()
    {
      return name;
    }

    @Override
    public Reader.Order order()
    {
      return order;
    }

    @Override
    public boolean referenceable()
    {
      return true;
    }

    @Override
    public boolean contentRequired()
    {
      return true;
    }

    @Override
    public Reader.Contents contents()
    {
      return Reader.Contents.MIXED;
    }

    @Override
    public boolean copyable()
    {
      return true;
    }

    @Override
    public String typeName()
    {
      return "typename";
    }

    @Override
    public boolean document()
    {
      return true;
    }

    @Override
    public boolean autoAttributes()
    {
      return true;
    }

    @Override
    public Class impl()
    {
      return this.getClass();
    }

    @Override
    public Class cls()
    {
      return impl();
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