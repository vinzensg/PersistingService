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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.ethz.inf.vs.californium.coap.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.OptionNumberRegistry;
import ch.ethz.inf.vs.californium.coap.POSTRequest;
import ch.ethz.inf.vs.californium.endpoint.LocalResource;
import ch.ethz.inf.vs.californium.endpoint.Resource;
import ch.ethz.inf.vs.persistingservice.parser.PayloadParser;
import ch.ethz.inf.vs.persistingservice.resources.persisting.PersistingResource;

/**
 * The Class PersistingResource is the top most resource of the persisting
 * resource. It adds the type resources.
 */
public class PersistingServiceResource extends LocalResource {
	
	Map<String, Resource> topResources = new HashMap<String, Resource>();
	

	/**
	 * Instantiates a new persisting resource and adds the type resources.
	 * <p>
	 * The type resources are:<br>
	 * string<br>
	 * number<br>
	 * 
	 * @param resourceIdentifier
	 *            the resource identifier
	 */
	public PersistingServiceResource(String resourceIdentifier) {
		super(resourceIdentifier);

	}
	
	public void cleanUp(String resName) {
		if (topResources.containsKey(resName)) {
			Resource topRes = topResources.get(resName);
			if (topRes.subResourceCount() == 0) {
				topResources.remove(resName);
				topRes.remove();
			}
		}
	}
	
	/*
	 * (topid)
	 * resid
	 * deviceroot
	 * deviceres
	 * options
	 * type
	 */
	public void performPOST(POSTRequest request) {
		String payload = request.getPayloadString();
		PayloadParser parsedPayload = new PayloadParser(payload);
		if (parsedPayload.containsLabels("resid", "deviceroot", "deviceres", "type")) {
			String resid = parsedPayload.getStringValue("resid");
			String type = parsedPayload.getStringValue("type");
			String deviceroot = parsedPayload.getStringValue("deviceroot");
			String deviceres = parsedPayload.getStringValue("deviceres");
			
			List<Option> options = null;
			if (parsedPayload.containsLabel("options")) {
				options = new ArrayList<Option>();
				String[] opts = (parsedPayload.getStringValue("options")).split("&");
				for (String opt : opts) {
					options.add(new Option(opt, OptionNumberRegistry.URI_QUERY));
				}
			}
		
	
			if (parsedPayload.containsLabel("topid")) {
				Resource topRes = null;
				String topid = parsedPayload.getStringValue("topid");
				if (topResources.containsKey(topid)) {
					topRes = topResources.get(topid);
				} else {
					topRes = new TopResource(topid);
					addSubResource(topRes);
					topResources.put(topid, topRes);
				}
				topRes.addSubResource(new PersistingResource(resid, type, deviceroot, deviceres, options, this, topid));
			} else {
				addSubResource(new PersistingResource(resid, type, deviceroot, deviceres, options, this, ""));
			}
			request.respond(CodeRegistry.RESP_CREATED);
		} else {
			request.respond(CodeRegistry.RESP_BAD_REQUEST, "Provide: \n" +
														   "(topid = ...)\n" +
														   "resid = ...\n" +
														   "deviceroot = ...\n" +
														   "deviceres = ...\n" +
														   "(options = ...)\n" +
														   "type = number | string");
		}
	}
}
