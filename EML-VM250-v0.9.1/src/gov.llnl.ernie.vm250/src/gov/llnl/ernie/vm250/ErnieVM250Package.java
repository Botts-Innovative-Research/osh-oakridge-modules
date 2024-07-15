/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */

package gov.llnl.ernie.vm250;

/* 
 * Copyright (c) 2016, Lawrence Livermore National Security, LLC.
 * All rights reserved.
 * 
 * Terms and conditions are given in "Notice" file.
 */
import gov.llnl.ernie.ErniePackage;
import gov.llnl.math.MathPackage;
import gov.llnl.utility.PackageResource;
import gov.llnl.utility.UtilityPackage;
import gov.llnl.utility.VersionInfo;
import gov.llnl.utility.xml.bind.Schema;
import java.util.logging.Logger;

/**
 *
 * @author nelson85
 */
@Schema(namespace = "http://ernie.llnl.gov/vm250",
        schema = "http://ernie.llnl.gov/vm250/schema/ernie-vm250.xsd",
        prefix = "vm250")
@Schema.Using(UtilityPackage.class)
@Schema.Using(MathPackage.class)
@Schema.Using(ErniePackage.class)
public class ErnieVM250Package extends PackageResource
{
  private static final ErnieVM250Package SELF = new ErnieVM250Package();
  public static final Logger LOGGER = SELF.getLogger();

  private ErnieVM250Package()
  {
  }

  static public ErnieVM250Package getInstance()
  {
    return SELF;
  }

  static public String getVersion()
  {
      VersionInfo vi = new VersionInfo(ErnieVM250Package.class);
      return vi.getVersion();
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