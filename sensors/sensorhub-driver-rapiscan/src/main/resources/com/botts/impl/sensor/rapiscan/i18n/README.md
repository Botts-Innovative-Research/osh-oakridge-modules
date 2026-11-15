# Rapiscan Radiation Portal Monitor

The Rapiscan driver connects to an RPM over TCP and publishes gamma, neutron, occupancy, and related data.

## Configuration

- Enter a unique serial number and lane ID.
- Under **Communication Settings**, add a TCP provider and enter the RPM host and port.
- Configure connection timeouts, retry behavior, and reachability checks.
- Enter the sensor position when it is not inherited from a lane system.
- Use **RPM Gamma Hardware Setup** and **RPM Neutron Hardware Setup** for the gamma and neutron hardware parameters; when the RPM publishes its daily setup messages (`SG1`–`SG3` and `SN1`–`SN2`) in the daily-file stream, the driver automatically updates these parameters in memory and coalesces any changes into an asynchronous `config.json` update without blocking the real-time stream.
- Enable **EML Analysis** only for a VM250 EML lane after uploading the external EML plugin archive in **Services > OSCAR Service Module > EML Plugin Path**, then enter its collimation state and width. Non-threat EML alarms (`RELEASE`) are suppressed from the Dashboard and automatically adjudicated as **EML Non-Threat** (code `9999`) with queryable EML metadata.
- Enable **Auto Start** to start the driver with the node.

<details class="oscar-video-guide">
  <summary>Video guide (#07, 11.8s): Upload External EML VM-250 Plugin &amp; Enable Per-Lane EML</summary>
  <video controls loading="lazy" width="720" height="405" preload="none" playsinline>
    <source src="/video-guides/videos/07-admin-eml-plugin-setup.mp4" type="video/mp4">
    <track kind="captions" src="/video-guides/captions/07-admin-eml-plugin-setup.vtt" srclang="en" label="English" default>
  </video>
</details>
