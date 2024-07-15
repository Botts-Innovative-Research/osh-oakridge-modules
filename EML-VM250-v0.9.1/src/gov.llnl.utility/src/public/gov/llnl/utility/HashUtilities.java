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

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 *
 * @author nelson85
 */
public class HashUtilities
{
  public static long hash(int[] values)
  {
    if (values == null)
      return 0;

    MessageDigest md = null;
    try
    {
      md = MessageDigest.getInstance("MD5");
    }
    catch (NoSuchAlgorithmException ex)
    {
      throw new RuntimeException("Unable to find MD5 MessageDigest", ex);
    }

    ByteBuffer buffer = ByteBuffer.allocate(values.length * Integer.SIZE / 8);

    for (int i = 0; i < values.length; ++i)
    {
      buffer.putInt(values[i]);
    }
    buffer.rewind();
    md.update(buffer);
    byte[] digest = md.digest();
    ByteBuffer out = ByteBuffer.wrap(digest);
    return out.getLong();
  }

  public static String byteArrayToHexString(byte[] a)
  {
    StringBuilder sb = new StringBuilder(a.length * 2);
    for (byte b : a)
    {
      sb.append(String.format("%02x", b & 255));
    }
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