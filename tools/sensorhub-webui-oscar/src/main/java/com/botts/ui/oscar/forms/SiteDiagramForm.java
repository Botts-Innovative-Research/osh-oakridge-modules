package com.botts.ui.oscar.forms;


import com.vaadin.event.MouseEvents;
import com.vaadin.server.FileResource;
import com.vaadin.ui.*;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;
import com.vaadin.v7.data.Property;
import com.vaadin.v7.ui.Field;
import org.sensorhub.ui.GenericConfigForm;

import java.awt.*;
import java.io.File;

/**
 * @author
 * @since
 */
public class SiteDiagramForm extends GenericConfigForm {

    @Override
    protected Field<?> buildAndBindField(String label, String propId, Property<?> prop) {
        Field<?> field = super.buildAndBindField(label, propId, prop);

        if (propId.equals("location.lon")) {
            var image = addImage("web/sitemaps/image.png");
            super.addComponents(image);
        }

        return field;
    }

    private VerticalLayout addImage(String imageUrl){
        VerticalLayout layout = new VerticalLayout();
        final Image siteMap = new Image();
        siteMap.setSource(new FileResource(new File("web/sitemaps/image.png")));

        HorizontalLayout coordinateLayout = new HorizontalLayout();

        Label pixelCoordinatesTitle = new Label("Coordinates: ");
        Label pixelCoordinates = new Label("");
        coordinateLayout.addComponents(pixelCoordinatesTitle, pixelCoordinates);
        layout.addComponent(coordinateLayout);
//        layout.addComponent(new Label("Clicked at x: " + ", y: " ));

        siteMap.addClickListener((MouseEvents.ClickListener) event -> {
            var lon = event.getRelativeX();
            var lat = event.getRelativeY();
            System.out.println(lon + " " + lat);

            pixelCoordinates.setValue(lon + ", " + lat);

        });

        layout.addComponent(siteMap);
        return layout;
    }
}
