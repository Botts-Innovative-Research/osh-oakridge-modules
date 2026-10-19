package com.botts.ui.oscar.forms;

import com.vaadin.v7.data.util.ObjectProperty;
import com.vaadin.v7.ui.TextField;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class OSCARServiceFormUploadTest {

    @Test
    public void recordsUploadedSpreadsheetKeyInBoundConfigurationField() {
        ObjectProperty<String> configuredPath = new ObjectProperty<>("");
        TextField field = new TextField();
        field.setPropertyDataSource(configuredPath);
        field.setBuffered(false);

        OSCARServiceForm.recordUploadedObjectKey(field, "canonical-50-lanes.csv");

        assertEquals("canonical-50-lanes.csv", configuredPath.getValue());
    }
}
