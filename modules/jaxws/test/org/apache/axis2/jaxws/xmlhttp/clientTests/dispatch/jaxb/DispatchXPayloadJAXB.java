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
package org.apache.axis2.jaxws.xmlhttp.clientTests.dispatch.jaxb;

import java.io.ByteArrayInputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Service;
import javax.xml.ws.http.HTTPBinding;

import org.apache.axis2.jaxws.message.util.Reader2Writer;
import org.apache.axis2.jaxws.TestLogger;

import test.EchoString;
import test.EchoStringResponse;
import test.ObjectFactory;

import junit.framework.TestCase;

public class DispatchXPayloadJAXB extends TestCase {
    
    private static XMLInputFactory inputFactory = XMLInputFactory.newInstance();

    public String HOSTPORT = "http://localhost:8080";
        
    private String ENDPOINT_URL = HOSTPORT + "/axis2/services/XPayloadSourceProvider";
    private QName SERVICE_NAME  = new QName("http://ws.apache.org/axis2", "XPayloadSourceProvider");
    private QName PORT_NAME  = new QName("http://ws.apache.org/axis2", "XPayloadSourceProviderPort");

    String XML_TEXT = "<p:echo xmlns:p=\"http://sample\">hello world</p:echo>";
    public Dispatch<Object> getDispatch() throws JAXBException {
        Service service = Service.create(SERVICE_NAME);
        service.addPort(PORT_NAME, HTTPBinding.HTTP_BINDING,ENDPOINT_URL);
        JAXBContext jbc = JAXBContext.newInstance("test");
        Dispatch<Object> dispatch = service.createDispatch(PORT_NAME, jbc, Service.Mode.PAYLOAD);
        return dispatch;
     }
    
    /**
    * Simple XML/HTTP Message Test
    * @throws Exception
    */
   public void testSimple() throws Exception {
       Dispatch<Object> dispatch = getDispatch();
       ObjectFactory factory = new ObjectFactory();
       EchoString request = factory.createEchoString();         
       request.setInput("SYNC JAXB XML PAYLOAD TEST");
       
       // Invoke the Dispatch<Object>
        TestLogger.logger.debug(">> Invoking sync Dispatch with JAX-B Parameter");
       EchoString response = (EchoString) dispatch.invoke(request);
       
       assertNotNull(response);

        TestLogger.logger.debug(">> Response content: " + response.getInput());
       
       assertTrue("[ERROR] - Response object was null", response != null);
       assertTrue("[ERROR] - No content in response object", response.getInput() != null);
       assertTrue("[ERROR] - Zero length content in response", response.getInput().length() > 0);
       assertTrue(response.getInput().equals(request.getInput()));

   }
}