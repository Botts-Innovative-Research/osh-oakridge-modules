/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 The Initial Developer is Botts Innovative Research Inc. Portions created by the Initial
 Developer are Copyright (C) 2025 the Initial Developer. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package com.botts.impl.system.lane;

import com.botts.impl.sensor.aspect.AspectConfig;
import com.botts.impl.sensor.aspect.AspectSensor;
import com.botts.impl.sensor.aspect.comm.ModbusTCPCommProvider;
import com.botts.impl.sensor.aspect.comm.ModbusTCPCommProviderConfig;
import com.botts.impl.sensor.rapiscan.EMLConfig;
import com.botts.impl.sensor.rapiscan.RapiscanConfig;
import com.botts.impl.sensor.rapiscan.RapiscanSensor;
import com.botts.impl.sensor.rs350.RS350Config;
import com.botts.impl.sensor.rs350.RS350Sensor;
import com.botts.impl.system.lane.config.AspectRPMConfig;
import com.botts.impl.system.lane.config.AxisCameraConfig;
import com.botts.impl.system.lane.config.CustomCameraConfig;
import com.botts.impl.system.lane.config.FFMpegConfig;
import com.botts.impl.system.lane.config.LaneConfig;
import com.botts.impl.system.lane.config.RPMConfig;
import com.botts.impl.system.lane.config.RS350RPMConfig;
import com.botts.impl.system.lane.config.RapiscanRPMConfig;
import com.botts.impl.system.lane.config.SonyCameraConfig;
import org.sensorhub.api.sensor.SensorConfig;
import org.sensorhub.impl.comm.TCPCommProviderConfig;
import org.sensorhub.impl.sensor.ffmpeg.FFMPEGSensor;
import org.sensorhub.impl.sensor.ffmpeg.config.FFMPEGConfig;
import org.vast.util.Asserts;

import java.util.Date;
import java.util.Locale;
import java.util.Objects;

/**
 * Bidirectional endpoint synchronizer keeping parent {@link LaneConfig#laneOptionsConfig}
 * ({@code ffmpegConfig} and {@code rpmConfig}) and child sensor {@code ModuleConfig} entries
 * ({@link FFMPEGConfig}, {@link RapiscanConfig}, {@link AspectConfig}, {@link RS350Config})
 * consistent whether an administrator edits {@code Lane Options}, edits a child sensor module
 * in the SensorHub Admin UI, or edits either block in {@code config.json}.
 */
public final class LaneEndpointSync {

    public enum SyncAction {
        NO_OP,
        REGISTER_NEW_CHILD,
        UPDATE_CHILD,
        UPDATE_PARENT
    }

    public record CameraSyncResult(
            SyncAction action,
            FFMpegConfig simpleConfig,
            FFMPEGConfig childConfig,
            boolean metadataUpdated
    ) {}

    public record RpmSyncResult(
            SyncAction action,
            RPMConfig simpleConfig,
            SensorConfig childConfig,
            boolean metadataUpdated
    ) {}

    public record ParsedRtspEndpoint(
            String scheme,
            String username,
            String password,
            String remoteHost,
            String streamPath
    ) {}

    private LaneEndpointSync() {
    }

    /**
     * Normalizes accidental full RTSP URLs pasted into {@code remoteHost} (for example
     * {@code rtsp://admin:secret@192.168.1.50:554/stream1}) into structured {@link FFMpegConfig}
     * fields so {@code rtsp://rtsp://...} is never produced.
     */
    public static void sanitizeSimpleCameraConfig(FFMpegConfig simpleConfig) {
        if (simpleConfig == null || simpleConfig.remoteHost == null) {
            return;
        }
        String trimmed = simpleConfig.remoteHost.trim();
        int schemeIdx = trimmed.indexOf("://");
        if (schemeIdx > 0) {
            ParsedRtspEndpoint parsed = parseRtspConnectionString(trimmed);
            if (parsed != null && parsed.remoteHost() != null && !parsed.remoteHost().isBlank()) {
                simpleConfig.remoteHost = parsed.remoteHost();
                if ((simpleConfig.username == null || simpleConfig.username.isBlank())
                        && parsed.username() != null && !parsed.username().isBlank()) {
                    simpleConfig.username = parsed.username();
                }
                if ((simpleConfig.password == null || simpleConfig.password.isEmpty())
                        && parsed.password() != null && !parsed.password().isEmpty()) {
                    simpleConfig.password = parsed.password();
                }
                if (simpleConfig instanceof CustomCameraConfig custom
                        && (custom.streamPath == null || custom.streamPath.isBlank())
                        && parsed.streamPath() != null && !parsed.streamPath().isBlank()
                        && !"/".equals(parsed.streamPath())) {
                    custom.streamPath = parsed.streamPath();
                }
            }
        } else {
            // Strip accidental trailing slash on host[:port]
            while (trimmed.endsWith("/")) {
                trimmed = trimmed.substring(0, trimmed.length() - 1);
            }
            simpleConfig.remoteHost = trimmed;
        }
    }

    public static String buildCameraConnectionString(FFMpegConfig ffmpegConfig) {
        Asserts.checkNotNull(ffmpegConfig);
        sanitizeSimpleCameraConfig(ffmpegConfig);
        Asserts.checkNotNull(ffmpegConfig.remoteHost);

        String defaultAxis = AxisCameraConfig.CodecEndpoint.H264.getPath();
        StringBuilder endpoint = new StringBuilder("rtsp://");

        if (ffmpegConfig.username != null && !ffmpegConfig.username.isBlank()) {
            endpoint.append(ffmpegConfig.username);
            endpoint.append(":");
            endpoint.append(ffmpegConfig.password != null ? ffmpegConfig.password : "");
            endpoint.append("@");
        }
        endpoint.append(ffmpegConfig.remoteHost.trim());

        if (ffmpegConfig instanceof SonyCameraConfig) {
            if (ffmpegConfig.remoteHost.contains(":")) {
                endpoint.append("/media/video1");
            } else {
                endpoint.append(SonyCameraConfig.streamPath);
            }
            return endpoint.toString();
        }

        String path = defaultAxis;
        if (ffmpegConfig instanceof AxisCameraConfig axisVideoConfig) {
            path = axisVideoConfig.streamPath != null ? axisVideoConfig.streamPath.getPath() : defaultAxis;
        } else if (ffmpegConfig instanceof CustomCameraConfig customVideoConfig) {
            path = (customVideoConfig.streamPath != null && !customVideoConfig.streamPath.isBlank())
                    ? customVideoConfig.streamPath
                    : defaultAxis;
        }

        endpoint.append(LaneSystem.normalizeStreamPath(path, defaultAxis));
        return endpoint.toString();
    }

    public static FFMPEGConfig createInitialFFmpegChildConfig(
            LaneConfig laneConfig,
            FFMpegConfig ffmpegConfig,
            int videoIndex) {
        String connectionString = buildCameraConnectionString(ffmpegConfig);
        FFMPEGConfig config = new FFMPEGConfig();
        config.connection.useTCP = true;
        config.connection.fps = 30;
        config.name = laneConfig.name + " - Camera " + videoIndex;
        config.serialNumber = cameraSerialNumber(laneConfig.uniqueID, videoIndex);
        config.autoStart = true;
        config.connection.connectionString = connectionString;
        config.moduleClass = FFMPEGSensor.class.getCanonicalName();
        config.connectionConfig.connectTimeout = 4500;
        config.connectionConfig.reconnectAttempts = -1;
        config.output.useHLS = true;
        config.output.useVideoFrames = false;
        config.connection.bufferSize = ffmpegConfig.bufferSize;
        ffmpegConfig.markSyncedEndpoint(cameraSyncToken(connectionString, ffmpegConfig.bufferSize));
        return config;
    }

    public static String cameraSerialNumber(String laneUniqueId, int videoIndex) {
        String normalizedUid = laneUniqueId == null ? "" : laneUniqueId;
        if (normalizedUid.startsWith("urn:osh:system:lane:")) {
            normalizedUid = normalizedUid.substring("urn:osh:system:lane:".length());
        }
        return "lane:" + normalizedUid + ":" + videoIndex;
    }

    public static int parseCameraIndexFromSerial(String serialNumber) {
        if (serialNumber == null || !serialNumber.startsWith("lane:")) {
            return -1;
        }
        int lastColon = serialNumber.lastIndexOf(':');
        if (lastColon <= 4 || lastColon + 1 >= serialNumber.length()) {
            return -1;
        }
        try {
            return Integer.parseInt(serialNumber.substring(lastColon + 1));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public static ParsedRtspEndpoint parseRtspConnectionString(String connectionString) {
        if (connectionString == null || connectionString.isBlank()) {
            return null;
        }
        String trimmed = connectionString.trim();
        String scheme = "rtsp";
        String remainder = trimmed;
        int schemeIdx = trimmed.indexOf("://");
        if (schemeIdx > 0) {
            scheme = trimmed.substring(0, schemeIdx).toLowerCase(Locale.ROOT);
            remainder = trimmed.substring(schemeIdx + 3);
        }

        int slashIdx = remainder.indexOf('/');
        String authority = slashIdx >= 0 ? remainder.substring(0, slashIdx) : remainder;
        String rawPath = slashIdx >= 0 ? remainder.substring(slashIdx) : "/";

        String username = "";
        String password = "";
        String hostPort = authority;
        int atIdx = authority.lastIndexOf('@');
        if (atIdx >= 0) {
            String userInfo = authority.substring(0, atIdx);
            hostPort = authority.substring(atIdx + 1);
            int colonIdx = userInfo.indexOf(':');
            if (colonIdx >= 0) {
                username = userInfo.substring(0, colonIdx);
                password = userInfo.substring(colonIdx + 1);
            } else {
                username = userInfo;
            }
        }

        String normalizedPath = LaneSystem.normalizeStreamPath(rawPath, "/");
        return new ParsedRtspEndpoint(scheme, username, password, hostPort.trim(), normalizedPath);
    }

    public static FFMpegConfig applyChildToSimpleCameraConfig(
            FFMPEGConfig childConfig,
            FFMpegConfig existingSimple) {
        String connStr = (childConfig != null && childConfig.connection != null)
                ? childConfig.connection.connectionString
                : null;
        int bufferSize = (childConfig != null && childConfig.connection != null)
                ? childConfig.connection.bufferSize
                : (existingSimple != null ? existingSimple.bufferSize : 0);

        ParsedRtspEndpoint parsed = parseRtspConnectionString(connStr);
        if (parsed == null) {
            return existingSimple;
        }

        FFMpegConfig target;
        if (existingSimple instanceof AxisCameraConfig axisCfg) {
            if (AxisCameraConfig.CodecEndpoint.H264.getPath().equals(parsed.streamPath())) {
                axisCfg.streamPath = AxisCameraConfig.CodecEndpoint.H264;
                target = axisCfg;
            } else if (AxisCameraConfig.CodecEndpoint.MJPEG.getPath().equals(parsed.streamPath())) {
                axisCfg.streamPath = AxisCameraConfig.CodecEndpoint.MJPEG;
                target = axisCfg;
            } else {
                CustomCameraConfig custom = new CustomCameraConfig();
                custom.streamPath = parsed.streamPath();
                target = custom;
            }
        } else if (existingSimple instanceof SonyCameraConfig sonyCfg
                && "/media/video1".equals(parsed.streamPath())
                && (parsed.remoteHost().endsWith(":554") || !parsed.remoteHost().contains(":"))) {
            String hostOnly = parsed.remoteHost().endsWith(":554")
                    ? parsed.remoteHost().substring(0, parsed.remoteHost().length() - 4)
                    : parsed.remoteHost();
            sonyCfg.remoteHost = hostOnly;
            sonyCfg.username = parsed.username();
            sonyCfg.password = parsed.password();
            sonyCfg.bufferSize = bufferSize;
            sonyCfg.markSyncedEndpoint(cameraSyncToken(connStr, bufferSize));
            return sonyCfg;
        } else if (existingSimple instanceof CustomCameraConfig customCfg) {
            customCfg.streamPath = parsed.streamPath();
            target = customCfg;
        } else {
            CustomCameraConfig custom = new CustomCameraConfig();
            custom.streamPath = parsed.streamPath();
            target = custom;
        }

        target.remoteHost = parsed.remoteHost();
        target.username = parsed.username();
        target.password = parsed.password();
        target.bufferSize = bufferSize;
        target.markSyncedEndpoint(cameraSyncToken(connStr.trim(), bufferSize));
        return target;
    }

    public static CameraSyncResult reconcileCamera(
            LaneConfig laneConfig,
            int videoIndex,
            FFMpegConfig simpleConfig,
            FFMPEGConfig existingChildConfig) {
        sanitizeSimpleCameraConfig(simpleConfig);
        if (existingChildConfig == null) {
            FFMPEGConfig created = createInitialFFmpegChildConfig(laneConfig, simpleConfig, videoIndex);
            return new CameraSyncResult(SyncAction.REGISTER_NEW_CHILD, simpleConfig, created, true);
        }

        String parentConn = buildCameraConnectionString(simpleConfig);
        String parentToken = cameraSyncToken(parentConn, simpleConfig.bufferSize);
        String childConn = existingChildConfig.connection != null && existingChildConfig.connection.connectionString != null
                ? existingChildConfig.connection.connectionString.trim()
                : "";
        int childBuffer = existingChildConfig.connection != null ? existingChildConfig.connection.bufferSize : 0;
        String childToken = cameraSyncToken(childConn, childBuffer);

        String lastSynced = normalizeCameraToken(simpleConfig.lastSyncedEndpoint(), simpleConfig.bufferSize);

        if (Objects.equals(parentToken, childToken)) {
            boolean stamped = !Objects.equals(simpleConfig.lastSyncedEndpoint(), parentToken);
            simpleConfig.markSyncedEndpoint(parentToken);
            return new CameraSyncResult(SyncAction.NO_OP, simpleConfig, existingChildConfig, stamped);
        }

        boolean parentWins = chooseParentWins(
                lastSynced,
                parentToken,
                childToken,
                laneConfig != null ? laneConfig.lastUpdated : null,
                existingChildConfig.lastUpdated);

        if (parentWins || childConn.isEmpty()) {
            existingChildConfig.connection.connectionString = parentConn;
            existingChildConfig.connection.bufferSize = simpleConfig.bufferSize;
            if (existingChildConfig.serialNumber == null || existingChildConfig.serialNumber.isBlank()) {
                existingChildConfig.serialNumber = cameraSerialNumber(
                        laneConfig != null ? laneConfig.uniqueID : "", videoIndex);
            }
            simpleConfig.markSyncedEndpoint(parentToken);
            return new CameraSyncResult(SyncAction.UPDATE_CHILD, simpleConfig, existingChildConfig, true);
        } else {
            FFMpegConfig updatedSimple = applyChildToSimpleCameraConfig(existingChildConfig, simpleConfig);
            return new CameraSyncResult(SyncAction.UPDATE_PARENT, updatedSimple, existingChildConfig, true);
        }
    }

    public static SensorConfig createInitialRpmChildConfig(LaneConfig laneConfig, RPMConfig rpmConfig) {
        sanitizeSimpleRpmConfig(rpmConfig);
        Asserts.checkNotNull(rpmConfig.remoteHost);

        SensorConfig config;
        if (rpmConfig instanceof AspectRPMConfig aspectRPMConfig) {
            AspectConfig aspectConfig = new AspectConfig();
            aspectConfig.serialNumber = laneConfig.uniqueID;
            aspectConfig.moduleClass = AspectSensor.class.getCanonicalName();

            var comm = aspectConfig.commSettings = new ModbusTCPCommProviderConfig();
            comm.protocol.remoteHost = aspectRPMConfig.remoteHost;
            comm.protocol.remotePort = aspectRPMConfig.remotePort;
            if (aspectRPMConfig.addressRange != null) {
                comm.protocol.addressRange.from = aspectRPMConfig.addressRange.from;
                comm.protocol.addressRange.to = aspectRPMConfig.addressRange.to;
            }
            comm.connection.connectTimeout = 5000;
            comm.connection.reconnectAttempts = 10;
            comm.moduleClass = ModbusTCPCommProvider.class.getCanonicalName();
            config = aspectConfig;
        } else if (rpmConfig instanceof RapiscanRPMConfig rapiscanRPMConfig) {
            RapiscanConfig rapiscanConfig = new RapiscanConfig();
            rapiscanConfig.serialNumber = laneConfig.uniqueID;
            rapiscanConfig.moduleClass = RapiscanSensor.class.getCanonicalName();

            if (rapiscanRPMConfig.emlConfig != null) {
                var eml = rapiscanConfig.emlConfig = new EMLConfig();
                eml.emlEnabled = rapiscanRPMConfig.emlConfig.emlEnabled;
                eml.isCollimated = rapiscanRPMConfig.emlConfig.isCollimated;
                eml.laneWidth = rapiscanRPMConfig.emlConfig.laneWidth;
            }

            var comm = rapiscanConfig.commSettings = new TCPCommProviderConfig();
            comm.protocol.remoteHost = rapiscanRPMConfig.remoteHost;
            comm.protocol.remotePort = rapiscanRPMConfig.remotePort;
            comm.connection.connectTimeout = 5000;
            comm.connection.reconnectAttempts = 10;
            config = rapiscanConfig;
        } else if (rpmConfig instanceof RS350RPMConfig rs350RPMConfig) {
            RS350Config rs350Config = new RS350Config();
            rs350Config.serialNumber = laneConfig.uniqueID;
            rs350Config.moduleClass = RS350Sensor.class.getCanonicalName();

            var comm = rs350Config.commSettings = new TCPCommProviderConfig();
            comm.protocol.remoteHost = rs350RPMConfig.remoteHost;
            comm.protocol.remotePort = rs350RPMConfig.remotePort;
            comm.connection.connectTimeout = 5000;
            comm.connection.reconnectAttempts = 10;
            config = rs350Config;
        } else {
            throw new IllegalArgumentException(
                    "RPM Config specified is invalid, config must be AspectRPMConfig, RapiscanRPMConfig or RS350RPMConfig");
        }

        config.name = laneConfig.name + " - RPM";
        config.autoStart = true;
        rpmConfig.markSyncedEndpoint(rpmSyncToken(rpmConfig));
        return config;
    }

    public static void sanitizeSimpleRpmConfig(RPMConfig rpmConfig) {
        if (rpmConfig == null || rpmConfig.remoteHost == null) {
            return;
        }
        String host = rpmConfig.remoteHost.trim();
        int schemeIdx = host.indexOf("://");
        if (schemeIdx >= 0) {
            host = host.substring(schemeIdx + 3);
        }
        int slashIdx = host.indexOf('/');
        if (slashIdx >= 0) {
            host = host.substring(0, slashIdx);
        }
        // If someone pasted host:port into remoteHost (non-IPv6), split port into remotePort
        int firstColon = host.indexOf(':');
        int lastColon = host.lastIndexOf(':');
        if (firstColon > 0 && firstColon == lastColon) {
            try {
                int parsedPort = Integer.parseInt(host.substring(firstColon + 1));
                host = host.substring(0, firstColon);
                rpmConfig.remotePort = parsedPort;
            } catch (NumberFormatException ignored) {
            }
        }
        rpmConfig.remoteHost = host.trim();
    }

    public static RpmSyncResult reconcileRpm(
            LaneConfig laneConfig,
            RPMConfig rpmConfig,
            SensorConfig existingChildConfig) {
        if (rpmConfig == null) {
            if (existingChildConfig != null) {
                RPMConfig extracted = extractSimpleRpmConfig(existingChildConfig, null);
                if (extracted != null) {
                    return new RpmSyncResult(SyncAction.UPDATE_PARENT, extracted, existingChildConfig, true);
                }
            }
            return new RpmSyncResult(SyncAction.NO_OP, null, existingChildConfig, false);
        }

        sanitizeSimpleRpmConfig(rpmConfig);
        if (existingChildConfig == null) {
            SensorConfig created = createInitialRpmChildConfig(laneConfig, rpmConfig);
            return new RpmSyncResult(SyncAction.REGISTER_NEW_CHILD, rpmConfig, created, true);
        }

        // If the RPM driver class itself changed (e.g. Rapiscan -> Aspect), parent config wins
        if (!rpmTypeMatches(rpmConfig, existingChildConfig)) {
            SensorConfig recreated = createInitialRpmChildConfig(laneConfig, rpmConfig);
            recreated.id = existingChildConfig.id;
            return new RpmSyncResult(SyncAction.UPDATE_CHILD, rpmConfig, recreated, true);
        }

        String parentToken = rpmSyncToken(rpmConfig);
        String childToken = childRpmSyncToken(existingChildConfig);
        if (Objects.equals(parentToken, childToken)) {
            boolean stamped = !Objects.equals(rpmConfig.lastSyncedEndpoint(), parentToken);
            rpmConfig.markSyncedEndpoint(parentToken);
            return new RpmSyncResult(SyncAction.NO_OP, rpmConfig, existingChildConfig, stamped);
        }

        boolean parentWins = chooseParentWins(
                rpmConfig.lastSyncedEndpoint(),
                parentToken,
                childToken,
                laneConfig != null ? laneConfig.lastUpdated : null,
                existingChildConfig.lastUpdated);

        if (parentWins || childToken.isEmpty()) {
            applySimpleRpmToChildConfig(rpmConfig, existingChildConfig);
            rpmConfig.markSyncedEndpoint(parentToken);
            return new RpmSyncResult(SyncAction.UPDATE_CHILD, rpmConfig, existingChildConfig, true);
        } else {
            RPMConfig updatedParent = extractSimpleRpmConfig(existingChildConfig, rpmConfig);
            return new RpmSyncResult(SyncAction.UPDATE_PARENT, updatedParent, existingChildConfig, true);
        }
    }

    public static void applySimpleRpmToChildConfig(RPMConfig rpmConfig, SensorConfig childConfig) {
        sanitizeSimpleRpmConfig(rpmConfig);
        if (rpmConfig instanceof AspectRPMConfig aspectRpm && childConfig instanceof AspectConfig aspectChild) {
            if (aspectChild.commSettings == null) {
                aspectChild.commSettings = new ModbusTCPCommProviderConfig();
                aspectChild.commSettings.moduleClass = ModbusTCPCommProvider.class.getCanonicalName();
            }
            aspectChild.commSettings.protocol.remoteHost = aspectRpm.remoteHost;
            aspectChild.commSettings.protocol.remotePort = aspectRpm.remotePort;
            if (aspectRpm.addressRange != null && aspectChild.commSettings.protocol.addressRange != null) {
                aspectChild.commSettings.protocol.addressRange.from = aspectRpm.addressRange.from;
                aspectChild.commSettings.protocol.addressRange.to = aspectRpm.addressRange.to;
            }
        } else if (rpmConfig instanceof RapiscanRPMConfig rapiscanRpm && childConfig instanceof RapiscanConfig rapiscanChild) {
            if (rapiscanChild.commSettings == null) {
                rapiscanChild.commSettings = new TCPCommProviderConfig();
            }
            rapiscanChild.commSettings.protocol.remoteHost = rapiscanRpm.remoteHost;
            rapiscanChild.commSettings.protocol.remotePort = rapiscanRpm.remotePort;
            if (rapiscanRpm.emlConfig != null) {
                if (rapiscanChild.emlConfig == null) {
                    rapiscanChild.emlConfig = new EMLConfig();
                }
                rapiscanChild.emlConfig.emlEnabled = rapiscanRpm.emlConfig.emlEnabled;
                rapiscanChild.emlConfig.isCollimated = rapiscanRpm.emlConfig.isCollimated;
                rapiscanChild.emlConfig.laneWidth = rapiscanRpm.emlConfig.laneWidth;
            }
        } else if (rpmConfig instanceof RS350RPMConfig rs350Rpm && childConfig instanceof RS350Config rs350Child) {
            if (rs350Child.commSettings == null) {
                rs350Child.commSettings = new TCPCommProviderConfig();
            }
            rs350Child.commSettings.protocol.remoteHost = rs350Rpm.remoteHost;
            rs350Child.commSettings.protocol.remotePort = rs350Rpm.remotePort;
        }
    }

    public static RPMConfig extractSimpleRpmConfig(SensorConfig childConfig, RPMConfig existingSimple) {
        if (childConfig instanceof AspectConfig aspectChild && aspectChild.commSettings != null) {
            AspectRPMConfig target = existingSimple instanceof AspectRPMConfig a ? a : new AspectRPMConfig();
            target.remoteHost = aspectChild.commSettings.protocol.remoteHost;
            target.remotePort = aspectChild.commSettings.protocol.remotePort;
            if (aspectChild.commSettings.protocol.addressRange != null) {
                if (target.addressRange == null) {
                    target.addressRange = new AspectRPMConfig.AddressRange();
                }
                target.addressRange.from = aspectChild.commSettings.protocol.addressRange.from;
                target.addressRange.to = aspectChild.commSettings.protocol.addressRange.to;
            }
            target.markSyncedEndpoint(rpmSyncToken(target));
            return target;
        } else if (childConfig instanceof RapiscanConfig rapiscanChild && rapiscanChild.commSettings != null) {
            RapiscanRPMConfig target = existingSimple instanceof RapiscanRPMConfig r ? r : new RapiscanRPMConfig();
            target.remoteHost = rapiscanChild.commSettings.protocol.remoteHost;
            target.remotePort = rapiscanChild.commSettings.protocol.remotePort;
            if (rapiscanChild.emlConfig != null) {
                if (target.emlConfig == null) {
                    target.emlConfig = new RapiscanRPMConfig.EMLConfig();
                }
                target.emlConfig.emlEnabled = rapiscanChild.emlConfig.emlEnabled;
                target.emlConfig.isCollimated = rapiscanChild.emlConfig.isCollimated;
                target.emlConfig.laneWidth = rapiscanChild.emlConfig.laneWidth;
            }
            target.markSyncedEndpoint(rpmSyncToken(target));
            return target;
        } else if (childConfig instanceof RS350Config rs350Child && rs350Child.commSettings != null) {
            RS350RPMConfig target = existingSimple instanceof RS350RPMConfig r ? r : new RS350RPMConfig();
            target.remoteHost = rs350Child.commSettings.protocol.remoteHost;
            target.remotePort = rs350Child.commSettings.protocol.remotePort;
            target.markSyncedEndpoint(rpmSyncToken(target));
            return target;
        }
        return existingSimple;
    }

    static boolean chooseParentWins(
            String lastSyncedToken,
            String parentToken,
            String childToken,
            Date parentLastUpdated,
            Date childLastUpdated) {
        if (lastSyncedToken != null && !lastSyncedToken.isBlank()) {
            boolean parentChanged = !Objects.equals(parentToken, lastSyncedToken);
            boolean childChanged = !Objects.equals(childToken, lastSyncedToken);
            if (childChanged && !parentChanged) {
                return false;
            }
            if (parentChanged && !childChanged) {
                return true;
            }
        }
        if (parentLastUpdated != null && childLastUpdated != null) {
            if (childLastUpdated.after(parentLastUpdated)) {
                return false;
            }
            if (parentLastUpdated.after(childLastUpdated)) {
                return true;
            }
        }
        return true;
    }

    private static boolean rpmTypeMatches(RPMConfig rpmConfig, SensorConfig childConfig) {
        return (rpmConfig instanceof AspectRPMConfig && childConfig instanceof AspectConfig)
                || (rpmConfig instanceof RapiscanRPMConfig && childConfig instanceof RapiscanConfig)
                || (rpmConfig instanceof RS350RPMConfig && childConfig instanceof RS350Config);
    }

    private static String cameraSyncToken(String connectionString, int bufferSize) {
        return (connectionString == null ? "" : connectionString.trim()) + "|buf=" + bufferSize;
    }

    private static String normalizeCameraToken(String stored, int fallbackBufferSize) {
        if (stored == null || stored.isBlank()) {
            return null;
        }
        if (stored.contains("|buf=")) {
            return stored;
        }
        return cameraSyncToken(stored, fallbackBufferSize);
    }

    static String rpmSyncToken(RPMConfig rpm) {
        if (rpm == null) {
            return "";
        }
        String base = (rpm.remoteHost == null ? "" : rpm.remoteHost.trim()) + ":" + rpm.remotePort;
        if (rpm instanceof AspectRPMConfig a && a.addressRange != null) {
            return base + "|addr=" + a.addressRange.from + "-" + a.addressRange.to;
        }
        if (rpm instanceof RapiscanRPMConfig r && r.emlConfig != null) {
            return base + "|eml=" + r.emlConfig.emlEnabled + ":" + r.emlConfig.isCollimated + ":" + r.emlConfig.laneWidth;
        }
        return base;
    }

    static String childRpmSyncToken(SensorConfig child) {
        if (child instanceof AspectConfig a && a.commSettings != null) {
            String base = (a.commSettings.protocol.remoteHost == null ? "" : a.commSettings.protocol.remoteHost.trim())
                    + ":" + a.commSettings.protocol.remotePort;
            if (a.commSettings.protocol.addressRange != null) {
                return base + "|addr=" + a.commSettings.protocol.addressRange.from
                        + "-" + a.commSettings.protocol.addressRange.to;
            }
            return base;
        }
        if (child instanceof RapiscanConfig r && r.commSettings != null) {
            String base = (r.commSettings.protocol.remoteHost == null ? "" : r.commSettings.protocol.remoteHost.trim())
                    + ":" + r.commSettings.protocol.remotePort;
            if (r.emlConfig != null) {
                return base + "|eml=" + r.emlConfig.emlEnabled + ":" + r.emlConfig.isCollimated + ":" + r.emlConfig.laneWidth;
            }
            return base;
        }
        if (child instanceof RS350Config rs && rs.commSettings != null) {
            return (rs.commSettings.protocol.remoteHost == null ? "" : rs.commSettings.protocol.remoteHost.trim())
                    + ":" + rs.commSettings.protocol.remotePort;
        }
        return "";
    }
}
