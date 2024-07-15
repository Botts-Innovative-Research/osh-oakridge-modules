/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.ernie.data;

import java.io.Serializable;
import java.util.List;

/**
 * Unified structure for Radiation Portal Monitor (RPM) scan data. This is the
 * generic interface exposing data from multiple portal types. Individual vendor
 * materials may expose additional information.
 *
 * @author nelson85
 */
public interface Record extends Serializable
{
//<editor-fold desc="Record specific identifiers" defaultstate="collapsed">
  ScanContextualInfo getContextualInfo();
//</editor-fold>
//<editor-fold desc="Portal specifics" defaultstate="collapsed">
  Lane getLane();

  /**
   * Get the vendor specific vps measurement.
   *
   * @return the vps data.
   */
  VPSMeasurement getVPSMeasurement();

  /**
   * Get the details of the vendor analysis. Most units have an inherent
   * detection algorithm. We use the vendor analysis when we are unable to
   * provide a more detailed ernie analysis for fallback.
   *
   * @return the vendor analysis or null if not available.
   */
  VendorAnalysis getVendorAnalysis();
//</editor-fold>
//<editor-fold desc="measurements" defaultstate="collapsed">

  /**
   * Get a measurements from a the gamma sensors.
   *
   * @return a list measurements containing all gamma sensor data.
   */
  List<SensorMeasurement> getGammaMeasurements();

  /**
   * Get a composite of all of the gamma measurements summed from the gamma
   * sensors.
   *
   * @param position
   * @return a virtual summed gamma sensor.
   */
  SensorMeasurement getCombinedGammaMeasurement(SensorPosition position);

  default SensorMeasurement getCombinedGammaMeasurement()
  {
    return this.getCombinedGammaMeasurement(SensorPosition.ALL);
  }

  /**
   * Get neutron measurements from a specified panel.
   *
   * @return a list measurements containing all neutron sensor data.
   */
  List<SensorMeasurement> getNeutronMeasurements();

  /**
   * Get a composite of all of the gamma measurements summed from the neutron
   * sensors.
   *
   * @param position
   * @return a virtual summed neutron sensor.
   */
  SensorMeasurement getCombinedNeutronMeasurement(SensorPosition position);
//</editor-fold>
//<editor-fold desc="derived" defaultstate="collapsed">

  /**
   * Check whether a record has data quality issues that prevent an ERNIE
   * analysis
   *
   * @return true if data quality issues found
   */
  boolean bad();

  /**
   * Get list of reasons that the record is 'bad'
   *
   * @return a list of strings. List will be empty if the record is not bad
   */
  List<String> getBadReasons();

  /**
   * Get the motion profile.
   *
   * @return the motion profile, or null if the motion cannot be determined.
   */
  VehicleMotion getVehicleMotion();

  /**
   * Get the Vehicle description for display.
   *
   * This is rescaled to match the vehicle dimensions of the scale.
   *
   * @return the vehicle type
   */
  VehicleInfo getVehicleInfo();

  /**
   * Get the class this vehicle was assigned to.
   *
   * @return
   */
  VehicleClass getVehicleClass();

  /**
   * Get the expected gamma background for this scan.
   *
   * @return a list of background measurements for the gamma sensors.
   */
  List<SensorBackground> getGammaBackgrounds();

  /**
   * Get the expected neutron background for this scan.
   *
   * @return a list of background measurements for the neutron sensors.
   */
  List<SensorBackground> getNeutronBackgrounds();

  SensorBackground getCombinedGammaBackground(SensorPosition p);

  default SensorBackground getCombinedGammaBackground()
  {
    return this.getCombinedGammaBackground(SensorPosition.ALL);
  }

  SensorBackground getCombinedNeutronBackground(SensorPosition p);

//</editor-fold>
  Object getInternal();

  void setVehicleClass(VehicleClass classify);

  default void setVehicleFeatures(double[] features)
  {
    return;
  }

  public void setVehicleInfo(VehicleInfo defaultVehicleInfo);

  void addFault(gov.llnl.ernie.Fault fault);

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