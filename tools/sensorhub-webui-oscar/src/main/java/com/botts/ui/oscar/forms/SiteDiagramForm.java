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
import org.sensorhub.api.database.IFederatedDatabase;
import org.sensorhub.api.datastore.obs.DataStreamFilter;
import org.sensorhub.api.datastore.system.SystemFilter;
import org.sensorhub.api.sensor.PositionConfig;
import org.sensorhub.ui.AdminUIModule;
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
            var database = getDataFromService();

            String path = getSitePathUrl(database);

            VerticalLayout layout = addImage("web/sitemaps/image.png");

            super.addComponents(layout);

            lonField = (TextField) field;
        }

        if(propId.equals("location.lat")) {
            latField = (TextField) field;
        }
        return field;
    }

    private VerticalLayout addImage(String imageUrl){
        VerticalLayout layout = new VerticalLayout();
        final Image siteMap = new Image();
        siteMap.setSource(new FileResource(new File("web/sitemaps/image.png")));

        double img_width = siteMap.getWidth();
        double img_height = siteMap.getHeight();

        HorizontalLayout coordinateLayout = new HorizontalLayout();
        Label pixelCoordinatesTitle = new Label("Coordinates: ");
        Label pixelCoordinates = new Label("");
        coordinateLayout.addComponents(pixelCoordinatesTitle, pixelCoordinates);
        layout.addComponent(coordinateLayout);

        siteMap.addClickListener((MouseEvents.ClickListener) event -> {
            int pixel_x = event.getRelativeX();
            int pixel_y = event.getRelativeY();
            System.out.println(pixel_x + " " + pixel_y);

            pixelCoordinates.setValue(pixel_x + ", " + pixel_y);

            lonField.setValue(String.valueOf(calcLonMapping(pixel_x,null, null, img_width)));
            latField.setValue(String.valueOf(calcLatMapping(pixel_y, null, null, img_height)));

        });

        layout.addComponent(siteMap);

        return layout;
    }

    public IFederatedDatabase getDataFromService(){
        var sysFilter = new SystemFilter.Builder()
                .withUniqueIDs("parentSystemUID")
                .includeMembers(true)
                .build();

        var db = getParentHub().getDatabaseRegistry().getFederatedDatabase();
        return db;
    }


    public String getSitePathUrl(IFederatedDatabase database){
        var matchingDs = database.getDataStreamStore().select(new DataStreamFilter.Builder()
                .withObservedProperties(DEF_SITE_PATH)
                .withCurrentVersion()
                .withLimit(1)
                .build());

        return "";

    }

    public void getURBoundBox(IFederatedDatabase database){
        var matchingDs = database.getDataStreamStore().select(new DataStreamFilter.Builder()
                .withObservedProperties(DEF_UR_BOUND)
                .withCurrentVersion()
                .withLimit(1)
                .build());
    }

    public void getLLBoundBox(IFederatedDatabase database){
        var matchingDs = database.getDataStreamStore().select(new DataStreamFilter.Builder()
                .withObservedProperties(DEF_LL_BOUND)
                .withCurrentVersion()
                .withLimit(1)
                .build());
    }


    // x = longitude
    // y = latitude
    // [x,y] = [lon, lat]

    /**
     * @param image_width the width of the sitemap uploaded
     * @param image_height the height of the sitemap uploaded
     * @param lowerLeftBound the lower left of the bounding box coordinates
     * @param upperRightBound the upper right of the bounding box coordinates
     * @param pixel_x the pixel_x given from clicking the sitemap
     * @param pixel_y the pixel_y given from clicking the sitemap
     */
    private List calcLinearMapping(double image_width, double image_height, int[] lowerLeftBound, int[] upperRightBound, int pixel_x, int pixel_y) {
        double longitude = (pixel_x - lowerLeftBound[0]) / (upperRightBound[1] - lowerLeftBound[0]) * image_width;
        double latitude = (upperRightBound[1] - pixel_y) / (upperRightBound[1] - lowerLeftBound[1]) * image_height;

        return List.of(longitude,latitude);
    }

    private double calcLonMapping (int pixel_x, int[] lowerLeftBound, int[] upperRightBound, double image_width) {
        return (pixel_x - lowerLeftBound[0]) / (upperRightBound[1] - lowerLeftBound[0]) * image_width;
    }

    private double calcLatMapping (int pixel_y, int[] lowerLeftBound, int[] upperRightBound, double image_height) {
        return (upperRightBound[1] - pixel_y) / (upperRightBound[1] - lowerLeftBound[1]) * image_height;
    }
}
