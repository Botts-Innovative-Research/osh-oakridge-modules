package com.botts.impl.service.oscar;

import com.botts.impl.service.oscar.webid.WebIdClient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

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

}
