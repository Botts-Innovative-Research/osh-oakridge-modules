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
import gov.llnl.ernie.vm250.data.VM250VehicleMotion;
import gov.llnl.ernie.vm250.data.VM250RecordInternal;
import gov.llnl.ernie.vm250.data.VM250Record;
import static gov.llnl.ernie.vm250.VM250RecordManipulatorUtilities.alterRecordVelocity;
import gov.llnl.ernie.manipulator.InjectionSourceLibrary;
import gov.llnl.ernie.data.Lane;
import gov.llnl.ernie.data.Record;
import gov.llnl.ernie.impl.RecordManipulatorOutputImpl;
import gov.llnl.ernie.manipulator.Manipulation;
import gov.llnl.ernie.manipulator.ManipulationDescription;
import gov.llnl.ernie.manipulator.ManipulationException;
import gov.llnl.ernie.manipulator.RecordManipulator;
import gov.llnl.ernie.internal.manipulator.AnisotropicModel;
import gov.llnl.ernie.internal.manipulator.ShieldingModel;
import gov.llnl.ernie.data.VehicleInfo;
import gov.llnl.math.DoubleArray;
import gov.llnl.math.matrix.Matrix;
import gov.llnl.math.matrix.MatrixColumnTable;
import gov.llnl.math.random.PoissonRandom;
import java.util.List;
import gov.llnl.ernie.data.SensorProperties;
import gov.llnl.ernie.data.SensorView;
import gov.llnl.ernie.rtk.DoubleSpectrum;
import gov.llnl.ernie.data.EnergyScale;
import gov.llnl.ernie.rtk.EnergyScaleFactory;
import gov.llnl.ernie.rtk.SpectrumAttributes;
import gov.llnl.ernie.rtk.view.FixedInstantList;
import gov.llnl.ernie.rtk.view.LinearTrace;

import gov.llnl.ernie.rtk.view.SensorViewEncounter;
import gov.llnl.ernie.rtk.view.Trace;
import gov.llnl.math.RebinUtilities;
import gov.llnl.math.euclidean.Vector3;
import gov.llnl.math.matrix.MatrixCollectors;
import gov.llnl.math.matrix.MatrixOps;
import gov.llnl.utility.xml.bind.ReaderInfo;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 *
 * @author nelson85
 */
@ReaderInfo(VM250RecordManipulatorReader.class)
public class VM250RecordManipulator implements RecordManipulator
{
  public static final double compactSourceExtent = 0.5;

  public static boolean applyShielding = true; // Enable/disable shielding
  
  private double nominalLaneWidth = 0;

  // Values tuned to match those found in real data records.
  public AnisotropicModel.Parameters passengerAreaModel = null;
  public List<AnisotropicModel.Parameters> cargoModel = new ArrayList<>();

  VM250MotionProfiler profiler = new VM250MotionProfiler();

  /**
   * (debug) Material shielding model for use with anisotropic sources,
   * currently Fe.
   */
  public ShieldingModel shieldingModel;
  /**
   * (debug) library of sources that can be injected
   */
  public InjectionSourceLibrary injectionSourceLibrary;

  /**
   * Create a record manipulator. The record manipulator is not ready for use
   * until the source library is loaded.
   */
  public VM250RecordManipulator()
  {
  }

//<editor-fold desc="loader" defaultstate="collapsed">
  public void setShieldingModel(ShieldingModel sm)
  {
    this.shieldingModel = sm;
  }

  public void setInjectionSourceLibrary(InjectionSourceLibrary isl)
  {
    this.injectionSourceLibrary = isl;
//
//    // FIXME this seems totally misplaced!
//    // normalize some sources:
//    for (int idx = 10000; idx < 10200; ++idx) // Medical training sources
//    {
//      injectionSourceLibrary.normalize(idx);
//    }
//    for (int idx = 20000; idx < 20200; ++idx) // Industrial training sources
//    {
//      injectionSourceLibrary.normalize(idx);
//    }
  }
//</editor-fold>

  /**
   * Initialize the class.
   *
   * Verifies all required resources are set and initializes any internal
   * memory.
   *
   */
  public void initialize()
  {
    ErniePackage.LOGGER.fine("Initializing");
    if (this.shieldingModel == null)
    {
      throw new RuntimeException("Shielding model not set");
    }
    if (this.injectionSourceLibrary == null)
    {
      throw new RuntimeException("Source library not set");
    }
  }

  /**
   * (debug) Method for MATLAB to create a manipulation object for testing.
   *
   * @param prefix
   * @return a blank manipulation record.
   */
  public Manipulation createManipulation(String prefix)
  {
    return new Manipulation(new ManipulationDescription(prefix));
  }

  @Override
  public Manipulation createManipulation(ManipulationDescription description)
  {
    return new Manipulation(description);
  }

  /**
   * Apply a manipulation to a record. This is the main method for this class.
   * It alters velocity and injects sources as directed by the manipulation
   * object. The method produces a new record with that same meta fields as the
   * original. It does not try to update the alarm flags, thus the alarm flags
   * are irregular.
   *
   * @param record is the base record to manipulate.
   * @param manipulation
   * @return a new record with the manipulation applied.
   * @throws ManipulationException when the alteration of velocity has failed.
   */
  @Override
  public Output applyManipulation(Record record, Manipulation manipulation)
          throws ManipulationException
  {
    RecordManipulatorOutputImpl output = new RecordManipulatorOutputImpl();
    long seed = record.getContextualInfo().getScanID();

    VM250Record recordIn = (VM250Record) record;

    // Alter the velocity if requested
    if (manipulation.isAlterVelocity())
    {
      recordIn = alterRecordVelocity((VM250Record) record,
              manipulation.getSpeedStart(), manipulation.getSpeedEnd());
    }

    // Duplicate the record
    VM250Record recordOut = new VM250Record(
            VM250RecordInternal.duplicate((VM250RecordInternal) recordIn.getInternal()));
    if (recordIn.getVehicleMotion() == null)
    {
      throw new ManipulationException("Record is not prepared!");
    }
    recordOut.setMotion(new VM250VehicleMotion(recordIn.getVehicleMotion()));

    VehicleInfo vehicleInfo = record.getVehicleInfo();
    
    double laneWidthFactor = 1.0;
    if (nominalLaneWidth != 0)
    {
      double laneWidth = record.getLane().getLaneWidth();
      laneWidthFactor = laneWidth / nominalLaneWidth;
      laneWidthFactor = Math.pow(laneWidthFactor, 1.5);
    }

    if (manipulation.isInjectCompact())
    {
      // If the source is in the front of the vehicle apply the passanger area
      // shielding model
      AnisotropicModel anisotropicModel = defineAnisotropicModel(vehicleInfo, manipulation,
              manipulation.getCompactPx());

      if (anisotropicModel != null)
      {
        anisotropicModel.setSeed(seed);
      }

      if (manipulation.getCompactSourceId() == null)
          throw new NullPointerException("Compact source Id is not set");

      if (!manipulation.getCompactSourceId().equals(Manipulation.EXTERNAL))
      {
        manipulation.setCompactSource(injectionSourceLibrary.getSource(manipulation.getCompactSourceId()));
//        manipulation.setCompactSourceSpectrum(getSourceGamma(manipulation.getCompactSourceId()));
      }
      addCompactSource(output, recordOut,
              manipulation.getCompactIntensity() * laneWidthFactor,
              manipulation.getCompactPx(),
              manipulation.getCompactPy(),
              manipulation.getCompactPz(),
              manipulation.getCompactSource(),
              anisotropicModel
      );
    }

    if (manipulation.isInjectDistributed())
    {

      double dx = (manipulation.getDistributedPx1() + manipulation.getDistributedPx2()) / 2;
      AnisotropicModel anisotropicModel = defineAnisotropicModel(vehicleInfo, manipulation, dx);
      if (anisotropicModel != null)
      {
        anisotropicModel.setSeed(seed);
      }

      if (!manipulation.getDistributedSourceId().equals(Manipulation.EXTERNAL))
      {
        manipulation.setDistributedSource(injectionSourceLibrary.getSource(manipulation.getDistributedSourceId()));
//        manipulation.setDistributedSourceSpectrum(getSourceGamma(manipulation.getDistributedSourceId()));
      }
      addDistributedSource(output, recordOut,
              manipulation.getDistributedIntensity1() * laneWidthFactor,
              manipulation.getDistributedIntensity2() * laneWidthFactor,
              manipulation.getDistributedPx1(),
              manipulation.getDistributedPx2(),
              manipulation.getDistributedPy(),
              manipulation.getDistributedPz(),
              manipulation.getDistributedSource(),
              anisotropicModel
      );
    }

    // Compute the SNR relative to the initial record.
    computeSNR(output, recordIn, recordOut);

    output.setRecord(recordOut);
    return output;
  }

//  /**
//   * (debug) Retrieve a source from the ERNIE injection library. This should
//   * always be 9 channel output. The source associated with given id may be
//   * stored internally as 9-channel or 256-channel (256-channel is better when
//   * drift != 1.0)
//   *
//   * @param id source id
//   * @param drift coefficient to apply. drift=1.0 returns original source
//   * @return 9-channel gamma spectrum
//   */
//  public double[] getSourceGamma(int id, double drift) throws ManipulationException
//  {
//    try
//    {
//      return injectionSourceLibrary.getGammaSpectrum(id, drift);
//    }
//    catch (Exception ex)
//    {
//      throw new ManipulationException(String.format(
//              "Exception encountered in RecordManipulator.getSourceGamma: %s",
//              ex.toString()));
//    }
//  }
//  public double[] getSourceGamma(int id) throws ManipulationException
//  {
//    return getSourceGamma(id, 1.0);
//  }
  /**
   * Convert the lane info into a set of sensor views for FOV calculation.
   *
   * @param lane
   * @return
   */
  public static List<SensorView> createPanels(Lane lane)
  {
    return lane.getGammaSensorProperties()
            .stream()
            .map(p -> p.getSensorView())
            .collect(Collectors.toList());
  }

  /**
   * (internal) Adds a compact source to the gamma spectrum of a scan. Uses the
   * source anisotropy model stored in the class if available.
   *
   * @param record
   * @param compactIntensity
   * @param compactPx
   * @param compactPy
   * @param compactPz
   * @param source
   * @return
   */
  private VM250Record addCompactSource(
          RecordManipulatorOutputImpl output,
          VM250Record record,
          double compactIntensity,
          double compactPx, double compactPy, double compactPz,
          DoubleSpectrum source,
          AnisotropicModel anisotropicModel)
          throws ManipulationException
  {
    if (source == null)
      throw new NullPointerException("source is null.");
    if (record == null)
      throw new NullPointerException("record is null.");

    try
    {
      // Use the lane to create the gamma sensors
      Lane lane = record.getLane();
      Duration period = lane.getGammaSensorProperties().get(0).getSamplePeriod();
      List<SensorView> panels = createPanels(lane);

      // Get the solid angle for 1m distant from detector so we can use
      // relative counts in the detector
      //
      //  FIXME the reference distance is not typically 1m for large panels
      double referenceDistance = source.getAttribute(SpectrumAttributes.DISTANCE, Double.class, 1.0);
      double pi0 = panels.get(0).computeDefaultSolidAngle(referenceDistance);

      // Get the vehicle motion profile
      VM250VehicleMotion mpr = record.getVehicleMotion();
      if (mpr == null)
        throw new ManipulationException("Motion is not available");
      double[] position = mpr.getPosition();

      // Compute the intensity as a function of position using the view angle.
      SensorViewEncounter sve = new SensorViewEncounter();
      Vector3 sourcePosition = Vector3.of(-compactPx, compactPy, compactPz);
      FixedInstantList intervals
              = new FixedInstantList(Instant.ofEpochMilli(0), period, position.length + 1);
      Trace trace = LinearTrace.create(intervals, position, sourcePosition);
      List<SensorViewEncounter.Output> out = sve.simulate(panels, intervals, trace);

      // Convert the output into
      Matrix.ColumnAccess intensity = out.stream()
              .map(p -> DoubleArray.multiply(p.solidAngle, compactIntensity / pi0))
              .collect(MatrixCollectors.asColumns());

      // Apply the shielding model
      Matrix shielding = null;
      if (applyShielding)
      {
        shielding = computeAnisotropy(position, sourcePosition, lane, this.compactSourceExtent, anisotropicModel);
      }

      // Set up to apply random statistics
      PoissonRandom pr = new PoissonRandom();
      pr.setSeed(record.getContextualInfo().getScanID());

      // Inject into the record
      inject(output, record, intensity, shielding, source, pr);

      // Store the audit information
      output.compactIntensity = intensity;
      output.compactSource = source;
      output.compactAnisotropicModel = anisotropicModel;
      output.compactShielding = shielding;

      return record;
    }
    catch (Exception ex)
    {
      throw new ManipulationException(
              "Exception encountered in RecordManipulator.addCompactSource",
              ex);
    }
  }

  private VM250Record addDistributedSource(RecordManipulatorOutputImpl output, VM250Record record,
          double distributedIntensity1, double distributedIntensity2,
          double distributedPx1, double distributedPx2,
          double distributedPy, double distributedPz, DoubleSpectrum source,
          AnisotropicModel anisotropicModel) throws ManipulationException
  {
    try
    {
      // Get the lane
      Lane lane = record.getLane();
      Duration period = lane.getGammaSensorProperties().get(0).getSamplePeriod();
      List<SensorView> panels = createPanels(lane);

      // Get the vehicle motion profile
      VM250VehicleMotion mpr = record.getVehicleMotion();
      double[] vehiclePosition = mpr.getPosition();

      // Integrate the distributed source
      SensorViewEncounter sve = new SensorViewEncounter();
      FixedInstantList intervals
              = new FixedInstantList(Instant.ofEpochMilli(0), period, vehiclePosition.length + 1);
      int n = 13;

      // Set up random number generator
      PoissonRandom pr = new PoissonRandom();
      pr.setSeed(record.getContextualInfo().getScanID());

      Matrix shielding = null;
      if (applyShielding)
      {
        shielding = computeAnisotropy(vehiclePosition,
                Vector3.of(-(distributedPx2 + distributedPx1) / 2, distributedPy, distributedPz), lane,
                (distributedPx2 - distributedPx1),
                anisotropicModel);
      }

      // Get the solid angle for 1m distant from detector so we can use
      // relative counts in the detector
      double kappa = (distributedPx2 - distributedPx1)
              / panels.get(0).computeDefaultSolidAngle(1.0)
              / 3.0 / (n - 1);
      for (int i = 0; i < n; ++i)
      {
        // Simpson's rule
        int coef = 2 + (2 * (i & 1));
        if (i == 0 || i == n - 1)
          coef = 1;

        // Compute the projection
        double f = ((double) i) / (n - 1);
        double pointIntensity = (1 - f) * distributedIntensity1 + f * distributedIntensity2;
        double px = (1 - f) * distributedPx1 + f * distributedPx2;
        Vector3 sourcePosition = Vector3.of(-px, distributedPy, distributedPz);
        Trace trace = LinearTrace.create(intervals, vehiclePosition, sourcePosition);
        List<SensorViewEncounter.Output> out = sve.simulate(panels, intervals, trace);

        // Convert to matrix
        Matrix.ColumnAccess intensity = out.stream().map(p -> p.solidAngle)
                .collect(MatrixCollectors.asColumns());
        MatrixOps.multiplyAssign(intensity, pointIntensity * coef * kappa);

        inject(output, record, intensity, shielding, source, pr);
      }

      output.distributedAnisotopicModel = anisotropicModel;
      return record;
    }
    catch (Exception ex)
    {
      throw new ManipulationException(
              "Exception encountered in RecordManipulator.addDistributedSource",
              ex);
    }
  }

  double[] edges = new double[]
  {
    0, 57, 119, 243, 504, 996, 1168, 1468, 2926, 3113
  };
  EnergyScale referenceScale = EnergyScaleFactory.newScale(edges);

  /**
   * inject: modify record with simulated gamma panel data for the given source
   * and intensity. If shielding is provided (shielding != null), it is applied
   * to the source before injection.
   *
   * @param recordInternal
   * @param intensity
   * @param shielding
   * @param source
   * @throws Exception
   */
  private void inject(RecordManipulatorOutputImpl output, VM250Record record, Matrix intensity,
          Matrix shielding, DoubleSpectrum sourceSpectrum, PoissonRandom pr) throws ManipulationException
  {
    VM250RecordInternal recordInternal = record.getInternal();
    // Verify the size of the injection
    if (recordInternal.getPanelData().length != intensity.columns()
            || recordInternal.getLength() != intensity.rows())
    {
      StringBuilder sb = new StringBuilder();
      sb.append("Size mismatch in data.\n");
      sb.append("record.panelData.length ").append(recordInternal.getPanelData().length).append("\n");
      sb.append("intensity.columns ").append(intensity.columns()).append("\n");
      sb.append("record.getLength ").append(recordInternal.getLength()).append("\n");
      sb.append("intensity.rows ").append(intensity.rows()).append("\n");

      throw new ManipulationException(sb.toString());
    }

    // Mark the record as injected
    recordInternal.setIsInjected(true);

    // Convert to 9 channel format for shielding calculation.
    // FIXME assumes the transport matrix for VM250 is same as RPM8
    double[] source;
    try
    {
      source = sourceSpectrum.rebin(referenceScale).toDoubles();
    }
    catch (RebinUtilities.RebinException ex)
    {
      throw new ManipulationException("rebin failure", ex);
    }

    double flux[] = null;
    if (shielding != null)
    {
      flux = this.shieldingModel.estimateFlux(source);
      output.flux = flux;
    }

    EnergyScale energyScale
            = record.getLane().getGammaSensorProperties().get(0).getEnergyScale();

    // FIXME there is a problem here.  Our shielding models are in terms of RPM8
    // data and the energy structure for the VM250 one channel.  If we bin
    // immediately to the 1 channel we can't compute shielding.  Thus we have to
    // convert once to RPM8 structure, then again to VM250.
    // Add Poisson data multiplying the intensity times the source distribution
    for (int i0 = 0; i0 < intensity.columns(); ++i0)
    {
      for (int i1 = 0; i1 < intensity.rows(); ++i1)
      {
        // Multiply the source by the intensity
        double[] expected;

        // Apply shielding if available
        if (shielding != null)
        {
          expected = this.shieldingModel.computeFromFlux(flux, shielding.get(i1, i0));
        }
        else
        {
          expected = source.clone();
        }
        DoubleArray.multiplyAssign(expected, intensity.get(i1, i0));

        // Convert 9 channels to 1 channel
        // FIXME drift paramters need to come into play when we do final conversion
        try
        {
          DoubleSpectrum post = new DoubleSpectrum(expected);
          post.setEnergyScale(referenceScale);
          post.rebinAssign(energyScale);
          expected = post.toDoubles();
        }
        catch (RebinUtilities.RebinException ex)
        {
          throw new RuntimeException(ex);
        }

        // Draw a sample. Note i2 goes over energy channels, i1 over time steps
        for (int i2 = 0; i2 < expected.length; ++i2)
        {
          int counts = pr.draw(expected[i2]);
          recordInternal.getPanelData()[i0].gammaData[i1][i2] += counts;
        }
      }
    }
  }

  /**
   *
   * @param position is the location of the front of the vehicle as a function
   * of time.
   * @param lane is the lane definition for injection
   * @return the areal density of the shielding for each lane and time position.
   */
  private static Matrix computeAnisotropy(
          double[] position,
          Vector3 source,
          Lane lane,
          double extent,
          AnisotropicModel anisotropicModel)
  {
    if (anisotropicModel == null)
      return null;

    List<SensorProperties> gammaProps = lane.getGammaSensorProperties();
    Matrix out = new MatrixColumnTable(position.length, gammaProps.size());

    // Sources positions are defined relative to be following the front
    // and thus are negative, but the anisotropic model was flipped from
    // that orientation.
    anisotropicModel.defineSource(-source.getX(), source.getY());

    // For each gamma sensor
    for (int j = 0; j < gammaProps.size(); j++)
    {
      SensorProperties detector = gammaProps.get(j);
      Vector3 origin = detector.getOrigin();
      for (int i = 0; i < position.length; i++)
      {
        out.set(i, j, anisotropicModel.computeBetween(
                position[i] - origin.getX() - extent / 2, origin.getY(),
                position[i] - origin.getX() + extent / 2, origin.getY()));
      }
    }
    return out;
  }

  /**
   * Get an anisotropic model based on the position of the source and the
   * requested cargo model.
   *
   * @param vehicleInfo
   * @param manipulation
   * @param x
   * @return
   */
  private AnisotropicModel defineAnisotropicModel(VehicleInfo vehicleInfo, Manipulation manipulation, double x)
  {
    AnisotropicModel anisotropicCompactModel = null;
    if (vehicleInfo.isInPassengerArea(x))
    {
      anisotropicCompactModel = new AnisotropicModel();
      anisotropicCompactModel.defineBox(vehicleInfo.getPassengerAreaStart(), vehicleInfo.getPassengerAreaEnd(),
              -2, 2, this.passengerAreaModel);
    }
    else if (vehicleInfo.isInPayloadArea(x) && manipulation.getCargoModelId() > 0)
    {
      anisotropicCompactModel = new AnisotropicModel();
      anisotropicCompactModel.defineBox(vehicleInfo.getPayloadAreaStart(), vehicleInfo.getPayloadAreaEnd(),
              -2, 2, this.cargoModel.get(manipulation.getCargoModelId() - 1));
    }
    return anisotropicCompactModel;
  }

  /**
   * Compute the SNR based on the final injection.
   *
   * @param output
   * @param recordIn
   * @param recordOut
   */
  private void computeSNR(RecordManipulatorOutputImpl output, VM250Record recordIn, VM250Record recordOut)
  {
    VM250RecordInternal recordInInternal = recordIn.getInternal();
    VM250RecordInternal recordOutInternal = recordOut.getInternal();

    // Add Poisson data multiplying the intensity times the source distribution
    double grossSignal = 0, grossBck = 0;
    double metricBest = 0;
    double metric = 0;
    for (int i0 = 0; i0 < recordInInternal.getNPanels(); ++i0)
    {
//      double metric = 0;
      for (int i1 = 0; i1 < recordInInternal.getLength(); ++i1)
      {

        // Draw a sample. Note i2 goes over energy channels, i1 over time steps
        int bck = 0;
        int signal = 0;
        for (int i2 = 0; i2 < recordInInternal.getPanelData()[i0].gammaData[i1].length; ++i2)
        {
          bck += recordInInternal.getPanelData()[i0].gammaData[i1][i2];
          signal += recordOutInternal.getPanelData()[i0].gammaData[i1][i2]
                  - recordInInternal.getPanelData()[i0].gammaData[i1][i2];
        }
        if (bck > 0)
        {
          metric += signal * signal / (double) bck;
        }
        grossSignal += signal;
        grossBck += bck;
      }

      // FIXME is the injected SNR supposed to be max per panel or over all panels
      // The previous was defined in a very confusing manner.
      metricBest = Math.max(metricBest, Math.sqrt(metric));
    }

    output.setInjectedSNR(metricBest);

    // Gross SNR is over all panels
    output.setGrossSNR(Math.sqrt(grossSignal * grossSignal / (double) grossBck));
  }

  @Override
  public InjectionSourceLibrary getLibrary()
  {
    return this.injectionSourceLibrary;
  }

  @Override
  public void injectGamma(Record record, double[] position, Matrix[] gammaData)
  {
    throw new UnsupportedOperationException("Unused but required by interface RecordManipulator.");
  }

  /**
   * @return the nominalLaneWidth
   */
  public double getNominalLaneWidth()
  {
    return nominalLaneWidth;
  }

  /**
   * @param nominalLaneWidth the nominalLaneWidth to set
   */
  public void setNominalLaneWidth(double nominalLaneWidth)
  {
    this.nominalLaneWidth = nominalLaneWidth;
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