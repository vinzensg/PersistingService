package resources;

import java.util.Set;

import ch.ethz.inf.vs.californium.endpoint.Resource;
import parser.PayloadParser;

public class StringResource extends TypeResource {

	public StringResource(String resourceIdentifier) {
		super(resourceIdentifier);
	}
	
	@Override
	protected boolean checkIfAlreadyExists(PayloadParser parsedPayload) {
		String newDevice = parsedPayload.getStringValue("deviceroot") + parsedPayload.getStringValue("deviceres");
		
		Set<Resource> subres = this.getSubResources();
		for (Resource res : subres) {
			String device = ((SpecificStringResource) res).getDevice();
			if (device.equals(newDevice)) {
				return true;
			}
		}
		
		return false;
	}

	@Override
	protected void addSubResource(PayloadParser parsedPayload) {
		addSubResource(new SpecificStringResource(parsedPayload.getStringValue("resid"), parsedPayload.getStringValue("deviceroot"), parsedPayload.getStringValue("deviceres")));
	}
	
}
