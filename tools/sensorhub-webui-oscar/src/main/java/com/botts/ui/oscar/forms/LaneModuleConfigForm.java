package com.botts.ui.oscar.forms;

import com.vaadin.v7.data.Property;
import com.vaadin.v7.ui.Field;
import org.sensorhub.ui.GenericConfigForm;

/**
 * @author Alex Almanza
 * @author Kalyn Stricklin
 * @since May 24, 2025
 */
public class LaneModuleConfigForm extends GenericConfigForm {

    @Override
    protected Field<?> buildAndBindField(String label, String propId, Property<?> prop) {
        Field<?> field = super.buildAndBindField(label, propId, prop);

        if (propId.endsWith("groupID"))
            field.setReadOnly(true);

        return field;
    }
}
