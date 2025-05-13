# OSCAR Admin UI Widgets

In order to use widgets found in here, you must add them to your `AdminUIConfig` inside of `config.json`.

Custom panels should be added under `"customPanels"` and custom forms should be added under `"customForms"`
An example can be shown below.

```json
  {
    "objClass": "org.sensorhub.ui.AdminUIConfig",
    "widgetSet": "org.sensorhub.ui.SensorHubWidgetSet",
    "bundleRepoUrls": [],
    "customPanels": [],
    "customForms": [
      {
        "configClass": "com.botts.impl.system.lane.config.LaneOptionsConfig",
        "uiClass": "com.botts.ui.oscar.forms.LaneConfigForm"
      }
    ],
    "id": "5cb05c9c-9123-4fa1-8731-ffaa51489678",
    "autoStart": true,
    "moduleClass": "org.sensorhub.ui.AdminUIModule",
    "name": "Admin UI"
  }
```