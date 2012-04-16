package resources.string;

import parser.PayloadParser;
import resources.TypeResource;

public class StringResource extends TypeResource {

	public StringResource(String resourceIdentifier) {
		super(resourceIdentifier);
	}

	@Override
	public void addSubResource(PayloadParser parsedPayload) {
		if (parsedPayload.containsLabels(new String[]{"push", "pushtarget", "datainterval"}) && parsedPayload.isBoolean("push") && parsedPayload.isInteger("datainterval")) {
			addSubResource(new SpecificStringResource(parsedPayload.getStringValue("resid"), parsedPayload.getStringValue("deviceuri"), parsedPayload.getBooleanValue("push"), parsedPayload.getStringValue("pushtarget"), parsedPayload.getIntValue("datainterval")));
		} else if (parsedPayload.containsLabels(new String[]{"push", "pushtarget"}) && parsedPayload.isBoolean("push")) {
			addSubResource(new SpecificStringResource(parsedPayload.getStringValue("resid"), parsedPayload.getStringValue("deviceuri"), parsedPayload.getBooleanValue("push"), parsedPayload.getStringValue("pushtarget"), 0));
		} else if (parsedPayload.containsLabel("datainterval") && parsedPayload.isInteger("datainterval")) {
			addSubResource(new SpecificStringResource(parsedPayload.getStringValue("resid"), parsedPayload.getStringValue("deviceuri"), false, null, parsedPayload.getIntValue("datainterval")));
		} else {
			addSubResource(new SpecificStringResource(parsedPayload.getStringValue("resid"), parsedPayload.getStringValue("deviceuri"), false, null, 0));
		}	}
	
}
