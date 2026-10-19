package com.botts.ui.oscar.forms;

import com.vaadin.v7.ui.TextField;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class OSCARServiceFormUploadTest {

    @Test
    public void recordsUploadedSpreadsheetKeyInBoundConfigurationField() {
        TextField field = new TextField();

        OSCARServiceForm.recordUploadedObjectKey(field, "canonical-50-lanes.csv");

        assertEquals("canonical-50-lanes.csv", field.getValue());
    }
}
