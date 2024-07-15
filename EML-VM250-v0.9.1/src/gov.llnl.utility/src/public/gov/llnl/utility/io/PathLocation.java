/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.utility.io;

import java.net.URI;
import java.net.URL;

/**
 *
 * @author nelson85
 */
public class PathLocation
{
  String file;
  int lineNumber;
  String section;

  public PathLocation(URI file, int lineNumber, String sectionName)
  {
    this.file = file.toString();
    this.lineNumber = lineNumber;
    this.section = sectionName;
  }

  public PathLocation(URL file, int lineNumber, String sectionName)
  {
    this.file = file.toString();
    this.lineNumber = lineNumber;
    this.section = sectionName;
  }

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    if (file != null)
      sb.append(file);
    else
      sb.append("unknown");

    if (lineNumber > 0)
      sb.append(":").append(lineNumber);

    if (section != null)
      sb.append("#").append(section);
    return sb.toString();
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