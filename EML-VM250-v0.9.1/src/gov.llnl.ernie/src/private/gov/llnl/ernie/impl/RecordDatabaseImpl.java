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
package gov.llnl.ernie.impl;

import gov.llnl.ernie.io.RecordDatabase;
import gov.llnl.ernie.utility.Formatter;
import static gov.llnl.ernie.utility.Formatter.format;
import static gov.llnl.ernie.utility.Formatter.mapFromStrings;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * Base implementation of RecordDatabase
 *
 * @author guensche1
 */
public abstract class RecordDatabaseImpl implements RecordDatabase
{

  private Properties propertyMap;
  private BufferedWriter audit;
  protected Connection dbConnection;
  private String databaseName;
  private String lastQuery = null;

  protected RecordDatabaseImpl()
  {
  }


  public void initialize()
  {
    if (propertyMap == null)
    {
      throw new RuntimeException("Properties are not set");
    }
    String dbName = propertyMap.getProperty("defaultDatabase");
    String connection = propertyMap.getProperty("database.connection");
    connect(dbName, connection);
  }

  /**
   * TODO might want to get away from having this public.
   *
   * @return
   */
  public Connection getConnection()
  {
    return dbConnection;
  }

  /**
   * Turns on auditing of the SQL queries being submitted.
   */
  @Override
  public void enableAudit()
  {
    try
    {
      audit = Files.newBufferedWriter(Paths.get("audit.txt"));
    }
    catch (IOException ex)
    {
      throw new RuntimeException("Failed to open audit");
    }
  }

  /**
   * Disables auditing of SQL queries.
   */
  @Override
  public void disableAudit()
  {
    try
    {
      audit.close();
      audit = null;
    }
    catch (IOException ex)
    {
      // ignore
    }
  }

  /**
   * Internal function called to log queries for review.
   *
   * @param query
   */
  public void logQuery(String query)
  {
    this.lastQuery = query;
    try
    {
      if (audit != null)
      {
        audit.write(query);
        audit.newLine();
      }
    }
    catch (IOException ex)
    {
      throw new RuntimeException(ex);
    }
  }

  // Close the connection if we don't need it anymore
  @Override
  public void close()
  {
    try
    {
      dbConnection.close();
    }
    catch (Exception ex)
    {
      // ignore
    }
  }

  // TODO this method is a security hole - implementation needs to
  public void sqlCommand(String command)
  {
    try (Statement statement = dbConnection.createStatement())
    {
      // Fetch table
      logQuery(command);
      statement.execute(command);
      ResultSet rs = statement.getResultSet();
    }
    catch (SQLException ex)
    {
      ex.printStackTrace();
    }
  }

  /**
   * Connect to the databaseName
   *
   * @param targetDatabase
   * @param connection
   */
  private void connect(String targetDatabase, String connection)
  {
    try
    {
      databaseName = targetDatabase;
      Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
      String connectionString = format(connection, mapFromStrings(
              "database", databaseName
      ));
      dbConnection = DriverManager.getConnection(connectionString,
              propertyMap.getProperty("database.user"), 
              propertyMap.getProperty("database.pwd"));
      prepare();
    }
    catch (ClassNotFoundException | SQLException | Formatter.FormatterException ex)
    {
      throw new RuntimeException(ex);
    }
  }

  public void setProperties(Properties properties)
  {
    this.propertyMap = properties;
  }
  
  /**
   * @return the propertyMap
   */
  public Properties getPropertyMap()
  {
    return propertyMap;
  }

  /**
   * @return the databaseName
   */
  public String getDatabaseName()
  {
    return databaseName;
  }

  /**
   * @return the lastQuery
   */
  public String getLastQuery()
  {
    return lastQuery;
  }

  protected void prepare() throws SQLException
  {
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