package com.botts.ui.oscar.forms;

import com.botts.impl.service.oscar.IFileHandler;
import com.botts.impl.service.oscar.OSCARServiceConfig;
import com.botts.impl.service.oscar.OSCARServiceModule;
import com.botts.impl.service.oscar.siteinfo.SitemapDiagramHandler;
import com.vaadin.server.FileDownloader;
import com.vaadin.server.StreamResource;
import com.botts.impl.service.oscar.siteinfo.SiteDiagramConfig;
import com.vaadin.ui.*;
import com.vaadin.v7.data.Property;
import com.vaadin.v7.data.fieldgroup.FieldGroup.CommitException;
import com.vaadin.v7.ui.Field;
import com.vaadin.v7.ui.TextField;
import com.vaadin.v7.ui.Upload;
import org.sensorhub.api.datastore.DataStoreException;
import org.sensorhub.ui.DisplayUtils;
import org.sensorhub.ui.FieldWrapper;
import org.sensorhub.ui.GenericConfigForm;
import org.sensorhub.ui.data.MyBeanItem;

import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicReference;

import static org.sensorhub.ui.AdminI18n.trConfig;

public class OSCARServiceForm extends GenericConfigForm {

    private static final String PROP_SPREADSHEET = "spreadsheetConfigPath";
    private static final String PROP_SITEMAP = "siteDiagramConfig.siteDiagramPath";
    private final OSCARServiceModule oscarService;


    public OSCARServiceForm() {
        oscarService = getParentHub().getModuleRegistry().getModuleByType(OSCARServiceModule.class);
    }

    private SiteDiagramConfig getEditedSiteDiagramConfig() {
        if (fieldGroup == null || !(fieldGroup.getItemDataSource() instanceof MyBeanItem<?>))
            return null;

        Object bean = ((MyBeanItem<?>) fieldGroup.getItemDataSource()).getBean();
        if (bean instanceof SiteDiagramConfig)
            return (SiteDiagramConfig) bean;
        if (bean instanceof OSCARServiceConfig)
            return ((OSCARServiceConfig) bean).siteDiagramConfig;
        return null;
    }

    @Override
    protected Field<?> buildAndBindField(String label, String propId, Property<?> prop) {
        Field<?> field = super.buildAndBindField(label, propId, prop);

        if (propId.endsWith(PROP_SPREADSHEET) || propId.endsWith(PROP_SITEMAP)) {

            IFileHandler fileHandler;

            if (propId.endsWith(PROP_SPREADSHEET)) {
                fileHandler = oscarService.getSpreadsheetHandler();
            } else if (propId.endsWith(PROP_SITEMAP)) {
                fileHandler = oscarService.getSitemapDiagramHandler();
            } else {
                fileHandler = null;
            }

            return new FieldWrapper<String>((Field<String>) field) {
                @Override
                protected Component initContent() {
                    final HorizontalLayout layout = new HorizontalLayout();
                    layout.setSpacing(true);

                    ((TextField) field).setBuffered(false);

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
                    AtomicReference<String> uploadedObjectKey = new AtomicReference<>();
                    AtomicReference<Throwable> uploadFailure = new AtomicReference<>();
                    upload.setReceiver(new Upload.Receiver() {
                        @Override
                        public OutputStream receiveUpload(String filename, String mimeType) {
                            String objectKey = SitemapDiagramHandler.normalizeFileName(filename);
                            uploadedObjectKey.set(objectKey);
                            uploadFailure.set(null);

                            if (!fileHandler.isValidFileType(objectKey, mimeType)) {
                                uploadFailure.set(new IllegalArgumentException(trConfig(
                                        OSCARServiceForm.class,
                                        "ui.invalidFileType",
                                        "Unsupported file type: {0}").replace("{0}", filename)));
                                return null;
                            }

                            try {
                                return fileHandler.handleUpload(objectKey);
                            } catch (DataStoreException e) {
                                uploadFailure.set(e);
                                return null;
                            }
                        }
                    });

                    upload.addSucceededListener((e) -> {
                        String objectKey = uploadedObjectKey.get();
                        boolean fileLoaded;

                        if (propId.endsWith(PROP_SITEMAP)) {
                            try {
                                // Coordinate fields live in nested subforms and
                                // remain buffered until the form is committed.
                                // Commit the entire site-diagram form before
                                // publishing its bounds with the uploaded image.
                                OSCARServiceForm.this.commit();
                            } catch (CommitException commitError) {
                                DisplayUtils.showErrorPopup(trConfig(
                                        OSCARServiceForm.class,
                                        "ui.uploadFailed",
                                        "Upload failed."), commitError);
                                return;
                            }

                            SiteDiagramConfig siteDiagramConfig = getEditedSiteDiagramConfig();
                            fileLoaded = oscarService.getSitemapDiagramHandler()
                                    .handleFile(objectKey, siteDiagramConfig);
                            if (fileLoaded)
                                ((Field<String>) field).setValue(objectKey);
                        } else {
                            fileLoaded = fileHandler.handleFile(objectKey);
                        }

                        if (!fileLoaded) {
                            String message = trConfig(
                                    OSCARServiceForm.class,
                                    "ui.loadFailed",
                                    "Unable to load file from {0}").replace("{0}", objectKey);
                            DisplayUtils.showErrorPopup(trConfig(
                                OSCARServiceForm.class,
                                "ui.loadFailed",
                                "Unable to load file from {0}").replace("{0}", objectKey),
                                new IllegalStateException(message));
                        } else {
                            DisplayUtils.showOperationSuccessful(trConfig(
                                OSCARServiceForm.class,
                                "ui.loadSucceeded",
                                "Successfully loaded file {0}!").replace("{0}", objectKey));
                        }
                    });

                    upload.addFailedListener((e) -> {
                        Throwable reason = uploadFailure.getAndSet(null);
                        if (reason == null)
                            reason = e.getReason();
                        DisplayUtils.showErrorPopup(trConfig(
                                OSCARServiceForm.class, "ui.uploadFailed", "Upload failed."), reason);
                    });

                    if (propId.endsWith(PROP_SPREADSHEET)) {
                        FileDownloader download = new FileDownloader(
                                new StreamResource(() ->
                                        oscarService.getSpreadsheetHandler().getDownloadStream(),
                                        oscarService.getSpreadsheetHandler().CONFIG_KEY));
                        Button button = new Button(trConfig(
                            OSCARServiceForm.class, "ui.download", "Download"));
                        button.addClickListener((event) -> {
                            if (oscarService.getSpreadsheetHandler().getDownloadStream() != null)
                                button.setEnabled(false);
                            else
                                DisplayUtils.showErrorPopup(trConfig(
                                    OSCARServiceForm.class,
                                    "ui.noLanes",
                                    "Unable to download the configuration because no lanes are loaded."),
                                    new IllegalArgumentException("File data is null"));
                        });
                        download.setErrorHandler(e -> {
                            DisplayUtils.showErrorPopup(trConfig(
                                OSCARServiceForm.class,
                                "ui.downloadFailed",
                                "Error downloading configuration file."), e.getThrowable());
                            button.setEnabled(true);
                        });
                        download.extend(button);
                        layout.addComponent(button);
                    }

                    return layout;
                }
            };
            
        }
        return field;
    }
}
