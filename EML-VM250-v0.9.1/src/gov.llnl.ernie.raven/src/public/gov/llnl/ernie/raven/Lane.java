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
import gov.llnl.ernie.analysis.AnalysisException;
import gov.llnl.ernie.api.ERNIE_lane;
import gov.llnl.ernie.api.Results;
import gov.llnl.ernie.vm250.ErnieVM250Package;
import gov.llnl.utility.io.ReaderException;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.stream.Stream;
import javax.xml.bind.JAXBException;
import org.zeromq.SocketType;
import org.zeromq.ZMQ;

/**
 *
 * @author nelson85
 */
public class Lane implements Runnable
{
  private final ZMQ.Context context = ZMQ.context(1);
  private Thread thread;
  private int portNum = -1;
  private ERNIE_lane ernieLane;
  private int lastScanID = -1;
  private int totalRequests = 0;
  private int totalGoodResponses = 0;
  private int totalErrorResponses = 0;

  public synchronized void start()
  {
    thread = new Thread(this, "ERNIE Lane");
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

  /**
   * Run ERNIE for each lane.
   */
  public void run()
  {
    try (ZMQ.Socket receiver = context.socket(SocketType.REP))
    {
      synchronized (this)
      {
        this.portNum = receiver.bindToRandomPort("tcp://localhost");
        this.notify();
      }

      ZMQ.Poller poller = context.poller();
      ZMQ.PollItem item = new ZMQ.PollItem(receiver, ZMQ.Poller.POLLIN);
      poller.register(item);
      Main.LOGGER.log(Level.INFO, "Listen on lane " + portNum);

      while (!Thread.interrupted())
      {
        // FIXME add watch dog logic for hung commands
        Main.LOGGER.log(Level.INFO, "Wait lane request");

        poller.poll(5000); // Timeout in order to check whether interrupted.
        if (poller.pollin(0))
        {
          // Multipart message:  String, bytes[]
          String cmdType = receiver.recvStr();
          byte[] cmdBytes = receiver.recv();

          String responseType = "";
          byte[] responseBytes = null;

          try
          {
            if (cmdType.equals(LaneProtos.LaneConfigureRequest.class.getSimpleName()))
            {
              LaneProtos.LaneConfigureRequest request = LaneProtos.LaneConfigureRequest.parseFrom(cmdBytes);

              // Load up an EML-VM250 lane instance based on the lane configuration
              // If the lane is already configured delete the old one first.
              ernieLane = new ERNIE_lane(
                      Integer.toString(request.getSiteId()),
                      request.getLaneId(),
                      request.getCollimated(),
                      request.getLaneWidth(),
                      request.getIntervals(),
                      request.getHoldin());
              
              // Create the response
              LaneProtos.LaneConfigureResponse.Builder response = LaneProtos.LaneConfigureResponse.newBuilder();
              response.setVersion(ErnieVM250Package.getVersion());

              // Generate the reader
              // Tell reader to generate the MD5 checksum
              // Get that MD5 checkum 
              // FIXME Properly create the MD5 map
              // Right now just create the MD5 from a string
              try
              {
                MessageDigest md = MessageDigest.getInstance("MD5");
                byte[] theDigest = md.digest(ernieLane.getClass().getName().getBytes("UTF-8"));

                // Convert byte[] to hex string
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < theDigest.length; ++i)
                {
                  String hex = Integer.toHexString(0xFF & theDigest[i]);
                  if (hex.length() == 1)
                  {
                    sb.append('0');
                  }
                  sb.append(hex);
                }

                response.putMd5Map("MD5", sb.toString());  
              }
              catch (NoSuchAlgorithmException | UnsupportedEncodingException ex)
              {
                // Failed so just create one 
                response.putMd5Map("MD5", ((Integer) cmdType.hashCode()).toString());
              }

              responseType = LaneProtos.LaneConfigureResponse.class.getSimpleName();
              responseBytes = response.build().toByteArray();
              
              // Update good response counter
                ++totalGoodResponses;
            }

            if (cmdType.equals(LaneProtos.ProcessScanDataRequest.class.getSimpleName()))
            {
              // Parse the incoming message
              LaneProtos.ProcessScanDataRequest request = LaneProtos.ProcessScanDataRequest.parseFrom(cmdBytes);

              // If not configured return an error
              if (this.ernieLane == null)
              {
                LaneProtos.LaneErrorResponse.Builder error = LaneProtos.LaneErrorResponse.newBuilder();
                error.setMessage("Lane is not configured");

                responseType = LaneProtos.LaneErrorResponse.class.getSimpleName();
                responseBytes = error.build().toByteArray();
                
                // Update error response counter
                ++totalErrorResponses;
              }
              else
              {
                // Process the scan 
                try
                {
                  // Convert the daily scan data into a stream
                  InputStream inputStream = new ByteArrayInputStream(
                          request.getScanData().getBytes(StandardCharsets.UTF_8));
                  Stream<String> dailyScanStream = new BufferedReader(
                          new InputStreamReader(inputStream)).lines();

                  // Run the EML-VM250 analysis on the daily scan data                  
                  Results results = ernieLane.process(dailyScanStream);

                  // Create the response 
                  LaneProtos.ProcessScanDataResponse.Builder response
                          = LaneProtos.ProcessScanDataResponse.newBuilder();
                  response.setXmlData(results.toXMLString());
                  responseType
                          = LaneProtos.ProcessScanDataResponse.class.getSimpleName();
                  responseBytes = response.build().toByteArray();

                  // Success update internal info
                  lastScanID = request.getScanId();
                  
                  // Update good response counter
                  ++totalGoodResponses;
                }
                catch (AnalysisException
                        | UnsupportedOperationException
                        | ReaderException
                        | IOException ex)
                {
                  Main.LOGGER.log(
                          Level.SEVERE,
                          "Failed to process scan id: " + request.getScanId(),
                          ex);
                  responseType = LaneProtos.LaneErrorResponse.class.getName();
                  LaneProtos.LaneErrorResponse.Builder error
                          = LaneProtos.LaneErrorResponse.newBuilder();
                  error.setMessage("Scan id: " + request.getScanId()
                          + ". Exception: " + ex.getMessage());
                  responseBytes = error.build().toByteArray();
                  
                  // Update error response counter
                  ++totalErrorResponses;
                }
                catch (JAXBException ex)
                {
                  Main.LOGGER.log(
                          Level.SEVERE,
                          "Failed to convert scan id " + request.getScanId()
                          + " results into XML data",
                          ex);
                  responseType = LaneProtos.LaneErrorResponse.class.getName();
                  LaneProtos.LaneErrorResponse.Builder error
                          = LaneProtos.LaneErrorResponse.newBuilder();
                  error.setMessage("Failed to convert scan id "
                          + request.getScanId()
                          + " results into XML data"
                          + ". Exception: " + ex.getMessage());
                  responseBytes = error.build().toByteArray();
                  
                  // Update error response counter
                  ++totalErrorResponses;
                }
              }
            }
          }

          // All errors are redirected here.  (We must produce a response)
          catch (InvalidProtocolBufferException ex)
          {
            Main.LOGGER.log(Level.SEVERE, "Exception in Lane", ex);
            responseType = LaneProtos.LaneErrorResponse.class.getName();
            LaneProtos.LaneErrorResponse.Builder error = LaneProtos.LaneErrorResponse.newBuilder();
            error.setMessage("Exception: " + ex.getMessage());
            responseBytes = error.build().toByteArray();
            
            // Update error response counter
            ++totalErrorResponses;
          }

          // Send to a multipart message back
          receiver.sendMore(responseType);
          receiver.send(responseBytes);
          
          // Update total requests counter
          ++totalRequests;
        }
      }
    }
  }

  public int getLastScanId()
  {
    return lastScanID;
  }
  
  public int getTotalRequests()
  {
    return totalRequests;
  }
  
  public int getTotalGoodResponses()
  {
    return totalGoodResponses;
  }
  
  public int getTotalErrorResponses()
  {
    return totalErrorResponses;
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