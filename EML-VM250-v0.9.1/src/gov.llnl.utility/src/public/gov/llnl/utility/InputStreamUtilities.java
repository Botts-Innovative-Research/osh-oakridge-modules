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

import gov.llnl.utility.internal.ByteArrayChannel;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SeekableByteChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utilities related to InputStreams.
 *
 * @author nelson85
 */
public class InputStreamUtilities
{
  private final static int BYTE_ARRAY_SIZE = 2048;

  /**
   * Reads all of the contents of an input stram into a byte array.
   *
   * @param inputStream is the stream to be read.
   * @return the contents of the stream.
   * @throws IOException If the stream cannot be read for any reason other than
   * the end of the file, if the input stream has been closed, or if some other
   * I/O error occurs.
   */
  public static byte[] readAllBytes(InputStream inputStream) throws IOException
  {
    byte[] buffer = new byte[1024];
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream())
    {
      while (true)
      {
        int len = inputStream.read(buffer);
        if (len == -1)
          break;
        baos.write(buffer, 0, len);
      }
      baos.flush();
      return baos.toByteArray();
    }
  }

  /**
   * Compute the checksum for an input stream.
   *
   * @param is
   * @return the checksum string.
   * @throws IOException if the stream could not be read.
   */
  public static String md5Checksum(InputStream is) throws IOException
  {
    MessageDigest md = null;
    try
    {
      md = MessageDigest.getInstance("MD5");
    }
    catch (NoSuchAlgorithmException ex)
    {
      throw new RuntimeException("Unable to find MD5 MessageDigest", ex);
    }
    byte[] bytes = new byte[BYTE_ARRAY_SIZE];
    int numBytes;
    while ((numBytes = is.read(bytes)) != -1)
    {
      md.update(bytes, 0, numBytes);
    }

    byte[] digest = md.digest();
    return HashUtilities.byteArrayToHexString(digest);
  }

  public static SeekableByteChannel newByteChannel(byte[] buffer)
  {
    return new ByteArrayChannel(buffer);
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