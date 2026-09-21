package com.botts.ui.oscar.forms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import org.junit.Test;
import org.sensorhub.api.comm.CommProviderConfig;
import org.sensorhub.impl.comm.HTTPConfig;
import org.sensorhub.impl.comm.IPConfig;
import org.sensorhub.impl.comm.RobustIPConnectionConfig;
import org.sensorhub.impl.comm.TCPCommProviderConfig;
import org.sensorhub.impl.comm.TCPConfig;
import org.sensorhub.impl.comm.UDPCommProviderConfig;
import org.sensorhub.impl.comm.UDPConfig;
import org.sensorhub.impl.module.RobustConnectionConfig;
import org.sensorhub.impl.sensor.ffmpeg.config.Connection;
import org.sensorhub.impl.sensor.ffmpeg.config.FFMPEGConfig;
import org.sensorhub.impl.sensor.ffmpeg.config.Output;
import org.sensorhub.impl.sensor.tstar.TSTARConfig;
import org.sensorhub.ui.AdminI18n;
import com.botts.impl.sensor.aspect.AspectConfig;
import com.botts.impl.sensor.aspect.comm.ModbusTCPCommProviderConfig;
import com.botts.impl.sensor.aspect.comm.ModbusTCPConfig;
import com.botts.impl.sensor.ds100.DS100Config;
import com.botts.impl.sensor.kromek.d3s.D3sConfig;
import com.botts.impl.sensor.kromek.d5.D5Config;
import com.botts.impl.sensor.kromek.d5.D5ConfigOutputs;
import com.botts.impl.sensor.proximity.GpioEnum;
import com.botts.impl.sensor.proximity.ProximityConfig;
import com.botts.impl.sensor.rapiscan.EMLConfig;
import com.botts.impl.sensor.rapiscan.RapiscanConfig;
import com.botts.impl.sensor.rapiscan.SetupGammaConfig;
import com.botts.impl.sensor.rs350.RS350Config;
import com.botts.impl.sensor.rs350.RS350Outputs;
import com.botts.impl.service.oscar.OSCARServiceConfig;
import com.botts.impl.sensor.wadwaz1.WADWAZ1Config;
import com.botts.impl.sensor.wapirz1.WAPIRZ1Config;
import com.botts.impl.sensor.zse18.ZSE18Config;
import com.botts.impl.sensor.zw100.ZW100Config;
import com.botts.impl.system.lane.config.LaneConfig;
import com.botts.impl.service.oscar.retention.StoragePressureRetentionConfig;
import com.botts.impl.service.oscar.siteinfo.SiteDiagramConfig;
import com.botts.impl.service.oscar.video.VideoRetentionConfig;
import com.botts.sensorhub.impl.zwave.comms.ZwaveCommServiceConfig;


public class OakridgeSensorI18nTest
{
    private static final Locale SPANISH = new Locale("es");
    private static final Locale GREEK = new Locale("el");

    private static final List<String> BUNDLE_PATHS = Arrays.asList(
        "/org/sensorhub/api/comm/i18n/ConfigMessages",
        "/org/sensorhub/impl/comm/i18n/ConfigMessages",
        "/org/sensorhub/impl/module/i18n/ConfigMessages",
        "/com/botts/impl/sensor/aspect/i18n/ConfigMessages",
        "/com/botts/impl/sensor/aspect/comm/i18n/ConfigMessages",
        "/org/sensorhub/impl/sensor/ffmpeg/config/i18n/ConfigMessages",
        "/com/botts/impl/sensor/kromek/d3s/i18n/ConfigMessages",
        "/com/botts/impl/sensor/kromek/d5/i18n/ConfigMessages",
        "/com/botts/impl/sensor/proximity/i18n/ConfigMessages",
        "/com/botts/impl/sensor/rapiscan/i18n/ConfigMessages",
        "/com/botts/impl/sensor/rs350/i18n/ConfigMessages",
        "/org/sensorhub/impl/sensor/tstar/i18n/ConfigMessages",
        "/com/botts/impl/sensor/ds100/i18n/ConfigMessages",
        "/com/botts/impl/sensor/wadwaz1/i18n/ConfigMessages",
        "/com/botts/impl/sensor/wapirz1/i18n/ConfigMessages",
        "/com/botts/impl/sensor/zse18/i18n/ConfigMessages",
        "/com/botts/impl/sensor/zw100/i18n/ConfigMessages",
        "/com/botts/sensorhub/impl/zwave/comms/i18n/ConfigMessages",
        "/com/botts/impl/service/oscar/i18n/ConfigMessages",
        "/com/botts/impl/service/oscar/siteinfo/i18n/ConfigMessages",
        "/com/botts/impl/service/oscar/video/i18n/ConfigMessages",
        "/com/botts/impl/service/oscar/retention/i18n/ConfigMessages",
        "/com/botts/ui/oscar/forms/i18n/ConfigMessages");

    private static final List<Class<?>> MODULE_CONFIGS = Arrays.asList(
        TCPCommProviderConfig.class,
        UDPCommProviderConfig.class,
        AspectConfig.class,
        ModbusTCPCommProviderConfig.class,
        FFMPEGConfig.class,
        D3sConfig.class,
        D5Config.class,
        ProximityConfig.class,
        RapiscanConfig.class,
        RS350Config.class,
        TSTARConfig.class,
        DS100Config.class,
        WADWAZ1Config.class,
        WAPIRZ1Config.class,
        ZSE18Config.class,
        ZW100Config.class,
        OSCARServiceConfig.class,
        ZwaveCommServiceConfig.class);

    private static final List<Class<?>> README_CONFIGS = Arrays.asList(
        OSCARServiceConfig.class,
        TCPCommProviderConfig.class,
        UDPCommProviderConfig.class,
        AspectConfig.class,
        ModbusTCPCommProviderConfig.class,
        FFMPEGConfig.class,
        D3sConfig.class,
        D5Config.class,
        ProximityConfig.class,
        RapiscanConfig.class,
        RS350Config.class,
        TSTARConfig.class,
        LaneConfig.class,
        DS100Config.class,
        WADWAZ1Config.class,
        WAPIRZ1Config.class,
        ZSE18Config.class,
        ZW100Config.class,
        ZwaveCommServiceConfig.class);

    private static final List<Class<?>> FORM_CONFIGS = Arrays.asList(
        CommProviderConfig.class,
        TCPCommProviderConfig.class,
        UDPCommProviderConfig.class,
        IPConfig.class,
        TCPConfig.class,
        UDPConfig.class,
        HTTPConfig.class,
        RobustIPConnectionConfig.class,
        RobustConnectionConfig.class,
        AspectConfig.class,
        ModbusTCPCommProviderConfig.class,
        ModbusTCPConfig.class,
        ModbusTCPConfig.AddressRange.class,
        FFMPEGConfig.class,
        Connection.class,
        Output.class,
        D3sConfig.class,
        D5Config.class,
        D5ConfigOutputs.class,
        ProximityConfig.class,
        RapiscanConfig.class,
        SetupGammaConfig.class,
        EMLConfig.class,
        RS350Config.class,
        RS350Outputs.class,
        TSTARConfig.class,
        DS100Config.class,
        DS100Config.DS100SensorDriverConfigurations.class,
        WADWAZ1Config.class,
        WADWAZ1Config.WADWAZSensorDriverConfigurations.class,
        WAPIRZ1Config.class,
        WAPIRZ1Config.WAPIRZSensorDriverConfigurations.class,
        ZSE18Config.class,
        ZSE18Config.ZSE18SensorDriverConfigurations.class,
        ZW100Config.class,
        ZW100Config.ZW100SensorDriverConfigurations.class,
        OSCARServiceConfig.class,
        SiteDiagramConfig.class,
        SiteDiagramConfig.LatLonLocation.class,
        VideoRetentionConfig.class,
        StoragePressureRetentionConfig.class,
        ZwaveCommServiceConfig.class,
        ZwaveCommServiceConfig.NodeList.class);


    @Test
    public void translatesRepresentativeSensorControls()
    {
        assertEquals("Controlador de sensor Aspect",
            AdminI18n.trConfig(SPANISH, AspectConfig.class, "module.name", null));
        assertEquals("Fotogramas de vídeo",
            AdminI18n.trConfig(SPANISH, Output.class, "useVideoFrames.label", null));
        assertEquals("État distant du fond",
            AdminI18n.trConfig(Locale.FRENCH, D5ConfigOutputs.class,
                "enableKromekSerialRemoteBackgroundStatusReport.label", null));
        assertEquals("No establecido",
            AdminI18n.trConfig(SPANISH, GpioEnum.class, "value.PIN_UNSET", null));
        assertEquals("Retención de ocupación",
            AdminI18n.trConfig(SPANISH, SetupGammaConfig.class, "occupancyHoldin.label", null));
        assertEquals("Détecteur de mouvement Zooz ZSE18-800LR",
            AdminI18n.trConfig(Locale.FRENCH, ZSE18Config.class, "module.name", null));
        assertEquals("Servicio de comunicación Z-Wave",
            AdminI18n.trConfig(SPANISH, ZwaveCommServiceConfig.class, "module.name", null));
        assertEquals("Haga clic en el mapa para seleccionar la ubicación del carril",
            AdminI18n.trConfig(SPANISH, SiteDiagramForm.class, "ui.selectLaneLocation", null));
        assertEquals("Πρόγραμμα οδήγησης αισθητήρα Aspect",
            AdminI18n.trConfig(GREEK, AspectConfig.class, "module.name", null));
        assertEquals("Καρέ βίντεο",
            AdminI18n.trConfig(GREEK, Output.class, "useVideoFrames.label", null));
        assertEquals("Δεν έχει οριστεί",
            AdminI18n.trConfig(GREEK, GpioEnum.class, "value.PIN_UNSET", null));
        assertEquals("Κάντε κλικ στον χάρτη για να επιλέξετε την τοποθεσία της λωρίδας",
            AdminI18n.trConfig(GREEK, SiteDiagramForm.class, "ui.selectLaneLocation", null));
        assertEquals("Módulo de servicio OSCAR",
            AdminI18n.trConfig(SPANISH, OSCARServiceConfig.class, "module.name", null));
        assertEquals("Période de requête vidéo (minutes)",
            AdminI18n.trConfig(Locale.FRENCH, VideoRetentionConfig.class,
                "videoQueryPeriod.label", null));
        assertEquals("Κάτω αριστερό όριο εγκατάστασης",
            AdminI18n.trConfig(GREEK, SiteDiagramConfig.class,
                "siteLowerLeftBound.label", null));
    }


    @Test
    public void everySensorModuleHasTranslatedPickerMetadata()
    {
        for (Class<?> configClass: MODULE_CONFIGS)
        {
            assertNotNull(configClass.getName() + " is missing a Spanish module name",
                AdminI18n.trConfig(SPANISH, configClass, "module.name", null));
            assertNotNull(configClass.getName() + " is missing a French module name",
                AdminI18n.trConfig(Locale.FRENCH, configClass, "module.name", null));
            assertNotNull(configClass.getName() + " is missing a Spanish module description",
                AdminI18n.trConfig(SPANISH, configClass, "module.description", null));
            assertNotNull(configClass.getName() + " is missing a French module description",
                AdminI18n.trConfig(Locale.FRENCH, configClass, "module.description", null));
            assertNotNull(configClass.getName() + " is missing a Greek module name",
                AdminI18n.trConfig(GREEK, configClass, "module.name", null));
            assertNotNull(configClass.getName() + " is missing a Greek module description",
                AdminI18n.trConfig(GREEK, configClass, "module.description", null));
        }
    }


    @Test
    public void translatedSensorBundlesContainEveryEnglishKey() throws Exception
    {
        for (String bundlePath: BUNDLE_PATHS)
        {
            Properties english = loadProperties(bundlePath + ".properties");
            Properties spanish = loadProperties(bundlePath + "_es.properties");
            Properties french = loadProperties(bundlePath + "_fr.properties");
            Properties greek = loadProperties(bundlePath + "_el.properties");

            assertEquals(bundlePath + " has different Spanish keys", english.keySet(), spanish.keySet());
            assertEquals(bundlePath + " has different French keys", english.keySet(), french.keySet());
            assertEquals(bundlePath + " has different Greek keys", english.keySet(), greek.keySet());
        }
    }


    @Test
    public void everySensorModuleHasHelpInEverySupportedLanguage() throws Exception
    {
        for (Class<?> configClass: README_CONFIGS)
        {
            String english = loadHelp(configClass, "i18n/README.md");
            String spanish = loadHelp(configClass, "i18n/README_es.md");
            String french = loadHelp(configClass, "i18n/README_fr.md");
            String greek = loadHelp(configClass, "i18n/README_el.md");

            assertNotEquals(configClass.getName() + " Spanish help was not translated", english, spanish);
            assertNotEquals(configClass.getName() + " French help was not translated", english, french);
            assertNotEquals(configClass.getName() + " Greek help was not translated", english, greek);
        }
    }


    @Test
    public void oscarManualCoversEveryDocumentedWorkflowInEverySupportedLanguage() throws Exception
    {
        for (String resourceName: Arrays.asList(
            "i18n/README.md",
            "i18n/README_es.md",
            "i18n/README_fr.md",
            "i18n/README_el.md"))
        {
            String manual = loadHelp(OSCARServiceConfig.class, resourceName);
            for (String requiredContent: Arrays.asList(
                "config.csv",
                "01-certificate-warning.png",
                "13-site-upload-success.png",
                "16-csv-upload-success.png",
                "19-event-details.png",
                "25-lane-options-filled.png",
                "Rapiscan",
                "Aspect",
                "RS350",
                "Sony",
                "Axis",
                "Custom"))
            {
                assertTrue(resourceName + " is missing " + requiredContent,
                    manual.contains(requiredContent));
            }
        }
    }


    @Test
    public void everySensorFormFieldHasLocalizableLabelAndDescription()
    {
        for (Class<?> configClass: FORM_CONFIGS)
        {
            for (Field field: configClass.getDeclaredFields())
            {
                if (!Modifier.isPublic(field.getModifiers()) || Modifier.isStatic(field.getModifiers()))
                    continue;

                String fieldKey = field.getName();
                assertNotNull(configClass.getName() + "." + fieldKey + " is missing a label",
                    AdminI18n.trConfig(Locale.ENGLISH, configClass, fieldKey + ".label", null));
                assertNotNull(configClass.getName() + "." + fieldKey + " is missing a description",
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


    private String loadHelp(Class<?> configClass, String resourceName) throws Exception
    {
        try (InputStream input = configClass.getResourceAsStream(resourceName))
        {
            assertNotNull(configClass.getName() + " is missing " + resourceName, input);
            String help = new String(input.readAllBytes(), StandardCharsets.UTF_8).trim();
            assertTrue(configClass.getName() + " has empty " + resourceName, !help.isEmpty());
            return help;
        }
    }
}
