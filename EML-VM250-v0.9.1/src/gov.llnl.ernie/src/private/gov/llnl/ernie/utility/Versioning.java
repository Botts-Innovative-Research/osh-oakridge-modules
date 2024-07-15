/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.ernie.utility;

import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author seilhan3
 */
public class Versioning
{

  private static Logger logger = Logger.getLogger(Versioning.class.getName());

  public static String getVersionInfo()
  {
    Enumeration resEnum;
    String versionInfo = "v4-beta1";
    try
    {
      resEnum = Thread.currentThread().getContextClassLoader().getResources(JarFile.MANIFEST_NAME);
      int elemCount = 0;
      while (resEnum.hasMoreElements())
      {
        elemCount++;
        try
        {
          URL url = (URL) resEnum.nextElement();
          logger.fine("Attempting to open '" + url.toExternalForm() + "'");
          InputStream is = url.openStream();
          if (is != null)
          {
            Manifest manifest = new Manifest(is);
            Attributes mainAttribs = manifest.getMainAttributes();
            String version = mainAttribs.getValue("Implementation-Version");
            String name = mainAttribs.getValue("Implementation-Title");
            logger.fine("name = '" + name + "'");
            logger.fine("version = '" + version + "'");
            if (name != null && "ERNIE".equals(name.trim()))
            {
              if (version != null)
              {
                return version;
              }
            }
          }
          else
          {
            logger.fine("Could not open '" + url.toExternalForm() + "'");
          }
        }
        catch (Exception e)
        {
          logger.log(Level.WARNING, "", e);
        }
      }
    }
    catch (Exception e)
    {
      logger.log(Level.WARNING, "Caught exception reading manifest file", e);
    }

    return versionInfo;
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