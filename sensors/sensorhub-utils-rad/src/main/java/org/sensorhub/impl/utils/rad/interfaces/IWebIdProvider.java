package org.sensorhub.impl.utils.rad.interfaces;

import org.sensorhub.impl.utils.rad.webid.WebIdClient;

// Band-Aid fix to circular dependency
public interface IWebIdProvider {
    public WebIdClient getWebIdClient();
}
