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

import gov.llnl.utility.TestSupport;
import gov.llnl.utility.io.ReaderException;
import gov.llnl.utility.xml.bind.Reader;
import java.util.EnumSet;
import java.util.function.BiConsumer;
import javax.xml.XMLConstants;
import static org.testng.Assert.*;
import org.testng.annotations.Test;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXParseException;

/**
 *
 * @author pham21
 */
public class SaxHandlerNGTest
{

  public SaxHandlerNGTest()
  {
  }

  /**
   * Test of SaxHandler constructor, of class SaxHandler.
   */
  @Test
  public void testConstructor()
  {
    System.out.println("SaxHandler");
    SaxHandler instance = new SaxHandler(new ReaderContextImpl());
    try
    {
      instance = new SaxHandler(null);
    }
    catch (NullPointerException ne)
    {
      assertEquals(ne.getMessage(), "readerContext fail");
    }
  }

  /**
   * Test of setDocumentLocator method, of class SaxHandler.
   */
  @Test
  public void testSetDocumentLocator()
  {
    System.out.println("setDocumentLocator");
    Locator locator = new TestLocator();
    SaxHandler instance = new SaxHandler(new ReaderContextImpl());
    instance.setDocumentLocator(locator);
  }

  /**
   * Test of startPrefixMapping method, of class SaxHandler.
   */
  @Test
  public void testStartPrefixMapping() throws Exception
  {
    System.out.println("startPrefixMapping");
    SaxHandler instance = new SaxHandler(new ReaderContextImpl());
    // Empty body
    instance.startPrefixMapping(null, null);
  }

  /**
   * Test of endPrefixMapping method, of class SaxHandler.
   */
  @Test
  public void testEndPrefixMapping() throws Exception
  {
    System.out.println("endPrefixMapping");
    SaxHandler instance = new SaxHandler(new ReaderContextImpl());
    // Empty body
    instance.endPrefixMapping(null);
  }

  /**
   * Test of startElement method, of class SaxHandler.
   */
  @Test
  public void testStartElement() throws Exception
  {
    // Incomplete test, can't test class attributes for correct states
    System.out.println("startElement");

    String nameSpace = "first";
    String localName = "#";
    String key = "##first";

    org.xml.sax.helpers.AttributesImpl attributes = new org.xml.sax.helpers.AttributesImpl();

    // Setup required parts
    ReaderContextImpl rci = new ReaderContextImpl();
    rci.currentHandlerContext = new HandlerContextImpl();
    // Setup handler
    BiConsumer<Object, Object> doNothing = (foo, bar) -> System.out.print("");
    ElementHandlerImpl firstHandler = new ElementHandlerImpl(
            key, EnumSet.of(Reader.Options.ANY_ALL), "", String.class, doNothing);
    // Setup HandlerList
    StackTraceElement trace = new StackTraceElement("", "", "", 0);
    ReaderBuilderImpl.HandlerList handlerList = new ReaderBuilderImpl.HandlerList();
    handlerList.firstHandler = firstHandler;
    handlerList.trace = trace;
    // Set handlerMap
    ElementHandlerMapImpl handlerMap = ElementHandlerMapImpl.newInstance("#ElementHandler", handlerList);
    rci.currentHandlerContext.handlerMap = handlerMap;

    // skip if body
    SaxHandler instance = new SaxHandler(rci);
    instance.startElement(nameSpace, localName, "", attributes);

    // test if body
    rci = new ReaderContextImpl();
    rci.currentHandlerContext = new HandlerContextImpl();
    // Setup handler
    firstHandler = new ElementHandlerImpl(
            key, EnumSet.of(Reader.Options.ANY_ALL), "", String.class, doNothing);
    // Setup HandlerList
    handlerList = new ReaderBuilderImpl.HandlerList();
    handlerList.firstHandler = firstHandler;
    handlerList.trace = trace;
    // Set handlerMap
    handlerMap = ElementHandlerMapImpl.newInstance("#ElementHandler", handlerList);
    rci.currentHandlerContext.handlerMap = handlerMap;

    attributes.addAttribute(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI, "schemaLocation", "", "string", "somewhere");
    instance = new SaxHandler(rci);
    instance.startElement(nameSpace, localName, "", attributes);

    // Test SAXExceptionProxy inner ReaderException
    try
    {
      instance = new SaxHandler(new ReaderContextImpl());
      instance.startElement("", "", "", new org.xml.sax.helpers.AttributesImpl());
    }
    catch (SaxHandler.SAXExceptionProxy sp)
    {
      assertEquals(sp.exception.getClass(), ReaderException.class);
    }

    // Test SAXExceptionProxy inner NullPointerException
    try
    {
      instance = new SaxHandler(new ReaderContextImpl());
      instance.startElement(null, null, null, null);
    }
    catch (SaxHandler.SAXExceptionProxy sp)
    {
      assertEquals(sp.exception.getClass(), NullPointerException.class);
    }

  }

  /**
   * Test of endElement method, of class SaxHandler.
   */
  @Test
  public void testEndElement() throws Exception
  {
    System.out.println("endElement");
    // Setup required parts
    ReaderContextImpl rci = new ReaderContextImpl();
    rci.currentHandlerContext = new HandlerContextImpl();
    // Setup handler
    BiConsumer<Object, Object> doNothing = (foo, bar) -> System.out.print("");
    ElementHandlerImpl firstHandler = new ElementHandlerImpl(
            "", EnumSet.of(Reader.Options.ANY_ALL), "", String.class, doNothing);
    // Setup HandlerList
    StackTraceElement trace = new StackTraceElement("", "", "", 0);
    ReaderBuilderImpl.HandlerList handlerList = new ReaderBuilderImpl.HandlerList();
    handlerList.firstHandler = firstHandler;
    handlerList.trace = trace;
    // Set handlerMap
    ElementHandlerMapImpl handlerMap = ElementHandlerMapImpl.newInstance("#ElementHandler", handlerList);
    rci.currentHandlerContext.handlerMap = handlerMap;
    rci.currentHandlerContext.currentHandler = firstHandler;

    SaxHandler instance = new SaxHandler(rci);
    instance.endElement(null, null, null);

    // Test SAXExceptionProxy inner ReaderException
    try
    {
      firstHandler = new ElementHandlerImpl(
              "", EnumSet.of(Reader.Options.ANY_ALL), "", String.class, SaxHandlerNGTest::throwError);
      rci = new ReaderContextImpl();
      rci.currentHandlerContext = new HandlerContextImpl();
      rci.currentHandlerContext.currentHandler = firstHandler;
      instance = new SaxHandler(rci);
      instance.endElement(null, null, null);
    }
    catch (SaxHandler.SAXExceptionProxy sp)
    {
      assertEquals(sp.exception.getClass(), ReaderException.class);
    }
  }

  public static void throwError(Object o1, Object o2)
  {
    throw new RuntimeException("");
  }

  /**
   * Test of characters method, of class SaxHandler.
   */
  @Test
  public void testCharacters() throws Exception
  {
    System.out.println("characters");
    // Setup required parts
    String nameSpace = "first";
    String localName = "#";
    String key = "##first";
    org.xml.sax.helpers.AttributesImpl attributes = new org.xml.sax.helpers.AttributesImpl();
    ReaderContextImpl rci = new ReaderContextImpl();
    rci.currentHandlerContext = new HandlerContextImpl();
    // Setup handler
    BiConsumer<Object, Object> doNothing = (foo, bar) -> System.out.print("");
    ElementHandlerImpl firstHandler = new ElementHandlerImpl(
            key, EnumSet.of(Reader.Options.ANY_ALL), "", String.class, doNothing);
    // Setup HandlerList
    StackTraceElement trace = new StackTraceElement("", "", "", 0);
    ReaderBuilderImpl.HandlerList handlerList = new ReaderBuilderImpl.HandlerList();
    handlerList.firstHandler = firstHandler;
    handlerList.trace = trace;
    // Set handlerMap
    ElementHandlerMapImpl handlerMap = ElementHandlerMapImpl.newInstance("#ElementHandler", handlerList);
    rci.currentHandlerContext.handlerMap = handlerMap;

    SaxHandler instance = new SaxHandler(rci);
    instance.startElement(nameSpace, localName, "", attributes);
    instance.characters(nameSpace.toCharArray() , 0, nameSpace.length());
  }

  /**
   * Test of ignorableWhitespace method, of class SaxHandler.
   */
  @Test
  public void testIgnorableWhitespace() throws Exception
  {
    System.out.println("ignorableWhitespace");
    // empty body
    SaxHandler instance = new SaxHandler(new ReaderContextImpl());
    instance.ignorableWhitespace(null, -1, -1);
  }

  /**
   * Test of warning method, of class SaxHandler.
   */
  @Test(expectedExceptions = { SAXParseException.class })
  public void testWarning() throws Exception
  {
    System.out.println("warning");
    SAXParseException ex = new SAXParseException("", null);
    SaxHandler instance = new SaxHandler(new ReaderContextImpl());
    instance.warning(ex);
  }

  /**
   * Test of error method, of class SaxHandler.
   */
  @Test(expectedExceptions = { SAXParseException.class })
  public void testError() throws Exception
  {
    System.out.println("error");    
    SAXParseException ex = new SAXParseException("", null);
    SaxHandler instance = new SaxHandler(new ReaderContextImpl());
    instance.error(ex);
  }

  /**
   * Test of fatalError method, of class SaxHandler.
   */
  @Test(expectedExceptions = { SAXParseException.class })
  public void testFatalError() throws Exception
  {
    System.out.println("fatalError");    
    SAXParseException ex = new SAXParseException("", null);
    SaxHandler instance = new SaxHandler(new ReaderContextImpl());
    instance.fatalError(ex);
  }

  /**
   * Test of isValidate method, of class SaxHandler.
   */
  @Test
  public void testIsValidate()
  {
    System.out.println("isValidate");
    SaxHandler instance = new SaxHandler(new ReaderContextImpl());
    assertFalse(instance.isValidate());
  }

  /**
   * Test of setValidate method, of class SaxHandler.
   */
  @Test
  public void testSetValidate()
  {
    System.out.println("setValidate");
    SaxHandler instance = new SaxHandler(new ReaderContextImpl());
    instance.setValidate(true);
    assertTrue(instance.isValidate());
    instance.setValidate(false);
    assertFalse(instance.isValidate());
  }

  public class TestLocator implements Locator
  {
    @Override
    public String getPublicId()
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getSystemId()
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int getLineNumber()
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int getColumnNumber()
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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