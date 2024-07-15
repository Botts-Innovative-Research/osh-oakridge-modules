/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gov.llnl.ernie.raven;

import java.lang.System.Logger.Level;
import java.time.Instant;
import java.util.LinkedList;
import java.util.logging.LogRecord;
import org.zeromq.SocketType;
import org.zeromq.ZMQ;
import java.util.logging.Logger;

/**
 *
 * @author nelson85
 */
public class Log implements Runnable
{
  private final ZMQ.Context context = ZMQ.context(1);
  private final String portDescription;
  private Thread thread;
  private int portNum = -1;
  private Lane[] lanes;
  LinkedList<LogRecord> records = new LinkedList<>();

  public Log(String port)
  {
    // Create a custom handler
    var myHandler = new java.util.logging.Handler()
    {
      @Override
      public void publish(LogRecord lr)
      {
        synchronized (records)
        {  
          records.push(lr);
          records.notify();
        }
      }

      @Override
      public void flush()
      {
      }

      @Override
      public void close() throws SecurityException
      {
      }
    };

    // Setup the Logger handler
    myHandler.setLevel(java.util.logging.Level.ALL);
    Main.LOGGER.addHandler(myHandler);
    // Setup the main logger so it'll send log messages via publishing channel
    // and doesn't print anything to console.
    Main.LOGGER.setLevel(java.util.logging.Level.ALL);
    Main.LOGGER.setUseParentHandlers(false);
   
    this.portDescription = port;
  }

  public synchronized void start()
  {
    thread = new Thread(this, "ERNIE Log");
    thread.setDaemon(true);
    thread.start();
  }

  synchronized public int getPortNum()
  {
    while (portNum == -1)
    {
      try
      {
        this.wait();
      }
      catch (InterruptedException ex)
      {
        throw new RuntimeException("Interrupted");
      }
    }
    return portNum;
  }

  @Override
  public void run()
  {
    try (ZMQ.Socket publish = context.socket(SocketType.PUB))
    {
      // Bind to a random port
      synchronized (this)
      {
        this.portNum = publish.bindToRandomPort(portDescription);
        this.notify();
      }

      while (true)
      {
        // Now we need to wait for meesages to happen on the logger and then publish them here.
        LogRecord record;
        synchronized (records)
        {

          // Uncomment if you want to test the log publishing log
          // Send 10 messages every 5 seconds
//          for (int i = 0; i < 10; ++i)
//          {
//            Thread.sleep(5000);
//            // Test send
//            LogProtos.LogMessage.Builder mesg = LogProtos.LogMessage.newBuilder();
//            mesg.setLevel(LogProtos.Level.valueOf(java.util.logging.Level.FINE.getName()));
//            mesg.setOrigin("Origin");
//            mesg.setMessage("Test Log Message");
//            Instant now = Instant.now();
//            mesg.setTimestamp(
//                    com.google.protobuf.Timestamp.newBuilder()
//                            .setSeconds(now.getEpochSecond())
//                            .setNanos(now.getNano())
//                            .build()
//            );
//            // One part message
//            publish.send(mesg.build().toByteArray());
//          }
          while (records.isEmpty())
          {
            records.wait();
          }
          record = records.removeFirst();
        }

        LogProtos.LogMessage.Builder mesg = LogProtos.LogMessage.newBuilder();
        mesg.setLevel(LogProtos.Level.valueOf(record.getLevel().getName()));
        mesg.setOrigin(record.getLoggerName());
        mesg.setMessage(record.getMessage());
        mesg.setTimestamp(
                com.google.protobuf.Timestamp.newBuilder()
                        .setSeconds(record.getInstant().getEpochSecond())
                        .setNanos(record.getInstant().getNano())
                        .build()
        );

        // One part message
        publish.send(mesg.build().toByteArray());
      }
    }
    catch (InterruptedException ex)
    {
      return;
    }
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