/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.utility.internal;

import gov.llnl.utility.annotation.Internal;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;

/**
 *
 * @author nelson85
 */
@Internal
public class ByteArrayChannel implements SeekableByteChannel
{
  byte[] buffer;
  int location;

  public ByteArrayChannel(byte[] buffer)
  {
    this.buffer = buffer;
  }

  @Override
  public int read(ByteBuffer bb) throws IOException
  {
    int end = location + bb.remaining();
    if (end > buffer.length)
      end = buffer.length;
    int copied = end - location;
    bb.put(buffer, location, end - location);
    location = end;
    return copied;
  }

  @Override
  public int write(ByteBuffer bb) throws IOException
  {
    int end = location + bb.remaining();
    if (end > buffer.length)
      end = buffer.length;
    int copied = end - location;
    System.arraycopy(bb.array(), bb.position(), buffer, location, copied);
    return copied;
  }

  @Override
  public long position() throws IOException
  {
    return location;
  }

  @Override
  public SeekableByteChannel position(long l) throws IOException
  {
    this.location = (int) l;
    return this;
  }

  @Override
  public long size() throws IOException
  {
    return buffer.length;
  }

  @Override
  public SeekableByteChannel truncate(long l) throws IOException
  {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public boolean isOpen()
  {
    return true;
  }

  @Override
  public void close() throws IOException
  {
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