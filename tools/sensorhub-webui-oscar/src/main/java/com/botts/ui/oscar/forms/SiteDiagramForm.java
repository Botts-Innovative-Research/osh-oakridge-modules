package com.botts.ui.oscar.forms;


import com.botts.impl.system.database.OccupancyVideoPurgePolicyConfig;
import com.vaadin.event.MouseEvents;
import com.vaadin.server.FileResource;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Image;
import com.vaadin.v7.data.Property;
import com.vaadin.v7.ui.Field;
import org.sensorhub.ui.FieldWrapper;
import org.sensorhub.ui.GenericConfigForm;
import org.sensorhub.ui.SystemDriverDatabaseConfigForm;
import org.sensorhub.ui.data.BaseProperty;

import java.io.File;
import java.util.Map;

/**
 * @author
 * @since
 */
public class SiteDiagramForm extends GenericConfigForm {

    @Override
    protected Field<?> buildAndBindField(String label, String propId, Property<?> prop) {
        Field<?> field = super.buildAndBindField(label, propId, prop);


        if (propId.endsWith("location") || propId.equals("location")) {
           return addImage(field, "");
        }

        return field;
    }

    private Field<?> addImage(Field<?> field, String imageUrl){

        return new FieldWrapper<Object>((Field<Object>) field){

            @Override
            protected Component initContent()
            {
                HorizontalLayout layout = new HorizontalLayout();
                layout.setSpacing(true);

                Image image = new Image();
                image.setSource(new FileResource(new File("image.png")));
                image.addClickListener((MouseEvents.ClickListener) event -> {
                    var lon = event.getRelativeX();
                    var lat = event.getRelativeY();

                });

                layout.addComponent(image);
                return layout;
            }
        };
    }
}
