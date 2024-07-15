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

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;

/**
 *
 * @author nelson85
 * @param <Type> is the type of object to be stored.
 */
public interface DataStreamWriter<Type> extends Closeable, PropertyInterface
{
  public void openFile(Path path) throws IOException;

  /**
   * Get the next input in the stream.
   *
   * @param object is the object to be written.
   * @throws java.io.IOException if there is a problem writing the file.
   * @throws WriterException if there data cannot be formated for this file.
   */
  public void put(Type object) throws IOException, WriterException;

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