package com.botts.ui.oscar.forms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.Properties;
import org.junit.Test;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.api.module.ModuleConfigBase;
import org.sensorhub.api.sensor.PositionConfig;
import org.sensorhub.api.sensor.SensorConfig;
import org.sensorhub.impl.sensor.SensorSystemConfig;
import org.sensorhub.ui.AdminI18n;
import com.botts.impl.system.lane.config.AspectRPMConfig;
import com.botts.impl.system.lane.config.AxisCameraConfig;
import com.botts.impl.system.lane.config.ConnectionConfig;
import com.botts.impl.system.lane.config.CustomCameraConfig;
import com.botts.impl.system.lane.config.FFMpegConfig;
import com.botts.impl.system.lane.config.LaneConfig;
import com.botts.impl.system.lane.config.LaneOptionsConfig;
import com.botts.impl.system.lane.config.RPMConfig;
import com.botts.impl.system.lane.config.RapiscanRPMConfig;


public class LaneConfigI18nTest
{
    private static final String BUNDLE_PATH =
        "/com/botts/impl/system/lane/config/i18n/ConfigMessages";


    @Test
    public void translatesLaneModuleAndNestedConfigurationMetadata()
    {
        assertEquals(
            "Sistema de carril",
            AdminI18n.trConfig(new Locale("es"), LaneConfig.class, "module.name", "Lane System"));
        assertEquals(
            "Supprimer les données avec la voie",
            AdminI18n.trConfig(Locale.FRENCH, LaneConfig.class, "autoDelete.label", "Delete Data"));
        assertEquals(
            "Activar análisis EML",
            AdminI18n.trConfig(new Locale("es"), RapiscanRPMConfig.EMLConfig.class, "emlEnabled.label", "Enable EML Analysis"));
        assertEquals(
            "Personnalisée",
            AdminI18n.trConfig(Locale.FRENCH, CustomCameraConfig.class, "type.name", "Custom"));
        assertEquals(
            "Σύστημα λωρίδας",
            AdminI18n.trConfig(new Locale("el"), LaneConfig.class, "module.name", "Lane System"));
        assertEquals(
            "Ενεργοποίηση ανάλυσης EML",
            AdminI18n.trConfig(new Locale("el"), RapiscanRPMConfig.EMLConfig.class, "emlEnabled.label", "Enable EML Analysis"));
    }


    @Test
    public void translatedLaneBundlesContainEveryEnglishKey() throws Exception
    {
        Properties english = loadProperties(BUNDLE_PATH + ".properties");
        Properties spanish = loadProperties(BUNDLE_PATH + "_es.properties");
        Properties french = loadProperties(BUNDLE_PATH + "_fr.properties");
        Properties greek = loadProperties(BUNDLE_PATH + "_el.properties");

        assertEquals(english.keySet(), spanish.keySet());
        assertEquals(english.keySet(), french.keySet());
        assertEquals(english.keySet(), greek.keySet());
    }


    @Test
    public void everyLaneFormFieldHasLocalizableLabelAndDescription()
    {
        for (Class<?> configClass: Arrays.asList(
            ModuleConfigBase.class,
            ModuleConfig.class,
            SensorConfig.class,
            SensorSystemConfig.class,
            SensorSystemConfig.SystemMember.class,
            PositionConfig.class,
            PositionConfig.LLALocation.class,
            PositionConfig.CartesianLocation.class,
            PositionConfig.EulerOrientation.class,
            LaneConfig.class,
            LaneOptionsConfig.class,
            ConnectionConfig.class,
            RPMConfig.class,
            FFMpegConfig.class,
            AspectRPMConfig.class,
            AspectRPMConfig.AddressRange.class,
            RapiscanRPMConfig.class,
            RapiscanRPMConfig.EMLConfig.class,
            AxisCameraConfig.class,
            CustomCameraConfig.class))
        {
            for (Field field: configClass.getDeclaredFields())
            {
                if (!Modifier.isPublic(field.getModifiers()) || Modifier.isStatic(field.getModifiers()))
                    continue;

                String fieldKey = field.getName();
                assertNotNull(
                    configClass.getName() + "." + fieldKey + " is missing a label",
                    AdminI18n.trConfig(Locale.ENGLISH, configClass, fieldKey + ".label", null));
                assertNotNull(
                    configClass.getName() + "." + fieldKey + " is missing a description",
                    AdminI18n.trConfig(Locale.ENGLISH, configClass, fieldKey + ".description", null));
            }
        }
    }


    private Properties loadProperties(String path) throws Exception
    {
        InputStream input = getClass().getResourceAsStream(path);
        assertNotNull("Missing resource " + path, input);

        Properties properties = new Properties();
        try (InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8))
        {
            properties.load(reader);
        }
        return properties;
    }
}
