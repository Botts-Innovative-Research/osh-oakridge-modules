package com.botts.ui.oscar.forms;


import com.botts.api.service.bucket.IBucketService;
import com.botts.api.service.bucket.IBucketStore;
import com.botts.impl.service.oscar.OSCARServiceModule;
import com.botts.impl.service.oscar.siteinfo.SiteInfoOutput;
import com.vaadin.event.MouseEvents;
import com.vaadin.server.FileResource;
import com.vaadin.ui.*;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;
import com.vaadin.v7.data.Property;
import com.vaadin.v7.ui.Field;
import com.vaadin.v7.ui.TextField;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.ui.GenericConfigForm;
import org.vast.util.Asserts;

import java.io.File;

import static com.botts.impl.service.oscar.Constants.SITE_MAP_BUCKET;
import static org.vast.swe.SWEHelper.getPropertyUri;

/**
 * @author
 * @since
 */
public class SiteDiagramForm extends GenericConfigForm {

    private TextField latField;
    private TextField lonField;

    public static final String DEF_SITE_PATH = getPropertyUri("SiteDiagramPath");
    public static final String DEF_LL_BOUND = getPropertyUri("LowerLeftBound");
    public static final String DEF_UR_BOUND = getPropertyUri("UpperRightBound");

    IBucketService bucketService;
    IBucketStore bucketStore;
    SiteInfoOutput siteInfoOutput;

    @Override
    protected Field<?> buildAndBindField(String label, String propId, Property<?> prop) {
        Field<?> field = super.buildAndBindField(label, propId, prop);

        bucketService = Asserts.checkNotNull(getParentHub().getModuleRegistry().getModuleByType(IBucketService.class));
        bucketStore = Asserts.checkNotNull(bucketService.getBucketStore());
        OSCARServiceModule oscarService = Asserts.checkNotNull(getParentHub().getModuleRegistry().getModuleByType(OSCARServiceModule.class));
        siteInfoOutput = (SiteInfoOutput) oscarService.getOSCARSystem().getOutputs().values().stream().filter(output -> output instanceof SiteInfoOutput).findFirst().get();
        Asserts.checkNotNull(siteInfoOutput);

        try {
            String imagePath = getSiteImagePath();

            if (imagePath != null) {
                var splitPath = imagePath.split("/")[imagePath.split("/").length - 1];
                var resolvedPath = bucketStore.getResourceURI(SITE_MAP_BUCKET, splitPath);

                if (resolvedPath != null && !resolvedPath.isEmpty()) {
                    if (propId.equals("location.lon")) {
                        addSiteMapComponent(resolvedPath);
                        lonField = (TextField) field;
                    }

                    if(propId.equals("location.lat")) {
                        latField = (TextField) field;
                    }
                }
            }
        } catch (SensorHubException e) {
            getOshLogger().error("Error building SiteMap Diagram field", e);
        }

        return field;
    }

    private void addSiteMapComponent(String imagePath){
        try{

            double[] bounds = getBoundingBoxCoordinates();
            double[] lowerLeftBound = {bounds[0], bounds[1]};
            double[] upperRightBound = {bounds[2], bounds[3]};

            VerticalLayout layout = createSiteMapLayout(imagePath, lowerLeftBound, upperRightBound);

            addComponent(layout);
        } catch (SensorHubException e){
            getOshLogger().error("Error building SiteMap Diagram field", e);
        }
    }

    /**
     * @param imagePath
     * @param lowerLeftBound
     * @param upperRightBound
     * @return
     */
    private VerticalLayout createSiteMapLayout(String imagePath, double[] lowerLeftBound, double[] upperRightBound) {
        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(true);

        HorizontalLayout coordinateLayout = new HorizontalLayout();
        Label pixelCoordinatesTitle = new Label("Pixel Coordinates: ");
        Label pixelCoordinates = new Label("Click map to select location of lane");
        coordinateLayout.addComponents(pixelCoordinatesTitle, pixelCoordinates);
        layout.addComponent(coordinateLayout);

        Image siteMap = new Image();

        File imageFile = new File(imagePath);

        if (!imageFile.exists()) {
            getOshLogger().error("Error building SiteMap Diagram image");
            layout.addComponent(new Label("No SiteMap Image Found"));
        } else {
            siteMap.setSource(new FileResource(imageFile));
            siteMap.setHeight("600px");
            siteMap.setWidth("800px");

            siteMap.addClickListener((MouseEvents.ClickListener) event -> {
                handleMapClick(event, pixelCoordinates, lowerLeftBound, upperRightBound, siteMap);
            });

            layout.addComponent(siteMap);
        }

        return layout;
    }

    /**
     * @param event
     * @param pixelCoordinates
     * @param lowerLeftBound
     * @param upperRightBound
     * @param siteMap
     */
    public void handleMapClick(MouseEvents.ClickEvent event, Label pixelCoordinates, double[] lowerLeftBound, double[] upperRightBound, Image siteMap) {

        int pixelX = event.getRelativeX();
        int pixelY = event.getRelativeY();
        pixelCoordinates.setValue(pixelX + ", " + pixelY);

        double imgWidth = siteMap.getWidth();
        double imgHeight = siteMap.getHeight();

        double longitude = calculateLongitude(pixelX, lowerLeftBound, upperRightBound, imgWidth);
        double latitude = calculateLatitude(pixelY, lowerLeftBound, upperRightBound, imgHeight);

        if (lonField != null)
            lonField.setValue(String.valueOf(longitude));

        if (latField != null)
            latField.setValue(String.valueOf(latitude));
    }


    public String getSiteImagePath() throws SensorHubException {
        return siteInfoOutput.getLatestRecord() != null ? siteInfoOutput.getLatestRecord().getStringValue(1) : null;
    }

    public double[] getBoundingBoxCoordinates() throws SensorHubException {
        var siteInfo  = siteInfoOutput.getLatestRecord();
        if (siteInfo == null)
            return null;

        var lowerLeftLon = siteInfo.getDoubleValue(2);
        var lowerLeftLat = siteInfo.getDoubleValue(3);
        var upperRightLon = siteInfo.getDoubleValue(4);
        var upperRightLat = siteInfo.getDoubleValue(5);

        return new double[]{lowerLeftLon, lowerLeftLat, upperRightLon, upperRightLat};
    }


    private double calculateLongitude (int pixelX, double[] lowerLeftBound, double[] upperRightBound, double imageWidth) {
        if (imageWidth == 0) return 0;

        return lowerLeftBound[0] + (pixelX / imageWidth) * (upperRightBound[0] - lowerLeftBound[0]);
    }

    private double calculateLatitude(int pixelY, double[] lowerLeftBound, double[] upperRightBound, double imageHeight) {
        if (imageHeight == 0) return 0;

        return upperRightBound[1] - (pixelY / imageHeight) * (upperRightBound[1] - lowerLeftBound[1]);
    }
}