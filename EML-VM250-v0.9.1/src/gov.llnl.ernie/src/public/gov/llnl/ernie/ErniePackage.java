/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.ernie;

import gov.llnl.math.MathPackage;
import gov.llnl.utility.PackageResource;
import gov.llnl.utility.UtilityPackage;
import gov.llnl.utility.xml.bind.Schema;
import java.util.logging.Logger;

/**
 *
 * @author nelson85
 */
@Schema(namespace = "http://ernie.llnl.gov",
        schema = "http://ernie.llnl.gov/schema/ernie.xsd",
        prefix = "ernie")
@Schema.Using(UtilityPackage.class)
@Schema.Using(MathPackage.class)
public class ErniePackage extends PackageResource
{
  private static final ErniePackage SELF = new ErniePackage();
  public static final Logger LOGGER = SELF.getLogger();

  private ErniePackage()
  {
  }

  static public ErniePackage getInstance()
  {
    return SELF;
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