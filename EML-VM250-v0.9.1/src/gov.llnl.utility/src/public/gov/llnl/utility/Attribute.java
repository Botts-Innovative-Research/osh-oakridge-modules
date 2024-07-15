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

/**
 *
 * @author monterial1
 */
public interface Attribute<T> {
  public String getName();

  public T getDefaultValue();

  public Class<T> getAttributeClass();

  @SuppressWarnings("unchecked")
  public static <T2> Attribute<T2> of(String name, Class<T2> cls)
  {
    return new Attribute()
    {
      @Override
      public String getName()
      {
        return name;
      }

      @Override
      public Object getDefaultValue()
      {
        return null;
      }

      @Override
      public Class getAttributeClass()
      {
        return cls;
      }
    };
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