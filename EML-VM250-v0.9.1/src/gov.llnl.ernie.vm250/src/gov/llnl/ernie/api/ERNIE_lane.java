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

import gov.llnl.ernie.Analysis;
import gov.llnl.ernie.analysis.AnalysisException;
import gov.llnl.ernie.classifier.Classifier;
import gov.llnl.ernie.data.AnalysisResult;
import gov.llnl.ernie.data.Record;
import gov.llnl.ernie.internal.data.LaneImpl;
import gov.llnl.ernie.io.RecordDatabase;
import gov.llnl.ernie.vm250.VM250RecordDatabase;
import gov.llnl.ernie.vm250.tools.DailyFileWriter;
import gov.llnl.utility.PathUtilities;
import gov.llnl.utility.io.ReaderException;
import gov.llnl.utility.xml.bind.DocumentReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 *
 * @author cmattoon
 */
public class ERNIE_lane
{
  String portId;
  int laneId;
  boolean collimated;
  double laneWidth;
  Analysis analysis;
  DailyFileLoader fileLoader;
  List<Path> searchPathList;

  public ERNIE_lane(String portId, int laneId, boolean isCollimated,
          double laneWidth, int intervals, int occupancyHoldin)
  {
    this.portId = portId;
    this.laneId = laneId;
    this.collimated = isCollimated;
    this.laneWidth = laneWidth;

    this.fileLoader = new DailyFileLoader();
    this.fileLoader.setDefaultIntervals(intervals);
    this.fileLoader.setDefaultOccupancyHoldin(occupancyHoldin);

    searchPathList = new ArrayList<Path>()
    {
      {
        // When setting the project's working directory to proj-ernie4
        add(Paths.get("config"));
        // When using the jar file in proj-ernie4/jars
        add(Paths.get("../config"));
        // When project's work directory is not set
        add(Paths.get("../../config"));
        add(Paths.get("../../../config"));
      }
    };

    try
    {
      // Load the feature builder from disk
      DocumentReader<Analysis> reader = DocumentReader.create(Analysis.class);

      Path analysisXmlPath = PathUtilities.resolve(
              Paths.get("vm250Analysis.xml"),
              searchPathList.stream().toArray(Path[]::new));

      analysis = reader.loadFile(analysisXmlPath);

      // Variables to set the classifier object
      Path machine_learning_model;
      Path machine_learning_metrics = PathUtilities.resolve(
              Paths.get("CMU_metric.csv"),
              searchPathList.stream().toArray(Path[]::new));
      boolean Verbose = false; // Set to true to see additional info      

      if (isCollimated)
      {
        machine_learning_model = PathUtilities.resolve(
                Paths.get("uber_SSL_model_collimated.txt"),
                searchPathList.stream().toArray(Path[]::new));
      }
      else
      {
        machine_learning_model = PathUtilities.resolve(
                Paths.get("uber_SSL_model_uncollimated.txt"),
                searchPathList.stream().toArray(Path[]::new));
      }

      // Create the correct classifier object and set it in the analysis object
      Classifier classifier = new Classifier(
              machine_learning_model.toAbsolutePath().toString(),
              machine_learning_metrics.toAbsolutePath().toString(),
              Verbose);
      
      analysis.setClassifier(classifier);
    }
    catch (ReaderException | IOException ex)
    {
      Logger.getLogger(ERNIE_lane.class.getName()).log(Level.SEVERE, null, ex);
    }
  }

  public Results process(Stream dailyFile) throws AnalysisException, UnsupportedOperationException, ReaderException, IOException
  {
    Path laneDbXmlPath = PathUtilities.resolve(
            Paths.get("vm250LaneDatabase.xml"),
            searchPathList.stream().toArray(Path[]::new));

    Record record = fileLoader.loadFirstOccupancy(dailyFile, laneDbXmlPath);
    return process(record);
  }

  public Results process(Record record) throws AnalysisException
  {
    LaneImpl lane = (LaneImpl) record.getLane();
    lane.setLaneWidth(laneWidth);

    AnalysisResult res;
    try
    {
      res = analysis.processRecord(record);
    }
    catch (AnalysisException ex)
    {
      res = analysis.allocateResult();
    }

    Results results = new Results(this.analysis, res);
    results.setPortID(portId);
    results.setLaneID(laneId);
    return results;
  }

  public static void main(String[] args) throws Exception
  {
    RecordDatabase recordDatabase;
    DailyFileWriter dfw = new DailyFileWriter();

    Path searchPaths[] =
    {
      Paths.get("config"), // When setting the project's 
      //working directory to proj-ernie4
      Paths.get("../config"), // When using the jar file in proj-ernie4/jars
      Paths.get("../../config"), // When project's work directory is not set
      Paths.get("../../../config"), // When project's work directory is not set
    };

    try
    {
      DocumentReader dbr = DocumentReader.create(RecordDatabase.class);

      Path recordDbXmlPath = PathUtilities.resolve(
              Paths.get("vm250RecordDatabase.xml"),
              searchPaths);

      recordDatabase = (VM250RecordDatabase) dbr.loadFile(recordDbXmlPath);
    }
    catch (ReaderException | IOException ex)
    {
      throw new Exception("Unable to load resources: " + ex.getMessage());
    }

    ERNIE_lane lane = new ERNIE_lane("samplePort", 4, true, 4.82, 5, 10);

    // IDs from DB table: VM250_NARR
    long[] rids =
    {
      80775656, 83089354, 81357886, 83389042, 82360750, 81859804, 82746957, 84685018
    };
    String[] descriptions =
    {
      "both_investigate", "ernie_investigate", "rpm_investigate", "both_release",
      "fallback_investigate", "fallback_release", "neutron", "neutron_fallback"
    };

    // Create the samples directory
    Files.createDirectories(Paths.get("samples"));

    for (int idx = 0; idx < rids.length; idx++)
    {
      System.out.println("Processing record ID: " + rids[idx]);

      Record record = recordDatabase.getRecord(rids[idx]);
      try
      {
        Results res = lane.process(record);
        res.saveToXML(new java.io.File("samples", descriptions[idx] + "_result.xml"));
        dfw.writeRecord(record, new java.io.File("samples", descriptions[idx] + "_daily.txt"), new java.util.ArrayList<>());
      }
      catch (AnalysisException ex)
      {
        throw new Exception(ex);
      }      
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