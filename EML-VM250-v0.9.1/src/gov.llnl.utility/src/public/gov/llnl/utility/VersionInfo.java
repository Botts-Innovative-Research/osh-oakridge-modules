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

import static gov.llnl.utility.PackageUtilities.getManifest;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 *
 * @author nelson85
 */
public class VersionInfo
{
  String specificationTitle;
  String specificationVersion;
  String implementationTitle;
  String implementationVersion;
  String builtDate;
  String builtBy;


  public VersionInfo()
  {
    this(VersionInfo.class);
  }

  public VersionInfo(Class cls)
  {
    Manifest manifest = getManifest(cls);
    if (manifest == null)
      return;
    Attributes attributes = manifest.getMainAttributes();
    specificationTitle = attributes.getValue("Specification-Title");
    specificationVersion = attributes.getValue("Specification-Version");
    implementationTitle = attributes.getValue("Implementation-Title");
    implementationVersion = attributes.getValue("Implementation-Version");
    builtDate = attributes.getValue("Built-Date");
    builtBy = attributes.getValue("Built-By");
  }

  public String getImplementationTitle()
  {
    return implementationTitle;
  }

  public String getVersion()
  {
    return specificationVersion;
  }

  public String getVersionString()
  {
    return String.format("%s(%s, %s)", implementationTitle, specificationVersion, builtDate);
  }

  @Override
  public String toString()
  {
    return getVersionString();
  }

  public static void main(String args[])
  {
    VersionInfo vi = new VersionInfo();
    System.out.println(vi.getVersion());
    System.out.println(vi.getVersionString());
  }
}

/*
Typical example Manifest:

  Name: java/util/
  Specification-Title: Java Utility Classes
  Specification-Version: 1.2
  Specification-Vendor: Example Tech, Inc.
  Implementation-Title: java.util
  Implementation-Version: build57
  Implementation-Vendor: Example Tech, Inc.
 */


/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */