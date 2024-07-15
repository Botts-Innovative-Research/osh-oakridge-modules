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

import gov.llnl.utility.annotation.Internal;
import gov.llnl.utility.io.ReaderException;
import gov.llnl.utility.xml.bind.SchemaManager;
import java.util.function.Consumer;
import javax.xml.XMLConstants;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 * @author nelson85
 */
@Internal
public class SaxHandler extends DefaultHandler
{
  private final ReaderContextImpl readerContext;
  private HandlerContextImpl handlerContext;
  private Consumer<Attributes> consumer = null;
  private boolean validate = false;
  private Consumer handleLocator;

  public static class SAXExceptionProxy extends SAXException
  {
    public Exception exception;

    SAXExceptionProxy(Exception ex)
    {
      super(ex);
      exception = ex;
    }
  }

  public SaxHandler(ReaderContextImpl context)
  {
    if (context == null)
      throw new NullPointerException("readerContext fail");
    this.readerContext = context;
  }

  @Override
  public void setDocumentLocator(org.xml.sax.Locator locator)
  {
    super.setDocumentLocator(locator);
    readerContext.setLocator(locator);
  }

  @Override
  public void startPrefixMapping(String prefix, String uri) throws SAXException
  {
    boolean b = uri.equals(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI);
    if (b)
    {
      consumer = (p) -> this.handleSchema(p);
    }
  }

  void handleSchema(Attributes attr)
  {
    String includedSchema = attr.getValue(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI, "schemaLocation");
    if (includedSchema != null)
    {
      SchemaManagerImpl schemaMgr = (SchemaManagerImpl) SchemaManager.getInstance();
      schemaMgr.processSchemaLocation(includedSchema);
    }
  }

  @Override
  public void endPrefixMapping(String string) throws SAXException
  {
  }

  @Override
  public void startElement(String uri, String localName, String qualifiedName, Attributes attr)
          throws SAXException
  {
    try
    {
      if (consumer != null)
      {
        consumer.accept(attr);
        consumer = null;
      }
      this.handlerContext = readerContext.startElement(uri, localName, qualifiedName, attr);
    }
    catch (ReaderException | NullPointerException ex)
    {
      throw new SAXExceptionProxy(ex);
    }
  }

  @Override
  public void endElement(String uri, String localName, String qualifiedName)
          throws SAXException
  {
    try
    {
      this.handlerContext = readerContext.endElement();
    }
    catch (ReaderException ex)
    {
      throw new SAXExceptionProxy(ex);
    }
  }

  @Override
  public void characters(char[] chars, int start, int length) throws SAXException
  {
    this.handlerContext.characters(chars, start, length);
  }

  @Override
  public void ignorableWhitespace(char[] chars, int i, int i1) throws SAXException
  {
    // not used
  }

  @Override
  public void warning(SAXParseException ex) throws SAXException
  {
    throw ex;
  }

  @Override
  public void error(SAXParseException ex) throws SAXException
  {
    throw ex;
  }

  @Override
  public void fatalError(SAXParseException ex) throws SAXException
  {
    throw ex;
  }

  public boolean isValidate()
  {
    return validate;
  }

  public void setValidate(boolean validate)
  {
    this.validate = validate;
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