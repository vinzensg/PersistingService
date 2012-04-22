package resources;

import java.util.Set;

import ch.ethz.inf.vs.californium.endpoint.Resource;

import parser.PayloadParser;

public class NumberResource extends TypeResource {
	
	public NumberResource(String resourceIdentifier) {
		super(resourceIdentifier);
	}
	
	@Override
	protected boolean checkIfAlreadyExists(PayloadParser parsedPayload) {
		String newDevice = parsedPayload.getStringValue("deviceroot") + parsedPayload.getStringValue("deviceres");
		
		Set<Resource> subres = this.getSubResources();
		for (Resource res : subres) {
			String device = ((SpecificNumberResource) res).getDevice();
			if (device.equals(newDevice)) {
				return true;
			}
		}
		
		return false;
	}

	@Override
	protected void addSubResource(PayloadParser parsedPayload) {
		addSubResource(new SpecificNumberResource(parsedPayload.getStringValue("resid"), parsedPayload.getStringValue("deviceroot"), parsedPayload.getStringValue("deviceres")));
	}
}
