package resources;

import resources.number.NumberResource;
import resources.string.StringResource;
import ch.ethz.inf.vs.californium.endpoint.LocalResource;

public class PersistingResource extends LocalResource {

	public PersistingResource(String resourceIdentifier) {
		super(resourceIdentifier);

		addSubResource(new StringResource("string"));
		addSubResource(new NumberResource("number"));
	}
	


}
