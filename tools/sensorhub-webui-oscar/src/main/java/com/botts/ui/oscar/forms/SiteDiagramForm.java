package com.botts.ui.oscar.forms;


import com.vaadin.event.MouseEvents;
import com.vaadin.server.FileResource;
import com.vaadin.ui.*;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;
import com.vaadin.v7.data.Property;
import com.vaadin.v7.ui.Field;
import com.vaadin.v7.ui.TextField;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.datastore.obs.DataStreamFilter;
import org.sensorhub.api.datastore.obs.ObsFilter;
import org.sensorhub.api.sensor.PositionConfig;
import org.sensorhub.ui.GenericConfigForm;

import java.io.File;
import java.util.List;

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

            double[] lowerLeftBound = getBoundingBoxCoordinates(DEF_LL_BOUND);
            double[] upperRightBound = getBoundingBoxCoordinates(DEF_UR_BOUND);

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
        File imageFile = new File(imagePath);

        if (!imageFile.exists()) {
            layout.addComponent(new Label("Image not found: " + imagePath));
        }
        siteMap.setSource(new FileResource(imageFile));

        siteMap.addClickListener((MouseEvents.ClickListener) event -> {
            handleMapClick(event, pixelCoordinates, lowerLeftBound, upperRightBound, siteMap);
        });

        layout.addComponent(siteMap);

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
        var obs = getParentHub().getDatabaseRegistry().getFederatedDatabase().getObservationStore().select(new ObsFilter.Builder()
                .withDataStreams(new DataStreamFilter.Builder()
                        .withObservedProperties(DEF_SITE_PATH)
                        .build())
                .withLatestResult()
                .build());

        if(obs == null) return null;

        var obsRes = obs.toList();
        if(obsRes.isEmpty()) return null;

        var path = obsRes.get(0).getResult().getStringValue(1);
        System.out.println(path);

        return path;
    }

    public double[] getBoundingBoxCoordinates( String observedProperty) throws SensorHubException {
        var obs = getParentHub().getDatabaseRegistry().getFederatedDatabase().getObservationStore().select(new ObsFilter.Builder()
                .withDataStreams(new DataStreamFilter.Builder()
                        .withObservedProperties(observedProperty)
                        .build())
                .withLatestResult()
                .build());

        if(obs == null) return null;

        var obsRes = obs.toList();
        if(obsRes.isEmpty()) return null;

        var lon = obsRes.get(0).getResult().getDoubleValue(1);
        var lat = obsRes.get(0).getResult().getDoubleValue(2);

        System.out.println("lon: " + lon);
        System.out.println("lat: " + lat);
        return new double[]{lon, lat};
    }



    // x = longitude
    // y = latitude
    // [x,y] = [lon, lat]

    private double calculateLongitude (int pixelX, double[] lowerLeftBound, double[] upperRightBound, double imageWidth) {
        if (imageWidth == 0) return 0;

        return lowerLeftBound[0] + (pixelX / imageWidth)  * (upperRightBound[0] - lowerLeftBound[0]);
    }

    private double calcLatitude (int pixelY, double[] lowerLeftBound, double[] upperRightBound, double imageHeight) {
        if (imageHeight == 0) return 0;

        return upperRightBound[1] - (pixelY / imageHeight) * (upperRightBound[1] - lowerLeftBound[1]);
    }


    /**
     * @param image_width the width of the sitemap uploaded
     * @param image_height the height of the sitemap uploaded
     * @param lowerLeftBound the lower left of the bounding box coordinates
     * @param upperRightBound the upper right of the bounding box coordinates
     * @param pixel_x the pixel_x given from clicking the sitemap
     * @param pixel_y the pixel_y given from clicking the sitemap
     */
    private List calcLinearMapping(double image_width, double image_height, double[] lowerLeftBound, double[] upperRightBound, int pixel_x, int pixel_y) {
        double longitude = (pixel_x - lowerLeftBound[0]) / (upperRightBound[1] - lowerLeftBound[0]) * image_width;
        double latitude = (upperRightBound[1] - pixel_y) / (upperRightBound[1] - lowerLeftBound[1]) * image_height;

        return List.of(longitude,latitude);
    }

}
