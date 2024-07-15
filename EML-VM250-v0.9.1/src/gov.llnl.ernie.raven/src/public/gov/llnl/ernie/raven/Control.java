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

import com.google.protobuf.InvalidProtocolBufferException;
import gov.llnl.ernie.vm250.ErnieVM250Package;
import org.zeromq.SocketType;
import org.zeromq.ZMQ;
import java.util.logging.Level;

/**
 *
 * @author nelson85
 */
public class Control implements Runnable
{
  private final ZMQ.Context context = ZMQ.context(1);
  private final String portDescription;
  Thread thread;
  private int portNum = -1;
  private Lane[] lanes;
  private int logPort = -1;

  static String CONFIG_REQUEST = ControlProtos.ConfigureRequest.class.getSimpleName();

  public Control(String port)
  {
    this.portDescription = port;
  }

  public synchronized void start()
  {
    thread = new Thread(this, "ERNIE Raven Control");
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
    boolean shouldRun = true;
    try (ZMQ.Socket receiver = context.socket(SocketType.REP))
    {
      synchronized (this)
      {
        this.portNum = receiver.bindToRandomPort(portDescription);
        this.notify();
      }

      ZMQ.Poller poller = context.poller();
      ZMQ.PollItem item = new ZMQ.PollItem(receiver, ZMQ.Poller.POLLIN);
      poller.register(item);
      Main.LOGGER.log(Level.INFO, "Listen on control channel " + portNum);

      while (shouldRun && !Thread.interrupted())
      {
        // FIXME add watch dog logic for hung commands
        Main.LOGGER.log(Level.INFO, "Wait command request");

        poller.poll(5000); // Timeout in order to check whether interrupted.
        if (poller.pollin(0))
        {
          String cmdType = receiver.recvStr();
          byte[] cmdBytes = receiver.recv();

          String responseType = "";
          byte[] responseBytes = null;

          try
          {
            Main.LOGGER.log(Level.INFO, "Got command request " + cmdType);

            // Configure Request
            if (cmdType.equals(ControlProtos.ConfigureRequest.class.getSimpleName()))
            {
              // Check if lanes are already configured, if so then send an error
              ControlProtos.ConfigureRequest request = ControlProtos.ConfigureRequest.parseFrom(cmdBytes);

              if (this.lanes != null)
              {
                ControlProtos.ErrorResponse.Builder response = ControlProtos.ErrorResponse.newBuilder();
                response.setMessage("Lanes was already configured");
                responseType = ControlProtos.ErrorResponse.class.getSimpleName();
                responseBytes = response.build().toByteArray();
              }
              else
              {
                ControlProtos.ConfigureResponse.Builder response = ControlProtos.ConfigureResponse.newBuilder();

                this.lanes = new Lane[request.getLanes()];
                for (int i = 0; i < this.lanes.length; ++i)
                {
                  this.lanes[i] = new Lane();
                  this.lanes[i].start();
                }

                // Collect the port numbers assigned
                for (int i = 0; i < this.lanes.length; ++i)
                {
                  response.addPort(this.lanes[i].getPortNum());
                }

                responseType = ControlProtos.ConfigureResponse.class.getSimpleName();
                responseBytes = response.build().toByteArray();
              }
            }

            // Status Request
            if (cmdType.equals(ControlProtos.StatusRequest.class.getSimpleName()))
            {
              // Create the status response 
              ControlProtos.StatusResponse.Builder response
                      = ControlProtos.StatusResponse.newBuilder();
              response.setVersion(ErnieVM250Package.getVersion());
              response.setControlPort(portNum);
              response.setLogPort(logPort);

              // Lane Status 
              if (lanes != null)
              {
                for (int i = 0; i < this.lanes.length; ++i)
                {
                  ControlProtos.LaneStatus.Builder laneStatusBuilder
                          = ControlProtos.LaneStatus.newBuilder();
                  laneStatusBuilder.setPort(this.lanes[i].getPortNum());
                  laneStatusBuilder.setMessages(i);
                  laneStatusBuilder.setLastScanId(this.lanes[i].getLastScanId());

                  response.addLaneStatus(laneStatusBuilder.build());
                }
              }
              else
              {
                // There's no lane data.
                ControlProtos.LaneStatus.Builder laneStatusBuilder
                        = ControlProtos.LaneStatus.newBuilder();
                laneStatusBuilder.setPort(-1);
                laneStatusBuilder.setMessages(-1);
                laneStatusBuilder.setLastScanId(-1);
                response.addLaneStatus(laneStatusBuilder.build());
              }

              responseType = ControlProtos.StatusResponse.class.getSimpleName();
              responseBytes = response.build().toByteArray();
            }

            // Terminate Request
            if (cmdType.equals(ControlProtos.TerminateRequest.class.getSimpleName()))
            {
              responseType = ControlProtos.TerminateResponse.class.getSimpleName();
              responseBytes = ControlProtos.TerminateResponse.newBuilder().build().toByteArray();
              shouldRun = false;
            }
          }

          // All errors are redirected here.  (We must produce a response)
          catch (InvalidProtocolBufferException ex)
          {
            Main.LOGGER.log(Level.SEVERE, "Exception in Control", ex);
            ControlProtos.ErrorResponse.Builder error = ControlProtos.ErrorResponse.newBuilder();
            error.setMessage("Exception: " + ex.getMessage());

            responseType = ControlProtos.ErrorResponse.class.getSimpleName();
            responseBytes = error.build().toByteArray();
          }

          // Send to a multipart message back
          receiver.sendMore(responseType);
          receiver.send(responseBytes);

          // If we're terminating, wait 1 seconds to give time for the
          // terminate response to be sent, so the client connection does not 
          // hang
          if (shouldRun == false)
          {
            try
            {
              Thread.sleep(1000);
            }
            catch (InterruptedException ex)
            {
              // Fail so just do old school loop
              for (int i = 0; i < 1000000000; ++i)
              {
                // Used up CPU cycle
              }
            }
          }

        }
      }

      Main.LOGGER.log(Level.INFO, "Command thread stop");
    }
  }

  /**
   * Stop internally call stop message on the control.
   */
  public void stop()
  {
    if (thread == null)
      return;

    // Send a zero byte message to wake up the poller
    ZMQ.Socket sender = context.socket(SocketType.REQ);
    sender.connect(portDescription + ":" + portNum);
    sender.sendMore(ControlProtos.TerminateRequest.class.getSimpleName());
    sender.send(new byte[0]);  // no need to send any contents for now

    // Wait for completion
    try
    {
      thread.join();
    }
    catch (InterruptedException ex)
    {
      Main.LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
    }
    thread = null;
  }

  public void setLogPort(int port)
  {
    logPort = port;
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