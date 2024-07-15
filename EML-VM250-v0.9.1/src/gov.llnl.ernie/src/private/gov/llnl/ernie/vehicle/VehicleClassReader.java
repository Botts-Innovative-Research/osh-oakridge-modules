/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.ernie.vehicle;

import gov.llnl.ernie.data.VehicleClass;
import gov.llnl.ernie.ErniePackage;
import gov.llnl.math.matrix.Matrix;
import gov.llnl.math.matrix.MatrixColumnTable;
import gov.llnl.utility.io.ReaderException;
import gov.llnl.utility.xml.bind.ObjectReader;
import gov.llnl.utility.xml.bind.Reader;
import java.util.List;
import org.xml.sax.Attributes;

/**
 *
 * @author nelson85
 */
@Reader.Declaration(pkg = ErniePackage.class, name = "vehicleClass",
        order = Reader.Order.SEQUENCE)
@Reader.Attribute(name = "id", type = Integer.class)
public class VehicleClassReader extends ObjectReader<VehicleClass>
{
  private VehicleClassImpl parent;

  @Override
  public VehicleClass start(Attributes attributes) throws ReaderException
  {
    this.parent = new VehicleClassImpl();
    this.parent.getInfo().setId(Integer.parseInt(attributes.getValue("id")));
    return parent;
  }

  @Override
  public ElementHandlerMap getHandlers() throws ReaderException
  {
    ReaderBuilder<VehicleClassImpl> builder = newBuilder(VehicleClassImpl.class);
    builder.element("type").callInteger((VehicleClassImpl v, Integer i) -> v.getInfo().setType(i));
    builder.element("trailerStart")
            .callDouble((VehicleClassImpl v, Double i) -> v.getInfo().setTrailerStartFromRear(i));
    builder.element("trailerEnd")
            .callDouble((VehicleClassImpl v, Double i) -> v.getInfo().setTrailerEndFromRear(i));
    builder.section(new InjectionSection());
    builder.section(new DisplaySection());
    builder.element("template").contents(double[].class)
            .call(VehicleClassImpl::setTemplate);
    builder.element("backgroundModels").list(new BackgroundReader())
            .call(
                    (VehicleClassImpl v, List<VehicleClass.BackgroundModel> c)
                    -> v.backgroundModels.addAll(c)
            );
    return builder.getHandlers();
  }

  @Override
  public Class<? extends VehicleClass> getObjectClass()
  {
    return VehicleClassImpl.class;
  }

//<editor-fold desc="internal" defaultstate="collapsed">
  @Reader.Declaration(pkg = ErniePackage.class, name = "backgroundModel", 
          order = Reader.Order.OPTIONS)
  public class BackgroundReader extends ObjectReader<VehicleClass.BackgroundModel>
  {
    @Override
    public VehicleClass.BackgroundModel start(Attributes attributes) throws ReaderException
    {
      return new BackgroundModelImpl();
    }

    @Override
    public ElementHandlerMap getHandlers() throws ReaderException
    {
      ReaderBuilder<BackgroundModelImpl> builder = newBuilder(BackgroundModelImpl.class);
      builder.element("matrix").contents(Matrix.class)
              .call((BackgroundModelImpl vc, Matrix m) -> vc.matrix = new MatrixColumnTable(m));
      return builder.getHandlers();
    }

    @Override
    public Class<? extends VehicleClass.BackgroundModel> getObjectClass()
    {
      return BackgroundModelImpl.class;
    }
  }

  private class InjectionSection extends Section
  {
    public InjectionSection()
    {
      super(Order.OPTIONS, "injection");
    }

    @Override
    public ElementHandlerMap getHandlers() throws ReaderException
    {
      ReaderBuilder<VehicleClassImpl> builder = newBuilder(VehicleClassImpl.class);
      builder.element("passengerAreaStart")
              .callDouble((VehicleClassImpl obj, Double d) -> obj.getInfo().setPassengerAreaStart(d));
      builder.element("passengerAreaEnd")
              .callDouble((VehicleClassImpl obj, Double d) -> obj.getInfo().setPassengerAreaEnd(d));
      builder.element("payloadAreaStart")
              .callDouble((VehicleClassImpl obj, Double d) -> obj.getInfo().setPayloadAreaStart(d));
      builder.element("payloadAreaEnd")
              .callDouble((VehicleClassImpl obj, Double d) -> obj.getInfo().setPayloadAreaEnd(d));
      return builder.getHandlers();
    }
  }

  private class DisplaySection extends Section
  {
    public DisplaySection()
    {
      super(Order.OPTIONS, "display");
    }

    @Override
    public ElementHandlerMap getHandlers() throws ReaderException
    {
      ReaderBuilder<VehicleClassImpl> builder = newBuilder(VehicleClassImpl.class);
      builder.element("vehicle_length")
              .callDouble((VehicleClassImpl obj, Double d) -> obj.getInfo().setVehicleLength(d));
      builder.element("cab_length")
              .callDouble((VehicleClassImpl obj, Double d) -> obj.getInfo().setCabLength(d));
      builder.element("isLongHaul").flag()
              .call((VehicleClassImpl obj, Boolean d) -> obj.getInfo().setIsLongHaul(d));
      builder.element("trailerPresent").flag()
              .call((VehicleClassImpl obj, Boolean d) -> obj.getInfo().setTrailerPresent(d));
      builder.element("hitchPresent").flag()
              .call((VehicleClassImpl obj, Boolean d) -> obj.getInfo().setHitchPresent(d));
      builder.element("hasWideAxel").flag()
              .call((VehicleClassImpl obj, Boolean d) -> obj.getInfo().setWideAxel(d));
      builder.element("bedOnly").flag()
              .call((VehicleClassImpl obj, Boolean d) -> obj.getInfo().setBedOnly(d));
      builder.element("numTrailerAxels")
              .callInteger((VehicleClassImpl obj, Integer d) -> obj.getInfo().setNumTrailerAxels(d));
      return builder.getHandlers();
    }
  }
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