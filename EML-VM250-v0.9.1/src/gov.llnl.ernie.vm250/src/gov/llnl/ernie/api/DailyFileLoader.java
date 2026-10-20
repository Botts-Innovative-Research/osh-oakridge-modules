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
package gov.llnl.ernie.api;

import gov.llnl.ernie.data.Record;
import gov.llnl.ernie.vm250.ErnieVM250Package;
import gov.llnl.ernie.vm250.data.VM250Occupancy;
import gov.llnl.ernie.vm250.data.VM250Record;
import gov.llnl.ernie.vm250.tools.VM250OccupancyConverter;
import gov.llnl.utility.io.ReaderException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.javatuples.Pair;
import org.javatuples.Triplet;

/**
 *
 * @author cmattoon
 */
public class DailyFileLoader
{

  // Immutable list that will help convert daily files to occupancy data
  static final List<String> BACKGROUND_TAGS_LIST = Collections.unmodifiableList(
          Arrays.asList("NB", "GB"));

  static final List<String> ERROR_TAGS_LIST = Collections.unmodifiableList(
          Arrays.asList("GH", "GL", "NH", "TC", "TT"));

  static final List<String> OCCUPANCY_TAGS_LIST = Collections.unmodifiableList(
          Arrays.asList("SP", "GS", "NS", "GA", "NA", "GX"));

  static final List<String> SPECTRA_TAGS_LIST = Collections.unmodifiableList(
          Arrays.asList("GS", "NS", "GA", "NA"));

  static final List<String> SETUP_TAGS_LIST = Collections.unmodifiableList(
          Arrays.asList("SG1", "SG2", "SG3", "SN1", "SN2"));

  static final List<String> IGNORED_TAGS_LIST = Collections.unmodifiableList(
          Arrays.asList("RW", "AB", "AR", "AM", "AA", "FP", "TE", "NNB", "IJ"));

  static final List<String> NEUTRON_TAGS_LIST = Collections.unmodifiableList(
          Arrays.asList("NB", "NH"));

  // FIXME proper name of this tag
  static final List<String> G_TAGS_LIST = Collections.unmodifiableList(
          Arrays.asList("GB", "GH", "GL"));

  String version;
  final int occupancyID;
  final int siteID;
  final int laneID;
  
  private Map<String, Object> defaultParameters;

  final List<Triplet<Instant, List<Integer>, String>> neutronBackgroundList = new ArrayList<>();
  final List<Triplet<Instant, List<Integer>, String>> gammaBackgroundList = new ArrayList<>();
  final List<Pair<Instant, String>> tamperFlagsList = new ArrayList<>();
  final List<VM250Occupancy> occupancyList = new ArrayList<>();

  public DailyFileLoader()
  {
    this(1, 1, 1);
  }

  public DailyFileLoader(int occupancyID, int siteID, int laneID)
  {
    this.occupancyID = occupancyID;
    this.siteID = siteID;
    this.laneID = laneID;
    
    this.defaultParameters = new HashMap<>();
    defaultParameters.put("Intervals", Integer.valueOf("5"));
    defaultParameters.put("OccupancyHoldin", Integer.valueOf("5"));
    defaultParameters.put("NSigma", Double.valueOf("6"));
  }

  public List<Record> load(String filename, String vm250LaneDatabaseXml) throws IOException, UnsupportedOperationException, ReaderException
  {
    return load(Files.lines(Paths.get(filename)), Instant.now(), Paths.get(vm250LaneDatabaseXml));
  }

  public List<Record> load(String filename, Instant baseTimestamp, String vm250LaneDatabaseXml) throws IOException, UnsupportedOperationException, ReaderException
  {
    return load(Files.lines(Paths.get(filename)), baseTimestamp, Paths.get(vm250LaneDatabaseXml));
  }

  public List<Record> load(Stream<String> input, Path vm250LaneDatabaseXmlPath) throws UnsupportedOperationException, ReaderException, IOException
  {
    return load(input, Instant.now(), vm250LaneDatabaseXmlPath);
  }

  public List<Record> load(Stream<String> input, Instant baseTimestamp, Path vm250LaneDatabaseXmlPath) throws UnsupportedOperationException, ReaderException, IOException
  {
    Map<String, Object> parameters = null;
    neutronBackgroundList.clear();
    gammaBackgroundList.clear();
    tamperFlagsList.clear();
    occupancyList.clear();
    
    List<String> datalines = input.collect(Collectors.toList());

    // Length and no data check
    if (datalines.isEmpty()
            || (datalines.size() == 1 && datalines.get(0).startsWith("No data")))
    {
      ErnieVM250Package.LOGGER.log(
              Level.WARNING,
              "NO data present!"
      );
      return null;
    }

    // Commas count check on the first 100 lines
    input = datalines.stream();
    Entry<Long, Long> result = input.limit(100).map(str -> str.chars().filter(ch -> ch == ',').count())
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
            .entrySet()
            .stream()
            .max(Comparator.comparing(Entry::getValue))
            .get();

    long ncommas = result.getKey();
    long count = result.getValue();
    long nlines = Math.min(datalines.size(), 100);

    if (count < .95 * nlines)
    {
      ErnieVM250Package.LOGGER.log(
              Level.WARNING,
              "Encountered many abnormal lines at start of file"
      );
    }

    // Check number of timestamps 
    long nTimestamps = ncommas - 4;
    if (nTimestamps > 2)
    {
      throw new UnsupportedOperationException(String.format(
              "Not supported: Found %d timestamps per line", nTimestamps));
    }

    if (nTimestamps > 0)
    {
      // Check timestamp format      
      String[] aLine = datalines.get(0).split(",");
      if (!Pattern.matches(
              "(^\\d{1,2}-\\d{1,2}-\\d{1,2}(\\.\\d+)?$)|(^\\d{1,2}:\\d{1,2}:\\d{1,2}(\\.\\d+)?$)",
              aLine[aLine.length - 1].strip()
      ))
      {
        throw new IllegalArgumentException(
                String.format("Unknown date format '%s'", aLine[aLine.length - 1]));
      }
    }

    Instant defaultTime = Instant.now();

    // Grab setup lines 
    HashMap<String, String> setupLines = new HashMap<>();
    for (String line : datalines)
    {
      String[] data = line.split(",");
      String tag = data[0];
      Instant time;

      if (SETUP_TAGS_LIST.contains(tag))
      {
        // Check if tag is already setup 
        if (setupLines.containsKey(tag))
        {
          ErnieVM250Package.LOGGER.log(
                  Level.WARNING,
                  "Setup repeated! Discarded setup data: {0}", line);
          continue;
        }

        if (nTimestamps > 0)
        {
          time = addTime(baseTimestamp, data[data.length - 1]);
        }
        else
        {
          time = defaultTime;
        }

        setupLines.put(tag, line);

        if (setupLines.size() == 5)
        {
          parameters = parseSetup(setupLines, time);
          setupLines.clear();
        }
      }
    }

    // check versioning
    if (parameters == null)
    {
      ErnieVM250Package.LOGGER.log(
              Level.WARNING, "No setup information found");
      parameters = defaultParameters;
    }

    boolean inOccupancy = false;
    int occupancyFirstLine = -1;
    Instant lastOccupancyTime = defaultTime;
    Instant time = defaultTime;
    List<Triplet<String, List<String>, Instant>> occLines = new ArrayList<>();
    boolean continuation = false;
    final long FIVE_SEC = 5L;
    final long TWO_HUNDRED_MS = 200L;   // 0.2 seconds or 200 milliseconds
    final long MIN_TIME_DIFF = 1L;      // Warn if records are closer together than 1 s
    final long OCCUPANCY_TIMEOUT = 10L; // 10s

    boolean skipLine = false;

    for (int idx = 0; idx < datalines.size(); ++idx)
    {
      if (skipLine)
      {
        skipLine = false;
        continue;
      }

      String line = datalines.get(idx).strip();
      ncommas = line.chars().filter(ch -> ch == ',').count();

      if (line.length() > 60 && !line.startsWith("IJ"))
      {
        ErnieVM250Package.LOGGER.log(Level.WARNING,
                String.format(
                        "Line #%d too long: %s",
                        idx, line
                )
        );
        continue;
      }

      if ((nTimestamps == 1 && ncommas != 5)
              || (nTimestamps == 2 && ncommas != 6)
              || (nTimestamps == 0 && ncommas != 4))
      {
        if (!SETUP_TAGS_LIST.contains(line.split(",")[0]))
        {
          ErnieVM250Package.LOGGER.log(Level.WARNING,
                  String.format(
                          "Line #%d appears malformed: %s",
                          idx, line
                  )
          );
          continue;
        }
      }

      // Use regex to check for invalid data (stars), non-numeric values
      // example: '*****' is invalid
      if (Pattern.matches("^.*\\*+.*$", line))
      {
        ErnieVM250Package.LOGGER.log(Level.WARNING,
                String.format(
                        "Line #%d contains non-numeric values: %s",
                        idx, line
                )
        );
        continue;
      }

      String[] vals = line.strip().split(",");
      for (int vdx=0; vdx<vals.length; vdx++)
      {
        vals[vdx] = vals[vdx].strip();
      }

      String tag = vals[0];

      // Sometimes comma and timestamp wrap over to start of the next line
      if (tag.isEmpty())
      {
        ErnieVM250Package.LOGGER.log(Level.WARNING,
                String.format(
                        "Line #%d appears malformed: %s",
                        idx, line
                )
        );
        continue;
      }

      if (nTimestamps > 0)
      {
        if (Pattern.matches(
                "(^\\d{1,2}-\\d{1,2}-\\d{1,2}(\\.\\d+)?$)|(^\\d{1,2}:\\d{1,2}:\\d{1,2}(\\.\\d+)?$)",
                vals[vals.length - 1]
        ))
        {
          time = addTime(baseTimestamp, vals[vals.length - 1]);
        }
        else
        {
          ErnieVM250Package.LOGGER.log(Level.WARNING,
                  String.format(
                          "Could not extract date from string '%s'",
                          vals[vals.length - 1]
                  )
          );
          // Just use previous timestamp
        }
      }
      else
      {
        if (tag.equals("GB"))
        {
          time = time.plus(FIVE_SEC, ChronoUnit.SECONDS);
        }
        else if (tag.equals("GS"))
        {
          time = time.plus(TWO_HUNDRED_MS, ChronoUnit.MILLIS);
        }
      }

      if (OCCUPANCY_TAGS_LIST.contains(tag))
      {
        if (!inOccupancy)
        {
          occupancyFirstLine = idx;
        }
        inOccupancy = true;
        occLines.add(Triplet.with(
                tag,
                Arrays.asList(vals[1], vals[2], vals[3], vals[4]),
                time
        ));
        lastOccupancyTime = time;

        if (tag.equals("GX")) // Normally signals end of occupancy
        {
          // Check ahead one line for any more tags related to this occupancy:
          int nextValsIndex = idx + 1;
          String[] nextVals;
          Instant timeNext;
          if (nextValsIndex != datalines.size())
          {
            nextVals = datalines.get(nextValsIndex).strip().split(",");

            if (SPECTRA_TAGS_LIST.contains(nextVals[0].strip()))
            {
              if (nextVals.length > 5) // Need a timestamp for the next check
              {
                timeNext = addTime(baseTimestamp, nextVals[nextVals.length - 1]);
                if (time.equals(timeNext))
                {
                  occLines.add(Triplet.with(
                          nextVals[0],
                          Arrays.asList(nextVals[1], nextVals[2], nextVals[3], nextVals[4]),
                          timeNext
                  ));
                  skipLine = true;
                }
              }
            }
          }

          try {
            addOccupancy(VM250Occupancy.of(occLines, continuation));
          }
          catch (Exception ex)
          {
            ErnieVM250Package.LOGGER.log(Level.WARNING,
                  String.format("Unable to parse occupancy starting on line %d",
                          occupancyFirstLine
                  )
            );

          }
          inOccupancy = false;
          occupancyFirstLine = -1;
          occLines.clear();
          continuation = false;

          if (nextValsIndex < datalines.size())
          {
            nextVals = datalines.get(nextValsIndex).strip().split(",");
            if (OCCUPANCY_TAGS_LIST.contains(nextVals[0].strip()))
            {
              if (nTimestamps > 0)
              {
                timeNext = addTime(time, nextVals[nextVals.length - 1]);
                Duration diffTime = Duration.between(time, timeNext);
                Duration minDiff = Duration.ofSeconds(MIN_TIME_DIFF);
                if (diffTime.compareTo(minDiff) < 0)
                {
                  continuation = true;
                }
              }
              else
              {
                // FIXME Assumption
                continuation = true;
              }
            }
          }
        } // End tag GX
      }// End tag is in OCCUPANCY_TAGS_LIST
      else
      {
        Duration diffTime = Duration.between(lastOccupancyTime, time);
        Duration maxDiff = Duration.ofSeconds(OCCUPANCY_TIMEOUT);
        if (inOccupancy && (diffTime.compareTo(maxDiff) > 0))
        {
          // Assume occupancy ended abnormally
          addOccupancy(VM250Occupancy.of(occLines));
          inOccupancy = false;
          occLines.clear();
          continuation = false;
        }

        List<String> temp;
        List<Integer> intList;
        if (NEUTRON_TAGS_LIST.contains(tag))
        {
          temp = Arrays.asList(vals[1], vals[2], vals[3], vals[4]);
          intList = temp.stream().map(s -> Integer.parseInt(s)).collect(Collectors.toList());
          neutronBackgroundList.add(Triplet.with(
                  time, intList, String.valueOf(tag.charAt(1))));
        }
        else if (G_TAGS_LIST.contains(tag))
        {
          temp = Arrays.asList(vals[1], vals[2], vals[3], vals[4]);
          intList = temp.stream().map(s -> Integer.parseInt(s)).collect(Collectors.toList());
          gammaBackgroundList.add(Triplet.with(
                  time, intList, String.valueOf(tag.charAt(1))));
        }
        else if (tag.equals("TC") || tag.equals("TT"))
        {
          tamperFlagsList.add(Pair.with(time, tag));

          if (inOccupancy)
          {
            temp = new ArrayList<>(Arrays.asList(vals));
            temp.remove(0);
            temp.remove(temp.size() - 1);
            occLines.add(Triplet.with(
                    tag,
                    temp,
                    time
            ));
          }
        }
        else if (SETUP_TAGS_LIST.contains(tag))
        {
          continue; // Already dealt with in parseSetup
        }
        else if (IGNORED_TAGS_LIST.contains(tag))
        {
          continue; // Skipping for now
        }
        else
        {
          ErnieVM250Package.LOGGER.log(Level.WARNING,
                  String.format(
                          "Encountered unexpected tag '%s'\n\tLine #%d contains unexpected tag : %s",
                          tag, idx, line
                  )
          );
        }
      }
    }// End for

    // Convert VM250Occupancies to VM250Records
    List<Record> out = new ArrayList<>();
    VM250OccupancyConverter converter = new VM250OccupancyConverter(
            vm250LaneDatabaseXmlPath,
            (Integer) parameters.get("Intervals"),
            (Integer) parameters.get("OccupancyHoldin"),
            (Double) parameters.get("NSigma"));
    
    for(VM250Occupancy occ : occupancyList)
    {
      out.add(converter.toRecord(occ));
    }

    return out;
  }

  public VM250Record loadFirstOccupancy(String filename, String vm250LaneDatabaseXml) throws IOException, UnsupportedOperationException, ReaderException
  {
    return loadFirstOccupancy(Files.lines(Paths.get(filename)), Paths.get(vm250LaneDatabaseXml));
  }

  public VM250Record loadFirstOccupancy(Stream input, Path vm250LaneDatabaseXmlPath) throws UnsupportedOperationException, ReaderException, IOException
  {
    List<Record> out = load(input, vm250LaneDatabaseXmlPath);
    return (VM250Record)out.get(0);
  }

  // Helper methods
  private Instant addTime(Instant baseTimestamp, String timeStr)
  {
    // TimeStr format should be HH:MM:SS.sss or HH-MM-SS.sss
    // Quick preprocess 
    timeStr = timeStr.replace(":", "-");
    String[] timeComponentArray = timeStr.split("-");

    // Extract time information
    long hours = Long.parseLong(timeComponentArray[0]);
    long minutes = Long.parseLong(timeComponentArray[1]);
    String[] secParts = timeComponentArray[2].split("\\.");
    long seconds = Long.parseLong(secParts[0]);
    long milliseconds = Long.parseLong(secParts[1]);

    // Add time to the base timestamp
    Instant out = baseTimestamp.plus(milliseconds, ChronoUnit.MILLIS);
    out = out.plus(seconds, ChronoUnit.SECONDS);
    out = out.plus(minutes, ChronoUnit.MINUTES);
    out = out.plus(hours, ChronoUnit.HOURS);
    return out;
  }

  private Map<String, Object> parseSetup(HashMap<String, String> setupLines, Instant time)
  {
    // Parse information about software version and VM250 setup from dictionary.

    // Preprocessing
    // Using regex to replace '***' with -1
    // In the raw data missing fields are mapped to '***'
    // Our logic mapped '***' to -1
    Map<String, Object> parameters = new HashMap<>();
    
    String[] keys = setupLines.keySet().toArray(new String[0]);
    for (int i = 0; i < setupLines.size(); ++i)
    {
      String key = keys[i];
      String value = setupLines.get(key);
      setupLines.put(key, value.replaceAll("\\*+", "-1"));
    }

    String vtag = setupLines.get("SG2").split(",")[6];
    String ver = setupLines.get("SG3").split(",")[5];
    this.version = ver + vtag;

    List<String> vehicleVersions = new ArrayList<String>()
    {
      {
        add("1.10.1A");
        add("1.20.1A");
        add("1.20.2A");
      }
    };

    List<String> trainVersions = new ArrayList<String>()
    {
      {
        add("2.00.1T");
        add("2.33.4A");
      }
    };

    if (!vehicleVersions.contains(this.version) && !trainVersions.contains(this.version))
    {
      throw new UnsupportedOperationException(
              String.format("Unrecognized version string '%s'", this.version));
    }

    ArrayList<String> vals = new ArrayList<>(Arrays.asList(setupLines.get("SG1").split(",")));
    vals.remove(0);
    parameters.put("siteId", this.siteID);
    parameters.put("laneId", this.laneID);
    parameters.put("DateTime", time);
    parameters.put("Version", this.version);
    parameters.put("GammaBackgroundHighFault", Integer.parseInt(vals.get(0)));
    parameters.put("GammaBackgroundLowFault", Integer.parseInt(vals.get(1)));
    parameters.put("Intervals", Integer.parseInt(vals.get(2)));
    parameters.put("OccupancyHoldin", Integer.parseInt(vals.get(3)));
    parameters.put("NSigma", Double.parseDouble(vals.get(4)));
    vals.clear();

    vals = new ArrayList<>(Arrays.asList(setupLines.get("SG2").split(",")));
    vals.remove(0);
    parameters.put("DetectorsOnLine", Integer.parseInt(vals.get(0)));
    parameters.put("MasterLowLevelDiscriminator", Double.parseDouble(vals.get(1)));
    parameters.put("MasterHighLevelDiscriminator", Double.parseDouble(vals.get(2)));
    parameters.put("RelayOutput", Integer.parseInt(vals.get(3)));
    parameters.put("Algorithm", vals.get(4));
    vals.clear();

    vals = new ArrayList<>(Arrays.asList(setupLines.get("SG3").split(",")));
    vals.remove(0);
    parameters.put("SlaveLowLevelDiscriminator", Double.parseDouble(vals.get(0)));
    parameters.put("SlaveHighLevelDiscriminator", Double.parseDouble(vals.get(1)));
    parameters.put("BackgroundNSigma", Double.parseDouble(vals.get(3)));
    if (vehicleVersions.contains(this.version))
    {
      parameters.put("SystemBackgroundTime", Integer.parseInt("0"));
      parameters.put("GammaBackgroundTime", Integer.parseInt(vals.get(2)));
    }
    else if (trainVersions.contains(this.version))
    {
      parameters.put("SystemBackgroundTime", Integer.parseInt(vals.get(2)));
      parameters.put("GammaBackgroundTime", Integer.parseInt("0"));
    }
    vals.clear();

    vals = new ArrayList<>(Arrays.asList(setupLines.get("SN1").split(",")));
    vals.remove(0);
    parameters.put("NeutronBackgroundHighFault", Integer.parseInt(vals.get(0)));
    parameters.put("NeutronMaxIntervals", Integer.parseInt(vals.get(1)));
    parameters.put("Alpha", Integer.parseInt(vals.get(2)));
    parameters.put("ZMax", Integer.parseInt(vals.get(3)));
    parameters.put("SequentialIntervals", Integer.parseInt(vals.get(4)));
    if (vehicleVersions.contains(this.version))
    {
      parameters.put("NeutronBackgroundTime", Integer.parseInt(vals.get(5)));
    }
    else if (trainVersions.contains(this.version))
    {
      parameters.put("NeutronBackgroundTime", Integer.parseInt("0"));
    }
    vals.clear();

    vals = new ArrayList<>(Arrays.asList(setupLines.get("SN2").split(",")));
    vals.remove(0);
    parameters.put("NeutronMasterLowerLevelDiscriminator", Double.parseDouble(vals.get(0)));
    parameters.put("NeutronMasterUpperLevelDiscriminator", Double.parseDouble(vals.get(1)));
    parameters.put("NeutronSlaveLowerLevelDiscriminator", Double.parseDouble(vals.get(2)));
    parameters.put("NeutronSlaveUpperLevelDiscriminator", Double.parseDouble(vals.get(3)));
    vals.clear();
    
    return parameters;
  }
  
  public void setDefaultIntervals(int val)
  {
    this.defaultParameters.replace("Intervals", val);
  }
  
  public void setDefaultOccupancyHoldin(int val)
  {
    this.defaultParameters.replace("OccupancyHoldin", val);
  }
  
  public void setDefaultNSigmaThreshold(double val)
  {
    this.defaultParameters.replace("NSigma", val);
  }

  private void addOccupancy(VM250Occupancy newOccupancy)
  {
    if (gammaBackgroundList.size() > 0)
    {
      newOccupancy.setLastGammaBackgroundData(gammaBackgroundList.get(gammaBackgroundList.size() - 1));
    }

    occupancyList.add(newOccupancy);

    if (!newOccupancy.isRealOccupancy())
    {
      ErnieVM250Package.LOGGER.log(Level.WARNING,
              String.format(
                      "Occupancy %d at %s likely not real",
                      occupancyList.size(), newOccupancy.getStartTime().toString()
              )
      );
    }
  }

  // FIXME REMOVE main once testing is done!
  public static void main(String[] args) throws IOException, Exception
  {
    DailyFileLoader dfl = new DailyFileLoader();
    List<Record> records = dfl.load(
            "C:/Users/PHAM21/Desktop/VM250_Daily_Files/NARR/F_J_L025_2016-06-08.txt", 
            "C:\\Projects\\ERNIE_HOME\\proj-ernie4\\config\\vm250LaneDatabase.xml"
    );
    System.out.println("Number of records: " + records.size());    
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
