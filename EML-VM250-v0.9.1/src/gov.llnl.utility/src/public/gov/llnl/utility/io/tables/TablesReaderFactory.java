/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.utility.io.tables;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

/**
 *
 * @author seilhan3
 */
public interface TablesReaderFactory<Reader extends TablesReader>
{
  /**
   * Set if there are column names in a header row. Do not skip the header if
   * the reader is to parse the header to get names.
   *
   * @param b
   */
  void setHasHeader(boolean b);

  /**
   * Skip number of marked or unmarked comment lines at top of file. Should not
   * include the header row if present.
   *
   * @param count
   */
  void setSkipRowCount(int count);

  /**
   * Define a pattern for skipping records that are marked as comments.
   *
   * @param pattern
   */
  void setCommentPattern(String pattern);

  /**
   * Open a file to create a reader.
   *
   * @param path
   * @return a new reader for the path.
   * @throws FileNotFoundException
   * @throws IOException
   */
  Reader openFile(Path path) throws FileNotFoundException, IOException;

  /**
   * Open a stream to create a reader.
   *
   * @param is
   * @return a new reader for the input stream.
   * @throws IOException
   */
  Reader openStream(InputStream is) throws IOException;

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