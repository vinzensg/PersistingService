/*******************************************************************************
 * Copyright (c) 2012, Institute for Pervasive Computing, ETH Zurich.
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the Institute nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE INSTITUTE AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE INSTITUTE OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * 
 * This file is part of the Californium (Cf) CoAP framework.
 ******************************************************************************/
package ch.ethz.inf.vs.persistingservice.resources;

import java.util.Set;

import ch.ethz.inf.vs.californium.coap.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.coap.POSTRequest;
import ch.ethz.inf.vs.californium.endpoint.LocalResource;
import ch.ethz.inf.vs.californium.endpoint.Resource;
import ch.ethz.inf.vs.persistingservice.parser.PayloadParser;


/**
 * The Class NumberResource implements the top resource to register a persisting
 * service for a device resource returning number values.
 */
public class NumberResource extends LocalResource {
	
	/**
	 * Instantiates a new number resource and sets the resource identifier.
	 * 
	 * @param resourceIdentifier
	 *            the resource identifier
	 */
	public NumberResource(String resourceIdentifier) {
		super(resourceIdentifier);
	}
	
	/**
	 * perform GET acts on get requests and responds with a list of all
	 * subresources with the resource identifier and the corresponding device.
	 * <p>
	 * Responose format:<br>
	 * resid = RESOURCE_IDENTIFIER; device = DEVICE_PATH<br>
	 */
	public void performGET(GETRequest request) {
		System.out.println("GET NUMBER SUBRESOURCES: list the subresources of resource 'number'");
		request.prettyPrint();
		
		String ret = "";
		Set<Resource> subResources = getSubResources();
		for (Resource res : subResources) {
			ret += "resid = " + res.getName();
			if (res instanceof SpecificNumberResource)
				ret += "; device = " + ((SpecificNumberResource) res).getDevice() + "\n";
		}

		request.respond(CodeRegistry.RESP_CONTENT, ret);
	}
	
	/**
	 * perform POST acts on post requests and creates a new subresource if the
	 * payload was specified correctly. Otherwise it responds with an error
	 * message.
	 * <p>
	 * Payload format:<br>
	 * resid = RESOURCE_IDENTIFIER<br>
	 * deviceroot = ROOT_PATH<br>
	 * deviceres = /RES_PATH<br>
	 */
	public void performPOST(POSTRequest request) {
		System.out.println("POST CREATE NUMBER RESOURCE: create new resource to observe device");
		request.prettyPrint();
		
		String ret = "";
		String payload = request.getPayloadString();
		PayloadParser parsedPayload = new PayloadParser(payload);
		if (parsedPayload.containsExactLabels(new String[]{"resid", "deviceroot", "deviceres"}) ) {
			if (!checkIfDeviceAlreadyExists(parsedPayload)) {
				if (!checkIfResidAlreadyExists(parsedPayload)) {
					addSubResource(new SpecificNumberResource(parsedPayload.getStringValue("resid"), parsedPayload.getStringValue("deviceroot"), parsedPayload.getStringValue("deviceres")));
					ret = "A new subresource was created: " + parsedPayload.getStringValue("resid") + "\n" +
						  "For the devide of address: " + parsedPayload.getStringValue("deviceroot") + parsedPayload.getStringValue("deviceres");
				} else {
					ret = "Resid already exists for another device. Provide a different resid.";
				}
			} else {
				ret = "Subresource already existing.";
			}
		} else { 
			ret = "Creating a new subresource did not work.";
		}
		request.respond(CodeRegistry.RESP_CONTENT, ret);
	}
	
	/**
	 * Check if a subresource for the specified device already exists.
	 * 
	 * @param parsedPayload
	 *            the parsed payload
	 * @return true, if the specified device already exists.
	 */
	private boolean checkIfDeviceAlreadyExists(PayloadParser parsedPayload) {
		String newDevice = parsedPayload.getStringValue("deviceroot") + parsedPayload.getStringValue("deviceres");
		
		Set<Resource> subres = this.getSubResources();
		for (Resource res : subres) {
			String device = ((SpecificNumberResource) res).getDevice();
			if (device.equals(newDevice))
				return true;
		}
		
		return false;
	}
	
	/**
	 * Check if a subresource with the specified resource identifier already
	 * exists.
	 * 
	 * @param parsedPayload
	 *            the parsed payload
	 * @return true, if the specified resource identifier already exists
	 */
	private boolean checkIfResidAlreadyExists(PayloadParser parsedPayload) {
		String newResid = parsedPayload.getStringValue("resid");
		
		Set<Resource> subres = this.getSubResources();
		for (Resource res : subres) {
			String resid = res.getName();
			if (resid.equals(newResid))
				return true;
		}
		return false;
	}
}
