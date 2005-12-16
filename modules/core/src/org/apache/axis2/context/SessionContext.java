/*
* Copyright 2004,2005 The Apache Software Foundation.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/


package org.apache.axis2.context;

import org.apache.axis2.AxisFault;
import org.apache.axis2.engine.AxisConfiguration;

/**
 * All the engine components are stateless across the executions and all the states should be kept in the
 * Contexts, there are three context Global, Session and Message.
 */
public class SessionContext extends AbstractContext {

    /**
     * @param parent
     */
    public SessionContext(AbstractContext parent) {
        super(parent);
    }

    public void init(AxisConfiguration axisConfiguration) throws AxisFault {
    }
}
