package resources.number;

import parser.PayloadParser;
import resources.TypeResource;

public class NumberResource extends TypeResource {
	
	public NumberResource(String resourceIdentifier) {
		super(resourceIdentifier);
	}

	@Override
	public void addSubResource(PayloadParser parsedPayload) {
		if (parsedPayload.containsLabels(new String[]{"push", "pushtarget", "datainterval"}) && parsedPayload.isBoolean("push") && parsedPayload.isInteger("datainterval")) {
			System.out.println("MODE 1");
			addSubResource(new SpecificNumberResource(parsedPayload.getStringValue("resid"), parsedPayload.getStringValue("deviceuri"), parsedPayload.getBooleanValue("push"), parsedPayload.getStringValue("pushtarget"), parsedPayload.getIntValue("datainterval")));
		} else if (parsedPayload.containsLabels(new String[]{"push", "pushtarget"}) && parsedPayload.isBoolean("push")) {
			System.out.println("MODE 2");
			addSubResource(new SpecificNumberResource(parsedPayload.getStringValue("resid"), parsedPayload.getStringValue("deviceuri"), parsedPayload.getBooleanValue("push"), parsedPayload.getStringValue("pushtarget"), 0));
		} else if (parsedPayload.containsLabel("datainterval") && parsedPayload.isInteger("datainterval")) {
			System.out.println("MODE 3");
			addSubResource(new SpecificNumberResource(parsedPayload.getStringValue("resid"), parsedPayload.getStringValue("deviceuri"), false, null, parsedPayload.getIntValue("datainterval")));
		} else {
			System.out.println("MODE 4");
			addSubResource(new SpecificNumberResource(parsedPayload.getStringValue("resid"), parsedPayload.getStringValue("deviceuri"), false, null, 0));
		}
	}
}
