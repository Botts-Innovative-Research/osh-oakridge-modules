/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.ernie.vm250;

import gov.llnl.ernie.ErniePackage;
import gov.llnl.ernie.data.Lane;
import gov.llnl.ernie.vm250.data.VM250RecordInternal;
import gov.llnl.ernie.vm250.data.VM250Record;
import gov.llnl.ernie.impl.RecordDatabaseImpl;
import gov.llnl.ernie.data.Record;
import gov.llnl.ernie.internal.data.LaneImpl;
import gov.llnl.ernie.io.LaneDatabase;
import gov.llnl.ernie.io.RecordException;
import gov.llnl.ernie.utility.Formatter;
import static gov.llnl.ernie.utility.Formatter.*;
import gov.llnl.ernie.vm250.data.VM250RecordInternal.VelocityReading;
import gov.llnl.utility.xml.bind.ReaderInfo;
import java.io.PrintStream;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Logger;

/**
 * {@link RecordDatabaseImpl} implementation to get {@link VM250RecordInternal}s
 * from a databaseName.
 *
 * @author mattoon1
 */
@ReaderInfo(VM250RecordDatabaseReader.class)
public class VM250RecordDatabase extends RecordDatabaseImpl
{
  static final Logger LOGGER = ErniePackage.LOGGER;
  private final List<PanelData> sampleDataCache = new LinkedList<>();
  private LaneDatabase laneDatabase;
  private PreparedStatement fetchRecordKeyQuery;
  private PreparedStatement fetchSegmentDescriptorQuery;
  private PreparedStatement fetchGammaPanelDataQuery;
  private PreparedStatement fetchNeutronPanelDataQuery;
  private PreparedStatement fetchVelocitiesQuery;
  private PreparedStatement fetchBackgroundErrorsQuery;
  private PreparedStatement fetchSetupQuery;
  private final int panels = 4; // FIXME get from querying panel data?

  //<editor-fold desc="public">
  VM250RecordDatabase()
  {
  }

  @Override
  public Record getRecord(long segmentDescriptorId) throws RecordException
  {
    try
    {
      RecordKey key = getRecordKey(segmentDescriptorId);

      VM250RecordInternal recordInternal;
      try
      {
        recordInternal = getRecordInternal(key);
      }
      catch (Exception ex)
      {
        throw new RecordException("get record internal failed", ex);
      }

      fetchLane(key, recordInternal.getLane());

      fetchBackgroundErrors(recordInternal, segmentDescriptorId);

      return new VM250Record(recordInternal);
    }
    catch (Exception ex)
    {
      throw new RecordException("Fail in load", ex);
    }
  }
//</editor-fold>

  //<editor-fold desc="internal" defaultstate="collapsed">
  @Override
  protected void prepare() throws SQLException
  {
    try
    {
      Function<String, String> sub = Formatter.mapFromStrings(new String[]
      {
        "database", getDatabaseName()
      });
      Function<String, String> f1 = Formatter.mapFromProperties(this.getPropertyMap());

      this.fetchRecordKeyQuery = this.dbConnection.prepareStatement(
              format(f1.apply("fetchRecordKey.query"),
                      f1, sub));
      this.fetchSegmentDescriptorQuery = this.dbConnection.prepareStatement(
              format(f1.apply("fetchSegmentDescriptor.query"),
                      f1, sub));
      this.fetchGammaPanelDataQuery = this.dbConnection.prepareStatement(
              format(f1.apply("fetchGammaPanelData.query"),
                      f1, sub));
      this.fetchNeutronPanelDataQuery = this.dbConnection.prepareStatement(
              format(f1.apply("fetchNeutronPanelData.query"),
                      f1, sub));
      this.fetchVelocitiesQuery = this.dbConnection.prepareStatement(
              format(f1.apply("fetchVelocities.query"),
                      f1, sub));
      this.fetchBackgroundErrorsQuery = this.dbConnection.prepareStatement(
              format(f1.apply("fetchBackgroundErrors.query"),
                      f1, sub));
      this.fetchSetupQuery = this.dbConnection.prepareStatement(
              format(f1.apply("fetchSetup.query"),
                      f1, sub));

    } catch (FormatterException ex)
    {
      throw new RuntimeException(ex);
    }
  }

  private VM250RecordDatabase.RecordKey getRecordKey(long segmentDescriptorId) throws Exception
  {
    VM250RecordDatabase.RecordKey key = new VM250RecordDatabase.RecordKey();
    this.fetchRecordKeyQuery.setLong(1, segmentDescriptorId);
    try (ResultSet rs = fetchRecordKeyQuery.executeQuery())
    {
      while (rs.next())
      {
        key.occupancyId = segmentDescriptorId;
        key.date = rs.getTimestamp("DateTime");
        key.dataSourceId = rs.getInt("siteId");
        key.rpmId = rs.getInt("laneId");
      }
    }
    return key;
  }

  private VM250RecordInternal getRecordInternal(RecordKey key) throws SQLException, Exception
  {
    VM250RecordInternal record = new VM250RecordInternal();
    record.setOccupancyId(key.occupancyId);
    fetchSegmentDescriptor(record, key);
    fetchPanelData(record, key.occupancyId);
    fetchVelocityData(record, key.occupancyId);
    fetchSetupInfo(record, key);

    return record;
  }

  void fetchSegmentDescriptor(VM250RecordInternal record, RecordKey key)
          throws SQLException, Exception
  {
    int dataSourceId, rpmId;
    Timestamp rpmDateTime;
    boolean continuation;
    int[] gammaBackground;
    double neutronBackground;
    boolean gammaAlarm;
    boolean neutronAlarm;
    boolean realOccupancy;

    this.fetchSegmentDescriptorQuery.setLong(1, key.occupancyId);
    try (ResultSet rs = fetchSegmentDescriptorQuery.executeQuery())
    {
      if (rs == null)
      {
        return;
      }

      rs.next();
      dataSourceId = rs.getInt("siteId");
      rpmId = rs.getInt("laneId");
      rpmDateTime = rs.getTimestamp("DateTime");
      continuation = rs.getBoolean("continuation");
      gammaBackground = new int[]
      {
        rs.getInt("lastGB1"), rs.getInt("lastGB2"),
        rs.getInt("lastGB3"), rs.getInt("lastGB4")
      };
      neutronBackground = rs.getDouble("neutronBackground");
      gammaAlarm = rs.getBoolean("gammaAlarm");
      neutronAlarm = rs.getBoolean("neutronAlarm");
      realOccupancy = rs.getBoolean("realOccupancy");
    }

    record.setSegmentDescription(dataSourceId, rpmId, rpmDateTime.toInstant(), continuation, gammaBackground,
            neutronBackground, gammaAlarm, neutronAlarm, realOccupancy);
  }

  // private component of getRecord
  private void fetchPanelData(VM250RecordInternal record, long segmentDescriptorId) throws SQLException, Exception
  {
    ArrayList<ArrayList<Integer>> gammaCounts = new ArrayList<>();
    for (int panel = 0; panel < 4; panel++)
    {
      gammaCounts.add(new ArrayList<>());
    }
    ArrayList<Boolean> gammaAlarm = new ArrayList<>();

    this.fetchGammaPanelDataQuery.setLong(1, segmentDescriptorId);
    try (ResultSet rs = fetchGammaPanelDataQuery.executeQuery())
    {
      while (rs.next())
      {
        for (int panel = 0; panel < 4; panel++)
        {
          gammaCounts.get(panel).add(rs.getInt(panel + 1));
        }
        gammaAlarm.add(rs.getInt("alarm") == 1);
      }
    }

    ArrayList<ArrayList<Integer>> neutronCounts = new ArrayList<>();
    for (int panel = 0; panel < 4; panel++)
    {
      neutronCounts.add(new ArrayList<>());
    }
    ArrayList<Boolean> neutronAlarm = new ArrayList<>();

    this.fetchNeutronPanelDataQuery.setLong(1, segmentDescriptorId);
    try (ResultSet rs = fetchNeutronPanelDataQuery.executeQuery())
    {
      while (rs.next())
      {
        for (int panel = 0; panel < 4; panel++)
        {
          neutronCounts.get(panel).add(rs.getInt(panel + 1));
        }
        neutronAlarm.add(rs.getInt("alarm") == 1);
      }
    }

    for (int panel = 0; panel < 4; panel++)
    {
      record.getPanelData()[panel].resize(gammaCounts.get(panel).size());
      for (int i = 0; i < gammaCounts.get(panel).size(); i++)
      {
        record.getPanelData()[panel].gammaData[i][0] = gammaCounts.get(panel).get(i);
      }

      record.getPanelData()[panel].resize_neutron(neutronCounts.get(panel).size());
      for (int i = 0; i < neutronCounts.get(panel).size(); i++)
      {
        record.getPanelData()[panel].neutronData[i][0] = neutronCounts.get(panel).get(i);
      }
    }

    record.getSegmentDescription().setGammaAlarm(gammaAlarm);
    record.getSegmentDescription().setNeutronAlarm(neutronAlarm);
  }

  // private component of getRecord
  private void fetchVelocityData(VM250RecordInternal record, long segmentDescriptorId) throws SQLException, Exception
  {
    record.setVelocities(new ArrayList<>());

    this.fetchVelocitiesQuery.setLong(1, segmentDescriptorId);
    try (ResultSet rs = fetchVelocitiesQuery.executeQuery())
    {
      while (rs.next())
      {
        record.getVelocities().add(
                record.new VelocityReading(
                        rs.getTimestamp("DateTime").toInstant(),
                        rs.getDouble("mps")));
      }
    }

  }
  
  /**
   * Fetch VM250 configuration info: # of pre- and post-samples written,
   * N-Sigma alarm threshold.
   * FIXME: this method should use a datetime in case settings changed during
   * operation, but currently the SQL query only returns the most common settings
   * for specified lane.
   * 
   * @param record
   * @param key
   * @throws SQLException 
   */
  private void fetchSetupInfo(VM250RecordInternal record, RecordKey key) throws SQLException
  {
    this.fetchSetupQuery.setInt(1, key.dataSourceId);
    this.fetchSetupQuery.setInt(2, key.rpmId);
    try (ResultSet rs = fetchSetupQuery.executeQuery())
    {
      while (rs.next())
      {
        int intervals = rs.getInt("Intervals");
        int occupancyHoldin = rs.getInt("OccupancyHoldin");
        double nSigma = rs.getDouble("NSigma");
        record.setSetupInfo(intervals, occupancyHoldin, nSigma);
      }
    }
  }

  // private component of getRecord
  private void fetchBackgroundErrors(VM250RecordInternal record, long segmentDescriptorId) throws SQLException, Exception
  {
    this.fetchBackgroundErrorsQuery.setLong(1, segmentDescriptorId);
    try (ResultSet rs = fetchBackgroundErrorsQuery.executeQuery())
    {
      while (rs.next())
      {
        record.setGammaHighBackground(rs.getBoolean(1));
        record.setGammaLowBackground(rs.getBoolean(2));
        record.setNeutronHighBackground(rs.getBoolean(3));
      }
    }
  }

//<editor-fold desc="lane" defaultstate="collapsed">
  private void fetchLane(RecordKey key, Lane lane) throws RecordException
  {
    try
    {
      fetchLaneInfo(key, (LaneImpl) lane);
    }
    catch (Exception ex)
    {
      throw new RecordException("fetch lane info failed", ex);
    }

    ((LaneImpl) lane).setRpmId(key.rpmId);
    ((LaneImpl) lane).setPortId(key.dataSourceId);
    ((LaneImpl) lane).setPanels(panels);

    // Use the lane database to get lane info
    laneDatabase.lookup(lane);
  }

  private Lane fetchLaneInfo(RecordKey key, LaneImpl lane) throws SQLException, Exception
  {
    // No database of lane info For VM250, so hard-coding for now
    // TODO REFACTOR need real values for lane Type, Vector, Conveyance, and Width
    lane.setLaneType("Primary");
    lane.setLaneVector("Land Crossing");
    lane.setLaneConveyance("Cargo");
    lane.setLaneWidth(4.5);

    return lane;
  }

  /**
   * @return the laneDatabase
   */
  public LaneDatabase getLaneDatabase()
  {
    return laneDatabase;
  }

  /**
   * @param laneDatabase the laneDatabase to set
   */
  public void setLaneDatabase(LaneDatabase laneDatabase)
  {
    this.laneDatabase = laneDatabase;
  }
//</editor-fold>

//<editor-fold desc="internal structures" defaultstate="collapsed">
  static public class PanelData
  {
    static public int size = 18;
    public int rspId;
    public long segmentDescriptorId;
    public long sampleId;
    public int[] data = new int[size];
  };

  /**
   * RecordKey encapsulates all the fields required to query a record from each
   * of the tables.
   */
  static public class RecordKey
  {
    long occupancyId;
    Timestamp date;
    int dataSourceId;
    int rpmId;

    // TODO both these methods should be removed after
    // switching to PreparedStatements.
    public String getDateString()
    {
      Date d = new Date(date.getTime());
      return d.toString();
    }

    public String getTimeDateString()
    {
      return date.toString();
    }
  };

//</editor-fold>
//<editor-fold desc="debug" defaultstate="collapsed">
  // Debugging routine to see the contents of result set.
  public void dumpResultSet(ResultSet rs) throws SQLException
  {
    dumpResultSet(rs, System.out);
  }

  // Debugging routine to see the contents of result set.
  public void dumpResultSet(ResultSet rs, PrintStream out) throws SQLException
  {
    if (rs == null)
    {
      return;
    }

    ResultSetMetaData rsmd = rs.getMetaData();
    for (int i = 1; i <= rsmd.getColumnCount(); ++i)
    {
      out.print(rsmd.getColumnName(i)); //+ "(" + rsmd.getColumnType(i) + ")\t");
    }
    out.println();

    while ((rs != null) && (rs.next()))
    {
      for (int i = 1; i <= rsmd.getColumnCount(); ++i)
      {
        out.print(rs.getString(i) + "\t");
      }
      out.println();
    }
  }
//</editor-fold>

//</editor-fold>
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