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

import static gov.llnl.utility.InputStreamUtilities.md5Checksum;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.jar.Manifest;

/**
 * Collection of utilities to deal with getting the Jar file and package
 * information for classes.
 */
public class PackageUtilities
{
  static public URL getClassURL(Class<?> klass)
  {
    String name = klass.getName();
    String classPath = "/" + name.replace('.', '/') + ".class";
    return klass.getResource(classPath);
  }
  
  static private String getClassBaseURL(Class<?> klass)
  {
    String name = klass.getName();
    String classPath = name.replace('.', '/') + ".class";
    URL url = klass.getResource(classPath);
    if (url == null)
      url = klass.getClassLoader().getResource(classPath);
    if (url == null)
      throw new RuntimeException("unable to locate resource " + classPath);
    String urlString = url.toString();
    urlString = urlString.substring(0, urlString.length() - classPath.length());
    return urlString;
  }

  static public URL getJarURL(Class<?> klass)
  {
    try
    {
      URL url = getClassURL(klass);
      String urlString = url.toString();
      if (urlString.startsWith("jar:"))
      {
        urlString = urlString.replaceFirst("jar:", "");
        urlString = urlString.substring(0, urlString.indexOf("!"));
        return new URL(urlString);
      }
      return null;
    }
    catch (MalformedURLException ex)
    {
      return null;
    }
  }

  static public String getJarMd5Checksum(Class<?> klass) throws IOException
  {
    URL url = getClassURL(klass);
    if (url == null)
      return "none";
    try (InputStream is = url.openStream())
    {
      return md5Checksum(is);
    }
  }

  static public URL getManifestURL(Class<?> klass)
  {
    try
    {
      String urlString = getClassBaseURL(klass);
      urlString = urlString + "META-INF/MANIFEST.MF";
      return new URL(urlString);
    }
    catch (MalformedURLException ex)
    {
      return null;
    }
  }

  public static Manifest getManifest(Class<?> cl)
  {
    Manifest manifest;
    URL url = getManifestURL(cl);
    try (InputStream stream = url.openStream())
    {
      manifest = new Manifest(stream);
    }
    catch (IOException ex)
    {
      return null;
    }
    return manifest;
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