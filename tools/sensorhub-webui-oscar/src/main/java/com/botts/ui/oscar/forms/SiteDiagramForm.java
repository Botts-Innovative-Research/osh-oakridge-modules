package com.botts.ui.oscar.forms;


import com.vaadin.event.MouseEvents;
import com.vaadin.server.FileResource;
import com.vaadin.ui.*;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;
import com.vaadin.v7.data.Property;
import com.vaadin.v7.ui.Field;
import com.vaadin.v7.ui.TextField;
import org.sensorhub.api.sensor.PositionConfig;
import org.sensorhub.ui.GenericConfigForm;
import java.io.File;
import java.util.List;

/**
 * @author
 * @since
 */
public class SiteDiagramForm extends GenericConfigForm {
    private transient PositionConfig config;
    private TextField latField;
    private TextField lonField;


    @Override
    protected Field<?> buildAndBindField(String label, String propId, Property<?> prop) {
        Field<?> field = super.buildAndBindField(label, propId, prop);

        if (propId.equals("location.lon")) {
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

        siteMap.getWidth();
        siteMap.getHeight();

        HorizontalLayout coordinateLayout = new HorizontalLayout();
        Label pixelCoordinatesTitle = new Label("Coordinates: ");
        Label pixelCoordinates = new Label("");
        coordinateLayout.addComponents(pixelCoordinatesTitle, pixelCoordinates);
        layout.addComponent(coordinateLayout);

        siteMap.addClickListener((MouseEvents.ClickListener) event -> {
            int lon = event.getRelativeX();
            int lat = event.getRelativeY();
            System.out.println(lon + " " + lat);

            pixelCoordinates.setValue(lon + ", " + lat);

            lonField.setValue(String.valueOf(lon));
            latField.setValue(String.valueOf(lat));

        });

        layout.addComponent(siteMap);

        return layout;
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
    private List calcLinearMapping(int image_width, int image_height, int[] lowerLeftBound, int[] upperRightBound, int pixel_x, int pixel_y) {
        double longitude = (pixel_x - lowerLeftBound[0]) / (upperRightBound[1] - lowerLeftBound[0]) * image_width;
        double latitude = (upperRightBound[1] - pixel_y) / (upperRightBound[1] - lowerLeftBound[1]) * image_height;

        return List.of(longitude,latitude);
    }
}
