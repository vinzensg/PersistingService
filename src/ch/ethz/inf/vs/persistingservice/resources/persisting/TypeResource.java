package ch.ethz.inf.vs.persistingservice.resources.persisting;

import ch.ethz.inf.vs.californium.coap.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.endpoint.LocalResource;

public class TypeResource extends LocalResource {

	private String type;

	public TypeResource(String resourceIdentifier, String type) {
		super(resourceIdentifier);
		this.type = type;
	}
	
	public void performGET(GETRequest request) {
		request.respond(CodeRegistry.RESP_CONTENT, this.type);
	}
}
