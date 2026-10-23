/*******************************************************************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 The Initial Developer is Botts Innovative Research Inc. Portions created by the Initial
 Developer are Copyright (C) 2025 the Initial Developer. All Rights Reserved.

 ******************************************************************************/

package com.botts.impl.system.lane;

/**
 * Exposes the completion boundary of a lane's synchronous child auto-start pass.
 * Child modules can finish that pass unavailable or unstartable; callers still
 * need to inspect asynchronous STARTING states separately.
 */
public interface IAutoStartDispatchStatus {
    boolean isAutoStartDispatchComplete();
}
