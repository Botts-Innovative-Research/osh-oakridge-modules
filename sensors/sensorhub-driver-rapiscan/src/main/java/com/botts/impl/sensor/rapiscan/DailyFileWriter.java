package com.botts.impl.sensor.rapiscan;

import net.opengis.swe.v20.Time;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class DailyFileWriter {

    public void writeRecord(File outputFile, String csvLine, Time timestamp){
        try{
            FileWriter fileWriter = new FileWriter(outputFile);

            try{
                fileWriter.write(csvLine+ timestamp.toString());
            }catch(IOException io){
                throw new RuntimeException(io);
            }

            fileWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
