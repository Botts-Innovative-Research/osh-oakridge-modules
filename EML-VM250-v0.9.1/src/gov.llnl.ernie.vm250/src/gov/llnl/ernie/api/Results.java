/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.ernie.api;

import gov.llnl.ernie.Analysis;
import gov.llnl.ernie.Analysis.RecommendedAction;
import gov.llnl.ernie.data.AnalysisResult;
import gov.llnl.ernie.data.Record;
import gov.llnl.ernie.vm250.data.VM250AnalysisSource;
import gov.llnl.ernie.vm250.data.VM250RecordInternal;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 *
 * @author mattoon1
 */
@XmlRootElement(name = "results")
@XmlAccessorType(XmlAccessType.FIELD)
public class Results
{

  public enum SourceType
  {
    NonEmitting(0), NORM(1), Threat(2);

    private SourceType(int value)
    {
      this.value = value;
    }

    public int getValue()
    {
      return value;
    }
    private final int value;
  }

  @XmlElement(name = "ERNIEContextualInfo", required = true)
  private ERNIEContextualInfo ernieContextualInfo;
  @XmlElement(name = "ScanContextualInfo", required = true)
  private ScanContextualInfo scanContextualInfo;
  @XmlElement(name = "ERNIEAnalysis", required = true)
  private ERNIEAnalysis ernieAnalysis;

  public Results()
  {
    ernieContextualInfo = new ERNIEContextualInfo();
    scanContextualInfo = new ScanContextualInfo();
    ernieAnalysis = new ERNIEAnalysis();
  }
  
  public Results(ERNIEContextualInfo ernieInfo, ScanContextualInfo scanInfo,
          ERNIEAnalysis analysis)
  {
    ernieContextualInfo = ernieInfo;
    scanContextualInfo = scanInfo;
    ernieAnalysis = analysis;
  }
  
  public Results(Analysis analysis, AnalysisResult result)
  {
    Thresholds thresh = new Thresholds(analysis.getThreshold());
    this.ernieContextualInfo = new ERNIEContextualInfo(
            "version4",  // FIXME where should version come from?
            analysis.getModelName(),
            thresh
    );
    
    Record record = result.getRecord();
    VM250RecordInternal internal = (VM250RecordInternal)record.getInternal();
    /*
    gov.llnl.ernie.data.ScanContextualInfo scanContext = result.getScanContextualInfo();
    gov.llnl.ernie.data.VendorAnalysis vendorAnalysis = result.getVendorAnalysis();
    */
    gov.llnl.ernie.data.VendorAnalysis vendorAnalysis = record.getVendorAnalysis();
    gov.llnl.ernie.data.Lane lane = record.getLane();

    RecommendedAction RPMresult = (
            vendorAnalysis.isAlarm() ? RPMresult = RecommendedAction.INVESTIGATE : RecommendedAction.RELEASE);

    this.scanContextualInfo = new ScanContextualInfo(
            String.valueOf(lane.getPortId()), (int)lane.getRpmId(),
            record.getContextualInfo().getTimestamp(), record.getContextualInfo().getScanID(), RPMresult,
            vendorAnalysis.isGammaAlarm(), vendorAnalysis.isNeutronAlarm(), vendorAnalysis.isScanError()
            );
    
    this.ernieAnalysis = new ERNIEAnalysis();
    ernieAnalysis.result = result.getRecommendedAction();
    ernieAnalysis.investigateProbability = result.getProbabilityInvestigate();
    ernieAnalysis.releaseProbability = result.getProbabilityRelease();
    // ernieAnalysis.gammaAlert means ERNIE raised an alarm specifically due to gamma data
    ernieAnalysis.gammaAlert = (
            result.getRecommendedAction() == RecommendedAction.INVESTIGATE &&
            result.getFullSource() != null &&
            result.getFullSource().getProbabilityInvestigate() > 0.5);
    ernieAnalysis.neutronAlert = vendorAnalysis.isNeutronAlarm();
    //ernieAnalysis.occupancyStart = ?? no clear equivalent for VM250
    //ernieAnalysis.occupancyEnd = ?? no clear equivalent for VM250

    if (result.isFallback())
    {
      ernieAnalysis.result = RecommendedAction.NONE;
      //this.setMessageInvalid(scanId);
      this.setYellowLightMessage("Went to fallback: " +
              String.join(", ", record.getBadReasons()));
    }

    try {
      ernieAnalysis.vehicleClass = record.getVehicleInfo().getId();
      ernieAnalysis.vehicleLength = record.getVehicleInfo().getVehicleLength();
    }
    catch (NullPointerException ex)
    {
      ; // vehicle info couldn't be extracted, likely due to fallback
    }
    
    // goal is to only add sources if we think a NORM or threat is present.
    // FIXME current method puts a threshold on investigate probability, should
    // be using 2nd classifier to make NORM / NonEmitting decision instead
    for (int sidx = 0; sidx < result.getNumberOfSources(); sidx++)
    {
      if (result.getSource(sidx).getProbabilityInvestigate() > 0.05)
      {
        ernieAnalysis.sources.add( new Source((VM250AnalysisSource)result.getSource(sidx)) );
      }
    }
    if (result.getFullSource() != null && result.getFullSource().getProbabilityInvestigate() > 0.05)
    {
      ernieAnalysis.overallSource = new Source((VM250AnalysisSource)result.getFullSource());
    }
  }

  /*
    * ERNIEContextualInfo stores ERNIE version info, machine-learning model id, etc.
   */
  @XmlAccessorType(XmlAccessType.FIELD)
  public static final class ERNIEContextualInfo
  {

    private String versionID;
    private String modelID;
    private Thresholds thresholds;

    public ERNIEContextualInfo()
    {
      thresholds = new Thresholds();
    }

    public ERNIEContextualInfo(String version, String model,
            Thresholds thresh)
    {
      versionID = version;
      modelID = model;
      thresholds = thresh;
    }

    public String getVersionID()
    {
      return versionID;
    }

    public void setVersionID(String version)
    {
      versionID = version;
    }

    public String getModelID()
    {
      return modelID;
    }

    public void setModelID(String model)
    {
      modelID = model;
    }

    public Thresholds getThresholds()
    {
      return thresholds;
    }

    public void setThresholds(Thresholds thresh)
    {
      thresholds = thresh;
    }
  }

  /*
    * Thresholds store the ERNIE operational settings used to generate Result
   */
  @XmlAccessorType(XmlAccessType.FIELD)
  public static final class Thresholds
  {

    protected double primary;

    public Thresholds()
    {
    }

    public Thresholds(double t1)
    {
      primary = t1;
    }

    public double getPrimary()
    {
      return primary;
    }

  }

  /*
    * Scan context stores location, date/time, segment ID and
    * RPM hardware alarms.
   */
  @XmlAccessorType(XmlAccessType.FIELD)
  public static final class ScanContextualInfo
  {

    @XmlElement(name = "portID", required = true)
    private String portID;
    @XmlElement(name = "laneID", required = true)
    private long laneID;
    @XmlElement(name = "dateTime", required = true)
    @XmlJavaTypeAdapter(InstantAdapter.class)
    private Instant dateTime;
    @XmlElement(name = "segmentId", required = true)
    private long segmentId;
    @XmlElement(name = "RPMResult", required = true)
    private RecommendedAction rpmResult;
    @XmlElement(name = "RPMgammaAlert")
    private boolean rpmGammaAlert;
    @XmlElement(name = "RPMneutronAlert")
    private boolean rpmNeutronAlert;
    @XmlElement(name = "RPMScanError")
    private boolean rpmScanError;

    public ScanContextualInfo()
    {
    }

    public ScanContextualInfo(String port, int lane, Instant datetime,
            long segment, RecommendedAction rpmresult, boolean gammaAlert,
            boolean neutronAlert, boolean scanError)
    {
      SetLocation(port, lane);
      setDateTime(datetime);
      setSegmentId(segment);
      setRPMflags(rpmresult, gammaAlert, neutronAlert, scanError);
    }

    public final void SetLocation(String port, int lane)
    {
      portID = port;
      laneID = lane;
    }

    public String getPortID()
    {
      return portID;
    }

    public long getLaneID()
    {
      return laneID;
    }

    /**
     * @return _dateTime
     */
    @XmlTransient
    public Instant getDateTime()
    {
      return dateTime;
    }

    /**
     * @param dateTime the _dateTime to set
     */
    public void setDateTime(Instant dateTime)
    {
      this.dateTime = dateTime;
    }
    
    public long getSegmentId()
    {
      return segmentId;
    }

    public final void setSegmentId(long segment)
    {
      segmentId = segment;
    }

    public RecommendedAction getRPMResult()
    {
      return rpmResult;
    }

    public boolean isRPMgammaAlert()
    {
      return rpmGammaAlert;
    }

    public boolean isRPMneutronAlert()
    {
      return rpmNeutronAlert;
    }

    public boolean isRPMScanError()
    {
      return rpmScanError;
    }

    public final void setRPMflags(RecommendedAction result, boolean gammaAlert,
            boolean neutronAlert, boolean scanError)
    {
      rpmResult = result;
      rpmGammaAlert = gammaAlert;
      rpmNeutronAlert = neutronAlert;
      rpmScanError = scanError;
    }

  }

  @XmlAccessorType(XmlAccessType.FIELD)
  public static final class ERNIEAnalysis
  {

    @XmlElement(required = true)
    private RecommendedAction result;
    private double investigateProbability;
    private double releaseProbability;
    @XmlElement(required = true)
    private boolean gammaAlert;
    @XmlElement(required = true)
    private boolean neutronAlert;
    private List<Source> sources;
    private Source overallSource;
    //@XmlElement(required = true)
    //private int occupancyStart;
    //@XmlElement(required = true)
    //private int occupancyEnd;
    @XmlElement(required = true)
    private int vehicleClass;
    @XmlElement(required = true)
    private double vehicleLength;

    private String message;
    private String yellowLightMessage;

    // inferred position (m) of front of the truck relative to first vps panel,
    // given at 0.1 s intervals
    @XmlTransient
    private double[] position;

    // estimated background count rate used during analysis
    @XmlTransient
    private double[][] background;

    public ERNIEAnalysis()
    {
      sources = new ArrayList<>();
    }

    public RecommendedAction getResult()
    {
      return result;
    }

    public double getInvestigateProbability()
    {
      return investigateProbability;
    }

    public double getReleaseProbability()
    {
      return releaseProbability;
    }

    public Source getSource(int index)
    {
      return sources.get(index);
    }

    public Source getOverallSource()
    {
      return overallSource;
    }

    public int getNumberOfSources()
    {
      return this.sources.size();
    }

    /*
    public int getOccupancyStart()
    {
      return occupancyStart;
    }

    public int getOccupancyEnd()
    {
      return occupancyEnd;
    }
    */

    public int getVehicleClass()
    {
      return vehicleClass;
    }

    public double getVehicleLength()
    {
      return vehicleLength;
    }

    public String getMessage()
    {
      return message;
    }

    public String getYellowLightMessage()
    {
      return yellowLightMessage;
    }

  }

  @XmlAccessorType(XmlAccessType.FIELD)
  public static final class Source
  {
    private String sourceType;
    private String classifierUsed;
    private double xLocation1;
    private double xLocation2;
    private double yLocation;
    private double zLocation;
    private double probabilityNonEmitting;
    private double probabilityNORM;
    private double probabilityThreat;

    // This order must match that of the Enum class 
    @XmlTransient
    private static final String[] SOURCE_TYPES =
    {
      "NonEmitting", "NORM", "Threat"
    };

    public Source()
    {
    }

    public Source(double xLoc1, double xLoc2, double yLoc, double zLoc,
            double[] sourceProbs)
    {
      xLocation1 = xLoc1;
      xLocation2 = xLoc2;
      yLocation = yLoc;
      zLocation = zLoc;
      probabilityNonEmitting = sourceProbs[0];
      probabilityNORM = sourceProbs[1];
      probabilityThreat = sourceProbs[2];
      sourceType = SOURCE_TYPES[getSourceTypeEnum().getValue()];
    }

    // TODO There should only be a single Source class - VM250Analysis should still
    // have the responsiblity to create Source objects and this method would become
    // a simple setter.
    public Source(VM250AnalysisSource as)
    {
      // Copy the position
      xLocation1 = as.getPositionX1();
      xLocation2 = as.getPositionX2();
      yLocation = as.getPositionY();
      zLocation = as.getPositionZ();

      // Copy the probabilities
      double releaseProb = as.getProbabilityRelease();
      if (releaseProb < 0.95)
      {
        probabilityNORM = releaseProb;
        probabilityNonEmitting = 0;
      }
      else
      {
        probabilityNonEmitting = releaseProb;
        probabilityNORM = 0;
      }
      probabilityThreat = as.getProbabilityInvestigate();

      // Assign a type based on the max.
      // NOTE The previous formulation was not correct at all.  The whole point of our analysis
      // is that we assign the type to match the recommendation.  Thus we must 
      // first determine if we recommend releasing or investigating before we 
      // decide which max to take.
      sourceType = SOURCE_TYPES[getSourceTypeEnum().getValue()];
      classifierUsed = as.getClassifierUsed();
    }

    /**
     * VM250 currently uses a 2-class Investigate/Release model, so set a
     * threshold on investigate probability to decide if we want to return NORM
     * @return 
     */
    public SourceType getSourceTypeEnum()
    {
      if (getProbabilityInvestigate() < 0.05)
      {
        return SourceType.NonEmitting;
      }
      else if (getProbabilityRelease() > getProbabilityInvestigate())
      {
        return SourceType.NORM;
      }
      else
      {
        return SourceType.Threat;
      }
    }

    public double getProbabilityRelease()
    {
      return probabilityNonEmitting + probabilityNORM;
    }

    public double getProbabilityInvestigate()
    {
      return probabilityThreat;
    }

    /**
     * @return the xLocation1
     */
    public double getxLocation1()
    {
      return xLocation1;
    }

    /**
     * @return the xLocation2
     */
    public double getxLocation2()
    {
      return xLocation2;
    }

    /**
     * @return the yLocation
     */
    public double getyLocation()
    {
      return yLocation;
    }

    /**
     * @return the zLocation
     */
    public double getzLocation()
    {
      return zLocation;
    }

    /**
     * @return the sourceType
     */
    public String getSourceType()
    {
      return sourceType;
    }

    public static int getSourceTypeIndex(String type)
    {
      return Arrays.asList(SOURCE_TYPES).indexOf(type);
    }

    public boolean isCompact()
    {
      // Can't add the boolean flag for the decision, so instead we mirror 
      // the functionality.  FIXME
      return (this.xLocation2 - this.xLocation1) < 3;
    }

    public double getProbabilityNonEmitting()
    {
      return probabilityNonEmitting;
    }

    public double getProbabilityNORM()
    {
      return probabilityNORM;
    }

    public double getProbabilityThreat()
    {
      return probabilityThreat;
    }

    /**
     * Get the probability for each of the source classes.
     *
     * @return
     */
    public double[] getSourceProbabilities()
    {
      double[] probabilities = new double[3];
      probabilities[0] = this.probabilityNonEmitting;
      probabilities[1] = this.probabilityNORM;
      probabilities[2] = this.probabilityThreat;

      return probabilities;
    }

    /**
     * @return the classifier used to set the data of this source.
     */
    public String getClassifierUsed()
    {
      return classifierUsed;
    }

    public void setClassifierUsed(String classifier)
    {
      classifierUsed = classifier;
    }
  }

  @XmlTransient
  private Exception exception;

  @XmlTransient
  private final String ResultInvalidMessage = "ERNIE determined that the record was invalid";
  @XmlTransient
  private final String ResultSuccessfulMessage = "ERNIE successfully processed the record";
  @XmlTransient
  private final String ResultErrorMessage = "An error was encountered during ERNIE processing";

  /**
   * @param context
   */
  public void setERNIEContextualInfo(ERNIEContextualInfo context)
  {
    this.ernieContextualInfo = context;
  }

  /**
   * @return the ERNIE version
   */
  @XmlTransient
  public String getVersionID()
  {
    return this.ernieContextualInfo.versionID;
  }

  /**
   * @return the ERNIE model name
   */
  @XmlTransient
  public String getModelID()
  {
    return this.ernieContextualInfo.modelID;
  }

  /**
   * @return ERNIE thresholds
   */
  @XmlTransient
  public Thresholds getThresholds()
  {
    return this.ernieContextualInfo.thresholds;
  }

  /**
   * @return the dateTime
   */
  @XmlTransient
  public Instant getDateTime()
  {
    return this.scanContextualInfo.dateTime;
  }

  /**
   * @param dateTime the _dateTime to set
   */
  public void setDateTime(Instant dateTime)
  {
    this.scanContextualInfo.dateTime = dateTime;
  }

  /**
   * @return the PortID
   */
  @XmlTransient
  public String getPortID()
  {
    return this.scanContextualInfo.portID;
  }

  /**
   * @param PortID the PortID to set
   */
  public void setPortID(String PortID)
  {
    this.scanContextualInfo.portID = PortID;
  }

  /**
   * @return the LaneID
   */
  @XmlTransient
  public long getLaneID()
  {
    return this.scanContextualInfo.laneID;
  }

  /**
   * @param LaneID the LaneID to set
   */
  public void setLaneID(long LaneID)
  {
    this.scanContextualInfo.laneID = LaneID;
  }

  /**
   * @return the ERNIE gammaAlert
   */
  public boolean getERNIEGammaAlert()
  {
    return this.ernieAnalysis.gammaAlert;
  }

  /**
   * @param GammaAlert the alert to set
   */
  public void setERNIEGammaAlert(boolean GammaAlert)
  {
    this.ernieAnalysis.gammaAlert = GammaAlert;
  }

  /**
   * @return the RMPGammaAlert
   */
  public boolean getRPMGammaAlert()
  {
    return this.scanContextualInfo.rpmGammaAlert;
  }

  /**
   * @param GammaAlert the RPMGammaAlert to set
   */
  public void setRPMGammaAlert(boolean GammaAlert)
  {
    this.scanContextualInfo.rpmGammaAlert = GammaAlert;
  }

  /**
   * @return the ERNIE NeutronAlert
   */
  public boolean getERNIENeutronAlert()
  {
    return this.ernieAnalysis.neutronAlert;
  }

  /**
   * @param NeutronAlert the alert to set
   */
  public void setERNIENeutronAlert(boolean NeutronAlert)
  {
    this.ernieAnalysis.neutronAlert = NeutronAlert;
  }

  /**
   * @return the RMPNeutronAlert
   */
  public boolean getRPMNeutronAlert()
  {
    return this.scanContextualInfo.rpmNeutronAlert;
  }

  /**
   * @param NeutronAlert the RPMNeutronAlert to set
   */
  public void setRPMNeutronAlert(boolean NeutronAlert)
  {
    this.scanContextualInfo.rpmNeutronAlert = NeutronAlert;
  }

  /**
   * @return whether an RPM scan error occurred
   */
  public boolean getRPMScanError()
  {
    return this.scanContextualInfo.rpmScanError;
  }

  /**
   * @param scanError the value of RPM scan error
   */
  public void setRPMScanError(boolean scanError)
  {
    this.scanContextualInfo.rpmScanError = scanError;
  }

  /**
   * @return the SegmentID
   */
  @XmlTransient
  public long getSegmentId()
  {
    return this.scanContextualInfo.segmentId;
  }

  /**
   * @param SegmentId the SegmentId to set
   */
  public void setSegmentId(long SegmentId)
  {
    this.scanContextualInfo.segmentId = SegmentId;
  }

  /**
   * @return the Result
   */
  @XmlTransient
  public RecommendedAction getResult()
  {
    return this.ernieAnalysis.result;
  }

  public boolean isFallback()
  {
    return getResult() == RecommendedAction.NONE;
  }

  /**
   * @param Result the Result to set
   */
  public void setResult(RecommendedAction Result)
  {
    this.ernieAnalysis.result = Result;
  }

  /**
   * @return the RPMResult
   */
  @XmlTransient
  public RecommendedAction getRPMResult()
  {
    return this.scanContextualInfo.rpmResult;
  }

  /**
   * @param RPMResult the RPMResult to set
   */
  public void setRPMResult(RecommendedAction RPMResult)
  {
    this.scanContextualInfo.rpmResult = RPMResult;
  }

  /**
   * @return the releaseProbability
   */
  @XmlTransient
  public double getReleaseProbability()
  {
    return this.ernieAnalysis.releaseProbability;
  }

  /**
   * @param releaseProbability the releaseProbability to set
   */
  public void setReleaseProbability(double releaseProbability)
  {
    this.ernieAnalysis.releaseProbability = releaseProbability;
  }

  /**
   * @return the investigateProbability
   */
  @XmlTransient
  public double getInvestigateProbability()
  {
    return this.ernieAnalysis.investigateProbability;
  }

  /**
   * @param investigateProbability the investigateProbability to set
   */
  public void setInvestigateProbability(double investigateProbability)
  {
    this.ernieAnalysis.investigateProbability = investigateProbability;
  }

  /**
   * @return the position
   */
  @XmlTransient
  public double[] getPosition()
  {
    return this.ernieAnalysis.position;
  }

  /**
   * @param position the position to set
   */
  public void setPosition(double[] position)
  {
    this.ernieAnalysis.position = position;
  }

  /**
   * @return the vehicleLength
   */
  @XmlTransient
  public double getVehicleLength()
  {
    return this.ernieAnalysis.vehicleLength;
  }

  /**
   * @param vehicleLength the vehicleLength to set
   */
  public void setVehicleLength(double vehicleLength)
  {
    this.ernieAnalysis.vehicleLength = vehicleLength;
  }

  /**
   * @return the vehicleClass
   */
  @XmlTransient
  public int getVehicleClass()
  {
    return this.ernieAnalysis.vehicleClass;
  }

  /**
   * @param vehicleClass the vehicleClass to set
   */
  public void setVehicleClass(int vehicleClass)
  {
    this.ernieAnalysis.vehicleClass = vehicleClass;
  }

  /**
   * @return the background
   */
  @XmlTransient
  public double[][] getBackground()
  {
    return this.ernieAnalysis.background;
  }

  /**
   * @param background the background to set
   */
  public void setBackground(double[][] background)
  {
    this.ernieAnalysis.background = background;
  }

  /**
   * @param source to be added to list of sources
   */
  public void addSource(Source source)
  {
    this.ernieAnalysis.sources.add(source);
  }

  public void setOverallSource(Source source)
  {
    ernieAnalysis.overallSource = source;
  }

  /**
   * @param index
   * @return Source at index
   */
  public Source getSource(int index)
  {
    return this.ernieAnalysis.sources.get(index);
  }

  /**
   * @return source from whole vehicle analysis
   */
  public Source getOverallSource()
  {
    return this.ernieAnalysis.getOverallSource();
  }

  /**
   * @return number of sources detected
   */
  public int getNumberOfSources()
  {
    return this.ernieAnalysis.sources.size();
  }

  /**
   * @return the Message
   */
  public String getMessage()
  {
    return this.ernieAnalysis.message;
  }

  /**
   * @param Message the Message to set
   */
  public void setMessage(String Message)
  {
    this.ernieAnalysis.message = Message;
  }

  /**
   * @return the exception
   */
  public Exception getException()
  {
    return exception;
  }

  /**
   * @param exception the exception to set
   */
  public void setException(Exception exception)
  {
    this.exception = exception;
  }

  public void setAction(int action)
  {
    setResult(RecommendedAction.values()[action]);
  }

  public void setMessageInvalid(java.util.UUID scanId)
  {
    setMessage(ResultInvalidMessage + " for record id " + scanId);
  }

  public void setMessageSuccessful(java.util.UUID scanId)
  {
    setMessage(ResultSuccessfulMessage + ": " + scanId);
  }

  public void setMessageError(java.util.UUID scanId, Exception ex)
  {
    setMessage(ResultErrorMessage + " for record id " + scanId + ": " + ex.getMessage());
    setException(ex);
  }

  /**
   * @return the YellowLightMessage
   */
  @XmlTransient
  public String getYellowLightMessage()
  {
    return ernieAnalysis.yellowLightMessage;
  }

  /**
   * @param YellowLightMessage the YellowLightMessage to set
   */
  public void setYellowLightMessage(String YellowLightMessage)
  {
    this.ernieAnalysis.yellowLightMessage = YellowLightMessage;
  }
  
  public static class InstantAdapter extends XmlAdapter<String, Instant> {

      //private static final String CUSTOM_FORMAT_STRING = "yyyy-MM-dd HH:mm:ss";

      public InstantAdapter() {}

      @Override
      public String marshal(Instant v) {
          return DateTimeFormatter.ISO_INSTANT.format(v);
      }

      @Override
      public Instant unmarshal(String v) {
          return Instant.parse(v);
      }

  }

  public void saveToXML(java.io.File outputFile) throws JAXBException
  {
    JAXBContext context = JAXBContext.newInstance(Results.class);

    Marshaller m = context.createMarshaller();
    m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

    m.marshal(this, outputFile);
  }

  public String toXMLString() throws JAXBException
  {
    JAXBContext context = JAXBContext.newInstance(Results.class);
    Marshaller m = context.createMarshaller();
    m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
    StringWriter sw = new StringWriter();
    m.marshal(this, sw);
    return sw.toString();
  }

  public static Results loadFromXML(java.io.File xmlFile) throws JAXBException
  {
    JAXBContext context = JAXBContext.newInstance(Results.class);

    Unmarshaller um = context.createUnmarshaller();
    Results results = (Results) um.unmarshal(xmlFile);

    return results;
  }

  public static Results loadFromXML(InputStream in) throws JAXBException
  {
    JAXBContext context = JAXBContext.newInstance(Results.class);

    Unmarshaller um = context.createUnmarshaller();
    Results results = (Results) um.unmarshal(in);

    return results;
  }

  public static Results loadFromXML(String xmlString) throws JAXBException
  {
    JAXBContext context = JAXBContext.newInstance(Results.class);
    Unmarshaller um = context.createUnmarshaller();
    StringReader reader = new StringReader(xmlString);
    Results results = (Results) um.unmarshal(reader);
    return results;
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