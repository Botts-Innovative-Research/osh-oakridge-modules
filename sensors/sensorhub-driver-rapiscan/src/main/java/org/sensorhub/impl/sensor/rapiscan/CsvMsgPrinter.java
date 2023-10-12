package org.sensorhub.impl.sensor.rapiscan;

import com.opencsv.CSVReader;

import java.io.*;

public class CsvMsgPrinter {
    void printMessages(InputStream inputStream) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        String string = bufferedReader.readLine();
        while (string != ""){
            System.out.println(string);
            string = bufferedReader.readLine();
        }
    }
}
