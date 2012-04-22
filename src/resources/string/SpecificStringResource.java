package resources.string;

import java.util.List;
import java.util.Timer;

import config.Constants;

import ch.ethz.inf.vs.californium.coap.DELETERequest;
import ch.ethz.inf.vs.californium.coap.MediaTypeRegistry;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.coap.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.endpoint.LocalResource;
import database.DatabaseConnection;
import database.type.string.StringType;
import database.type.string.StringTypeRepository;

public class SpecificStringResource extends LocalResource {
	
	private String resourceIdentifier;
	
	private StringTypeRepository stringTypeRepository;
	
	private String deviceROOT;
	private String deviceURI;
	private String device;
	
	private Timer timer;

	public SpecificStringResource(String resourceIdentifier, String deviceROOT, String deviceURI) {
		super(resourceIdentifier);
		setTitle("Resource to sign up for observing a String value");
		setResourceType("stringtype");
		this.resourceIdentifier = resourceIdentifier;
		stringTypeRepository = new StringTypeRepository(StringType.class, DatabaseConnection.getCouchDbConnector(), deviceURI);

		this.deviceROOT = deviceROOT;
		this.deviceURI = deviceURI;
		this.device = deviceROOT + deviceURI;
	}
	
	public String getDevice() {
		return this.device;
	}
	
	public void performGET(GETRequest request) {
		String response = "GET request received.\n";
		List<StringType> result = stringTypeRepository.queryDevice();
		System.out.println("Result: " + result.get(0));
		System.out.println("Size of Result: " + result.size());
		for (StringType element : result) {
			response += element.getDevice() + " ";
			response += element.getStringValue() + "\n";
			response += element.getDateTime() + "\n";
		}
		request.respond(CodeRegistry.RESP_CONTENT, response);
	}
	
	public void performDELETE(DELETERequest request) {
		timer.cancel();
		timer.purge();
		String responseString = "The Resource " + resourceIdentifier + " has been removed";
		Response response = new Response(CodeRegistry.RESP_CONTENT);
		response.setPayload(responseString);
		response.setContentType(MediaTypeRegistry.TEXT_PLAIN);
		request.respond(response);
		this.remove();
	}
}
