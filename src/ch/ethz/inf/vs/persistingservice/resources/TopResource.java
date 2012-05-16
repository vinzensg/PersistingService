package ch.ethz.inf.vs.persistingservice.resources;

import java.util.Set;

import ch.ethz.inf.vs.californium.coap.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.endpoint.LocalResource;
import ch.ethz.inf.vs.californium.endpoint.Resource;

public class TopResource extends LocalResource{
	
	public TopResource(String resourceIdentifier) {
		super(resourceIdentifier);	
	}
	
	public void performGET(GETRequest request) {
		String ret = "";
		Set<Resource> subResources = getSubResources();
		
		for (Resource res : subResources) {
			ret += res.getPath() + "\n";
		}
		request.respond(CodeRegistry.RESP_CONTENT, ret);
	}
	
}
