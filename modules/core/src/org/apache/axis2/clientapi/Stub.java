/*
* Copyright 2001-2004 The Apache Software Foundation.
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

package org.apache.axis2.clientapi;

import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.ServiceContext;
import org.apache.axis2.deployment.DeploymentException;
import org.apache.axis2.description.ServiceDescription;
import org.apache.axis2.om.OMAbstractFactory;
import org.apache.axis2.om.OMElement;
import org.apache.axis2.om.OMFactory;
import org.apache.axis2.om.OMNamespace;
import org.apache.axis2.om.impl.llom.builder.StAXOMBuilder;
import org.apache.axis2.om.impl.llom.factory.OMXMLBuilderFactory;
import org.apache.axis2.soap.*;
import org.apache.axis2.soap.impl.llom.SOAPProcessingException;
import org.apache.wsdl.WSDLService;

import javax.xml.stream.XMLStreamReader;


/**
 * @author chathura@opensource.lk
 */
public abstract class Stub{

    public static final int SOAP_11 =0;
    public static final int SOAP_12 =1;


    protected ConfigurationContext _configurationContext;
    protected static ServiceDescription _service;
    protected ServiceContext _serviceContext;
    protected EndpointReference toEPR;

    protected boolean doRest=false;

    protected String senderTransport = Constants.TRANSPORT_HTTP;
    protected String listenerTransport =Constants.TRANSPORT_HTTP ;
    protected boolean useSeparateListener;
    //Default SOAP version
    protected int soapVesrion = SOAP_11;

    public void setTransportInfo(String senderTransport,String listenerTransport,boolean useSeparateListener)throws AxisFault{
        this.senderTransport = senderTransport;
        this.listenerTransport=listenerTransport;
        this.useSeparateListener=useSeparateListener;
    }

    public void setDoREST(boolean doRest) {
        this.doRest = doRest;
    }


    /**
     * If _maintainSession is set to True all the calls will use the same
     * ServiceContext and the user can Share information through that
     * ServiceContext across operations.
     */
    protected boolean _maintainSession = false;
    protected String _currentSessionId = null;


    protected Stub() throws DeploymentException, AxisFault {

    }

    /**
     * Set the soap version
     * @param soapVersion
     */
    public void setSOAPVersion(int soapVersion){
        this.soapVesrion = soapVersion;
    }

//	public abstract void _setSessionInfo(Object key, Object value) throws Exception;
//
//	public abstract Object _getSessionInfo(Object key) throws Exception ;

    public void _setSessionInfo(String key, Object value) throws java.lang.Exception {
        if (!_maintainSession) {
            //TODO Comeup with a Exception
            throw new java.lang.Exception(
                    "Client is running the session OFF mode: Start session before saving to a session ");
        }
        _configurationContext.getServiceContext(_currentSessionId).setProperty(
                key, value);
    }


    public Object _getSessionInfo(String key) throws java.lang.Exception {
        if (!_maintainSession) {
            //TODO Comeup with a Exception
            throw new java.lang.Exception(
                    "Client is running the session OFF mode: Start session before saving to a session ");
        }
        return _configurationContext.getServiceContext(_currentSessionId)
                .getProperty(key);
    }

    public void _startSession() {
        _maintainSession = true;
        _currentSessionId = getID();
    }

    public void _endSession() {
        _maintainSession = false;
    }

    protected String _getServiceContextID() {
        if (_maintainSession)
            return _currentSessionId;
        else
            return getID();
    }

    private String getID() {
        //TODO Get the UUID generator to generate values
        return Long.toString(System.currentTimeMillis());
    }


    protected SOAPEnvelope createEnvelope() throws SOAPProcessingException {
        return getFactory(this.soapVesrion).getDefaultEnvelope();
    }

    protected void setValueRPC(SOAPEnvelope env,
                               String methodNamespaceURI,
                               String methodName,
                               String[] paramNames,
                               Object[] values) {
        SOAPBody body = env.getBody();
        OMFactory fac = this.getFactory(this.soapVesrion);

        OMNamespace methodNamespace = fac.createOMNamespace(methodNamespaceURI,
                "ns1");
        OMElement elt = fac.createOMElement(methodName, methodNamespace);
        if (paramNames != null) {
            //find the relevant object here, convert it and add it to the elt
            for (int i = 0; i < paramNames.length; i++) {
                String paramName = paramNames[i];
                Object value = values[i];
                elt.addChild(StubSupporter.createRPCMappedElement(paramName,
                        fac.createOMNamespace("", null), //empty namespace
                        value,
                        fac));
            }
        }
        body.addChild(elt);
    }


    protected OMElement getElementFromReader(XMLStreamReader reader) {
        StAXOMBuilder builder = OMXMLBuilderFactory.createStAXOMBuilder(
                OMAbstractFactory.getOMFactory(), reader);
        return builder.getDocumentElement();
    }

    protected void setValueDoc(SOAPEnvelope env, OMElement value) {
        setValueDoc(env,value,false);
    }

    protected void setValueDoc(SOAPEnvelope env, OMElement value,boolean isHeader) {

        if (value != null) {
            if (isHeader){
                SOAPHeader header = env.getHeader();
                header.addChild(value);
            }else{
                SOAPBody body = env.getBody();
                body.addChild(value);
            }

        }
    }

//    /**
//     * use this method to handle the faults
//     * @param env
//     */
//    protected void checkFault(SOAPEnvelope env) throws AxisFault{
//        SOAPBody body = env.getBody();
//        if (body.hasFault()){
//           SOAPFault fault = body.getFault();
//           if (null!=fault.getException()){
//               throw new AxisFault(fault.getException());
//           }else{
//               String message = "";
//               message = message + "Code =" + fault.getCode()==null?"":
//                       fault.getCode().getValue()==null?"":fault.getCode().getValue().getText();
//               message = message + "Actor = "+fault.getRole()==null?"":
//                                   fault.getRole().getRoleValue();
//               //add the details here
//               throw new AxisFault(message);
//           }
//        }
//    }

    /**
     * Extract the correct element - A util method
     * @param env
     * @param type
     * @return the relevant element to be databound
     */
    protected OMElement getElement(SOAPEnvelope env, String type) {
        SOAPBody body = env.getBody();
        OMElement element = body.getFirstElement();

        if (WSDLService.STYLE_RPC.equals(type)) {
            return element.getFirstElement(); //todo this needs to be fixed
        } else if (WSDLService.STYLE_DOC.equals(type)) {
            return element;
        } else {
            throw new UnsupportedOperationException("Unsupported type");
        }

    }

//    protected Object getValue(SOAPEnvelope env,String type,Class outputType){
//        SOAPBody body = env.getBody();
//        OMElement element = body.getFirstElement();
//
//        if (WSDLService.STYLE_RPC.equals(type)){
//            return StubSupporter.getRPCMappedElementValue(element.getFirstElement(),outputType);
//        }else if (WSDLService.STYLE_DOC.equals(type)){
//            return element;
//        }else {
//            throw new UnsupportedOperationException("Unsupported type");
//        }
//
//    }
    /**
     * get the message context
     */
    protected MessageContext getMessageContext() throws AxisFault {
        return new MessageContext(_configurationContext);
    }


    private SOAPFactory getFactory(int soapVersion) {
        if (soapVersion==SOAP_11){
            return OMAbstractFactory.getSOAP11Factory();
        }else if (soapVersion==SOAP_12){
            return OMAbstractFactory.getSOAP12Factory();
        }else{
            throw new RuntimeException("Unknown SOAP version");
        }
    }
}

