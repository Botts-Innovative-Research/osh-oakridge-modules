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

import gov.llnl.utility.internal.xml.bind.SchemaBuilderUtilities;
import gov.llnl.utility.io.ReaderException;
import org.xml.sax.Attributes;

/**
 * Convenience class to serve as the base class for anything than needs text
 * content.
 *
 * FIXME: this class should be removed in favor of the annotation processor
 * which can verify that all of the required classes are created properly.
 *
 * <p>
 * Example:
 * <pre>
 * {@code
 *   public class FooReader extends ObjectStringReader<Foo>
 *   {
 *     public FooReader(Order order, int options, String name, SchemaResolver.PackageResource schema)
 *     {
 *       super(Order.ALL, Options2.NONE, "foo", MySchema.SCHEMA);
 *     }
 *
 *     public Object start(ReaderContext context, Attributes attributes) throws ReaderException
 *     {
 *       return new Foo();
 *     }
 *     public Foo contents(ReaderContext context, Foo object, String textContents) throws ReaderException
 *     {
 *        throw new UnsupportedOperationException("Not supported yet.");
 *     }
 *     public Class&lt;Foo&gt; getObjectClass()
 *     {
 *       return Foo.class;
 *     }
 *   }
 * }
 * </pre>
 *
 * @param <Component> is the class for objects produced by this Reader.
 */
public abstract class ObjectStringReader<Component>
        extends ObjectReader<Component>
{

  /**
   * {@inheritDoc}
   * <p>
   * start() is optional for classed derived from ObjectStringReader.
   * <p>
   */
  @Override
  public Component start(Attributes attributes) throws ReaderException
  {
    return null;
  }

  /**
   * {@inheritDoc}
   * <p>
   * All classes derived from ObjectStringReader must implement contents().
   */
  @Override
  abstract public Component contents(String textContents)
          throws ReaderException;

  /**
   * {@inheritDoc}
   * <p>
   * {@code end} is optional for ObjectStringReader.
   */
  @Override
  public Component end() throws ReaderException
  {
    return null;
  }

  /**
   * {@inheritDoc}
   * <p>
   * ObjectStringReader is for objects that do not contain child elements. All
   * classes derived from ObjectString may not implement getHandlers().
   */
  @Override
  final public ElementHandlerMap getHandlers()
  {
    return null;
  }

//<editor-fold desc="schema">
  @Override
  public void createSchemaType(SchemaBuilder builder) throws ReaderException
  {
    SchemaBuilderUtilities.createSchemaTypeDefaultString(this, builder);
  }

//</editor-fold>
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