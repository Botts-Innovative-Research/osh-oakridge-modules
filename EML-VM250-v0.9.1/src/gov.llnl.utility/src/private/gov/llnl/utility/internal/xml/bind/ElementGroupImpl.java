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

import gov.llnl.utility.xml.bind.ElementGroup;
import gov.llnl.utility.annotation.Internal;
import gov.llnl.utility.xml.DomBuilder;
import gov.llnl.utility.xml.bind.Reader.Options;
import gov.llnl.utility.xml.bind.Reader.Order;
import java.util.EnumSet;

/**
 *
 * @author nelson85
 */
@Internal
public abstract class ElementGroupImpl implements ElementGroup
{
  EnumSet<Options> flags;
  private ElementGroupImpl parent;

  public ElementGroupImpl(ElementGroupImpl parent, EnumSet<Options> flags)
  {
    this.parent = parent;
    this.flags = flags;
  }

  public EnumSet<Options> getElementOptions()
  {
    return null;
  }

  /**
   * @return the parent
   */
  public ElementGroup getParent()
  {
    return parent;
  }

  public static ElementGroupImpl newInstance(ElementGroupImpl group, Order order, EnumSet<Options> options)
  {
    switch (order)
    {
      case ALL:
        return new AllGroup(group, options);
      case OPTIONS:
        return new OptionsGroup(group, options);
      case SEQUENCE:
        return new SequenceGroup(group, options);
      case CHOICE:
        return new ChoiceGroup(group, options);
      case FREE:
        return new FreeGroup(group, options);
      default:
        throw new RuntimeException("Unknown order " + order);
    }
  }

  @Override
  public DomBuilder createSchemaGroup(DomBuilder type)
  {
    DomBuilder group = createSchemaGroupImpl(type);
    SchemaBuilderUtilities.applyOptions(group, flags);
    return group;
  }

  public abstract DomBuilder createSchemaGroupImpl(DomBuilder type);

  @Internal
  public static class AllGroup extends ElementGroupImpl
  {
    public AllGroup(ElementGroupImpl parent, EnumSet<Options> flags)
    {
      super(parent, flags);
    }

    @Override
    public DomBuilder createSchemaGroupImpl(DomBuilder type)
    {
      return type.element("xs:all");
    }
  }

  @Internal
  public static class OptionsGroup extends ElementGroupImpl
  {
    public OptionsGroup(ElementGroupImpl parent, EnumSet<Options> flags)
    {
      super(parent, flags);
    }

    // Default is OPTIONAL
    @Override
    public EnumSet<Options> getElementOptions()
    {
      return EnumSet.of(Options.OPTIONAL);
    }

    @Override
    public DomBuilder createSchemaGroupImpl(DomBuilder type)
    {
      return type.element("xs:all");
    }
  }

  public static class SequenceGroup extends ElementGroupImpl
  {
    public SequenceGroup(ElementGroupImpl parent, EnumSet<Options> flags)
    {
      super(parent, flags);
    }

    @Override
    public DomBuilder createSchemaGroupImpl(DomBuilder type)
    {
      return type.element("xs:sequence");
    }
  }

  public static class ChoiceGroup extends ElementGroupImpl
  {
    public ChoiceGroup(ElementGroupImpl parent, EnumSet<Options> flags)
    {
      super(parent, flags);
    }

    @Override
    public DomBuilder createSchemaGroupImpl(DomBuilder type)
    {
      return type.element("xs:choice");
    }
  }

  public static class FreeGroup extends ElementGroupImpl
  {
    public FreeGroup(ElementGroupImpl parent, EnumSet<Options> flags)
    {
      super(parent, flags);
    }

    @Override
    public DomBuilder createSchemaGroupImpl(DomBuilder type)
    {
      return type.element("xs:choice")
              .attr("maxOccurs", "unbounded");
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