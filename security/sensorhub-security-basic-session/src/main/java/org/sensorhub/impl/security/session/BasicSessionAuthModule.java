/*******************************************************************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 The Initial Developer is GeoRobotix Innovative Research Inc. Portions created by the Initial
 Developer are Copyright (C) 2026 the Initial Developer. All Rights Reserved.

 ******************************************************************************/

package org.sensorhub.impl.security.session;

import org.eclipse.jetty.security.Authenticator;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.module.AbstractModule;


public class BasicSessionAuthModule extends AbstractModule<BasicSessionConfig>
{
    Authenticator authenticator;

    @Override
    protected void doStart() throws SensorHubException
    {

        authenticator = new BasicSessionAuthenticator(
                config,
                getParentHub().getSecurityManager(),
                getLogger()
        );

        getParentHub().getSecurityManager().registerAuthenticator(authenticator);
    }


    @Override
    protected void doStop() throws SensorHubException
    {
        if (authenticator != null) {
            getParentHub().getSecurityManager().unregisterAuthenticator(authenticator);
            if (authenticator instanceof BasicSessionAuthenticator)
                ((BasicSessionAuthenticator) authenticator).close();
        }
        this.authenticator = null;
    }


    @Override
    public void cleanup() throws SensorHubException
    {
    }
}
