package ch.ethz.inf.vs.persistingservice.resources;

import java.util.Set;


import ch.ethz.inf.vs.californium.coap.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.coap.MediaTypeRegistry;
import ch.ethz.inf.vs.californium.coap.POSTRequest;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.endpoint.LocalResource;
import ch.ethz.inf.vs.californium.endpoint.Resource;
import ch.ethz.inf.vs.persistingservice.parser.PayloadParser;

public abstract class TypeResource extends LocalResource {
	
	public TypeResource(String resourceIdentifier) {
		super(resourceIdentifier);
	}
	
	public void performGET(GETRequest request) {
		String responseString = "The subresources are:\n" +
								"=====================\n\n";
		Response response = new Response(CodeRegistry.RESP_CONTENT);
		Set<Resource> subResources = getSubResources();
		for (Resource res : subResources) {
			responseString += res.getName() + "\n";
		}
		response.setPayload(responseString);
		response.setContentType(MediaTypeRegistry.TEXT_PLAIN);
		request.respond(response);
	}
	
	public void performPOST(POSTRequest request) {
		request.prettyPrint();
		String responseString = null;
		String payload = request.getPayloadString();
		PayloadParser parsedPayload = new PayloadParser(payload);
		if (parsedPayload.containsExactLabels(new String[]{"resid", "deviceroot", "deviceres"}) ) {
			if (!checkIfAlreadyExists(parsedPayload)) {
				addSubResource(parsedPayload);
				responseString = "A new subresource was created: " + parsedPayload.getStringValue("resid") + "\n" +
								 "For the devide of address: " + parsedPayload.getStringValue("deviceroot") + parsedPayload.getStringValue("deviceres");
			} else {
				responseString = "Subresource already existed.";
			}
		} else {
			responseString = "Creating a new subresource did not work.";
		}
		Response response = new Response(CodeRegistry.RESP_CONTENT);
		response.setPayload(responseString);
		response.setContentType(MediaTypeRegistry.TEXT_PLAIN);
		request.respond(response);
	}
	
	protected abstract boolean checkIfAlreadyExists(PayloadParser parsedPayload);
	
	protected abstract void addSubResource(PayloadParser parsedPayload);
}
