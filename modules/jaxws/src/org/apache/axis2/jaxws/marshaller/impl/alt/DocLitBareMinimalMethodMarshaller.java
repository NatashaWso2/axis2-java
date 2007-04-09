/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.axis2.jaxws.marshaller.impl.alt;

import org.apache.axis2.jaxws.ExceptionFactory;
import org.apache.axis2.jaxws.description.EndpointDescription;
import org.apache.axis2.jaxws.description.EndpointInterfaceDescription;
import org.apache.axis2.jaxws.description.OperationDescription;
import org.apache.axis2.jaxws.description.ParameterDescription;
import org.apache.axis2.jaxws.i18n.Messages;
import org.apache.axis2.jaxws.marshaller.MethodMarshaller;
import org.apache.axis2.jaxws.message.Message;
import org.apache.axis2.jaxws.message.Protocol;
import org.apache.axis2.jaxws.message.factory.MessageFactory;
import org.apache.axis2.jaxws.registry.FactoryRegistry;
import org.apache.axis2.jaxws.runtime.description.marshal.MarshalServiceRuntimeDescription;
import org.apache.axis2.jaxws.utility.ConvertUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.namespace.QName;
import javax.xml.ws.WebServiceException;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.TreeSet;


/**
 * The Doc/Lit Wrapped Minimal Marshaller is used when 1) The web service is Doc/Lit mininal, and 2)
 * JAXB artifacts are missing (i.e. we don't have ObjectFactories)
 */
public class DocLitBareMinimalMethodMarshaller implements MethodMarshaller {

    private static Log log = LogFactory.getLog(DocLitBareMinimalMethodMarshaller.class);

    public DocLitBareMinimalMethodMarshaller() {
        super();
    }

    public Object demarshalResponse(Message message, Object[] signatureArgs,
                                    OperationDescription operationDesc)
            throws WebServiceException {

        EndpointInterfaceDescription ed = operationDesc.getEndpointInterfaceDescription();
        EndpointDescription endpointDesc = ed.getEndpointDescription();

        // Note all exceptions are caught and rethrown with a WebServiceException
        try {
            // Sample Document message
            // ..
            // <soapenv:body>
            //    <m:return ... >...</m:param>
            // </soapenv:body>
            //
            // Important points.
            //   1) There is no operation element in the message
            //   2) The data blocks are located underneath the operation element. 
            //   3) The name of the data blocks (m:param) are defined by the schema.
            //      (SOAP indicates that the name of the element is not important, but
            //      for document processing, we will assume that the name corresponds to 
            //      a schema root element)
            //   4) The type of the data block is defined by schema; thus in most cases
            //      an xsi:type will not be present
            ParameterDescription[] pds = operationDesc.getParameterDescriptions();
            MarshalServiceRuntimeDescription marshalDesc =
                    MethodMarshallerUtils.getMarshalDesc(endpointDesc);
            TreeSet<String> packages = marshalDesc.getPackages();

            // Get the return value.
            Class returnType = operationDesc.getResultActualType();
            Object returnValue = null;
            if (returnType != void.class) {
                // Use "byJavaType" unmarshalling if necessary
                Class byJavaType = null;
                if (MethodMarshallerUtils.isJAXBBasicType(returnType)) {
                    byJavaType = returnType;
                }
                // If the webresult is in the header, we need the name of the header so that we can find it.
                Element returnElement = null;
                if (operationDesc.isResultHeader()) {
                    returnElement = MethodMarshallerUtils
                            .getReturnElement(packages, message, byJavaType, true,
                                              operationDesc.getResultTargetNamespace(),
                                              operationDesc.getResultName());
                } else {
                    returnElement = MethodMarshallerUtils
                            .getReturnElement(packages, message, byJavaType, false, null, null);
                }
                //TODO should we allow null if the return is a header?
                //Validate input parameters for operation and make sure no input parameters are null.
                //As per JAXWS Specification section 3.6.2.3 if a null value is passes as an argument 
                //to a method then an implementation MUST throw WebServiceException.
                returnValue = returnElement.getTypeValue();
                if (ConvertUtils.isConvertable(returnValue, returnType)) {
                	returnValue = ConvertUtils.convert(returnValue, returnType);
                }                
                if (returnValue == null) {
                    throw ExceptionFactory.makeWebServiceException(Messages.getMessage(
                            "NullParamErr1", "Return", operationDesc.getJavaMethodName(),
                            "doc/lit"));
                }
            }

            // We want to use "by Java Type" unmarshalling for 
            // allall non-JAXB objects
            Class[] javaTypes = new Class[pds.length];
            for (int i = 0; i < pds.length; i++) {
                ParameterDescription pd = pds[i];
                Class type = pd.getParameterActualType();
                if (MethodMarshallerUtils.isJAXBBasicType(type)) {
                    javaTypes[i] = type;
                }
            }

            // Unmarshall the ParamValues from the Message
            List<PDElement> pvList = MethodMarshallerUtils.getPDElements(pds,
                                                                         message,
                                                                         packages,
                                                                         false, // output
                                                                         javaTypes); // byJavaType unmarshalling

            // Populate the response Holders
            MethodMarshallerUtils.updateResponseSignatureArgs(pds, pvList, signatureArgs);

            return returnValue;
        } catch (Exception e) {
            throw ExceptionFactory.makeWebServiceException(e);
        }
    }

    public Object[] demarshalRequest(Message message, OperationDescription operationDesc)
            throws WebServiceException {

        EndpointInterfaceDescription ed = operationDesc.getEndpointInterfaceDescription();
        EndpointDescription endpointDesc = ed.getEndpointDescription();

        // Note all exceptions are caught and rethrown with a WebServiceException
        try {
            // Sample Document message
            // ..
            // <soapenv:body>
            //    <m:param .. >...</m:param>
            // </soapenv:body>
            //
            // Important points.
            //   1) There is no operation element under the body.
            //   2) The data blocks are located underneath the body.  
            //   3) The name of the data blocks (m:param) are defined by the schema
            //   4) The type of the data block (data:foo) is defined by schema (and probably
            //      is not present in the message
            ParameterDescription[] pds = operationDesc.getParameterDescriptions();
            MarshalServiceRuntimeDescription marshalDesc =
                    MethodMarshallerUtils.getMarshalDesc(endpointDesc);
            TreeSet<String> packages = marshalDesc.getPackages();

            // We want to use "by Java Type" unmarshalling for 
            // allall non-JAXB objects
            Class[] javaTypes = new Class[pds.length];
            for (int i = 0; i < pds.length; i++) {
                ParameterDescription pd = pds[i];
                Class type = pd.getParameterActualType();
                // If it is a JAXB basic type or it has no annotations
                if (MethodMarshallerUtils.isJAXBBasicType(type)) {
                    javaTypes[i] = type;
                } else {
                    Annotation annos[] = type.getAnnotations();
                    if (annos == null || annos.length == 0) {
                        javaTypes[i] = type;
                    }
                }
            }

            // Unmarshal the ParamValues from the message
            List<PDElement> pvList = MethodMarshallerUtils.getPDElements(pds,
                                                                         message,
                                                                         packages,
                                                                         true, // input
                                                                         javaTypes); // never unmarshal by type for doc/lit bare

            // Build the signature arguments
            Object[] sigArguments = MethodMarshallerUtils.createRequestSignatureArgs(pds, pvList);

            // TODO This needs more work.  We need to check inside holders of input params.  We also
            // may want to exclude header params from this check
            //Validate input parameters for operation and make sure no input parameters are null.
            //As per JAXWS Specification section 3.6.2.3 if a null value is passes as an argument 
            //to a method then an implementation MUST throw WebServiceException.
            if (sigArguments != null) {
                for (Object argument : sigArguments) {
                    if (argument == null) {
                        throw ExceptionFactory.makeWebServiceException(Messages.getMessage(
                                "NullParamErr1", "Input", operationDesc.getJavaMethodName(),
                                "doc/lit"));

                    }
                }
            }
            return sigArguments;
        } catch (Exception e) {
            throw ExceptionFactory.makeWebServiceException(e);
        }
    }

    public Message marshalResponse(Object returnObject,
                                   Object[] signatureArgs,
                                   OperationDescription operationDesc, Protocol protocol)
            throws WebServiceException {

        EndpointInterfaceDescription ed = operationDesc.getEndpointInterfaceDescription();
        EndpointDescription endpointDesc = ed.getEndpointDescription();

        // We want to respond with the same protocol as the request,
        // It the protocol is null, then use the Protocol defined by the binding
        if (protocol == null) {
            protocol = Protocol.getProtocolForBinding(endpointDesc.getBindingType());
        }

        // Note all exceptions are caught and rethrown with a WebServiceException
        try {
            // Sample Document message
            // ..
            // <soapenv:body>
            //    <m:return ... >...</m:param>
            // </soapenv:body>
            //
            // Important points.
            //   1) There is no operation element in the message
            //   2) The data blocks are located underneath the operation element. 
            //   3) The name of the data blocks (m:param) are defined by the schema.
            //      (SOAP indicates that the name of the element is not important, but
            //      for document processing, we will assume that the name corresponds to 
            //      a schema root element)
            //   4) The type of the data block is defined by schema; thus in most cases
            //      an xsi:type will not be present

            // Get the operation information
            ParameterDescription[] pds = operationDesc.getParameterDescriptions();
            MarshalServiceRuntimeDescription marshalDesc =
                    MethodMarshallerUtils.getMarshalDesc(endpointDesc);
            TreeSet<String> packages = marshalDesc.getPackages();

            // Create the message 
            MessageFactory mf = (MessageFactory)FactoryRegistry.getFactory(MessageFactory.class);
            Message m = mf.create(protocol);

            // Put the return object onto the message
            Class returnType = operationDesc.getResultActualType();
            if (returnType != void.class) {
                // TODO should we allow null if the return is a header?
                //Validate input parameters for operation and make sure no input parameters are null.
                //As per JAXWS Specification section 3.6.2.3 if a null value is passes as an argument 
                //to a method then an implementation MUST throw WebServiceException.
                if (returnObject == null) {
                    throw ExceptionFactory.makeWebServiceException(Messages.getMessage(
                            "NullParamErr1", "Return", operationDesc.getJavaMethodName(),
                            "doc/lit"));

                }

                // Use byJavaType marshalling if necessary
                Class byJavaType = null;
                if (MethodMarshallerUtils.isJAXBBasicType(returnType)) {
                    byJavaType = returnType;
                }

                Element returnElement = null;
                QName returnQName = new QName(operationDesc.getResultTargetNamespace(),
                                              operationDesc.getResultName());
                if (marshalDesc.getAnnotationDesc(returnType).hasXmlRootElement()) {
                    returnElement = new Element(returnObject, returnQName);
                } else {
                    returnElement = new Element(returnObject, returnQName, returnType);
                }
                MethodMarshallerUtils.toMessage(returnElement, returnType,
                                                marshalDesc, m,
                                                byJavaType,
                                                operationDesc.isResultHeader());
            }

            // Convert the holder objects into a list of JAXB objects for marshalling
            List<PDElement> pdeList = MethodMarshallerUtils.getPDElements(marshalDesc,
                                                                          pds,
                                                                          signatureArgs,
                                                                          false, // output
                                                                          false, false);

            // We want to use "by Java Type" marshalling for 
            // all body elements and all non-JAXB objects
            for (PDElement pde : pdeList) {
                ParameterDescription pd = pde.getParam();
                Class type = pd.getParameterActualType();
                if (MethodMarshallerUtils.isJAXBBasicType(type)) {
                    pde.setByJavaTypeClass(type);
                }
            }

            // Put values onto the message
            MethodMarshallerUtils.toMessage(pdeList, m, packages);

            return m;
        } catch (Exception e) {
            throw ExceptionFactory.makeWebServiceException(e);
        }
    }

    public Message marshalRequest(Object[] signatureArguments, OperationDescription operationDesc)
            throws WebServiceException {

        EndpointInterfaceDescription ed = operationDesc.getEndpointInterfaceDescription();
        EndpointDescription endpointDesc = ed.getEndpointDescription();
        Protocol protocol = Protocol.getProtocolForBinding(endpointDesc.getClientBindingID());

        // Note all exceptions are caught and rethrown with a WebServiceException
        try {
            // Sample Document message
            // ..
            // <soapenv:body>
            //    <m:param .. >...</m:param>
            // </soapenv:body>
            //
            // Important points.
            //   1) There is no operation element under the body.
            //   2) The data blocks are located underneath the body.  
            //   3) The name of the data blocks (m:param) are defined by the schema
            //   4) The type of the data block (data:foo) is defined by schema (and probably
            //      is not present in the message

            // Get the operation information
            ParameterDescription[] pds = operationDesc.getParameterDescriptions();
            MarshalServiceRuntimeDescription marshalDesc =
                    MethodMarshallerUtils.getMarshalDesc(endpointDesc);
            TreeSet<String> packages = marshalDesc.getPackages();

            // TODO This needs more work.  We need to check inside holders of input params.  We also
            // may want to exclude header params from this check
            //Validate input parameters for operation and make sure no input parameters are null.
            //As per JAXWS Specification section 3.6.2.3 if a null value is passes as an argument 
            //to a method then an implementation MUST throw WebServiceException.
            if (pds.length > 0) {
                if (signatureArguments == null) {
                    throw ExceptionFactory.makeWebServiceException(Messages.getMessage(
                            "NullParamErr1", "Input", operationDesc.getJavaMethodName(),
                            "doc/lit"));
                }
                if (signatureArguments != null) {
                    for (Object argument : signatureArguments) {
                        if (argument == null) {
                            throw ExceptionFactory.makeWebServiceException(Messages.getMessage(
                                    "NullParamErr1", "Input", operationDesc.getJavaMethodName(),
                                    "doc/lit"));
                        }
                    }
                }
            }
            // Create the message 
            MessageFactory mf = (MessageFactory)FactoryRegistry.getFactory(MessageFactory.class);
            Message m = mf.create(protocol);

            // The input object represent the signature arguments.
            // Signature arguments are both holders and non-holders
            // Convert the signature into a list of JAXB objects for marshalling
            List<PDElement> pdeList = MethodMarshallerUtils.getPDElements(marshalDesc,
                                                                          pds,
                                                                          signatureArguments,
                                                                          true,  // input
                                                                          false, false);

            // We want to use "by Java Type" marshalling for 
            // all body elements and all non-JAXB objects
            for (PDElement pde : pdeList) {
                ParameterDescription pd = pde.getParam();
                Class type = pd.getParameterActualType();
                if (MethodMarshallerUtils.isJAXBBasicType(type)) {
                    pde.setByJavaTypeClass(type);
                }
            }

            // Put values onto the message...marshalling by type
            MethodMarshallerUtils.toMessage(pdeList, m, packages);

            return m;
        } catch (Exception e) {
            throw ExceptionFactory.makeWebServiceException(e);
        }
    }

    public Message marshalFaultResponse(Throwable throwable, OperationDescription operationDesc,
                                        Protocol protocol) throws WebServiceException {

        EndpointInterfaceDescription ed = operationDesc.getEndpointInterfaceDescription();
        EndpointDescription endpointDesc = ed.getEndpointDescription();
        MarshalServiceRuntimeDescription marshalDesc =
                MethodMarshallerUtils.getMarshalDesc(endpointDesc);

        // We want to respond with the same protocol as the request,
        // It the protocol is null, then use the Protocol defined by the binding
        if (protocol == null) {
            protocol = Protocol.getProtocolForBinding(endpointDesc.getBindingType());
        }

        // Note all exceptions are caught and rethrown with a WebServiceException
        try {
            // Create the message 
            MessageFactory mf = (MessageFactory)FactoryRegistry.getFactory(MessageFactory.class);
            Message m = mf.create(protocol);

            // Put the fault onto the message
            MethodMarshallerUtils.marshalFaultResponse(throwable,
                                                       marshalDesc,
                                                       operationDesc,
                                                       m);
            return m;
        } catch (Exception e) {
            throw ExceptionFactory.makeWebServiceException(e);
        }
    }

    public Throwable demarshalFaultResponse(Message message, OperationDescription operationDesc)
            throws WebServiceException {

        EndpointInterfaceDescription ed = operationDesc.getEndpointInterfaceDescription();
        EndpointDescription endpointDesc = ed.getEndpointDescription();
        MarshalServiceRuntimeDescription marshalDesc =
                MethodMarshallerUtils.getMarshalDesc(endpointDesc);

        // Note all exceptions are caught and rethrown with a WebServiceException
        try {
            Throwable t = MethodMarshallerUtils
                    .demarshalFaultResponse(operationDesc, marshalDesc, message);
            return t;
        } catch (Exception e) {
            throw ExceptionFactory.makeWebServiceException(e);
        }
    }

}
