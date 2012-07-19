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
package ch.ethz.inf.vs.persistingservice.resources.persisting;

import java.io.IOException;

import ch.ethz.inf.vs.californium.coap.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.endpoint.LocalResource;

/**
 * The Class DeviceInfoResource is used to retrieve information about the device, by accessing its well known core resource.
 */
public class DeviceInfoResource extends LocalResource {

	/** The device. */
	private String device;
	
	/** The deviceROOT. */
	private String deviceROOT;

	/**
	 * Instantiates a new device info resource.
	 *
	 * @param resourceIdentifier the resource identifier
	 * @param device the device
	 */
	public DeviceInfoResource(String resourceIdentifier, String device, String deviceROOT) {
		super(resourceIdentifier);
		this.device = device;
		this.deviceROOT = deviceROOT;
	}
	
	/**
	 * performGET responds with the device info requested from the well-known core resource of the device.
	 */
	public void performGET(GETRequest request) {
		System.out.println("GET DEVICE INFO: get the info (well-known core) of device " + device);
		request.prettyPrint();
		
		String ret = "";
		
		Request req = new GETRequest();
		req.setURI(deviceROOT+"/.well-known/core");
		req.enableResponseQueue(true);
		
		try {
			req.execute();
		} catch (IOException e) {
			System.err.println("Exception : " + e.getMessage());
		}
		
		try {
			Response resp = req.receiveResponse();
			if (resp!= null) ret = resp.getPayloadString();
		} catch (InterruptedException e) {
			System.err.println("Exception : " + e.getMessage());
		}
		
		request.respond(CodeRegistry.RESP_CONTENT, ret);
	}

}
