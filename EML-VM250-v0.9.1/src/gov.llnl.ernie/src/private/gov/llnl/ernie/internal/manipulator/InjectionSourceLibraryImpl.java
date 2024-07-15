/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.ernie.internal.manipulator;

import gov.llnl.ernie.manipulator.InjectionSourceLibrary;
import gov.llnl.ernie.rtk.DoubleSpectraList;
import gov.llnl.ernie.rtk.DoubleSpectrum;
import gov.llnl.ernie.rtk.SpectrumAttributes;
import gov.llnl.utility.io.ReaderException;
import gov.llnl.utility.xml.bind.DocumentReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

/**
 *
 * @author mattoon1
 */
public class InjectionSourceLibraryImpl implements InjectionSourceLibrary
{
  HashMap<String, DoubleSpectrum> _map;
  HashMap<String, Integer> _sourceTypes;

  // bin boundaries for 47-channel neutron data are available in
  // ernie.simulation.CompactNeutronSourceModel
  private Path path;

  public InjectionSourceLibraryImpl()
  {
    _map = new HashMap<>();
    _sourceTypes = new HashMap<>();
  }

  @Override
  public synchronized DoubleSpectrum getSource(String id)
  {
  // Check if id is already in _map
    DoubleSpectrum out = this._map.get(id);
    if (out != null)
      return out;

    try
    {
    // check if id without query is already in _map
      String[] idq = id.split("\\?");
      out = this._map.get(idq[0]);
      if (out != null)
      {
        out = processQuery(out,idq);
        this._map.put(id, out);
        return out;
      }
      // Otherwise try to load the required library
      else 
{        String[] str = idq[0].split("#");
        Path p = path.resolve(str[0]);
        DocumentReader<DoubleSpectraList> dslr = DocumentReader.create(DoubleSpectraList.class);
        DoubleSpectraList dsl = dslr.loadFile(p);
        int i = 0;
        for (DoubleSpectrum s : dsl)
        {
          this._map.put(str[0] + "#" + i, s);
          i++;
        }

        // If no query, return id
        if (idq.length==1){
          return this._map.get(id);
        }
      
        // If a query is in the id, process the query, add it to the hashmap, 
        // and then return the query-processed DoubleSpectrum 
        else{
          DoubleSpectrum out3 = this._map.get(idq[0]);
          out3 = processQuery(out3, idq);
          this._map.put(id, out3);
          return out3;
        }
      }
    }
    catch (ReaderException | IOException ex)
    {
      throw new RuntimeException(ex);
    }
  }

  /**
   * This is used from python to build an index for drawing injects
   */
  public void extract(Path to, Path library) throws ReaderException, IOException
  {
    try (BufferedWriter bw = Files.newBufferedWriter(to);
            PrintWriter pw = new PrintWriter(bw))
    {
      DocumentReader<DoubleSpectraList> dslr = DocumentReader.create(DoubleSpectraList.class);
      DoubleSpectraList dsl = dslr.loadFile(this.path.resolve(library));
      int i = 0;
      for (DoubleSpectrum s : dsl)
      {
        pw.println(library.getFileName() + "#" + i);
        i++;
      }
    }
  }

  public void setPath(Path get)
  {
    this.path = get;
  }
  
  public DoubleSpectrum processQuery(DoubleSpectrum ds, String[] idq)
  {
    DoubleSpectrum ds2 = new DoubleSpectrum();
    ds2.assign(ds);
    
    String[] queries = idq[1].split("&");
    for (String query : queries)
    {
      if (query.startsWith("intensity="))
      {
        Double intensity =  Double.parseDouble(query.substring(10));
        ds2 = ds2.multiplyAssign(intensity);
        double flux = ds2.getAttribute(SpectrumAttributes.GAMMA_FLUX_TOTAL, Double.class, 0.0);
        double glines = ds2.getAttribute(SpectrumAttributes.GAMMA_FLUX_LINES, Double.class, 0.0);
        double gdose = ds2.getAttribute(SpectrumAttributes.GAMMA_DOSE, Double.class, 0.0);
        String title = ds2.getAttribute(SpectrumAttributes.TITLE, String.class, "");
        ds2.setAttribute(SpectrumAttributes.GAMMA_FLUX_TOTAL, flux*intensity);
        ds2.setAttribute(SpectrumAttributes.GAMMA_FLUX_LINES, glines*intensity);
        ds2.setAttribute(SpectrumAttributes.GAMMA_DOSE, gdose*intensity);
        ds2.setAttribute(SpectrumAttributes.TITLE, title+'x'+intensity.toString());
        
        continue;
      }
      throw new UnsupportedOperationException("Unsupported query " + query);
    }
  return ds2;
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