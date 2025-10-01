package com.botts.ui.oscar.forms;

import com.botts.impl.service.oscar.OSCARServiceModule;
import com.vaadin.server.AbstractClientConnector;
import com.vaadin.server.FileDownloader;
import com.vaadin.server.StreamResource;
import com.botts.impl.service.oscar.siteinfo.SiteDiagramConfig;
import com.vaadin.ui.*;
import com.vaadin.v7.data.Property;
import com.vaadin.v7.ui.Field;
import com.vaadin.v7.ui.TextField;
import com.vaadin.v7.ui.Upload;
import org.sensorhub.api.datastore.DataStoreException;
import org.sensorhub.ui.DisplayUtils;
import org.sensorhub.ui.FieldWrapper;
import org.sensorhub.ui.GenericConfigForm;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;

public class OSCARServiceForm extends GenericConfigForm {

    private static final String PROP_SPREADSHEET = "spreadsheetConfigPath";
    private static final String PROP_SITEMAP = "siteDiagramConfig.siteDiagramPath";
    private static final String PROP_LLB = "siteDiagramConfig.siteLowerLeftBound";
    private static final String PROP_URB = "siteDiagramConfig.siteUpperRightBound";
    private final OSCARServiceModule oscarService;

    private TextField spreadsheetPath;
    private TextField siteDiagramPath;

    private SiteDiagramConfig.LatLonLocation siteLowerLeftBound;
    private SiteDiagramConfig.LatLonLocation siteUpperRightBound;

    public OSCARServiceForm() {
        oscarService = getParentHub().getModuleRegistry().getModuleByType(OSCARServiceModule.class);

        siteLowerLeftBound = oscarService.getConfiguration().siteDiagramConfig.siteLowerLeftBound;
        siteUpperRightBound = oscarService.getConfiguration().siteDiagramConfig.siteUpperRightBound;
    }

    @Override
    protected Field<?> buildAndBindField(String label, String propId, Property<?> prop) {
        Field<?> field = super.buildAndBindField(label, propId, prop);


        if(propId.endsWith(PROP_SPREADSHEET) || propId.endsWith(PROP_SITEMAP)) {
            if (propId.endsWith(PROP_SPREADSHEET))
                spreadsheetPath = (TextField) field;

            if (propId.endsWith(PROP_SITEMAP))
                siteDiagramPath = (TextField) field;


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
                    textField.setReadOnly(true);
                    textField.setVisible(false);
                    layout.addComponent(textField);
                    layout.setComponentAlignment(textField, Alignment.MIDDLE_LEFT);

                    // Create upload button
                    Upload upload = new Upload();
                    layout.addComponent(upload);
                    layout.setComponentAlignment(upload, Alignment.MIDDLE_LEFT);
                    upload.addSucceededListener(event -> {
                        if(propId.endsWith(PROP_SITEMAP))
                            oscarService.reportStatus("Successfully uploaded sitemap diagram");
                        if(propId.endsWith(PROP_SPREADSHEET))
                            oscarService.reportStatus("Successfully uploaded spreadsheet");
                    });
                    upload.setReceiver(new Upload.Receiver() {
                        @Override
                        public OutputStream receiveUpload(String filename, String mimeType) {

                            try {
                                if(propId.endsWith(PROP_SPREADSHEET)) {
                                    if (filename.endsWith(".csv") || mimeType.contains("csv")) {
                                        spreadsheetPath.setValue(filename);
                                        return oscarService.getSpreadsheetHandler().handleUpload(filename);
                                    }
                                }

                                if(propId.endsWith(PROP_SITEMAP)) {
                                    if (filename.endsWith(".png") || mimeType.contains("jpg")) {
                                        siteDiagramPath.setValue(filename);
                                        return oscarService.getSitemapDiagramHandler().handleUpload(filename, siteLowerLeftBound, siteUpperRightBound);
                                    }
                                }

                            } catch (FileNotFoundException e) {
                                throw new IllegalStateException(e);
                            } catch (DataStoreException e) {
                            }

                            return new OutputStream() {
                                @Override
                                public void write(int b) {}
                            };
                    });

                    upload.addSucceededListener((e) -> {
                        boolean fileLoaded = oscarService.getSpreadsheetHandler().handleFile(e.getFilename());
                        if (!fileLoaded)
                            DisplayUtils.showErrorPopup("Unable to load lanes from file.", new IllegalStateException());
                        else
                            DisplayUtils.showOperationSuccessful("Successfully loaded lanes from file!");
                    });

                    FileDownloader download = new FileDownloader(
                            new StreamResource(() ->
                            oscarService.getSpreadsheetHandler().getDownloadStream(),
                            oscarService.getSpreadsheetHandler().CONFIG_KEY));
                    Button button = new Button("Download");
                    button.addClickListener((event) -> {
                        if (oscarService.getSpreadsheetHandler().getDownloadStream() != null)
                            button.setEnabled(false);
                        else
                            DisplayUtils.showErrorPopup("Unable to download config because there are no lanes loaded.", new IllegalArgumentException("File data is null"));
                    });
                    download.setErrorHandler(e -> {
                        DisplayUtils.showErrorPopup("Error downloading config file.", e.getThrowable());
                        button.setEnabled(true);
                    });
                    download.extend(button);
                    layout.addComponent(button);

                    return layout;
                }
            };
        }
        return field;
    }

}
