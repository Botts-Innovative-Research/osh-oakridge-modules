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

import gov.llnl.utility.TestSupport.TestElement;
import gov.llnl.utility.TestSupport.TestPackage;
import gov.llnl.utility.TestSupport.TestReaderContext;
import gov.llnl.utility.UtilityPackage;
import gov.llnl.utility.io.ReaderException;
import gov.llnl.utility.xml.DomBuilder;
import gov.llnl.utility.xml.bind.Reader.ProcessContents;
import java.util.EnumSet;
import static org.testng.Assert.*;
import org.testng.annotations.Test;
import org.xml.sax.Attributes;

/**
 *
 * @author pham21
 */
public class ReaderNGTest
{

  public ReaderNGTest()
  {
  }

  /**
   * Test of start method, of class Reader.
   */
  @Test
  public void testStart() throws Exception
  {
    System.out.println("start");
    Reader instance = new ReaderImpl();
    assertNull(instance.start(null));
  }

  /**
   * Test of contents method, of class Reader.
   */
  @Test
  public void testContents() throws Exception
  {
    System.out.println("contents");
    Reader instance = new ReaderImpl();
    assertNull(instance.contents(""));
  }

  /**
   * Test of end method, of class Reader.
   */
  @Test
  public void testEnd() throws Exception
  {
    System.out.println("end");
    Reader instance = new ReaderImpl();
    assertNull(instance.end());
  }

  /**
   * Test of getHandlers method, of class Reader.
   */
  @Test
  public void testGetHandlers() throws Exception
  {
    System.out.println("getHandlers");
    Reader instance = new ReaderImpl();
    assertNull(instance.getHandlers());
  }

  /**
   * Test of getObjectClass method, of class Reader.
   */
  @Test
  public void testGetObjectClass()
  {
    System.out.println("getObjectClass");
    Reader instance = new ReaderImpl();
    assertSame(instance.getObjectClass(), Reader.class);

    // Test RuntimeException
    ReaderVoidImpl rvi = new ReaderVoidImpl();
    try
    {
      rvi.getObjectClass();
    }
    catch (RuntimeException re)
    {
      assertEquals(re.getMessage(), "object class not defined " + rvi.getClass().getCanonicalName());
    }
  }

  /**
   * Test of getDeclaration method, of class Reader.
   */
  @Test
  public void testGetDeclaration()
  {
    System.out.println("getDeclaration");
    Reader instance = new ReaderImpl();
    Reader.Declaration result = instance.getDeclaration();
    assertNotNull(result);

    // Test NullPointerException
    ReaderNoDec rnd = new ReaderNoDec();
    String exceptionMsg = "";
    try
    {
      rnd.getDeclaration();
    }
    catch (NullPointerException ne)
    {
      exceptionMsg = ne.getMessage();
    }
    assertEquals(exceptionMsg, "Declaration not found for " + rnd.getClass());

    // Covered all Declaration 
    ReaderVoidImpl rv = new ReaderVoidImpl();
    result = rv.getDeclaration();
    assertEquals(result.pkg().getClass(), Class.class);
    assertEquals(result.name(), "ReaderVoidImpl");
    assertEquals(result.order(), Reader.Order.FREE);
    assertFalse(result.referenceable());
    assertFalse(result.contentRequired());
    assertEquals(result.contents(), Reader.Contents.ELEMENTS);
    assertFalse(result.copyable());
    assertEquals(result.typeName(), Reader.Declaration.NULL);
    assertTrue(result.document());
    assertEquals(Reader.Declaration.NULL, "");
    assertFalse(result.autoAttributes());
    assertEquals(result.cls(), void.class);
    assertEquals(result.impl(), void.class);
  }

  /**
   * Test of getXmlName method, of class Reader.
   */
  @Test
  public void testGetXmlName()
  {
    System.out.println("getXmlName");
    Reader instance = new ReaderImpl();
    assertEquals(instance.getXmlName(), "ReaderImpl");
  }

  /**
   * Test of getPackage method, of class Reader.
   */
  @Test
  public void testGetPackage()
  {
    System.out.println("getPackage");
    Reader instance = new ReaderImpl();
    assertEquals(instance.getPackage().getClass(), UtilityPackage.class);
    assertEquals(instance.getPackage(), UtilityPackage.SELF);
  }

  /**
   * Test of getHandlerKey method, of class Reader.
   */
  @Test
  public void testGetHandlerKey()
  {
    System.out.println("getHandlerKey");
    Reader instance = new ReaderImpl();
    assertEquals(instance.getHandlerKey(), "");
  }

  /**
   * Test of getSchemaType method, of class Reader.
   */
  @Test
  public void testGetSchemaType()
  {
    System.out.println("getSchemaType");
    Reader instance = new ReaderImpl();
    assertEquals(instance.getSchemaType(), "");
  }

  /**
   * Test of createSchemaType method, of class Reader.
   */
  @Test
  public void testCreateSchemaType() throws Exception
  {
    System.out.println("createSchemaType");
    SchemaBuilder builder = new SchemaBuilder();
    ReaderImpl instance = new ReaderImpl();
    instance.createSchemaType(builder);
    assertNotNull(instance.context);
  }

  /**
   * Test of createSchemaElement method, of class Reader.
   */
  @Test
  public void testCreateSchemaElement() throws Exception
  {
    System.out.println("createSchemaElement");
    SchemaBuilder builder = new SchemaBuilder();
    String name = "name";
    DomBuilder group = new DomBuilder(new TestElement("First"));
    boolean topLevel = true;
    ReaderImpl instance = new ReaderImpl();
    DomBuilder result = instance.createSchemaElement(builder, name, group, topLevel);
    assertNotNull(result);
    TestElement te = (TestElement) result.toElement();
    assertTrue(te.attrMap.size() > 0);
  }

  /**
   * Test of setContext method, of class Reader.
   */
  @Test
  public void testSetContext()
  {
    System.out.println("setContext");
    TestReaderContext context = new TestReaderContext();
    ReaderImpl instance = new ReaderImpl();
    instance.setContext(context);
    assertSame(instance.context, context);
  }

  /**
   * Test of getContext method, of class Reader.
   */
  @Test
  public void testGetContext()
  {
    System.out.println("getContext");
    ReaderImpl instance = new ReaderImpl();
    TestReaderContext context = new TestReaderContext();
    instance.setContext(context);
    assertSame(instance.getContext(), context);
  }

  /**
   * Test of getAttributesDecl method, of class Reader.
   */
  @Test
  public void testGetAttributesDecl()
  {
    System.out.println("getAttributesDecl");
    Reader instance = new ReaderAnyAttrib();

    // First if branch that uses AttributesDecl
    Reader.Attribute[] result = instance.getAttributesDecl();
    assertNotNull(result);
    assertTrue(result.length > 0);

    // Second if branch that uses Attribute
    ReaderVoidImpl rvi = new ReaderVoidImpl();
    result = rvi.getAttributesDecl();
    assertNotNull(result);
    assertTrue(result.length > 0);
    // Test Attribute
    Reader.Attribute attr = result[0];
    assertEquals(attr.name(), Reader.Attribute.NULL);
    assertSame(attr.type(), String.class);
    assertFalse(attr.required());
    assertEquals(Reader.Attribute.NULL, "##null");

    ReaderNoDec rnd = new ReaderNoDec();
    assertNull(rnd.getAttributesDecl());
  }

  /**
   * Test of getAnyAttributeDecl method, of class Reader.
   */
  @Test
  public void testGetAnyAttributeDecl()
  {
    System.out.println("getAnyAttributeDecl");
    Reader instance = new ReaderAnyAttrib();
    Reader.AnyAttribute result = instance.getAnyAttributeDecl();
    assertNotNull(result);
    assertEquals(result.id(), Reader.AnyAttribute.NULL);
    assertEquals(result.namespace(), "##any");
    assertEquals(result.processContents(), ProcessContents.Strict);
    assertEquals(Reader.AnyAttribute.NULL, "##null");
  }

  /**
   * Test of getTextContents method, of class Reader.
   */
  @Test
  public void testGetTextContents()
  {
    System.out.println("getTextContents");
    Reader instance = new ReaderAnyAttrib();
    Reader.TextContents result = instance.getTextContents();
    assertNotNull(result);
    assertEquals(result.base(), "xs:string");
  }

  /**
   * Test others in Reader
   */
  @Test
  public void testOthers() throws NoSuchFieldException, NoSuchMethodException
  {
    System.out.println("ElementDeclaration");
    TestDummy td = new TestDummy();
    Reader.ElementDeclaration ed = td.getClass().getDeclaredField("str").getAnnotation(Reader.ElementDeclaration.class);
    assertSame(ed.pkg(), TestPackage.class);
    assertEquals(ed.name(), "str");
    assertSame(ed.type(), String.class);

    System.out.println("NoSchema");
    Reader.NoSchema ns = td.getClass().getAnnotation(Reader.NoSchema.class);
    assertNotNull(ns);
    
    System.out.println("Element");
    Reader.Element e = td.getClass().getDeclaredMethod("method", null).getAnnotation(Reader.Element.class);
    assertNotNull(e);
    assertEquals(e.name(), "method");
    assertSame(e.type(), Reader.Element.NULL.class);
    assertFalse(e.any());
    assertFalse(e.required());
    assertFalse(e.unbounded());
    assertFalse(e.deferrable());
    Reader.Element.NULL nullObj = new Reader.Element.NULL();     
    
    // For coverage 
    Reader.Order order = Reader.Order.ALL;
    order = Reader.Order.OPTIONS;
    order = Reader.Order.SEQUENCE;
    order = Reader.Order.CHOICE;
    order = Reader.Order.FREE;
    Reader.Contents contents = Reader.Contents.NONE;
    contents = Reader.Contents.TEXT;
    contents = Reader.Contents.ELEMENTS;
    contents = Reader.Contents.MIXED;
    Reader.ProcessContents pc = Reader.ProcessContents.Strict;
    pc = Reader.ProcessContents.Lax;
    pc = Reader.ProcessContents.Skip;
  }
  
  /**
   * Test enum Options 
   */
  @Test
  public void testOptions()
  {
    System.out.println("enum Options");
    // test is really for coverage
    EnumSet<Reader.Options> flags = EnumSet.of(
            Reader.Options.OPTIONAL,
            Reader.Options.REQUIRED,
            Reader.Options.UNBOUNDED,
            Reader.Options.ANY_OTHER,
            Reader.Options.ANY_ALL,
            Reader.Options.ANY_SKIP,
            Reader.Options.ANY_STRICT,
            Reader.Options.ANY_LAX,
            Reader.Options.NO_REFERENCE,
            Reader.Options.DEFERRABLE,
            Reader.Options.NO_CACHE,
            Reader.Options.NO_ID
    );
    
    for(Reader.Options opt : flags)
    {
      opt.getKey();
      opt.getValue();
    }
  }

  // <editor-fold defaultstate="collapsed" desc="Support Classes">
  @Reader.Declaration(pkg = UtilityPackage.class,
          name = "ReaderImpl", cls = Reader.class)
  public class ReaderImpl implements Reader
  {
    public ReaderContext context;

    @Override
    public Object start(Attributes attributes) throws ReaderException
    {
      return null;
    }

    @Override
    public ElementHandlerMap getHandlers() throws ReaderException
    {
      return null;
    }

    @Override
    public String getHandlerKey()
    {
      return "";
    }

    @Override
    public String getSchemaType()
    {
      return "";
    }

    @Override
    public void setContext(ReaderContext context)
    {
      this.context = context;
    }

    @Override
    public ReaderContext getContext()
    {
      return context;
    }

  }

  @Reader.Declaration(pkg = UtilityPackage.class,
          name = "ReaderVoidImpl")
  @Reader.Attribute()
  public class ReaderVoidImpl implements Reader
  {
    @Override
    public Object start(Attributes attributes) throws ReaderException
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ElementHandlerMap getHandlers() throws ReaderException
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getHandlerKey()
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getSchemaType()
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setContext(ReaderContext context)
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ReaderContext getContext()
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
  }

  public class ReaderNoDec implements Reader
  {
    @Override
    public Object start(Attributes attributes) throws ReaderException
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ElementHandlerMap getHandlers() throws ReaderException
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getHandlerKey()
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getSchemaType()
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setContext(ReaderContext context)
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ReaderContext getContext()
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

  }

  @Reader.AttributesDecl(@Reader.Attribute)
  @Reader.TextContents()
  @Reader.AnyAttribute()
  public class ReaderAnyAttrib implements Reader
  {
    @Override
    public Object start(Attributes attributes) throws ReaderException
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ElementHandlerMap getHandlers() throws ReaderException
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getHandlerKey()
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getSchemaType()
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setContext(ReaderContext context)
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ReaderContext getContext()
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
  }

  @Reader.NoSchema()
  public class TestDummy
  {
    @Reader.ElementDeclaration(pkg = TestPackage.class, name = "str")
    String str = "str";

    @Reader.Element(name="method")
    public void method()
    {
    }
  }

  // </editor-fold>
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