/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.ernie.vehicle2;

import gov.llnl.ernie.vehicle.BackgroundModelReader;
import gov.llnl.ernie.data.VehicleClass;
import gov.llnl.ernie.ErniePackage;
import gov.llnl.ernie.data.VehicleRepresentative;
import gov.llnl.utility.io.ReaderException;
import gov.llnl.utility.xml.bind.ObjectReader;
import gov.llnl.utility.xml.bind.Reader;
import java.util.List;
import org.xml.sax.Attributes;

/**
 *
 * @author nelson85
 */
@Reader.Declaration(pkg = ErniePackage.class, name = "vehicleClass2",
        order = Reader.Order.SEQUENCE)
@Reader.Attribute(name = "index", type = Integer.class)
public class Vehicle2ClassReader extends ObjectReader<VehicleClass>
{
  private Vehicle2ClassImpl parent;

  @Override
  public VehicleClass start(Attributes attributes) throws ReaderException
  {
    this.parent = new Vehicle2ClassImpl();
    this.parent.getInfo().setId(Integer.parseInt(attributes.getValue("index")));
    return parent;
  }

  @Override
  public ElementHandlerMap getHandlers() throws ReaderException
  {
    ReaderBuilder<Vehicle2ClassImpl> builder = newBuilder(Vehicle2ClassImpl.class);
    builder.element("type").callInteger((Vehicle2ClassImpl v, Integer i) -> v.getInfo().setType(i));
    builder.element("trailerStart")
            .callDouble((Vehicle2ClassImpl v, Double i) -> v.getInfo().setTrailerStartFromRear(i));
    builder.element("trailerEnd")
            .callDouble((Vehicle2ClassImpl v, Double i) -> v.getInfo().setTrailerEndFromRear(i));
    builder.section(new InjectionSection());
    builder.section(new DisplaySection());
        builder.element("representatives").list(new VehicleRepresentativeReader())
            .call(
                    (Vehicle2ClassImpl v, List<VehicleRepresentative> c)
                    -> v.representatives.addAll(c)
            );

    builder.element("backgroundModels").list(new BackgroundModelReader())
            .call(
                    (Vehicle2ClassImpl v, List<VehicleClass.BackgroundModel> c)
                    -> v.backgroundModels.addAll(c)
            );
    return builder.getHandlers();
  }

  @Override
  public Class<? extends VehicleClass> getObjectClass()
  {
    return Vehicle2ClassImpl.class;
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
      ReaderBuilder<Vehicle2ClassImpl> builder = newBuilder(Vehicle2ClassImpl.class);
      builder.element("passengerAreaStart")
              .callDouble((Vehicle2ClassImpl obj, Double d) -> obj.getInfo().setPassengerAreaStart(d));
      builder.element("passengerAreaEnd")
              .callDouble((Vehicle2ClassImpl obj, Double d) -> obj.getInfo().setPassengerAreaEnd(d));
      builder.element("payloadAreaStart")
              .callDouble((Vehicle2ClassImpl obj, Double d) -> obj.getInfo().setPayloadAreaStart(d));
      builder.element("payloadAreaEnd")
              .callDouble((Vehicle2ClassImpl obj, Double d) -> obj.getInfo().setPayloadAreaEnd(d));
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
      ReaderBuilder<Vehicle2ClassImpl> builder = newBuilder(Vehicle2ClassImpl.class);
      builder.element("vehicle_length")
              .callDouble((Vehicle2ClassImpl obj, Double d) -> obj.getInfo().setVehicleLength(d));
      builder.element("cab_length")
              .callDouble((Vehicle2ClassImpl obj, Double d) -> obj.getInfo().setCabLength(d));
      builder.element("isLongHaul").flag()
              .call((Vehicle2ClassImpl obj, Boolean d) -> obj.getInfo().setIsLongHaul(d));
      builder.element("trailerPresent").flag()
              .call((Vehicle2ClassImpl obj, Boolean d) -> obj.getInfo().setTrailerPresent(d));
      builder.element("hitchPresent").flag()
              .call((Vehicle2ClassImpl obj, Boolean d) -> obj.getInfo().setHitchPresent(d));
      builder.element("hasWideAxel").flag()
              .call((Vehicle2ClassImpl obj, Boolean d) -> obj.getInfo().setWideAxel(d));
      builder.element("bedOnly").flag()
              .call((Vehicle2ClassImpl obj, Boolean d) -> obj.getInfo().setBedOnly(d));
      builder.element("numTrailerAxels")
              .callInteger((Vehicle2ClassImpl obj, Integer d) -> obj.getInfo().setNumTrailerAxels(d));
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