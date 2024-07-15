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

import gov.llnl.ernie.data.VehicleClass;
import gov.llnl.ernie.ErniePackage;
import gov.llnl.math.matrix.Matrix;
import gov.llnl.utility.io.WriterException;
import gov.llnl.utility.xml.bind.ObjectWriter;

/**
 *
 * @author nelson85
 */
public class Vehicle2ClassWriter extends ObjectWriter<VehicleClass>
{
  public Vehicle2ClassWriter()
  {
    super(Options.NONE, "vehicleClass2", ErniePackage.getInstance());
  }

  @Override
  public void attributes(WriterAttributes attributes, VehicleClass object) throws WriterException
  {
    attributes.add("index", ((Vehicle2ClassImpl) object).getInfo().getId());
  }

  @Override
  public void contents(VehicleClass object) throws WriterException
  {
    Vehicle2ClassImpl impl = (Vehicle2ClassImpl) object;
    WriterBuilder builder = newBuilder();
    builder.element("type").putInteger(impl.getInfo().getType());
    builder.element("trailerStart").putDouble(impl.getInfo().getTrailerStartFromRear());
    builder.element("trailerEnd").putDouble(impl.getInfo().getTrailerEndFromRear());
    builder.section(new InjectionSection());
    builder.section(new DisplaySection());
    builder.element("representatives")
            .putList(object.getRepresentatives(), new VehicleRepresentativeWriter());
    builder.element("backgroundModels")
            .putList(object.getBackgroundModels(), new BackgroundModelWriter());
  }

//<editor-fold desc="internal" defaultstate="collapsed">  
  private class InjectionSection extends Section
  {
    public InjectionSection()
    {
      super("injection");
    }

    @Override
    public void contents(VehicleClass object) throws WriterException
    {
      Vehicle2ClassImpl impl = (Vehicle2ClassImpl) object;
      WriterBuilder builder = newBuilder();
      builder.element("passengerAreaStart").putDouble(impl.getInfo().getPassengerAreaStart());
      builder.element("passengerAreaEnd").putDouble(impl.getInfo().getPassengerAreaEnd());
      builder.element("payloadAreaStart").putDouble(impl.getInfo().getPayloadAreaStart());
      builder.element("payloadAreaEnd").putDouble(impl.getInfo().getPayloadAreaEnd());
    }
  }

  private class DisplaySection extends Section
  {
    public DisplaySection()
    {
      super("display");
    }

    @Override
    public void contents(VehicleClass object) throws WriterException
    {
      Vehicle2ClassImpl impl = (Vehicle2ClassImpl) object;
      WriterBuilder builder = newBuilder();
      builder.element("vehicle_length").putDouble(impl.getInfo().getVehicleLength());
      builder.element("cab_length").putDouble(impl.getInfo().getCabLength());
      builder.element("isLongHaul").putFlag(impl.getInfo().isLongHaul());
      builder.element("trailerPresent").putFlag(impl.getInfo().isTrailerPresent());
      builder.element("hitchPresent").putFlag(impl.getInfo().isHitchPresent());
      builder.element("hasWideAxel").putFlag(impl.getInfo().isWideAxel());
      builder.element("bedOnly").putFlag(impl.getInfo().isBedOnly());
      builder.element("numTrailerAxels").putInteger(impl.getInfo().getNumTrailerAxels());
    }
  }

  private class BackgroundModelWriter extends ObjectWriter<VehicleClass.BackgroundModel>
  {
    public BackgroundModelWriter()
    {
      super(Options.NONE, "backgroundModel", Vehicle2ClassWriter.this.getPackage());
    }

    @Override
    public void attributes(WriterAttributes attributes, VehicleClass.BackgroundModel object) throws WriterException
    {
    }

    @Override
    public void contents(VehicleClass.BackgroundModel object) throws WriterException
    {
      WriterBuilder builder = newBuilder();
      builder.element("matrix").contents(Matrix.class).put(object.toMatrix());
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