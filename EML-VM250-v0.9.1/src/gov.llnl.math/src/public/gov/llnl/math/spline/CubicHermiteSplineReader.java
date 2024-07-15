/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.math.spline;

import gov.llnl.math.MathPackage;
import gov.llnl.math.spline.CubicHermiteSpline.ControlPoint;
import gov.llnl.utility.io.ReaderException;
import gov.llnl.utility.xml.bind.ObjectReader;
import gov.llnl.utility.xml.bind.Reader;
import java.util.ArrayList;
import org.xml.sax.Attributes;

/**
 *
 * @author nelson85
 */
@Reader.Declaration(pkg = MathPackage.class, name = "cubicHermiteSpline",
        cls = CubicHermiteSpline.class,
        order = Reader.Order.SEQUENCE,
        referenceable = true)
@Reader.Attribute(name = "type", type = String.class)
@Reader.Attribute(name = "endBehavior", type = String.class)
public class CubicHermiteSplineReader extends ObjectReader<CubicHermiteSpline>
{
  ArrayList<CubicHermiteSpline.ControlPoint> cp = new ArrayList<>();
  double[] x;
  double[] y;
  double[] m;
  private String type;
  private EndBehavior end = EndBehavior.LINEAR;
//
//  public CubicHermiteSplineReader()
//  {
//    super(Order.SEQUENCE, Options.NONE, "cubicHermiteSpline", MathPackage.getInstance());
//  }

  @Override
  public CubicHermiteSpline start(Attributes attributes) throws ReaderException
  {
    x = null;
    y = null;
    m = null;
    end = EndBehavior.LINEAR;
    cp.clear();
    this.type = attributes.getValue("type");
    String end = attributes.getValue("endBehavior");
    if (end != null)
      this.end = EndBehavior.valueOf(end.toUpperCase().trim());
    return null;
  }

  @Override
  public CubicHermiteSpline end() throws ReaderException
  {
    CubicHermiteSpline out = null;
    if (cp != null)
    {
      //     ControlPoint[] control = cp.toArray(new ControlPoint[0]);
      SplineUtilities.sort(cp);
      x = SplineUtilities.extractControlX(cp);
      y = SplineUtilities.extractControlY(cp);
      if (type != null)
      {
        m = new double[cp.size()];
        for (int i = 0; i < cp.size(); ++i)
        {
          m[i] = cp.get(i).m;
        }
      }
    }

    if (x != null && y != null)
    {
      if (x.length != y.length)
        throw new ReaderException("Mismatched control vector lengths");
    }

    if (type == null && x != null && y != null && m != null)
    {
      out = CubicHermiteSplineFactory.create(x, y, m);
    }
    if ("natural".equals(type) && x != null && y != null)
    {
      out = CubicHermiteSplineFactory.createNatural(x, y);
    }
    if ("monotonic".equals(type) && x != null && y != null)
    {
      out = CubicHermiteSplineFactory.createMonotonic(x, y);
    }
    if (out == null)
      throw new ReaderException("Unable to load spline");
    out.setEndBehavior(end);
    return out;
  }

  @Override
  public ElementHandlerMap getHandlers() throws ReaderException
  {
    ReaderBuilder<CubicHermiteSplineReader> rb = newBuilder().using(this);
    rb.element("x")
            .call(CubicHermiteSplineReader::setX, double[].class)
            .optional();
    rb.element("y")
            .call(CubicHermiteSplineReader::setY, double[].class)
            .optional();
    rb.element("m")
            .call(CubicHermiteSplineReader::setM, double[].class)
            .optional();
    rb.element("point")
            .call(CubicHermiteSplineReader::addControl, CubicHermiteSpline.ControlPoint.class)
            .unbounded();
    return rb.getHandlers();
  }

  void setX(double[] x)
  {
    this.x = x;
  }

  void setY(double[] y)
  {
    this.y = y;
  }

  void setM(double[] m)
  {
    this.m = m;
  }

  void addControl(ControlPoint cp)
  {
    this.cp.add(cp);
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