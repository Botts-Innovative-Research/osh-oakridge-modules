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
import gov.llnl.utility.annotation.Internal;
import gov.llnl.utility.io.ReaderException;
import gov.llnl.utility.xml.bind.Reader;
import gov.llnl.utility.xml.bind.ReaderContext;
import gov.llnl.utility.xml.bind.SchemaBuilder;
import org.xml.sax.Attributes;

/**
 *
 * @author nelson85
 */
@Internal
public abstract class SectionImpl<Component> implements Reader.SectionInterface<Component>
{
  private ReaderContext context;

  @Override
  public Component start(Attributes attributes) throws ReaderException
  {
    return null;
  }

  @Override
  public Component contents(String textContents) throws ReaderException
  {
    return null;
  }

  @Override
  public Component end() throws ReaderException
  {
    return null;
  }

  @Override
  final public String getHandlerKey()
  {
    String name = getXmlName();
    // Using annotaion @Reader.Declaration on derived classes, 
    // name will be a constant expression and cannot be null.
    if (name == null)
      return null;
    // Using annotaion @Reader.Declaration on derived classes, 
    // resource will not be null. 
    PackageResource resource = this.getPackage();
    if (resource != null)
      return name + "#" + resource.getNamespaceURI();
    return name + "#";
  }

  final protected String getXmlPrefix()
  {
    // Using annotaion @Reader.Declaration on derived classes, 
    // resource will not be null. 
    PackageResource resource = this.getPackage();
    if (resource == null)
      return "";
    return resource.getSchemaPrefix() + ":";
  }

  @Override
  public String getSchemaType()
  {
    String className = this.getClass().getName();
    return className.replaceAll("[A-z]*\\.", "").replaceAll("[$]", "-") + "-type";
  }

  @Override
  final public void setContext(ReaderContext context)
  {
    if (this.context != null && this.context != context && context != null)
      throw new RuntimeException("reentrant issue ");
    this.context = context;
  }

  @Override
  public ReaderContext getContext()
  {
    return this.context;
  }

//<editor-fold desc="schema" defaultstate="collapsed">
  @Override
  public void createSchemaType(SchemaBuilder builder) throws ReaderException
  {
    SchemaBuilderUtilities.createSchemaTypeDefault(this, builder);
//    UtilityPackage.LOGGER.fine("Create schema type for " + this.getSchemaType());
//
//    // Create a type definition
//    DomBuilder type = builder.getRoot()
//            .element("xs:complexType")
//            .attr("name", this.getSchemaType());
//
//    Reader.Declaration decl = this.getDeclaration();
//
//    if (decl.contents() == Contents.TEXT)
//    {
//      type = type.element("xs:simpleContent")
//              .element("xs:extension").attr("base", "xs:string");
//
//      if (decl.anyAttributes())
//        type.element("xs:anyAttribute")
//                .attr("processContents", "skip");
//    }
//    else
//    {
//      if (decl.contents() == Contents.MIXED)
//      {
//        type.attr("mixed", "true");
//      }
//      // Add in definitions for each element
//      this.setContext(builder.getReaderContext(getObjectClass()));
//      ElementHandlerMap handlers = this.getHandlers();
//      if (handlers != null)
//      {
//        handlers.createSchemaType(builder, type);
//      }
//    }
//
//    // Automatically set up attributes that are attributed
//    Class<? extends Component> objectClass = getObjectClass();
//    if (objectClass != null && decl.autoAttributes())
//    {
//      // sort methods before tyring to write xsd.
//      Method[] methods = objectClass.getDeclaredMethods();
//      Arrays.sort(methods,
//              (Method o1, Method o2)
//              -> o1.getName().compareToIgnoreCase(o2.getName()));
//      for (Method method : methods)
//      {
//        Reader.Attribute attr = method.getAnnotation(Reader.Attribute.class);
//        if (attr == null)
//          continue;
//        AttributeHandlers.declareAttribute(type, attr, method);
//      }
//    }
//
//    // Add the standard attribs
//    if (decl.referenceable())
//      type.element("xs:attributeGroup").attr("ref", "util:object-attribs");
//
//    // Add optional attribs
//    if (decl.anyAttributes())
//      type.element("xs:anyAttribute").attr("processContents", "skip");
//
//// FIXME this was rarely used but may be useful.  Add back when we find the use case
////    // Attribs are defined in an included xsd file.  
////    // Looks for ns:element-type-attribs
////    if (Options.Check.defineAttr(getOptions()))
////      type.element("xs:attributeGroup")
////              .attr("ref", this.getXmlPrefix() + this.getSchemaType() + "-attribs");
//    AttributesUtilities.createSchemaType(type, this.getAttributesDecl());
//
////    return type;
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