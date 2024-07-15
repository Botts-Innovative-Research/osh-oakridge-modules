/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.ernie.data;

import java.util.Map;

/**
 *
 * @author nelson85
 */
public interface AnalysisContextualInfo
{
  /**
   * Gets the ERNIE analysis version used to process this result.
   *
   * @return the ERNIE version.
   */
  String getVersionID();

  /**
   * Map containing settings related to the ERNIE analysis.
   *
   * These settings shall include all relevant thresholds used to make decisions
   * as well as the checksum for configuration files loaded into ERNIE.
   *
   * FIXME figure out how to properly compute the checksum for included files
   * including those files that are included during the load of the processor.
   * This may require a specialized DocumentLoader that checksums the document
   * and parses the imports. Alternatively, we may need to add an import hook to
   * the document loader with this responsiblity.
   *
   * @return
   */
  Map<String, String> getSettings();

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