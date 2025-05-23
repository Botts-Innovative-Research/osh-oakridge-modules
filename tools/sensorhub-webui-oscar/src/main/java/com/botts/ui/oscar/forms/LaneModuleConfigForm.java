package com.botts.ui.oscar.forms;

import com.vaadin.v7.data.Property;
import com.vaadin.v7.ui.Field;
import org.sensorhub.ui.GenericConfigForm;

public class LaneModuleConfigForm extends GenericConfigForm {


    @Override
    protected Field<?> buildAndBindField(String label, String propId, Property<?> prop) {

        Field<?> field = super.buildAndBindField(label, propId, prop);

        // disable groupID (read only)
        if (propId.equals("id") || propId.endsWith("groupID"))
            field.setReadOnly(true);

        return field;
    }
}