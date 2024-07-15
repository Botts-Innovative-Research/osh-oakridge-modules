/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.math;

import gov.llnl.utility.PackageResource;
import gov.llnl.utility.UtilityPackage;
import gov.llnl.utility.annotation.Singleton;
import gov.llnl.utility.xml.bind.Schema;
import java.util.logging.Logger;

/**
 *
 * @author nelson85
 */
@Singleton
@Schema(namespace = "http://math.llnl.gov",
        schema = "http://math.llnl.gov/schema/math.xsd",
        prefix = "math")
@Schema.Using(UtilityPackage.class)
public final class MathPackage extends PackageResource
{
  private static final MathPackage INSTANCE = new MathPackage();
  private static final Logger LOGGER = INSTANCE.getLogger();

  private MathPackage()
  {
  }

  static public MathPackage getInstance()
  {
    return INSTANCE;
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