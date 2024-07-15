/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.utility.internal.xml.bind.readers;

import gov.llnl.utility.DurationUtilities;
import gov.llnl.utility.UtilityPackage;
import gov.llnl.utility.io.ReaderException;
import gov.llnl.utility.xml.bind.ObjectReader;
import gov.llnl.utility.xml.bind.Reader;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.xml.sax.Attributes;

/**
 *
 * @author nelson85
 */
@Reader.Declaration(
        pkg = UtilityPackage.class,
        name = "duration",
        referenceable = true,
        contents = Reader.Contents.TEXT)
@Reader.TextContents(base = "xs:string")
@Reader.Attribute(name = "units", type = String.class)
public class DurationContents extends ObjectReader<Duration>
{
  private ChronoUnit timeUnit;

  @Override
  public Class<Duration> getObjectClass()
  {
    return Duration.class;
  }

  public Duration start(Attributes attr) throws ReaderException
  {
    timeUnit = null;
    String units = attr.getValue("units");
    if (units == null)
      return null;

    switch (units)
    {
      case "ns":
        this.timeUnit = ChronoUnit.NANOS;
        break;
      case "us":
        this.timeUnit = ChronoUnit.MICROS;
        break;
      case "ms":
        this.timeUnit = ChronoUnit.MILLIS;
        break;
      case "s":
        this.timeUnit = ChronoUnit.SECONDS;
        break;
      default:
        throw new ReaderException("Unknown unit");
    }

    return null;
  }

  @Override
  public Duration contents(String textContents) throws DateTimeParseException, NumberFormatException
  {
    if (timeUnit == null)
    {
      try
      {
        textContents =  DurationUtilities.format(textContents);
      }
      catch (ParseException ex)
      {
        Logger.getLogger(DurationContents.class.getName()).log(Level.SEVERE, null, ex);
      }
        return Duration.parse(textContents);
     }
    return Duration.of(Long.parseLong(textContents), timeUnit);
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