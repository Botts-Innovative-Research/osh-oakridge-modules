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
import org.xml.sax.Attributes;

/**
 * Wrapper type for passing both the contents and the attributes to a method
 * when using(this). This was replaced with a call pattern. It is now internal
 * code.
 *
 * <pre>
 * {@code
 *
 * builder.using(this).element("foo").call(MyReader::setFoo);
 * ...
 *
 * void setFoo(Attributes attr, String contents)
 *   {
 *     ...
 *   }
 * }
 * </pre>
 *
 * @author nelson85
 */
@Internal
class AttributeContents
{
  private Attributes attributes;
  String contents;

  AttributeContents(Attributes attributes)
  {
    this.attributes = attributes;
  }

  /**
   * @return the attributes
   */
  public Attributes getAttributes()
  {
    return attributes;
  }

  /**
   * @return the contents
   */
  public String getContents()
  {
    return contents;
  }

  /**
   * For an unknown reason the attributes can be destroyed while we are holding
   * on to them. Thus we will cache the values.
   */
  static class AttributesImpl implements Attributes
  {
    String[] namespace;
    String[] key;
    String[] value;

    AttributesImpl(Attributes attr)
    {
      int n = attr.getLength();
      namespace = new String[n];
      key = new String[n];
      value = new String[n];
      for (int i = 0; i < n; ++i)
      {
        namespace[i] = attr.getURI(i);
        key[i] = attr.getLocalName(i);
        value[i] = attr.getValue(i);
      }
    }

    @Override
    public int getLength()
    {
      return value.length;
    }

    @Override
    public String getURI(int i)
    {
      return namespace[i];
    }

    @Override
    public String getLocalName(int i)
    {
      return key[i];
    }

    @Override
    public String getQName(int i)
    {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getType(int i)
    {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getValue(int i)
    {
      return value[i];
    }

    @Override
    public int getIndex(String string, String string1)
    {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getIndex(String string)
    {
      for (int i = 0; i < key.length; ++i)
      {
        if (key[i].equals(string))
          return i;
      }
      return -1;
    }

    @Override
    public String getType(String string, String string1)
    {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getType(String string)
    {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getValue(String string, String string1)
    {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getValue(String string)
    {
      for (int i = 0; i < key.length; ++i)
      {
        if (key[i].equals(string))
          return value[i];
      }
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