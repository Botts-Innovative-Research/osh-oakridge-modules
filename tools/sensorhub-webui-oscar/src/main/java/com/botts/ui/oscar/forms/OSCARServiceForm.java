package com.botts.ui.oscar.forms;

import com.botts.impl.service.oscar.OSCARServiceModule;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.*;
import com.vaadin.v7.data.Property;
import com.vaadin.v7.ui.Field;
import com.vaadin.v7.ui.PasswordField;
import com.vaadin.v7.ui.TextField;
import com.vaadin.v7.ui.Upload;
import org.sensorhub.api.datastore.DataStoreException;
import org.sensorhub.ui.FieldWrapper;
import org.sensorhub.ui.GenericConfigForm;
import org.sensorhub.ui.data.ComplexProperty;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class OSCARServiceForm extends GenericConfigForm {

    private static final String PROP_SPREADSHEET = "spreadsheetConfigPath";
    private final OSCARServiceModule oscarService;

    public OSCARServiceForm() {
        oscarService = getParentHub().getModuleRegistry().getModuleByType(OSCARServiceModule.class);
    }

    @Override
    protected Field<?> buildAndBindField(String label, String propId, Property<?> prop) {
        Field<?> field = super.buildAndBindField(label, propId, prop);
        if (propId.equals(PROP_SPREADSHEET)) {
            return new FieldWrapper<String>((Field<String>) field) {
                @Override
                protected Component initContent() {
                    final HorizontalLayout layout = new HorizontalLayout();
                    layout.setSpacing(true);

                    ((TextField)field).setBuffered(false);

                    // Create and show file path field by default
                    TextField textField = new TextField();
                    textField.setNullRepresentation("");
                    textField.setBuffered(false);
                    textField.setWidth(field.getWidth(), field.getWidthUnits());
                    textField.setPropertyDataSource(field.getPropertyDataSource());
                    layout.addComponent(textField);
                    layout.setComponentAlignment(textField, Alignment.MIDDLE_LEFT);

                    // Create upload button
                    Upload upload = new Upload();
                    layout.addComponent(upload);
                    layout.setComponentAlignment(upload, Alignment.MIDDLE_LEFT);
                    upload.setReceiver(new Upload.Receiver() {
                        @Override
                        public OutputStream receiveUpload(String filename, String mimeType) {
                            if (filename.endsWith(".csv") || mimeType.contains("csv")) {
                                try {
                                    return oscarService.getSpreadsheetHandler().handleUpload(filename);
                                } catch (DataStoreException e) {
                                    throw new RuntimeException(e);
                                }
                            }

                            return new OutputStream() {
                                @Override
                                public void write(int b) {}
                            };
                        }
                    });
                    return layout;
                }
            };
        }
        return field;
    }

}
