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
import gov.llnl.utility.annotation.Internal;
import gov.llnl.utility.io.ReaderException;
import gov.llnl.utility.xml.DomBuilder;
import gov.llnl.utility.xml.bind.Reader;
import gov.llnl.utility.xml.bind.Reader.Options;
import gov.llnl.utility.xml.bind.SchemaBuilder;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.EnumSet;

/**
 *
 * @author nelson85
 */
@Internal
public class SchemaBuilderUtilities
{
  public static <Component> DomBuilder createSchemaTypeDefaultString(Reader<Component> reader, SchemaBuilder builder) throws ReaderException
  {
    // Create a type definition
    DomBuilder type = builder.getRoot()
            .element("xs:complexType")
            .attr("name", reader.getSchemaType());

    Reader.Declaration decl = reader.getDeclaration();
    Reader.TextContents textContents = reader.getTextContents();
    DomBuilder contents = type.element("xs:simpleContent")
            .element("xs:extension");
    if (textContents != null)
      contents.attr("base", textContents.base());
    else
      contents.attr("base", "xs:string");

//    if (decl.anyAttributes())
//      contents.element("xs:anyAttribute")
//              .attr("processContents", "skip");
//    else
    if (decl.referenceable())
      contents.element("xs:attributeGroup").attr("ref", "util:object-attribs");

//    // Attribs are defined in an included xsd file.
//    // Looks for ns:element-type-attribs
//    if (Reader.Options.Check.defineAttr(reader.getOptions()))
//      contents.element("xs:attributeGroup")
//              .attr("ref", SchemaBuilder.getXmlPrefix(reader)
//                      + reader.getSchemaType() + "-attribs");
    if (decl.copyable())
      contents.element("xs:attribute")
              .attr("name", "copy_of").attr("type", "xs:string");

    AttributesUtilities.createSchemaType(contents, reader.getAttributesDecl(), reader.getAnyAttributeDecl());
    return type;
  }

  public static <Component> DomBuilder createSchemaTypeDefault(Reader<Component> reader, SchemaBuilder builder) throws ReaderException
  {
    UtilityPackage.LOGGER.fine("Create schema type for " + reader.getSchemaType());

    // Create a type definition
    DomBuilder type = builder.getRoot()
            .element("xs:complexType")
            .attr("name", reader.getSchemaType());

    Reader.Declaration decl = reader.getDeclaration();
//    int options = reader.getOptions();

    if (decl.contents() == Reader.Contents.TEXT)
    {
      Reader.TextContents textContents = reader.getTextContents();
      type = type.element("xs:simpleContent")
              .element("xs:extension");
      if (textContents != null)
        type.attr("base", textContents.base());
      else
        type.attr("base", "xs:string");

    }
    else
    {
      if (decl.contents() == Reader.Contents.MIXED)
      {
        type.attr("mixed", "true");
      }

      // Add in definitions for each element
      Class cls = reader.getObjectClass();
      if (decl.impl() != null && !decl.impl().equals(void.class))
        cls = decl.impl();
      reader.setContext(builder.getReaderContext(cls));
      Reader.ElementHandlerMap handlers = reader.getHandlers();
      if (handlers != null)
      {
        handlers.createSchemaType(builder, type);
      }
    }

//    if (decl.referenceable())
//    {
//      type.attr("minOccurs","0");
//    }
    // Automatically set up attributes that are attributed
    Class objectClass = reader.getObjectClass();
    if (objectClass != null && decl.autoAttributes())
    {
      // sort methods before tyring to write xsd.
      Method[] methods = objectClass.getDeclaredMethods();
      Arrays.sort(methods,
              (Method o1, Method o2) -> o1.getName().compareToIgnoreCase(o2.getName()));
      for (Method method : methods)
      {
        Reader.Attribute attr = method.getAnnotation(Reader.Attribute.class);
        if (attr == null)
          continue;
        AttributeHandlers.declareAttribute(type, attr, method);
      }
    }

    // Add the standard attribs
    if (decl.referenceable())
      type.element("xs:attributeGroup").attr("ref", "util:object-attribs");

//    // Add optional attribs
//    if (decl.anyAttributes())
//      type.element("xs:anyAttribute").attr("processContents", "skip");
    if (decl.copyable())
      type.element("xs:attribute").attr("name", "copy_of").attr("type", "xs:string");

    AttributesUtilities.createSchemaType(type, reader.getAttributesDecl(), reader.getAnyAttributeDecl());

    return type;
  }

  public static <Component> DomBuilder createSchemaElementDefault(
          Reader<Component> reader,
          SchemaBuilder builder,
          String name,
          DomBuilder group,
          boolean topLevel)
          throws ReaderException
  {
    String ns = SchemaBuilder.getXmlPrefix(reader);

    // Otherwise reference the type
    DomBuilder out = group.element("xs:element")
            .attr("name", name);

    PackageResource resource = reader.getPackage();
    if (resource != null)
      ns = resource.getSchemaPrefix() + ":";
    String schematype = reader.getSchemaType();
    if (!schematype.isEmpty())
      out.attr("type", ns + schematype);

    if (topLevel)
    {
      String util = UtilityPackage.getInstance().getNamespaceURI();
      Class objectClass = reader.getObjectClass();
      if (objectClass != null)
        out.attrNS(util, "util:class", objectClass.getName());
    }

    return out;
  }

  public static <Obj> DomBuilder createSchemaElementSimple(Reader<Obj> reader, SchemaBuilder builder, String name, DomBuilder group, boolean topLevel)
  {
    DomBuilder out = group.element("xs:element").attr("name", name);
    Reader.Declaration decl = reader.getDeclaration();

    Reader.TextContents textContents = reader.getTextContents();
    if (textContents == null)
      throw new NullPointerException("text contents not set on " + reader.getClass());
    out.attr("type", textContents.base());

    if (topLevel)
    {
      String util = UtilityPackage.getInstance().getNamespaceURI();
      Class objectClass = reader.getObjectClass();
      if (objectClass != null)
        out.attrNS(util, "util:class", objectClass.getName());
    }

    return out;
  }

  static void applyOptions(DomBuilder group, EnumSet<Options> options)
  {
    if (options == null)
      return;

    for (Options ops : options)
    {
      if (ops.getKey() != null)
        group.attr(ops.getKey(), ops.getValue());
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