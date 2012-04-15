package resources;

import parser.PayloadParser;

public class NumberResource extends TypeResource {
	
	public NumberResource(String resourceIdentifier) {
		super(resourceIdentifier);
	}

	@Override
	public void addSubResource(PayloadParser parsedPayload) {
		addSubResource(new SpecificNumberResource(parsedPayload.getValue("resid"), parsedPayload.getValue("deviceuri")));
	}

}
