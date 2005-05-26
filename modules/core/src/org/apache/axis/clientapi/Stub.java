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

package org.apache.axis.clientapi;

import org.apache.axis.addressing.EndpointReference;
import org.apache.axis.context.ConfigurationContext;
import org.apache.axis.context.MessageContext;
import org.apache.axis.context.ServiceContext;
import org.apache.axis.deployment.DeploymentException;
import org.apache.axis.description.ServiceDescription;
import org.apache.axis.engine.AxisFault;
import org.apache.axis.om.OMAbstractFactory;
import org.apache.axis.om.OMElement;
import org.apache.axis.om.OMFactory;
import org.apache.axis.om.OMNamespace;
import org.apache.axis.soap.SOAPEnvelope;
import org.apache.axis.soap.SOAPBody;
import org.apache.axis.soap.SOAPFactory;
import org.apache.wsdl.WSDLService;



/**
 * @author chathura@opensource.lk
 *
 */
public abstract class Stub {

    protected ConfigurationContext _configurationContext;
    protected static ServiceDescription _service;
    protected ServiceContext _serviceContext;
    protected EndpointReference toEPR ;



    /**
     * If _maintainSession is set to True all the calls will use the same
     * ServiceContext and the user can Share information through that
     * ServiceContext across operations.
     */
    protected boolean _maintainSession = false;
    protected String _currentSessionId = null;


    protected Stub()throws DeploymentException, AxisFault{

    }

//	public abstract void _setSessionInfo(Object key, Object value) throws Exception;
//
//	public abstract Object _getSessionInfo(Object key) throws Exception ;

    public void _setSessionInfo(String key, Object value)throws java.lang.Exception{
        if(!_maintainSession){
            //TODO Comeup with a Exception
            throw new java.lang.Exception("Client is running the session OFF mode: Start session before saving to a session ");
        }
        _configurationContext.getServiceContext(_currentSessionId).setProperty(key, value);
    }


    public Object _getSessionInfo(String key) throws java.lang.Exception{
        if(!_maintainSession){
            //TODO Comeup with a Exception
            throw new java.lang.Exception("Client is running the session OFF mode: Start session before saving to a session ");
        }
        return _configurationContext.getServiceContext(_currentSessionId).getProperty(key);
    }

    public void _startSession(){
        _maintainSession = true;
        _currentSessionId = getID() ;
    }

    public void _endSession(){
        _maintainSession = false;
    }

    protected String _getServiceContextID(){
        if(_maintainSession)
            return _currentSessionId;
        else
            return getID();
    }

    private String getID(){
        //TODO Get the UUID generator to generate values
        return Long.toString(System.currentTimeMillis());
    }

    //todo make this compliant with the SOAP12
    protected SOAPEnvelope createEnvelope(){
        SOAPEnvelope env = getFactory().getDefaultEnvelope();
        return env;
    }

    protected void setValueRPC(SOAPEnvelope env,String methodNamespaceURI,String methodName,String[] paramNames,Object[] values){
        SOAPBody body = env.getBody();
        OMFactory fac = this.getFactory();

        OMNamespace methodNamespace = fac.createOMNamespace(methodNamespaceURI,"ns1");
        OMElement elt =  fac.createOMElement(methodName,methodNamespace);
        if (paramNames!=null){
        //find the relevant object here, convert it and add it to the elt
        for (int i = 0; i < paramNames.length; i++) {
            String paramName = paramNames[i];
            Object value  = values[i];
            elt.addChild(StubSupporter.createRPCMappedElement(paramName,
                    fac.createOMNamespace("",null),//empty namespace
                    value,
                    fac));
        }
        }
        body.addChild(elt);
    }


    protected void setValueDOC(SOAPEnvelope env,OMElement value){
        SOAPBody body = env.getBody();
        body.addChild(value);
    }


    protected Object getValue(SOAPEnvelope env,String type,Class outputType){
        SOAPBody body = env.getBody();
        OMElement element = body.getFirstElement();

        if (WSDLService.STYLE_RPC.equals(type)){
            return StubSupporter.getRPCMappedElementValue(element.getFirstElement(),outputType);
        }else if (WSDLService.STYLE_DOC.equals(type)){
            return element;
        }else {
            throw new UnsupportedOperationException("Unsupported type");
        }

    }
    /**
     * get the message context
     */
    protected MessageContext getMessageContext() throws AxisFault {
            return new MessageContext(_configurationContext);
    }


    private SOAPFactory getFactory(){
        return OMAbstractFactory.getSOAP11Factory();
    }
}

