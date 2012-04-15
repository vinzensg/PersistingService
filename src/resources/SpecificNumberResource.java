package resources;

import ch.ethz.inf.vs.californium.endpoint.LocalResource;

public class SpecificNumberResource extends LocalResource {
	
	public SpecificNumberResource(String resourceIdentifier, String deviceURI) {
		super(resourceIdentifier);
		setTitle("Resource to sign up for observing a Number value");
		setResourceType("numbertype");
	}

}
