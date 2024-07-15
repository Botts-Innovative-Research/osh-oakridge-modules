/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.ernie.internal.io;

import gov.llnl.ernie.ErniePackage;
import gov.llnl.ernie.data.SensorPosition;
import gov.llnl.ernie.data.VPSProperties;
import gov.llnl.ernie.io.LaneDatabase;
import gov.llnl.math.euclidean.Vector3;
import gov.llnl.utility.ArrayEncoding;
import gov.llnl.utility.io.ReaderException;
import gov.llnl.utility.xml.bind.ObjectReader;
import gov.llnl.utility.xml.bind.Reader;
import java.text.ParseException;
import java.time.Duration;
import org.xml.sax.Attributes;

/**
 *
 * @author nelson85
 */
@Reader.Declaration(pkg = ErniePackage.class, name = "laneDefinition",
        order = Reader.Order.FREE, referenceable = true)
public class LaneDefinitionReader extends ObjectReader<LaneDefinition>
{

  @Override
  public LaneDefinition start(Attributes attributes) throws ReaderException
  {
    return new LaneDefinition();
  }

  @Override
  public LaneDefinition end() throws ReaderException
  {
    LaneDefinition lane = (LaneDefinition) getObject();
    for (int[] group : lane.gammaGroup)
    {
      if (group == null)
        throw new ReaderException("gamma group not defined");
    }
    for (int[] group : lane.neutronGroup)
    {
      if (group == null)
        throw new ReaderException("neutron group not defined");
    }
    return null;
  }

  @Override
  public ElementHandlerMap getHandlers() throws ReaderException
  {
    ReaderBuilder<LaneDefinition> builder = newBuilder(LaneDefinition.class);    
    builder.element("laneWidth").callDouble((p, v) -> p.laneWidth = v);
    builder.element("laneHeight").callDouble((p, v) -> p.laneHeight = v);
    builder.element("vpsProperties").contents(VPSProperties.class).call((p,v) -> p.vpsProperties = v);
    builder.section(new GammaSection());
    builder.section(new NeutronSection());

    return builder.getHandlers();
  }

  @Override
  public Class<? extends LaneDatabase> getObjectClass()
  {
    return LaneDatabase.class;
  }

  public static class PanelGroup
  {
    SensorPosition i;
    int[] j;
  }

  @Reader.Declaration(pkg = ErniePackage.class, name = "panelGroup",
          contents = Reader.Contents.TEXT)
  @Reader.Attribute(name = "loc", type = SensorPosition.class)
  public static class PanelGroupReader extends ObjectReader<PanelGroup>
  {
    @Override
    public PanelGroup contents(String textContents) throws ReaderException
    {
      try
      {
        getObject().j = ArrayEncoding.decodeIntegers(textContents);
        if (getObject().j == null)
          getObject().j = new int[0];
      }
      catch (ParseException ex)
      {
        throw new ReaderException(ex);
      }
      return null;
    }

    @Override
    public PanelGroup start(Attributes attributes) throws ReaderException
    {
      PanelGroup pg = new PanelGroup();
      pg.i = SensorPosition.valueOf(attributes.getValue("loc").toUpperCase());
      return pg;
    }

    @Override
    public Class<? extends PanelGroup> getObjectClass()
    {
      return PanelGroup.class;
    }
  }

  private class GammaSection extends Section
  {
    public GammaSection()
    {
      super(Reader.Order.FREE, "gammaPanels");
    }

    @Override
    public ElementHandlerMap getHandlers() throws ReaderException
    {
      ReaderBuilder<LaneDefinition> builder = newBuilder(LaneDefinition.class);

      builder.element("energyEdges")
              .call((p, v) -> p.energyEdges = v, double[].class);
      builder.element("energyFactors")
              .call((p, v) -> p.addEnergyFactors(v), double[].class);

      builder.element("panelHeight")
              .callDouble((p, v) -> p.gammaPanelHeight = v);
      builder.element("panelWidth")
              .callDouble((p, v) -> p.gammaPanelWidth = v);
      builder.element("panelThickness")
              .callDouble((p, v) -> p.gammaPanelThickness = v);
      builder.element("collimated")
              .callBoolean((p, v) -> p.collimated = v);
      builder.element("sideCollimator")
              .callDouble((p, v) -> p.gammaSideCollimator = v);

      builder.element("samplePeriod")
              .call((p, v) -> p.gammaSamplePeriod = v, Duration.class);

      builder.element("panel")
              .call((p, v) -> p.addGammaPanel(v), Vector3.class);

      builder.element("group")
              .reader(new PanelGroupReader())
              .call((p, v) -> p.gammaGroup[v.i.getValue()] = v.j);

      builder.element("yFactors").call((p, v) -> p.gammaYFactors = v, double[].class);
      builder.element("zFactors").call((p, v) -> p.gammaZFactors = v, double[].class);
      return builder.getHandlers();
    }
  }
 
  private class NeutronSection extends Section
  {
    public NeutronSection()
    {
      super(Reader.Order.FREE, "neutronPanels");
    }

    @Override
    public ElementHandlerMap getHandlers() throws ReaderException
    {
      ReaderBuilder<LaneDefinition> builder = newBuilder(LaneDefinition.class);

      builder.element("panelHeight")
              .callDouble((p, v) -> p.neutronPanelHeight = v);
      builder.element("panelWidth")
              .callDouble((p, v) -> p.neutronPanelWidth = v);
      builder.element("panelThickness")
              .callDouble((p, v) -> p.neutronPanelThickness = v);

      builder.element("samplePeriod")
              .call((p, v) -> p.neutronSamplePeriod = v, Duration.class);

      builder.element("panel")
              .call((p, v) -> p.addNeutronPanel(v), Vector3.class);

      builder.element("group")
              .reader(new PanelGroupReader())
              .call((p, v) -> p.neutronGroup[v.i.getValue()] = v.j);

      builder.element("yFactors").call((p, v) -> p.neutronYFactors = v, double[].class);
      builder.element("zFactors").call((p, v) -> p.neutronZFactors = v, double[].class);
      return builder.getHandlers();
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