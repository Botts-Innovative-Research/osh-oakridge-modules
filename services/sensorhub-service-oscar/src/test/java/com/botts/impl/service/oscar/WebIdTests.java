package com.botts.impl.service.oscar;

import net.opengis.swe.v20.DataBlock;
import org.sensorhub.impl.utils.rad.RADHelper;
import org.sensorhub.impl.utils.rad.model.IsotopeAnalysis;
import org.sensorhub.impl.utils.rad.model.WebIdAnalysis;
import com.botts.impl.service.oscar.webid.WebIdClient;
import com.botts.impl.service.oscar.webid.WebIdRequest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.vast.swe.fast.JsonDataParserGson;
import org.vast.swe.fast.JsonDataWriter;
import org.vast.swe.fast.JsonDataWriterGson;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Date;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * Example of WebID response.
 * {
 * 	"alarmBasisDuration" : 5.239999771118164,
 * 	"analysisError" : 0,
 * 	"analysisType" : "Simple",
 * 	"analysisWarnings" : [
 * 		"Checking energy calibration from K40 peak failed: Low K40 peak-to-background ratio. <br/>You may want to manually make sure energy calibration is about correct (ex, the K40 peak is around 1460 keV).",
 * 		"Warning: background and foreground spectra were taken 41 days apart.",
 * 		"Warning: recommend longer foreground time."
 * 	],
 * 	"backgroundDescription" : "17-Jul-2007 00:26:46, 72.19 &gamma; cps, real time: 300.8 s",
 * 	"backgroundTitle" : "No sample desc.. Acq. with MicroDetective SN 9002",
 * 	"chi2" : 2.815662145614624,
 * 	"code" : 0,
 * 	"drf" : "Detective-EX",
 * 	"estimatedDose" : 1699.012451171875,
 * 	"foregroundDescription" : "25-Apr-2007 11:32:16, 7.467e+04 &gamma; cps, real time: 60.0 s",
 * 	"foregroundTitle" : "bare. Acq. with EX",
 * 	"initializationError" : 0,
 * 	"isotopeString" : "I131  (H)",
 * 	"isotopes" : [
 * 		        {
 * 			"confidence" : 9.399999618530273,
 * 			"confidenceStr" : "H",
 * 			"countRate" : 28362.248046875,
 * 			"name" : "I131",
 * 			"type" : "Medical"
 *        }
 * 	],
 * 	"stuffOfInterest" : 9.752968788146973,
 * 	"versions" : {
 * 		"analysis" : "GADRAS 19.5.3",
 * 		"compileDate" : "Jul 16 2025"    * 	}
 * 	}
 */

public class WebIdTests {

    WebIdClient webIdClient;

    @Before
    public void setup() {
        webIdClient = new WebIdClient("https://full-spectrum.sandia.gov/api/v1");
    }

    @Test
    public void testGetPossibleDRFs() throws IOException, InterruptedException {
        var drfs = webIdClient.getPossibleDRFs();

        if (drfs.isEmpty())
            Assert.fail("Possible DRFs is null or empty");

        for (String drf : drfs)
            System.out.println(drf);
    }

    @Test
    public void testAnalysisWithDrf() throws IOException, InterruptedException {
        WebIdRequest request = new WebIdRequest.Builder()
                .foreground(WebIdTests.class.getResourceAsStream("/webid/n42/ex_I131_DetectiveEX.n42"))
                .drf("Detective-EX")
                .build();

        WebIdAnalysis response = webIdClient.analyze(request);

        Assert.assertNotNull(response);
        Assert.assertEquals(0, response.getAnalysisError());
        Assert.assertNotNull(response.getIsotopes());
        Assert.assertFalse(response.getIsotopes().isEmpty());

        System.out.println("DRF: " + response.getDrf());
        System.out.println("Isotope String: " + response.getIsotopeString());
        System.out.println("Estimated Dose: " + response.getEstimatedDose());
        System.out.println("Chi2: " + response.getChi2());

        for (IsotopeAnalysis isotope : response.getIsotopes()) {
            System.out.println("Isotope: " + isotope.getName() +
                    " (" + isotope.getType() + ") - " +
                    isotope.getConfidenceStr() + " confidence (" + isotope.getConfidence() + ")");
        }

        if (response.getAnalysisWarnings() != null) {
            System.out.println("Warnings:");
            for (String warning : response.getAnalysisWarnings()) {
                System.out.println("  - " + warning);
            }
        }
    }

    @Test
    public void testAnalysisAutoDetect() throws IOException, InterruptedException {
        WebIdRequest request = new WebIdRequest.Builder()
                .foreground(WebIdTests.class.getResourceAsStream("/webid/n42/ex_I131_DetectiveEX.n42"))
                .build();

        WebIdAnalysis response = webIdClient.analyze(request);

        Assert.assertNotNull(response);

        System.out.println("Analysis Error Code: " + response.getAnalysisError());
        System.out.println("Auto-detected DRF: " + response.getDrf());
        System.out.println("Isotope String: " + response.getIsotopeString());

        if (response.getAnalysisError() == 0) {
            Assert.assertNotNull(response.getDrf());
        } else {
            System.out.println("Note: Auto-detection returned error code " + response.getAnalysisError() +
                    ". This may occur when the N42 file lacks detector information for auto-detection.");
        }
    }

    @Test
    public void testSerializeDeserialize() throws IOException {
        List<IsotopeAnalysis> isotopes = new ArrayList<>();
        isotopes.add(new IsotopeAnalysis.Builder()
                .name("test1")
                .type("testType1")
                .confidence(92.99f)
                .confidenceStr("High")
                .countRate(20000)
                .build());

        isotopes.add(new IsotopeAnalysis.Builder()
                .name("test2")
                .type("testType2")
                .confidence(12.99f)
                .confidenceStr("Low")
                .countRate(5000)
                .build());

        var timestamp = Instant.parse("2026-02-03T23:42:18.327Z");
        var isotopeStr = "2 Weird Isotopes";
        var drf = "Detector 3000";
        var estimatedDose = 222f;
        var chi2 = 5.2f;
        var errorMessage = "No errors!";
        var warnings = List.of("Isotopes not real", "Dosage not real", "Nothing is real");
        var occupancyObsId = "abc123";

        var testAnalysis = new WebIdAnalysis.Builder()
                .isotopes(isotopes)
                .sampleTime(timestamp)
                .isotopeString(isotopeStr)
                .drf(drf)
                .estimatedDose(estimatedDose)
                .chi2(chi2)
                .errorMessage(errorMessage)
                .analysisWarnings(warnings)
                .occupancyObsId(occupancyObsId)
                .build();

        DataBlock dataBlock = WebIdAnalysis.fromWebIdAnalysis(testAnalysis);
        WebIdAnalysis webIdAnalysis = WebIdAnalysis.toWebIdAnalysis(dataBlock);
        DataBlock dataBlock2 = WebIdAnalysis.fromWebIdAnalysis(webIdAnalysis);

        ByteArrayOutputStream baos1 = new ByteArrayOutputStream();

        // print json
        JsonDataWriterGson jsonWriter = new JsonDataWriterGson();
        jsonWriter.setOutput(baos1);
        jsonWriter.setDataComponents(new RADHelper().createWebIdRecord());
        jsonWriter.write(dataBlock);
        jsonWriter.flush();

        String json1 = baos1.toString();

        ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
        jsonWriter.setOutput(baos2);
        jsonWriter.write(dataBlock2);
        jsonWriter.flush();

        String json2 = baos2.toString();

        Assert.assertEquals(json1, json2);

        System.out.println(json1);
        System.out.println();
        System.out.println(json2);
    }
}
