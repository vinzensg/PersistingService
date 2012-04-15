package resources;

import parser.PayloadParser;

public class StringResource extends TypeResource {

	public StringResource(String resourceIdentifier) {
		super(resourceIdentifier);
	}

	@Override
	public void addSubResource(PayloadParser parsedPayload) {
		addSubResource(new SpecificStringResource(parsedPayload.getValue("resid"), parsedPayload.getValue("deviceuri")));
	}
	
	

}
