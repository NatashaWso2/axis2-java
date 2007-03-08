/*
 * Copyright 2004,2005 The Apache Software Foundation.
 * Copyright 2006 International Business Machines Corp.
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
package org.apache.axis2.jaxws.description;

import java.net.URL;
import javax.xml.namespace.QName;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.jaxws.ExceptionFactory;

public class DescriptionKey {

    private QName serviceName = null;

    private URL wsdlUrl = null;

    private Class serviceClass = null;

    private ConfigurationContext configContext = null;

    public DescriptionKey(QName serviceName, URL wsdlUrl, Class serviceClass,
            ConfigurationContext configContext) {
        super();
        this.serviceName = serviceName;
        this.wsdlUrl = wsdlUrl;
        this.serviceClass = serviceClass;
        this.configContext = configContext;

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof DescriptionKey)) {
            return false;
        }

        DescriptionKey description = (DescriptionKey) o;

        if (serviceName == null) {
            throw ExceptionFactory
                    .makeWebServiceException(org.apache.axis2.i18n.Messages
                            .getMessage("DescriptionRegistryErr0"));
        }

        return description.serviceName.equals(this.serviceName)
                && description.wsdlUrl !=null ? description.wsdlUrl.equals(this.wsdlUrl): this.wsdlUrl == null
                && description.serviceClass == this.serviceClass
                && description.configContext == this.configContext;
    }

    @Override
    public int hashCode() {

        int hash = 1;
        hash = 31 * hash + ((serviceName == null) ? 0 : serviceName.hashCode());
        hash = hash + ((wsdlUrl == null) ? 0 : wsdlUrl.hashCode());
        hash = hash + ((serviceClass == null) ? 0 : serviceClass.hashCode());
        hash = hash + ((configContext == null) ? 0 : configContext.hashCode());
        return hash;

    }

    public ConfigurationContext getConfigContext() {
        return configContext;
    }

    public void setConfigContext(ConfigurationContext configContext) {
        this.configContext = configContext;
    }

    public QName getServiceName() {
        return serviceName;
    }

    public void setServiceName(QName serviceName) {
        this.serviceName = serviceName;
    }

    public URL getWsdlUrl() {
        return wsdlUrl;
    }

    public void setWsdlUrl(URL wsdlUrl) {
        this.wsdlUrl = wsdlUrl;
    }

    public Class getServiceClass() {
        return serviceClass;
    }

    public void setServiceClass(Class serviceClass) {
        this.serviceClass = serviceClass;
    }

    public String printKey() {

        String sName = (serviceName != null) ? serviceName.toString() : "";
        String sWsdlURL = (wsdlUrl != null) ? wsdlUrl.toString() : "";
        String sClass = (serviceClass != null) ? serviceClass.toString() : "";
        String sConfig = (configContext != null) ? configContext.toString()
                : "";
        String key = sName + sWsdlURL + sClass + sConfig;
        return key;
    }
}
