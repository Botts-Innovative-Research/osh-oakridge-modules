package com.botts.ui.oscar.forms;


import com.botts.api.service.bucket.IBucketService;
import com.botts.api.service.bucket.IBucketStore;
import com.botts.impl.service.bucket.BucketService;
import com.vaadin.event.MouseEvents;
import com.vaadin.server.FileResource;
import com.vaadin.ui.*;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;
import com.vaadin.v7.data.Property;
import com.vaadin.v7.ui.Field;
import com.vaadin.v7.ui.TextField;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.datastore.DataStoreException;
import org.sensorhub.api.datastore.obs.DataStreamFilter;
import org.sensorhub.api.datastore.obs.ObsFilter;
import org.sensorhub.api.sensor.PositionConfig;
import org.sensorhub.ui.GenericConfigForm;
import org.sensorhub.ui.data.ComplexProperty;

import java.io.File;

import static com.botts.impl.service.oscar.Constants.SITE_MAP_BUCKET;
import static org.vast.swe.SWEHelper.getPropertyUri;

/**
 * @author
 * @since
 */
public class SiteDiagramForm extends GenericConfigForm {
    private transient PositionConfig config;

    private TextField latField;
    private TextField lonField;

    public static final String DEF_SITE_PATH = getPropertyUri("SiteDiagramPath");
    public static final String DEF_LL_BOUND = getPropertyUri("LowerLeftBound");
    public static final String DEF_UR_BOUND = getPropertyUri("UpperRightBound");

    IBucketService bucketService;
    IBucketStore bucketStore;

    @Override
    public void build(String propId, ComplexProperty prop, boolean includeSubForms) {
        bucketService = getParentHub().getModuleRegistry().getModuleByType(IBucketService.class);
        bucketStore = bucketService.getBucketStore();
        super.build(propId, prop, includeSubForms);
    }

    @Override
    protected Field<?> buildAndBindField(String label, String propId, Property<?> prop) {
        Field<?> field = super.buildAndBindField(label, propId, prop);

        if (propId.equals("location.lon")) {
            addSiteMapComponent();
            lonField = (TextField) field;
        }

        if(propId.equals("location.lat")) {
            latField = (TextField) field;
        }


        return field;
    }

    private void addSiteMapComponent(){
        try{
            String imagePath = getSiteImagePath();

            double[] bounds = getBoundingBoxCoordinates();
            double[] lowerLeftBound = {bounds[0], bounds[1]};
            double[] upperRightBound = {bounds[2], bounds[3]};

            VerticalLayout layout = createSiteMapLayout(imagePath, lowerLeftBound, upperRightBound);
            super.addComponents(layout);

        } catch (SensorHubException e){
            e.printStackTrace();
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

        try {
            if(bucketService == null || bucketStore == null)
                return null;

            var resolvedImagePath = bucketStore.getResourceURI(SITE_MAP_BUCKET, imagePath);

            File imageFile = new File(resolvedImagePath);

            if (!imageFile.exists()) {
                layout.addComponent(new Label("Image not found: " + imageFile.getAbsolutePath()));
            }
            siteMap.setSource(new FileResource(imageFile));
            siteMap.setHeight("600px");
            siteMap.setWidth("800px");

            siteMap.addClickListener((MouseEvents.ClickListener) event -> {
                handleMapClick(event, pixelCoordinates, lowerLeftBound, upperRightBound, siteMap);
            });

            layout.addComponent(siteMap);
        } catch (DataStoreException e) {
            throw new RuntimeException(e);
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
        double latitude = calcLatitude(pixelY, lowerLeftBound, upperRightBound, imgHeight);

        if (lonField != null)
            lonField.setValue(String.valueOf(longitude));

        if (latField != null)
            latField.setValue(String.valueOf(latitude));
    }


    public String getSiteImagePath() throws SensorHubException {
        var query = getParentHub().getDatabaseRegistry().getFederatedDatabase().getObservationStore().select(new ObsFilter.Builder()
                .withDataStreams(new DataStreamFilter.Builder()
                        .withObservedProperties(DEF_SITE_PATH)
                        .build())
                .withLatestResult()
                .build());


        var result = query.findFirst();

        if(result.isEmpty())
            return null;

        var obs = result.get();

        return obs.getResult().getStringValue(1);
    }

    public double[] getBoundingBoxCoordinates() throws SensorHubException {
        var query = getParentHub().getDatabaseRegistry().getFederatedDatabase().getObservationStore().select(new ObsFilter.Builder()
                .withDataStreams(new DataStreamFilter.Builder()
                        .withObservedProperties(DEF_LL_BOUND, DEF_UR_BOUND)
                        .build())
                .withLatestResult()
                .build());


        var result = query.findFirst();

        if(result.isEmpty())
            return null;

        var obs = result.get();

        var lowerLeftLon = obs.getResult().getDoubleValue(2);
        var lowerLeftLat = obs.getResult().getDoubleValue(3);
        var upperRightLon = obs.getResult().getDoubleValue(4);
        var upperRightLat = obs.getResult().getDoubleValue(5);


        return new double[]{lowerLeftLon, lowerLeftLat, upperRightLon, upperRightLat};
    }



    // x = longitude
    // y = latitude
    // [x,y] = [lon, lat]
    private double calculateLongitude (int pixelX, double[] lowerLeftBound, double[] upperRightBound, double imageWidth) {
        if (imageWidth == 0) return 0;

        return lowerLeftBound[0] + (pixelX / imageWidth) * (upperRightBound[0] - lowerLeftBound[0]);
    }

    private double calcLatitude (int pixelY, double[] lowerLeftBound, double[] upperRightBound, double imageHeight) {
        if (imageHeight == 0) return 0;

        return upperRightBound[1] - (pixelY / imageHeight) * (upperRightBound[1] - lowerLeftBound[1]);
    }
}