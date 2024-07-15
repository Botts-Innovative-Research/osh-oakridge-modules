/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.ernie.vm250.tools;

import gov.llnl.ernie.data.Record;
import gov.llnl.ernie.io.RecordWriter;
import gov.llnl.ernie.io.RecordWriterException;
import gov.llnl.ernie.manipulator.Manipulation;
import gov.llnl.ernie.vm250.ErnieVM250Package;
import gov.llnl.ernie.vm250.data.VM250RecordInternal;
import gov.llnl.utility.xml.bind.Reader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Writes out {@link VM250Record} data to a 'daily file', in a format extending
 * that for "TSA Firmware Version 1.10.1A and Version 2.00.1T Ethernet TCP and
 * RS-232Output". Oak Ridge NL will be the consumers of this data.
 *
 * @author guensche1
 */
@Reader.Declaration(pkg = ErnieVM250Package.class, name = "dailyFileWriter", referenceable = true)
public class DailyFileWriter implements RecordWriter
{
  private String outputDir = "dailyFiles";
  
  /**
   * Write out a daily file using record and manipulation data.
   *
   * @param record
   * @param outputfile
   * @param manipulations
   */
  @Override
  public void writeRecord(Record record, File outputfile, List<Manipulation> manipulations) throws RecordWriterException
  {
    VM250RecordInternal internal = (VM250RecordInternal) record.getInternal();
    Instant recordStartInstant = record.getContextualInfo().getTimestamp();
    long time = recordStartInstant.toEpochMilli();

    // SP records will be interspersed with GS/NS records. This map will help us know where to insert them based upon timestamps.
    TreeMap<Long, VM250RecordInternal.VelocityReading> velocityMap = new TreeMap<>();
    internal.getVelocities().forEach(v -> velocityMap.put(v.timestamp.toEpochMilli(), v));

    try (FileWriter writer = new FileWriter(outputfile))
    {
      // Write out 'empty' Neutron Background record (all zeroes)
      writer.write("NB,000000,000000,000000,000000," + getTimeString(time) + "\n");

      /**
       * Write 4 bg's for gamma (1 per panel) from
       * internal.segment.gammaBackground. Backgrounds are in counts/second whereas 'GS' are in counts/sample.
       */
      double[] gammaBackground = internal.getSegmentDescription().getGammaBackground();
      writer.write("GB,");
      for (int i = 0; i < gammaBackground.length; i++)
      {
        writer.write(String.format("%06d,", new Double(gammaBackground[i]).intValue()));
      }
      writer.write(getTimeString(time));
      writer.write("\n");

      // IJ record will be the first record of the occupancy (after any background records).
      addIJRecord(record, manipulations, writer);

      // Write out neutron reading, 'NS', 10 gamma readings, 'GS' (first 5 are background, the remaining are occupancy)
      // followed by one NS, five GS, ...
      VM250RecordInternal.PanelData[] panelData = internal.getPanelData();
      int gammaSize = panelData[0].size();
      int neutronSize = panelData[0].size_neutron();
      int neutronIdx = 0;

      for (int gammaIdx = 0; gammaIdx < gammaSize; gammaIdx++)
      {
        time += 200;
        if (gammaIdx == 0 || (gammaIdx > 9 && gammaIdx % 5 == 0))
        {
          writer.write("NS,");
          for (VM250RecordInternal.PanelData pd : panelData)
          {
            writer.write(String.format("%06d", pd.neutronData[neutronIdx][0]) + ",");
          }
          writer.write(getTimeString(time) + "\n");
          neutronIdx++;
        }

        // Write SP record before or after GS record depending upon which one has earlier timestamp.
        VM250RecordInternal.VelocityReading closestVelocity = getClosestVelocity(time, velocityMap);
        if (closestVelocity != null && closestVelocity.timestamp.toEpochMilli() <= time)
        {
          writeSPRecord(closestVelocity, writer);
          closestVelocity = null;
        }

        writer.write("GS,");
        for (VM250RecordInternal.PanelData pd : panelData)
        {
          writer.write(String.format("%06d", pd.gammaData[gammaIdx][0]) + ",");
        }
        writer.write(getTimeString(time) + "\n");

        if (closestVelocity != null)
        {
          writeSPRecord(closestVelocity, writer);
        }
      }

      // Write out last neutron record if it wasn't written out in gamma loop above.
      if (gammaSize % 5 == 0 && neutronIdx < neutronSize)
      {
        writer.write("NS,");
        for (VM250RecordInternal.PanelData pd : panelData)
        {
          writer.write(String.format("%06d", pd.neutronData[neutronIdx][0]) + ",");
        }
        writer.write(getTimeString(time) + "\n");
      }

      // TODO will we need this?
//      vehicleCount++;
      // Write out Neutron bacground for "end of occupancy marker" GX record.
      double neutronBackground = internal.getSegmentDescription().getNeutronBackground();
      int nbgAsInt = new Double(neutronBackground * 1000).intValue();
      writer.write(String.format("GX,%06d,%06d,%06d,%06d,%s\n", 1, nbgAsInt, 0, 0, getTimeString(time)));
    }
    catch (IOException ex)
    {
      throw new RecordWriterException(ex.getMessage());
    }
  }
  
  @Override
  @Reader.Attribute(name = "dirname")
  public void setOutputDir(String dirname)
  {
    outputDir = dirname;
  }
  
  @Override
  public String getOutputDir()
  {
    return outputDir;
  }

  /**
   * Used by {@link #getTodaysDir(java.io.File)}.
   */
  private static Clock UTC = Clock.systemUTC();

  /**
   * @param record
   * @param manipulations
   * @param writer
   * @throws IOException
   */
  private void addIJRecord(Record record, List<Manipulation> manipulations, FileWriter writer) throws IOException
  {
    /**
     * <pre>
     * 1. InjectedScanId  (unique id that is assigned to each injected scan by FTBT)
     * 2. ManipulationNumber  (1 for 1st injected source, 2 for 2nd, etc.)
     * 3. Intensity1
     * 4. Intensity2
     * 5. X1
     * 6. X2
     * 7. Y
     * 8. Z
     * 9. SourceType  (an enumeration, 1=NORM, 2=Med, 3=Ind, 4=Fiss, 5=Contam)
     * 10. SNum  (index into the ERNIE source library)
     * 11. CargoModel (type of anisotropic shielding used when simulating the source)
     * </pre>
     */
    int count = 0;
    for (Manipulation manipulation : manipulations)
    {
      count++;
      writer.write("IJ,");
      writer.write(record.getContextualInfo().getScanID() + ",");
      writer.write(count + ",");
      if (manipulation.isInjectDistributed())
      {
        writer.write(manipulation.getDistributedIntensity1() + ",");
        writer.write(manipulation.getDistributedIntensity2() + ",");
        writer.write(manipulation.getDistributedPx1() + ",");
        writer.write(manipulation.getDistributedPx2() + ",");
        writer.write(manipulation.getDistributedPy() + ",");
        writer.write(manipulation.getDistributedPz() + ",");
        writer.write(manipulation.getSourceType() + ",");
        writer.write(manipulation.getDistributedSourceId() + ",");
      }
      else if (manipulation.isInjectCompact())
      {
        writer.write(manipulation.getCompactIntensity() + ",");
        writer.write(manipulation.getCompactIntensity() + ",");
        writer.write(manipulation.getCompactPx() + ",");
        writer.write(manipulation.getCompactPx() + ",");
        writer.write(manipulation.getCompactPy() + ",");
        writer.write(manipulation.getCompactPz() + ",");
        writer.write(manipulation.getSourceType() + ",");
        writer.write(manipulation.getCompactSourceId() + ",");
      }

      writer.write(manipulation.getCargoModelId() + "\n");
    }
  }

  /**
   * Given a time (long) find the closest
   * {@link VM250RecordInternal.VelocityReading} in velocityMap. Velocity will
   * either be before time or within 200 ms after time, else null.
   *
   * @param time
   * @param velocityMap
   * @return
   */
  private VM250RecordInternal.VelocityReading getClosestVelocity(long time, TreeMap<Long, VM250RecordInternal.VelocityReading> velocityMap)
  {
    Map.Entry<Long, VM250RecordInternal.VelocityReading> floorEntry = velocityMap.floorEntry(time);
    if (floorEntry != null)
    {
      return velocityMap.remove(floorEntry.getKey());
    }

    Map.Entry<Long, VM250RecordInternal.VelocityReading> ceilingEntry = velocityMap.ceilingEntry(time);
    // If ceilingEntry is within 200ms of time then we want it, else it will have to wait.
    if (ceilingEntry != null && ceilingEntry.getKey() < time + 200)
    {
      return velocityMap.remove(ceilingEntry.getKey());
    }

    return null;
  }

  /**
   * SP record: the first field is time to cover 1 foot, second field is MPH(99
   * max), third field is kPH(999 max), fourth field is an incrementing car
   * count.
   *
   * @param velocity
   * @param writer
   * @throws IOException
   */
  private void writeSPRecord(VM250RecordInternal.VelocityReading velocity, Writer writer) throws IOException
  {
    double mPerS = velocity.velocity;
    Double kph = mPerS * 3.6;
    Double mph = kph * 0.6213711922;
    Double timeForOneFoot = 1.0 / (mph * 1.46667);
    long time = velocity.timestamp.toEpochMilli();
    // TODO verify %6.3f is padding...
    writer.write(String.format("SP,%.4f,%6.3f,%6.3f,000000,%s\n", timeForOneFoot, mph, kph, getTimeString(time)));
  }

  /**
   * Given a long return a UTC time of day as "HH-mm-ss.SS" where SS is
   * milli-seconds.
   *
   * @param time
   * @return
   */
  private String getTimeString(long time)
  {
    LocalTime lt = LocalTime.from(Instant.ofEpochMilli(time).atZone(ZoneId.of("GMT-7")));
    return String.format("%d-%d-%d.%d", lt.getHour(), lt.getMinute(), lt.getSecond(), lt.getNano() / 1000000);
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