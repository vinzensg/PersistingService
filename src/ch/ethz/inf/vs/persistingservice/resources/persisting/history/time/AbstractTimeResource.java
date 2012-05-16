package ch.ethz.inf.vs.persistingservice.resources.persisting.history.time;

import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.endpoint.LocalResource;

public abstract class AbstractTimeResource extends LocalResource {
	
	public AbstractTimeResource(String resourceIdentifier) {
		super(resourceIdentifier);
	}

	public abstract void acceptGetRequest(GETRequest request, AbstractQuery query);

}
