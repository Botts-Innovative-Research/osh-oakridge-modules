package com.botts.impl.system.database;

import com.botts.impl.system.database.helpers.ChartGenerator;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import org.sensorhub.api.database.IObsSystemDatabase;
import java.io.File;
import java.io.IOException;


/**
 **
 * @author Kalyn Stricklin
 * @since Sept 05, 2025
 */
public class ReportGenerator {

    private ChartGenerator chartGenerator;
    private IObsSystemDatabase database;
    private Document document;

    public static final String destination = "/files/pdfTest.pdf";


    public ReportGenerator(IObsSystemDatabase database) {
        this.database = database;
    }


    public void generateReport() throws IOException {

        File file = new File(destination);
        file.getParentFile().mkdirs();

        PdfDocument pdf = new PdfDocument(new PdfWriter(file));
        document = new Document(pdf);


        document.close();
        System.out.println("pdf created");
    }
}



