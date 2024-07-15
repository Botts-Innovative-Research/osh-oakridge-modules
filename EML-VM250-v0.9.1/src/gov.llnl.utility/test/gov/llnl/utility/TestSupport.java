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

import static gov.llnl.utility.ClassUtilities.INTEGER_PRIMITIVE;
import gov.llnl.utility.ClassUtilities.IntegerPrimitive;
import gov.llnl.utility.annotation.Internal;
import gov.llnl.utility.internal.xml.bind.SectionImpl;
import gov.llnl.utility.internal.xml.bind.readers.PrimitiveReaderImpl;
import gov.llnl.utility.io.PathLocation;
import gov.llnl.utility.io.ReaderException;
import gov.llnl.utility.xml.DomBuilder;
import gov.llnl.utility.xml.bind.DocumentReader;
import gov.llnl.utility.xml.bind.ElementGroup;
import gov.llnl.utility.xml.bind.ObjectReader;
import gov.llnl.utility.xml.bind.PolymorphicReader;
import gov.llnl.utility.xml.bind.PropertyMap;
import gov.llnl.utility.xml.bind.Reader;
import gov.llnl.utility.xml.bind.Reader.AnyHandler;
import gov.llnl.utility.xml.bind.ReaderContext;
import gov.llnl.utility.xml.bind.Schema;
import gov.llnl.utility.xml.bind.SchemaBuilder;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import org.w3c.dom.Attr;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Comment;
import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.DOMException;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.EntityReference;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;
import org.w3c.dom.TypeInfo;
import org.w3c.dom.UserDataHandler;
import org.xml.sax.Attributes;

/**
 * Contain static classes needed by other test
 *
 * @author pham21
 */
public class TestSupport
{
  // <editor-fold defaultstate="collapsed" desc="Class TestElement">
  public static class TestElement implements Element
  {
    public Document document;
    public String tagName = "";
    public List<Node> childrenList = new ArrayList<>();
    public Map<String, String> attrMap = new HashMap<>();

    public TestElement(String tagName)
    {
      document = new TestDocument();
      this.tagName = tagName;
    }

    @Override
    public String getTagName()
    {
      return tagName;
    }

    @Override
    public String getAttribute(String name)
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setAttribute(String name, String value) throws DOMException
    {
      attrMap.put(name, value);
    }

    @Override
    public void removeAttribute(String name) throws DOMException
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Attr getAttributeNode(String name)
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Attr setAttributeNode(Attr newAttr) throws DOMException
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Attr removeAttributeNode(Attr oldAttr) throws DOMException
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public NodeList getElementsByTagName(String name)
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getAttributeNS(String namespaceURI, String localName) throws DOMException
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setAttributeNS(String namespaceURI, String qualifiedName, String value) throws DOMException
    {
      attrMap.put(namespaceURI+":"+qualifiedName, value);
    }

    @Override
    public void removeAttributeNS(String namespaceURI, String localName) throws DOMException
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Attr getAttributeNodeNS(String namespaceURI, String localName) throws DOMException
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Attr setAttributeNodeNS(Attr newAttr) throws DOMException
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public NodeList getElementsByTagNameNS(String namespaceURI, String localName) throws DOMException
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean hasAttribute(String name)
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean hasAttributeNS(String namespaceURI, String localName) throws DOMException
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public TypeInfo getSchemaTypeInfo()
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setIdAttribute(String name, boolean isId) throws DOMException
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setIdAttributeNS(String namespaceURI, String localName, boolean isId) throws DOMException
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setIdAttributeNode(Attr idAttr, boolean isId) throws DOMException
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getNodeName()
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getNodeValue() throws DOMException
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setNodeValue(String nodeValue) throws DOMException
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public short getNodeType()
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Node getParentNode()
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public NodeList getChildNodes()
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Node getFirstChild()
    {
      return childrenList.get(0);
    }

    @Override
    public Node getLastChild()
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Node getPreviousSibling()
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Node getNextSibling()
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public NamedNodeMap getAttributes()
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Document getOwnerDocument()
    {
      return document;
    }

    @Override
    public Node insertBefore(Node newChild, Node refChild) throws DOMException
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Node replaceChild(Node newChild, Node oldChild) throws DOMException
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Node removeChild(Node oldChild) throws DOMException
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Node appendChild(Node newChild) throws DOMException
    {
      childrenList.add(newChild);
      return newChild;
    }

    @Override
    public boolean hasChildNodes()
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Node cloneNode(boolean deep)
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void normalize()
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isSupported(String feature, String version)
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getNamespaceURI()
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getPrefix()
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setPrefix(String prefix) throws DOMException
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getLocalName()
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean hasAttributes()
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getBaseURI()
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public short compareDocumentPosition(Node other) throws DOMException
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getTextContent() throws DOMException
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setTextContent(String textContent) throws DOMException
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isSameNode(Node other)
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String lookupPrefix(String namespaceURI)
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isDefaultNamespace(String namespaceURI)
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String lookupNamespaceURI(String prefix)
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isEqualNode(Node arg)
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Object getFeature(String feature, String version)
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Object setUserData(String key, Object data, UserDataHandler handler)
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Object getUserData(String key)
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

  }
  // </editor-fold>

  // <editor-fold defaultstate="collapsed" desc="Class TestDocument">
  public static class TestDocument implements Document
  {
    @Override
    public DocumentType getDoctype()
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public DOMImplementation getImplementation()
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Element getDocumentElement()
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Element createElement(String tagName) throws DOMException
    {
      return new TestElement(tagName);
    }

    @Override
    public DocumentFragment createDocumentFragment()
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Text createTextNode(String data)
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Comment createComment(String data)
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public CDATASection createCDATASection(String data) throws DOMException
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ProcessingInstruction createProcessingInstruction(String target, String data) throws DOMException
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Attr createAttribute(String name) throws DOMException
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public EntityReference createEntityReference(String name) throws DOMException
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public NodeList getElementsByTagName(String tagname)
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Node importNode(Node importedNode, boolean deep) throws DOMException
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Element createElementNS(String namespaceURI, String qualifiedName) throws DOMException
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Attr createAttributeNS(String namespaceURI, String qualifiedName) throws DOMException
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public NodeList getElementsByTagNameNS(String namespaceURI, String localName)
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Element getElementById(String elementId)
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getInputEncoding()
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getXmlEncoding()
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean getXmlStandalone()
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setXmlStandalone(boolean xmlStandalone) throws DOMException
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getXmlVersion()
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setXmlVersion(String xmlVersion) throws DOMException
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean getStrictErrorChecking()
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setStrictErrorChecking(boolean strictErrorChecking)
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getDocumentURI()
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setDocumentURI(String documentURI)
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Node adoptNode(Node source) throws DOMException
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public DOMConfiguration getDomConfig()
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void normalizeDocument()
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Node renameNode(Node n, String namespaceURI, String qualifiedName) throws DOMException
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getNodeName()
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getNodeValue() throws DOMException
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setNodeValue(String nodeValue) throws DOMException
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public short getNodeType()
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Node getParentNode()
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public NodeList getChildNodes()
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Node getFirstChild()
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Node getLastChild()
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Node getPreviousSibling()
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Node getNextSibling()
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public NamedNodeMap getAttributes()
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Document getOwnerDocument()
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Node insertBefore(Node newChild, Node refChild) throws DOMException
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Node replaceChild(Node newChild, Node oldChild) throws DOMException
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Node removeChild(Node oldChild) throws DOMException
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Node appendChild(Node newChild) throws DOMException
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean hasChildNodes()
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Node cloneNode(boolean deep)
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void normalize()
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isSupported(String feature, String version)
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getNamespaceURI()
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getPrefix()
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setPrefix(String prefix) throws DOMException
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getLocalName()
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean hasAttributes()
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getBaseURI()
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public short compareDocumentPosition(Node other) throws DOMException
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getTextContent() throws DOMException
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setTextContent(String textContent) throws DOMException
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isSameNode(Node other)
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String lookupPrefix(String namespaceURI)
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isDefaultNamespace(String namespaceURI)
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String lookupNamespaceURI(String prefix)
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isEqualNode(Node arg)
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Object getFeature(String feature, String version)
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Object setUserData(String key, Object data, UserDataHandler handler)
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Object getUserData(String key)
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
  }
  // </editor-fold>

  // <editor-fold defaultstate="collapsed" desc="Class TestElementHandler">
  public static class TestElementHandler implements Reader.ElementHandler
  {
    public Reader.ElementHandler nextHandler;
    public String key;
    public BiConsumer<Object, Object> method;
    public ElementGroup parentElementGroup;

    @Override
    public Reader getReader()
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void createSchemaElement(SchemaBuilder builder, DomBuilder type) throws ReaderException
    {
      // doing something
    }

    @Override
    public String getKey()
    {
      return key;
    }

    @Override
    public String getName()
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public BiConsumer getMethod()
    {
      return method;
    }

    @Override
    public EnumSet<Reader.Options> getOptions()
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Class getTargetClass()
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Reader.ElementHandler getNextHandler()
    {
      return nextHandler;
    }

    @Override
    public ElementGroup getParentGroup()
    {
      return parentElementGroup;
    }

    @Override
    public Object getParent(ReaderContext.HandlerContext context)
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean mustReference()
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean hasTextContent()
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
  }
  // </editor-fold>

  // <editor-fold defaultstate="collapsed" desc="Class TestAnyHandler">
  public static class TestAnyHandler implements AnyHandler
  {
    public Reader.ElementHandler nextHandler;
    public String key;
    public BiConsumer<Object, Object> method;
    public ElementGroup parentElementGroup;

    @Override
    public Reader.ElementHandler getHandler(String namespaceURI, String localName, String qualifiedName, Attributes attr) throws ReaderException
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Reader getReader()
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void createSchemaElement(SchemaBuilder builder, DomBuilder type) throws ReaderException
    {
      // Doing something
    }

    @Override
    public String getKey()
    {
      return key;
    }

    @Override
    public String getName()
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public BiConsumer getMethod()
    {
      return method;
    }

    @Override
    public EnumSet<Reader.Options> getOptions()
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Class getTargetClass()
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Reader.ElementHandler getNextHandler()
    {
      return nextHandler;
    }

    @Override
    public ElementGroup getParentGroup()
    {
      return parentElementGroup;
    }

    @Override
    public Object getParent(ReaderContext.HandlerContext context)
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean mustReference()
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean hasTextContent()
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
  }
  // </editor-fold>

  // <editor-fold defaultstate="collapsed" desc="Class TestElementGroup">
  public static class TestElementGroup implements ElementGroup
  {
    public ElementGroup parent;

    @Override
    public DomBuilder createSchemaGroup(DomBuilder type)
    {
      return type;
    }

    @Override
    public EnumSet<Reader.Options> getElementOptions()
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ElementGroup getParent()
    {
      return parent;
    }
  }
  // </editor-fold>

  // <editor-fold defaultstate="collapsed" desc="Class TestReader">
  @Reader.Declaration(pkg = TestPackage.class,
          name = "#TestReader", order = Reader.Order.SEQUENCE,
          referenceable = true, contents = Reader.Contents.TEXT,
          autoAttributes = true, typeName = "TestReaderType")
  static public class TestReader<T> extends ObjectReader<T>
  {
    public Class<T> cls;
    public T obj;

    /**
     * Used to build the schema.
     */
    @Internal
    TestReader()
    {
      this.cls = null;
    }

    private TestReader(Class<T> cls)
    {
      this.cls = cls;
    }

    static public <T> TestReader<T> of(Class<T> cls)
    {
      return new TestReader<>(cls);
    }

    @Override
    public T start(Attributes attributes) throws ReaderException
    {
      return obj;
    }

    @Override
    public T end() throws ReaderException
    {
      return obj;
    }

    @Override
    public ElementHandlerMap getHandlers() throws ReaderException
    {
      ReaderBuilder<T> builder = this.newBuilder();
      builder.using(this)
              .anyElement(cls)
              .call(TestReader::setObj);
      return builder.getHandlers();
    }

    @Override
    public Class getObjectClass()
    {
      return cls;
    }

    public void setObj(T obj)
    {
      this.obj = obj;
    }

    @Override
    public T contents(String textContents) throws ReaderException
    {
      return obj;
    }

    @Override
    public DomBuilder createSchemaElement(SchemaBuilder builder, String name, DomBuilder group, boolean topLevel) throws ReaderException
    {
      return group;
    }

  }
  // </editor-fold>

  // <editor-fold defaultstate="collapsed" desc="Class TestReaderMixed">
  @Reader.Declaration(pkg = TestPackage.class,
          name = "TestReader", order = Reader.Order.SEQUENCE,
          contentRequired = true, contents = Reader.Contents.MIXED)
  static public class TestReaderMixed<T> extends ObjectReader<T>
  {
    Class<T> cls;
    T obj;

    /**
     * Used to build the schema.
     */
    @Internal
    TestReaderMixed()
    {
      this.cls = null;
    }

    private TestReaderMixed(Class<T> cls)
    {
      this.cls = cls;
    }

    static public <T> TestReaderMixed<T> of(Class<T> cls)
    {
      return new TestReaderMixed<>(cls);
    }

    @Override
    public T start(Attributes attributes) throws ReaderException
    {
      obj = null;
      return null;
    }

    @Override
    public T end() throws ReaderException
    {
      return obj;
    }

    @Override
    public ElementHandlerMap getHandlers() throws ReaderException
    {
      ReaderBuilder<T> builder = this.newBuilder();
      builder.using(this)
              .anyElement(cls)
              .call(TestReaderMixed::setObj);
      return builder.getHandlers();
    }

    @Override
    public Class getObjectClass()
    {
      return cls;
    }

    void setObj(T obj)
    {
      this.obj = obj;
    }

  }
  // </editor-fold>

  // <editor-fold defaultstate="collapsed" desc="Class TestSection">
  @Reader.Declaration(pkg = UtilityPackage.class,
          name = "TestSection", contents = Reader.Contents.TEXT)
  static public class TestSection implements Reader.SectionInterface
  {
    public ReaderContext readerContext;
    public Attributes attributes;
    public ElementHandlerMap elementHandlerMap;

    @Override
    public Object start(Attributes attributes) throws ReaderException
    {
      this.attributes = attributes;
      return null;
    }

    @Override
    public Object contents(String textContents) throws ReaderException
    {
      return null;
    }

    @Override
    public Object end() throws ReaderException
    {
      return null;
    }

    @Override
    public Object getObject()
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ElementHandlerMap getHandlers() throws ReaderException
    {
      return elementHandlerMap;
    }

    @Override
    public String getHandlerKey()
    {
      return "TestSection";
    }

    @Override
    public String getSchemaType()
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setContext(ReaderContext context)
    {
      readerContext = context;
    }

    @Override
    public ReaderContext getContext()
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public DomBuilder createSchemaElement(SchemaBuilder builder, String name, DomBuilder group, boolean topLevel) throws ReaderException
    {
      return group;
    }

  }
  // </editor-fold>

  // <editor-fold defaultstate="collapsed" desc="Class TestReaderContext">
  static public class TestReaderContext implements ReaderContext
  {
    public HandlerContext handlerContext;

    @Override
    public DocumentReader getDocumentReader()
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Object getLastObject()
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public <T> T get(String name, Class<T> cls) throws ReaderException
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Iterable<Map.Entry<String, Object>> getReferences()
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public <Obj> Obj put(String name, Obj object) throws ReaderException
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public <Obj> Obj putScoped(String name, Obj object) throws ReaderException
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getElementPath()
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public URL getExternal(String extern) throws ReaderException
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public URI getFile()
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public PathLocation getLocation()
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public HandlerContext getCurrentHandlerContext()
    {
      return handlerContext;
    }

    @Override
    public HandlerContext getLastHandlerContext()
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setErrorHandler(ExceptionHandler handler)
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void handleException(Throwable ex) throws ReaderException
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setPropertyHandler(PropertyMap handler)
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public <T, T2> void addDeferred(T target, BiConsumer<T, T2> method, String refId, Class<T2> cls) throws ReaderException
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

  }
  // </editor-fold>

  // <editor-fold defaultstate="collapsed" desc="Class TestPolyReader">
  @Reader.Declaration(pkg = TestPackage.class,
          name = "TestPolyReader", contents = Reader.Contents.TEXT,
          cls = String.class)
  static public class TestPolyReader extends PolymorphicReader<String>
  {
    @Override
    public ObjectReader<? extends String>[] getReaders() throws ReaderException
    {
      return group(new TestReader(String.class));
    }

  }
  // </editor-fold>

  // <editor-fold defaultstate="collapsed" desc="Class TestPackage">
  @Schema(namespace = "TestPackage",
          schema = "http://utility.llnl.gov/schema/utility.xsd",
          prefix = "util")
  static public class TestPackage extends PackageResource
  {
    public static final TestPackage SELF = new TestPackage();
    public static final String namespace = "TestPackage";

    private TestPackage()
    {
    }

    public static TestPackage getInstance()
    {
      return SELF;
    }

    @Override
    public String getNamespaceURI()
    {
      return namespace;
    }
  }
  // </editor-fold>

  // <editor-fold defaultstate="collapsed" desc="Class TestSectionImpl">
  @Reader.Declaration(pkg = TestSectionImplPackage.class,
          name = "TestSectionImpl", order = Reader.Order.CHOICE,
          cls = Double.class)
  static public class TestSectionImpl extends SectionImpl
  {
    @Override
    public Object getObject()
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Reader.ElementHandlerMap getHandlers() throws ReaderException
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
  }
  // </editor-fold>

  // <editor-fold defaultstate="collapsed" desc="Class TestSectionImplPackage">
  @Schema(namespace = "http://utility.llnl.gov",
          schema = "http://utility.llnl.gov/schema/utility.xsd",
          prefix = "TestSectionPrefix")
  public static class TestSectionImplPackage extends PackageResource
  {
    public static final TestSectionImplPackage SELF = new TestSectionImplPackage();

    private TestSectionImplPackage()
    {
    }

    public static TestSectionImplPackage getInstance()
    {
      return SELF;
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