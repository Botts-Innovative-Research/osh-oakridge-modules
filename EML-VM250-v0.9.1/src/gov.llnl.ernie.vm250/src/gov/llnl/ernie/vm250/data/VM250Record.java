/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.ernie.vm250.data;

import gov.llnl.ernie.data.Record;
import gov.llnl.ernie.data.Lane;
import gov.llnl.ernie.data.ScanContextualInfo;
import gov.llnl.ernie.data.SensorPosition;
import gov.llnl.ernie.data.VPSMeasurement;
import gov.llnl.ernie.data.VehicleClass;
import gov.llnl.ernie.data.VehicleInfo;
import gov.llnl.ernie.data.VendorAnalysis;
import gov.llnl.math.matrix.Matrix;
import gov.llnl.ernie.data.SensorBackground;
import gov.llnl.ernie.data.SensorMeasurement;
import gov.llnl.ernie.impl.CombinedSensorBackground;
import gov.llnl.ernie.impl.CombinedSensorMeasurement;
import gov.llnl.ernie.impl.ScanContextualInfoImpl;
import gov.llnl.ernie.impl.SensorPropertiesCombined;
import gov.llnl.ernie.vehicle.VehicleInfoRescaled;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.IntStream;

/**
 *
 * @author nelson85
 */
public class VM250Record implements Record
{
  private static final long serialVersionUID = gov.llnl.utility.UUIDUtilities.createLong("VM250Record-v1");

//<editor-fold desc="fields" defaultstate="collapsed">
  private final VM250RecordInternal internal;

  private final List<SensorMeasurement> gammaMeasurements = new ArrayList<>();
  private final List<SensorMeasurement> neutronMeasurements = new ArrayList<>();
  private final List<SensorBackground> gammaBackgrounds = new ArrayList<>();
  private final List<SensorBackground> neutronBackgrounds = new ArrayList<>();

  // Computed by PreprocessorBackground
  public transient Matrix gammaBackgroundGross; // time x panel
  public transient Matrix gammaBackgroundSpectrum; // energy x panel
  public transient double[] gammaBackgroundPanelRate; // 1 x panel  
  private VehicleInfo vehicleInfo;


  // Diagnostics for postprocess
  public enum Fault implements gov.llnl.ernie.Fault
  {
    BAD_GAMMAS("gamma data missing from some panels"),
    BAD_LENGTH("encounter too short"),
    BAD_VELOCITY("Velocity too high"),
    MISSING_VELOCITY("No reliable velocity readings found"),
    MISSING_GAMMA_BACKGROUND("Missing gamma background data"),
    MISSING_GAMMA_PANELS("Missing data from one or more gamma panels"),
    BAD_BACKGROUND("Background estimator failed"),
    BAD_VEHICLE_MOTION("Motion profile failed"),
    INCORRECT_NUM_PANELS("Incorrect numbers of panels"),
    BAD_DATA("Bad data in record"),
    UNABLE_TO_CLASSIFY_VEHICLE("No vehicle class");

    private String reason;

    Fault(String reason)
    {
      this.reason = reason;
    }

    public String getReason()
    {
      return this.reason;
    }
  }
  transient private EnumSet<Fault> faults = EnumSet.noneOf(Fault.class);

  /**
   * The motion data extract from the velocity readings. Computed by
   * {@link #postprocess}
   */
  private VM250VehicleMotion motion;
  public VehicleClass vehicleClass;
  public double bestVelocity;

  private VendorAnalysis vendorAnalysis;
//</editor-fold>
//<editor-fold desc="ctor" defaultstate="collapsed">

  /**
   * @param record the VM250 record to wrap
   */
  public VM250Record(VM250RecordInternal record)
  {
    this.internal = record;
    // Sanity checks.
    if (record == null)
    {
      throw new RuntimeException("Record is null");
    }

    this.vendorAnalysis = new VM250VendorAnalysis(record);

    if (record.getLane() == null)
    {
      throw new RuntimeException("Lane is not configured in record.");
    }

    // Set up views for all available data.
    for (int i = 0; i < record.getLane().getGammaSensorProperties().size(); ++i)
    {
      gammaMeasurements.add(new VM250GammaMeasurement(i, this));
      gammaBackgrounds.add(new VM250GammaPanelBackground(i, this));
    }
    for (int i = 0; i < record.getLane().getNeutronSensorProperties().size(); ++i)
    {
      neutronMeasurements.add(new VM250NeutronMeasurement(i, this));
      neutronBackgrounds.add(new VM250NeutronPanelBackground(i, this));
    }
  }

//</editor-fold>
//<editor-fold desc="info" defaultstate="collapsed">
  /**
   * Determine if this record is unsuitable for analysis.
   *
   * @return true if something required for analysis is missing.
   */
  @Override
  public boolean bad()
  {
    return (!this.faults.isEmpty())
            || internal.isGammaHighBackground() || internal.isGammaLowBackground()
            || internal.isNeutronHighBackground()
            || (this.getInternal() == null)
            || (this.getLane() == null)
            || this.getVPSMeasurement().getGammaOccupancyStart() == 0
            || (this.getVPSMeasurement().getGammaOccupancyEnd()
            <= this.getVPSMeasurement().getGammaOccupancyStart())
            || this.getCombinedGammaMeasurement(SensorPosition.ALL).size()
            - this.getVPSMeasurement().getGammaOccupancyEnd() <= 0
            || (motion == null)
            || (!motion.isGood())
            || (this.motion.getVehicleLength() > 80)
            || (this.vehicleClass == null);
  }

  @Override
  public List<String> getBadReasons()
  {
    List<String> badReasons = new ArrayList();
    for (Fault fault : this.faults)
    {
      badReasons.add(fault.getReason());
    }
    if (internal.isGammaHighBackground())
    {
      badReasons.add("High gamma background");
    }
    if (internal.isGammaLowBackground())
    {
      badReasons.add("Low gamma background");
    }
    if (internal.isNeutronHighBackground())
    {
      badReasons.add("High neutron background");
    }
    /*if (this.getInternal().getVelocities().isEmpty())
    {
      badReasons.add("no velocity info");
    }*/
    if (this.getVPSMeasurement().getGammaOccupancyStart() == 0)
    {
      badReasons.add("missing occupancy start");
    }
    if (this.getVPSMeasurement().getGammaOccupancyEnd()
            <= this.getVPSMeasurement().getGammaOccupancyStart())
    {
      badReasons.add("zero-length record");
    }
    if (this.getCombinedGammaMeasurement(SensorPosition.ALL).size()
            - this.getVPSMeasurement().getGammaOccupancyEnd() <= 0)
    {
      badReasons.add("missing post-samples");
    }
    if (this.getLane() == null)
    {
      badReasons.add("unable to assign lane");
    }
    if (this.motion == null)
    {
      badReasons.add("unable to extract motion profile");
    }
    else
    {
      if (!this.motion.isGood())
      {
        badReasons.add("bad motion profile");
      }
      if (this.motion.getVehicleLength() > 80)
      {
        badReasons.add("unphysical vehicle length");
      }
    }
    if (this.vehicleClass == null)
    {
      badReasons.add("unable to type vehicle");
    }
    return badReasons;
  }

  @Override
  public Lane getLane()
  {
    return internal.getLane();
  }
//</editor-fold>
//<editor-fold desc="radiation measurements" defaultstate="collapsed">    

  @Override
  public List<SensorMeasurement> getGammaMeasurements()
  {
    return gammaMeasurements;
  }

  @Override
  public List<SensorBackground> getGammaBackgrounds()
  {
    return Collections.<SensorBackground>unmodifiableList(this.gammaBackgrounds);
  }

  @Override
  public SensorMeasurement getCombinedGammaMeasurement(SensorPosition position)
  {
    List<SensorMeasurement> values
            = Arrays.asList(IntStream.of(getLane().getGammaPanels(position))
                    .mapToObj(p -> this.gammaMeasurements.get(p))
                    .toArray(p -> new SensorMeasurement[p]));
    return new CombinedSensorMeasurement(
            new SensorPropertiesCombined(getLane().getGammaSensorProperties().get(0)),
            values);
  }

  @Override
  public SensorBackground getCombinedGammaBackground(SensorPosition position)
  {
    List<SensorBackground> values
            = Arrays.asList(IntStream.of(getLane().getGammaPanels(position))
                    .mapToObj(p -> this.gammaBackgrounds.get(p))
                    .toArray(p -> new SensorBackground[p]));
    return new CombinedSensorBackground(
            new SensorPropertiesCombined(getLane().getGammaSensorProperties().get(0)),
            values);
  }

  @Override
  public List<SensorMeasurement> getNeutronMeasurements()
  {
    return Collections.<SensorMeasurement>unmodifiableList(this.neutronMeasurements);
  }

  @Override
  public List<SensorBackground> getNeutronBackgrounds()
  {
    return Collections.<SensorBackground>unmodifiableList(neutronBackgrounds);
  }

  public SensorMeasurement getCombinedNeutronMeasurement(SensorPosition position)
  {
    List<SensorMeasurement> values
            = Arrays.asList(IntStream.of(getLane().getGammaPanels(position))
                    .mapToObj(p -> this.neutronMeasurements.get(p))
                    .toArray(p -> new SensorMeasurement[p]));
    return new CombinedSensorMeasurement(
            new SensorPropertiesCombined(getLane().getNeutronSensorProperties().get(0)),
            values);
  }

  @Override
  public SensorBackground getCombinedNeutronBackground(SensorPosition position)
  {
    List<SensorBackground> values
            = Arrays.asList(IntStream.of(getLane().getGammaPanels(position))
                    .mapToObj(p -> this.neutronBackgrounds.get(p))
                    .toArray(p -> new SensorBackground[p]));
    return new CombinedSensorBackground(
            new SensorPropertiesCombined(getLane().getNeutronSensorProperties().get(0)),
            values);
  }
//</editor-fold>
//<editor-fold desc="motion" defaultstate="collapsed">

  @Override
  public VPSMeasurement getVPSMeasurement()
  {
    return new VM250VPSMeasurement(this);
  }

  @Override
  public VM250VehicleMotion getVehicleMotion()
  {
    return motion;
  }

  @Override
  public VehicleInfo getVehicleInfo()
  {
    if (vehicleClass == null)
      throw new NullPointerException("vehicle class is not set");
    if (motion == null)
      throw new NullPointerException("motion is not set");

    return new VehicleInfoRescaled(vehicleClass.getInfo(), motion.getVehicleLength(), new double[0]);
  }

  @Override
  public VehicleClass getVehicleClass()
  {
    return vehicleClass;
  }
//</editor-fold>
//<editor-fold desc="vendor">

  @Override
  public VendorAnalysis getVendorAnalysis()
  {
    return vendorAnalysis;
  }

  @Override
  public VM250RecordInternal getInternal()
  {
    return internal;
  }
//</editor-fold>

  public void setMotion(VM250VehicleMotion extract)
  {
    this.motion = extract;
  }

  // extra stuff (specific to VM250Record):
  /**
   * @param vehicleType the vehicleType to set
   */
  public void setVehicleType(VehicleClass vehicleType)
  {
    this.vehicleClass = vehicleType;
    // TODO which should it be?
    this.internal.setVehicleClass(vehicleClass);
  }

  @Override
  public void setVehicleClass(VehicleClass classify)
  {
    this.vehicleClass = classify;
  }

  @Override
  public void addFault(gov.llnl.ernie.Fault fault)
  {
    if (fault == null || !(fault instanceof Fault))
    {
      throw new RuntimeException("Bad fault");
    }

    faults.add((Fault) fault);
  }

  @Override
  public void setVehicleInfo(VehicleInfo defaultVehicleInfo)
  {
    this.vehicleInfo = defaultVehicleInfo;
  }
  
  @Override
  public ScanContextualInfo getContextualInfo()
  {
    return new ScanContextualInfoImpl(
            internal.getOccupancyId(),                // Scan ID
            internal.getLane().getPortId(),           // Port ID
            internal.getDataSourceId(),               // Site ID
            internal.getLane().getRpmId(),            // Lane ID
            0,                                        // Vehicle ID doesn't have VM250 analog
            1,                                        // numSegments
            internal.getRpmDateTime()                 // Timestamp
    );
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