/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.ernie.io;

import gov.llnl.ernie.data.Record;
import java.io.IOException;
import java.util.Iterator;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Produces a series of records for processing.
 *
 * Iterable is a bit abusive as we can only traverse the list once. But we don't
 * want interfaces to required RecordSteam were a List&lt;Record&gt; would also
 * work.
 *
 * @author nelson85
 */
public interface RecordStream extends Iterable<Record>, Supplier<Record>
{

  /**
   * Gets the next record from the source or returns null.
   *
   * @return the next record or null.
   */
  Record next() throws IOException;

  default Record get()
  {
    try
    {
      return next();
    }
    catch (IOException ex)
    {
      throw new RuntimeException(ex);
    }
  }

  /**
   * Get an iterator for this stream.
   *
   * This can only be traversed once as it will consume all the records in the
   * stream.
   *
   * @return
   */
  default Iterator<Record> iterator()
  {
    return new Iterator<Record>()
    {
      Record next_ = null;

      @Override
      public boolean hasNext()
      {
        if (next_ == null)
        {
          try
          {
            next_ = RecordStream.this.next();
          }
          catch (IOException ex)
          {
            throw new RuntimeException(ex);
          }
        }
        return next_ != null;
      }

      @Override
      public Record next()
      {
        Record out = next_;
        next_ = null;
        return next_;
      }
    };
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